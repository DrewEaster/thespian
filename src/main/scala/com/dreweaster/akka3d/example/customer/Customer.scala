package com.dreweaster.akka3d.example.customer

import com.dreweaster.akka3d.domain.{AggregateRootState, AggregateRootType, AggregateRoot}
import com.dreweaster.akka3d.example.customer.CustomerEvents.{CustomerAgeChanged, CustomerNameChanged, CustomerCreated}
import com.dreweaster.akka3d.example.customer.CustomerCommands.{ChangeCustomerAge, ChangeCustomerName, CreateCustomer}

object Customer extends AggregateRootType {
  val typeInfo = Customer
}

case class CustomerState(name: String = "", age: Int = 0) extends AggregateRootState {
  def update = {
    case CustomerCreated(cName, cAge) => copy(name = cName, age = cAge)
    case CustomerNameChanged(newName) => copy(name = name)
    case CustomerAgeChanged(newAge) => copy(age = age)
  }
}

class Customer extends AggregateRoot[CustomerState] {
  var state = CustomerState

  val handleCommand = {
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
