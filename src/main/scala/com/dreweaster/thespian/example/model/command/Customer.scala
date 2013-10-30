package com.dreweaster.thespian.example.model.command

import com.dreweaster.thespian.domain.{AggregateRootType, AggregateRoot}
import com.dreweaster.thespian.example.model.command.CustomerEvents.{CustomerCreated, CustomerNameChanged, CustomerAgeChanged}
import com.dreweaster.thespian.example.model.command.CustomerCommands.{ChangeCustomerAge, ChangeCustomerName, CreateCustomer}

object Customer extends AggregateRootType {
  val typeInfo = classOf[Customer]
}

case class CustomerState(name: String = "", age: Int = 0) {
  def update: Any => CustomerState = {
    case CustomerCreated(cName, cAge) => copy(name = cName, age = cAge)
    case CustomerNameChanged(newName) => copy(name = name)
    case CustomerAgeChanged(newAge) => copy(age = age)
  }
}

class Customer extends AggregateRoot {
  var state = CustomerState()

  def fetchState = state

  def applyState = e => state = state.update(e)

  def loadState = s => state = s.asInstanceOf[CustomerState]

  def handleCommand: Receive = {
    case CreateCustomer(name, age) => unitOfWork {
      CustomerCreated(name, age)
    }
    case ChangeCustomerName(newName) => unitOfWork {
      CustomerNameChanged(newName)
    }
    case ChangeCustomerAge(newAge) => unitOfWork {
      CustomerAgeChanged(newAge)
    }
  }
}
