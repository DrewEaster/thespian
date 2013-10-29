package com.dreweaster.akka3d.domain

import akka.persistence.{SnapshotOffer, EventsourcedProcessor}
import akka.actor.{Actor, Props, ActorRef, ActorSystem}
import java.util.UUID

case object SaveSnapshot

case class Event(id: UUID, sequenceNumber: Long, data: Any)

trait AggregateRootState {
  def update: Any => AggregateRootState
}

trait AggregateRoot[S <: AggregateRootState] extends EventsourcedProcessor {
  val uuid = UUID.fromString(context.self.path.name)

  var state: S

  def updateState(event: Any) {
    state = state.update(event).asInstanceOf[S]
  }

  val receiveCommand = {
    case SaveSnapshot => saveSnapshot(state)
    case _ => handleCommand
  }

  val receiveReplay: Receive = {
    case SnapshotOffer(metadata, snapshot: S) => state = snapshot
    case event => updateState(event)
  }

  def unitOfWork(events: Any*) = {
    for (e <- events) persist(e)(eventHandler)
  }

  def handleCommand: Receive

  private def eventHandler(event: Any) = {
    updateState(event)
    context.system.eventStream.publish(Event(uuid, currentPersistentMessage.get.sequenceNr, event))
  }
}

trait AggregateRootType {
  val typeInfo: Class[_ <: AggregateRoot]
}

object DomainModel {
  def apply(name: String)(roots: AggregateRootType*) = {
    new DomainModel(name, roots)
  }
}

class DomainModel(name: String, types: Seq[AggregateRootType]) {
  val system = ActorSystem(name)

  val typeRegistry = types.foldLeft(Map[Class[_ <: AggregateRoot], ActorRef]()) {
    (acc, at) => acc + (at.typeInfo -> system.actorOf(Props(classOf[AggregateRootCache], at), at.typeInfo.getName))
  }

  def aggregateRootOf(aggregateRootType: AggregateRootType, id: UUID) = {
    if (typeRegistry.contains(aggregateRootType.typeInfo)) {
      AggregateRootRef(id, typeRegistry(aggregateRootType.typeInfo))
    } else {
      throw new IllegalArgumentException("The aggregate type is not supported by this domain model!")
    }
  }

  def shutdown() {
    system.shutdown()
  }
}

case class CommandWrapper(id: UUID, command: Any, sender: ActorRef)

case class AggregateRootRef(id: UUID, cache: ActorRef) {
  def tell(message: Any)(implicit sender: ActorRef = null) {
    cache ! CommandWrapper(id, message, sender)
  }

  def !(message: Any)(implicit sender: ActorRef = null) {
    cache ! CommandWrapper(id, message, sender)
  }
}

class AggregateRootCache(aggregateRootType: AggregateRootType) extends Actor {
  def receive = {
    case message: CommandWrapper =>
      val aggregate = context.child(message.id.toString).getOrElse {
        context.actorOf(Props(aggregateRootType.typeInfo), message.id.toString)
      }
      aggregate.tell(message.command, message.sender)
  }
}