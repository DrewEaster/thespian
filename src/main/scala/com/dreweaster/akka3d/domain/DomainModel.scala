package com.dreweaster.akka3d.domain

import akka.persistence.{SnapshotOffer, EventsourcedProcessor}
import akka.actor.{Actor, Props, ActorRef, ActorSystem}
import java.util.UUID
import akka.contrib.pattern.{DistributedPubSubExtension, DistributedPubSubMediator}
import akka.contrib.pattern.DistributedPubSubMediator.Publish

case object SaveSnapshot

case class Event(id: UUID, sequenceNumber: Long, data: Any)

trait AggregateRoot extends EventsourcedProcessor {

  val mediator = DistributedPubSubExtension(context.system).mediator

  val uuid = UUID.fromString(context.self.path.name)

  val topic = getClass.getName

  def fetchState: Any

  def applyState: Any => Unit

  def loadState: Any => Unit

  val receiveCommand: Receive = {
    case SaveSnapshot => saveSnapshot(fetchState)
    case msg => handleCommand(msg)
  }

  val receiveReplay: Receive = {
    case SnapshotOffer(metadata, snapshot: Any) => loadState(snapshot)
    case event => applyState(event)
  }

  def unitOfWork(events: Any*) = {
    for (e <- events) persist(e)(eventHandler)
  }

  def handleCommand: Receive

  private def eventHandler(event: Any) = {
    applyState(event)
    mediator ! Publish(topic, Event(uuid, currentPersistentMessage.get.sequenceNr, event))
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

  def subscribe(aggregateRootType: AggregateRootType, subscriberProps: Props) = {
    system.actorOf(ReadModelSupervisor.props(aggregateRootType, subscriberProps))
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

object ReadModelSupervisor {
  def props(aggregateRootType: AggregateRootType, subscriberProps: Props) =
    Props.create(classOf[ReadModelSupervisor], aggregateRootType, subscriberProps)
}

class ReadModelSupervisor(val aggregateRootType: AggregateRootType, subscriberProps: Props) extends Actor {

  import DistributedPubSubMediator.{Subscribe, SubscribeAck}

  val mediator = DistributedPubSubExtension(context.system).mediator

  val subscriber = context.actorOf(subscriberProps)

  val topic = aggregateRootType.getClass.getName.replaceAll("\\$", "")  // FIXME: Hack here to remove $

  mediator ! Subscribe(topic, self)

  def receive: Receive = {
    case ack@SubscribeAck(Subscribe(`topic`, `self`)) => context become ready
  }

  def ready: Receive = {
    case msg: Any => subscriber forward msg
  }
}