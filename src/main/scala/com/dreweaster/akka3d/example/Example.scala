package com.dreweaster.akka3d.example

import akka.actor._
import com.dreweaster.akka3d.domain.{DomainModel}
import com.dreweaster.akka3d.example.customer.Customer
import com.dreweaster.akka3d.example.customer.CustomerCommands.{ChangeCustomerAge, ChangeCustomerName, CreateCustomer}
import java.util.UUID
import com.dreweaster.akka3d.example.customer.CustomerEvents.{CustomerAgeChanged, CustomerNameChanged, CustomerCreated}
import com.dreweaster.akka3d.example.customer.CustomerCommands.ChangeCustomerAge
import com.dreweaster.akka3d.example.customer.CustomerCommands.ChangeCustomerName
import com.dreweaster.akka3d.example.customer.CustomerCommands.CreateCustomer
import com.dreweaster.akka3d.domain.Event

case class CustomerDTO(id: UUID, seqNum: Long, name: String, age: Int)

class CustomerReadModel extends Actor {
  val model = scala.collection.mutable.Map[UUID, CustomerDTO]()

  def receive = {
    case Event(id, seqNum, CustomerCreated(name, age)) => {
      model.put(id, CustomerDTO(id, seqNum, name, age))
    }
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

object Example extends App {

  val model = DomainModel("akka3d-example") {
    Customer
  }

  val customerId = UUID.randomUUID
  val customer = model.aggregateRootOf(Customer, customerId)

  customer ! CreateCustomer("Andrew", 33)
  customer ! ChangeCustomerName("Andy")
  customer ! ChangeCustomerAge(34)

  model.shutdown()
}
