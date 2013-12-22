package com.dreweaster.thespian

import com.typesafe.config.Config
import akka.actor._
import com.dreweaster.thespian.domain.{Command, AggregateFactory, Event, DomainModel}
import com.dreweaster.thespian.example.model.command.Customer
import java.util.UUID
import scala.concurrent.duration.Duration

trait DomainDriven {
  val system: ActorSystem
  lazy val domainModel = Thespian(system).domainModel

  implicit def anyToCommand(data: Any) = Command(data = data)

  def aggregateRootOf(factory: AggregateFactory, id: UUID) = {
    domainModel.aggregateRootOf(factory, id)
  }

  def processManagerOf = ???

  def subscribe(factory: AggregateFactory, subscriberProps: Props) = {
    domainModel.subscribe(factory, subscriberProps)
  }

  def publish(event: Event, factory: AggregateFactory) = ???

  def schedule(message:Any, id:UUID, processManager: AggregateFactory, duration:Duration) = ???
}

trait DomainDrivenActor extends DomainDriven {
  self: Actor =>
  val system = context.system
}

class ThespianImpl(config: Config, system: ActorSystem) extends Extension {
  // TODO: this needs to be loaded from DSL style config builder (currently badly hardcoded!)
  val domainModel = DomainModel(system) {
    Customer
  }
}

object Thespian extends ExtensionId[ThespianImpl] with ExtensionIdProvider {

  override def lookup = Thespian

  override def createExtension(system: ExtendedActorSystem) = new ThespianImpl(system.settings.config, system)

  override def get(system: ActorSystem): ThespianImpl = super.get(system)
}