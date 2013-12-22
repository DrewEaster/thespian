package com.dreweaster.thespian.example.model.command

import com.dreweaster.thespian.domain.{State, Aggregate, AggregateFactory}
import com.dreweaster.thespian.example.model.command.CustomerEvents.{CustomerCreated, CustomerNameChanged, CustomerAgeChanged}
import com.dreweaster.thespian.example.model.command.CustomerCommands.{ChangeCustomerAge, ChangeCustomerName, CreateCustomer}
import com.dreweaster.thespian.example.model.command.CustomerExceptions.{CustomerNotCreatedException, CustomerAlreadyCreatedException}

object Customer extends AggregateFactory {

  val aggregateType = "Customer"

  def newInstance: Aggregate[_] = new Customer
}

case class CustomerState(name: String = "", age: Int = 0, created: Boolean = false) extends State {
  def applyEvent = {
    case CustomerCreated(cName, cAge) => copy(name = cName, age = cAge, created = true)
    case CustomerNameChanged(newName) => copy(name = name)
    case CustomerAgeChanged(newAge) => copy(age = age)
  }
}

class Customer extends Aggregate[CustomerState] {
  def initialState: CustomerState = CustomerState()

  def handleCommand = {
    case CreateCustomer(name, age) => {
      if (!currentState.created) {
        publish(CustomerCreated(name, age))
      } else throw new CustomerAlreadyCreatedException(context.id)
    }
    case ChangeCustomerName(newName) => {
      if (currentState.created) {
        publish(CustomerNameChanged(newName))
      } else throw new CustomerNotCreatedException(context.id)
    }
    case ChangeCustomerAge(newAge) => {
      if (currentState.created) {
        publish(CustomerAgeChanged(newAge))
      } else throw new CustomerNotCreatedException(context.id)
    }
  }
}
