package com.dreweaster.akka3d.example.model.query

import java.util.UUID
import akka.actor.{Props, Actor}
import com.dreweaster.akka3d.domain.Event
import com.dreweaster.akka3d.example.model.command.CustomerEvents.{CustomerAgeChanged, CustomerNameChanged, CustomerCreated}

case class CustomerDTO(id: UUID, seqNum: Long, name: String, age: Int)

case class GetCustomer(id: UUID)

object CustomerReadModel {
  val props = Props[CustomerReadModel]
}

class CustomerReadModel extends Actor {

  val model = scala.collection.mutable.Map[UUID, CustomerDTO]()

  def receive = {
    case GetCustomer(id) => sender ! model.get(id)
    case Event(id, seqNum, CustomerCreated(name, age)) => model.put(id, CustomerDTO(id, seqNum, name, age))
    case Event(id, seqNum, CustomerNameChanged(newName)) => {
      val customer = model(id)
      if (customer != null) model.put(id, customer.copy(name = newName, seqNum = seqNum))
    }
    case Event(id, seqNum, CustomerAgeChanged(newAge)) => {
      val customer = model(id)
      if (customer != null) model.put(id, customer.copy(age = newAge, seqNum = seqNum))
    }
  }
}