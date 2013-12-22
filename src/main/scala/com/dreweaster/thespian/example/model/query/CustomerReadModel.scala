package com.dreweaster.thespian.example.model.query

import java.util.UUID
import akka.actor.{Props, Actor}
import com.dreweaster.thespian.domain.Event
import com.dreweaster.thespian.example.model.command.CustomerEvents.{CustomerAgeChanged, CustomerNameChanged, CustomerCreated}

case class CustomerDTO(id: UUID, seqNum: Long, name: String, age: Int)

case class GetCustomer(id: UUID)

object CustomerReadModel {
  val props = Props[CustomerReadModel]
}

class CustomerReadModel extends Actor {

  val model = scala.collection.mutable.Map[UUID, CustomerDTO]()

  def receive = {
    case GetCustomer(id) => sender ! model.get(id)
    case e@Event(_, _, _, CustomerCreated(name, age)) => model.put(e.id, CustomerDTO(e.id, e.sequenceNumber, name, age))
    case e@Event(_, _, _, CustomerNameChanged(newName)) => {
      val customer = model(e.id)
      if (customer != null) model.put(e.id, customer.copy(name = newName, seqNum = e.sequenceNumber))
    }
    case e@Event(_, _, _, CustomerAgeChanged(newAge)) => {
      val customer = model(e.id)
      if (customer != null) model.put(e.id, customer.copy(age = newAge, seqNum = e.sequenceNumber))
    }
  }
}