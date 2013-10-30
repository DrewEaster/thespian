package com.dreweaster.akka3d.example

import com.dreweaster.akka3d.domain.DomainModel
import java.util.UUID
import com.dreweaster.akka3d.example.model.command.{CustomerCommands, Customer}
import CustomerCommands.ChangeCustomerAge
import CustomerCommands.ChangeCustomerName
import CustomerCommands.CreateCustomer
import com.dreweaster.akka3d.example.model.query.{GetCustomer, CustomerReadModel}
import scala.concurrent.duration._
import akka.util.Timeout
import akka.pattern.ask
import scala.concurrent.ExecutionContext
import ExecutionContext.Implicits.global

object Example extends App {

  implicit val timeout = Timeout(5 seconds)

  val domainModel = DomainModel("akka3d-example") {
    Customer
  }

  val readModel = domainModel.subscribe(Customer, CustomerReadModel.props)

  val customerId = UUID.randomUUID
  val customer = domainModel.aggregateRootOf(Customer, customerId)

  Thread.sleep(2500)

  customer ! CreateCustomer("Andrew", 33)
  customer ! ChangeCustomerName("Andy")
  customer ! ChangeCustomerAge(34)

  Thread.sleep(2500)

  (readModel ? GetCustomer(customerId)).map {
    case response => println(response)
  }

  Thread.sleep(2500)
  domainModel.shutdown()
}
