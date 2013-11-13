package com.dreweaster.thespian

import com.typesafe.config.Config
import akka.actor._
import com.dreweaster.thespian.domain.{Event, AggregateRootType, DomainModel}
import com.dreweaster.thespian.example.model.command.Customer
import java.util.UUID
import scala.concurrent.duration.Duration

trait DomainDriven {
  val system: ActorSystem
  lazy val domainModel = Thespian(system).domainModel

  def aggregateRootOf(aggregateRootType: AggregateRootType, id: UUID) = {
    domainModel.aggregateRootOf(aggregateRootType, id)
  }

  def processManagerOf = ???

  def subscribe(aggregateRootType: AggregateRootType, subscriberProps: Props) = {
    domainModel.subscribe(aggregateRootType, subscriberProps)
  }

  def publish(event: Event, aggregateRootType: AggregateRootType) = ???

  def schedule(message:Any, id:UUID, processManager: AggregateRootType, duration:Duration) = ???
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