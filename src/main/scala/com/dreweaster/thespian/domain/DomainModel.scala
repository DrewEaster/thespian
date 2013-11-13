package com.dreweaster.thespian.domain

import akka.persistence.EventsourcedProcessor
import akka.actor._
import java.util.UUID
import akka.contrib.pattern.{DistributedPubSubExtension, DistributedPubSubMediator}
import scala.concurrent.duration._
import akka.contrib.pattern.DistributedPubSubMediator.Publish
import akka.persistence.SnapshotOffer

case object SaveSnapshot

case class Event(id: UUID, sequenceNumber: Long, data: Any)

case object Idling

case object Shutdown

trait AggregateRoot extends EventsourcedProcessor {

  val mediator = DistributedPubSubExtension(context.system).mediator

  val uuid = UUID.fromString(context.self.path.name)

  val topic = getClass.getName

  context.setReceiveTimeout(5 minutes)

  def fetchState: Any

  def applyState: Any => Unit

  def loadState: Any => Unit

  val receiveCommand: Receive = {
    case SaveSnapshot => saveSnapshot(fetchState)
    case ReceiveTimeout => context.parent ! Idling
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

  /**
   * Invoked after successful persistence of an event and after internal state has been updated. This allows
   * implementors to carry out post-persistence actions if they so wish - e.g. sending email to an infrastructure
   * service.
   */
  def handleEvent: Receive

  private def eventHandler(event: Any) = {
    applyState(event)
    handleEvent(event)
    mediator ! Publish(topic, Event(uuid, currentPersistentMessage.get.sequenceNr, event))
  }
}

trait AggregateRootType {
  val typeInfo: Class[_ <: AggregateRoot]
}

object DomainModel {
  def apply(system: ActorSystem)(roots: AggregateRootType*) = {
    new DomainModel(system, roots)
  }
}

class DomainModel(system: ActorSystem, types: Seq[AggregateRootType]) {

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

  // TODO: How can we make this persistent?
  val terminatingChildren = scala.collection.mutable.Map[UUID, CommandStash]()

  def receive = {
    case Idling => {
      // TODO: What if, for some reason, stash already exists for this aggregate?
      val childId = UUID.fromString(sender.path.name)
      terminatingChildren + (childId -> CommandStash())
      sender ! PoisonPill
    }
    case Terminated(child) => {
      val childId = UUID.fromString(child.path.name)
      terminatingChildren.remove(childId).map {
        cmdStash =>
          if (!cmdStash.isEmpty) {
            val aggregate = context.watch(context.actorOf(Props(aggregateRootType.typeInfo), childId.toString))
            cmdStash forwardTo aggregate
          }
      }
    }
    case message: CommandWrapper =>
      terminatingChildren.get(message.id) match {
        case Some(cmdStash) => cmdStash stash message
        case None => {
          val aggregate = context.child(message.id.toString).getOrElse {
            context.watch(context.actorOf(Props(aggregateRootType.typeInfo), message.id.toString))
          }
          aggregate.tell(message.command, message.sender)
        }
      }
  }
}

case class CommandStash(commands: List[CommandWrapper] = List[CommandWrapper]()) {
  def isEmpty = commands.isEmpty

  def stash(cmd: CommandWrapper) = CommandStash(cmd :: commands)

  def forwardTo(aggregate: ActorRef) = for (message <- commands) {
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

  val topic = aggregateRootType.getClass.getName.replaceAll("\\$", "") // FIXME: Hack here to remove $

  mediator ! Subscribe(topic, self)

  def receive: Receive = {
    case ack@SubscribeAck(Subscribe(`topic`, `self`)) => context become ready
  }

  def ready: Receive = {
    case msg: Any => subscriber forward msg
  }
}