package com.dreweaster.thespian.domain

import akka.persistence.EventsourcedProcessor
import akka.actor._
import java.util.UUID
import akka.contrib.pattern.{DistributedPubSubExtension, DistributedPubSubMediator}
import scala.concurrent.duration._
import akka.contrib.pattern.DistributedPubSubMediator.Publish
import akka.persistence.SnapshotOffer
import akka.pattern.ask

import scala.collection.mutable.ListBuffer

trait State {
  type EventHandler = PartialFunction[Any, State]

  def applyEvent: EventHandler
}

trait AggregateContext {
  val id: UUID
  val version: Long

  def publish(event: Any): Unit
}

case class ListBufferAggregateContext(id: UUID, version: Long) extends AggregateContext {
  private val eventBuffer = new ListBuffer[Any]()

  def publish(event: Any) {
    eventBuffer.append(event)
  }

  def events = eventBuffer.toList
}

object Aggregate {
  val threadLocalContext = new ThreadLocal[AggregateContext]
}

trait Aggregate[T <: State] {

  final def context: AggregateContext = Aggregate.threadLocalContext.get

  final def publish(event: Any) = context.publish(event)

  private var state: T = initialState

  final def saveSnapshot: Any = state

  final def restoreFromSnapshot(snapshot: Any) = state = snapshot.asInstanceOf[T]

  def initialState: T

  final def currentState = state

  type CommandHandler = Any => Unit

  def handleCommand: CommandHandler

  final def applyEvent(event: Any) = state = state.applyEvent(event).asInstanceOf[T]

  final def applyEvents(events: Seq[Any]) = state = events.foldLeft(state)((acc, event) => {
    acc.applyEvent(event).asInstanceOf[T]
  })
}

trait AggregateFactory {
  val aggregateType: String

  def newInstance: Aggregate[_]
}

case object SaveSnapshot

case class Event(id: UUID, correlationId: UUID, sequenceNumber: Long, data: Any)

case class Command(correlationId: UUID = UUID.randomUUID(), data: Any)

case class CommandAck(command: Command)

case class CommandNack(command: Command, exception: Throwable)

case object Idling

case object Shutdown

class AggregateRoot(val aggregate: Aggregate[_], val topic: String) extends EventsourcedProcessor {

  val mediator = DistributedPubSubExtension(context.system).mediator

  val uuid = UUID.fromString(context.self.path.name)

  var lastCommand: Option[Command] = None

  context.setReceiveTimeout(5 minutes)

  val receiveCommand: Receive = {
    case SaveSnapshot => saveSnapshot(aggregate.saveSnapshot)
    case ReceiveTimeout => context.parent ! Idling
    case command@Command(_, data) => {
      lastCommand = Some(command)
      val context = new ListBufferAggregateContext(uuid, lastSequenceNr)
      var wasError = false
      Aggregate.threadLocalContext.set(context)
      try {
        aggregate.handleCommand(data)
        // TODO: sender ! CommandAck(command)
      } catch {
        case ex: Throwable => wasError = true // TODO: CommandNack(command, ex)
      }
      finally {
        Aggregate.threadLocalContext.remove()
      }

      if (!wasError) {
        var i = lastSequenceNr
        for (e <- context.events) {
          i = i + 1
          persist(Event(uuid, command.correlationId, i, e))(eventHandler)
        }
      }
    }
  }

  val receiveReplay: Receive = {
    case SnapshotOffer(metadata, snapshot: Any) => aggregate.restoreFromSnapshot(snapshot)
    case event => aggregate.applyEvent(event)
  }

  private def eventHandler(event: Event) = {
    aggregate.applyEvent(event.data)
    mediator ! Publish(topic, event)
  }
}

object DomainModel {
  def apply(system: ActorSystem)(roots: AggregateFactory*) = {
    new DomainModel(system, roots)
  }
}

class DomainModel(system: ActorSystem, types: Seq[AggregateFactory]) {

  val typeRegistry = types.foldLeft(Map[String, ActorRef]()) {
    (acc, factory) => acc + (factory.aggregateType -> system.actorOf(Props(classOf[AggregateRootCache], factory), factory.aggregateType))
  }

  def aggregateRootOf(factory: AggregateFactory, id: UUID) = {
    if (typeRegistry.contains(factory.aggregateType)) {
      AggregateRootRef(id, typeRegistry(factory.aggregateType))
    } else {
      throw new IllegalArgumentException("The aggregate type is not supported by this domain model!")
    }
  }

  def subscribe(factory: AggregateFactory, subscriberProps: Props) = {
    system.actorOf(ReadModelSupervisor.props(factory, subscriberProps))
  }
}

case class CommandWrapper(id: UUID, command: Command, sender: ActorRef)

case class AggregateRootRef(id: UUID, cache: ActorRef) {

  def tell(command: Command)(implicit sender: ActorRef = null) {
    cache ! CommandWrapper(id, command, sender)
  }

  def !(command: Command)(implicit sender: ActorRef = null) {
    cache ! CommandWrapper(id, command, sender)
  }
}

class AggregateRootCache(factory: AggregateFactory) extends Actor {

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
            val aggregate = context.watch(context.actorOf(AggregateRootActor.props(factory.newInstance, factory.aggregateType), childId.toString))
            cmdStash forwardTo aggregate
          }
      }
    }
    case message: CommandWrapper =>
      terminatingChildren.get(message.id) match {
        case Some(cmdStash) => cmdStash stash message
        case None => {
          val aggregate = context.child(message.id.toString).getOrElse {
            context.watch(context.actorOf(AggregateRootActor.props(factory.newInstance, factory.aggregateType), message.id.toString))
          }
          aggregate.tell(message.command, message.sender)
        }
      }
  }
}

object AggregateRootActor {
  def props(aggregate: Aggregate[_], topic: String) =
    Props.create(classOf[AggregateRoot], aggregate, topic)
}

case class CommandStash(commands: List[CommandWrapper] = List[CommandWrapper]()) {
  def isEmpty = commands.isEmpty

  def stash(cmd: CommandWrapper) = CommandStash(cmd :: commands)

  def forwardTo(aggregate: ActorRef) = for (message <- commands) {
    aggregate.tell(message.command, message.sender)
  }
}

object ReadModelSupervisor {
  def props(factory: AggregateFactory, subscriberProps: Props) =
    Props.create(classOf[ReadModelSupervisor], factory, subscriberProps)
}

class ReadModelSupervisor(val factory: AggregateFactory, subscriberProps: Props) extends Actor {

  import DistributedPubSubMediator.{Subscribe, SubscribeAck}

  val mediator = DistributedPubSubExtension(context.system).mediator

  val subscriber = context.actorOf(subscriberProps)

  val topic = factory.aggregateType.replaceAll("\\$", "") // FIXME: Hack here to remove $

  mediator ! Subscribe(topic, self)

  def receive: Receive = {
    case ack@SubscribeAck(Subscribe(`topic`, `self`)) => context become ready
  }

  def ready: Receive = {
    case msg: Any => subscriber forward msg
  }
}