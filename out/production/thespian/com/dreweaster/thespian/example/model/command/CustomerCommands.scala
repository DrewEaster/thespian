package com.dreweaster.thespian.example.model.command

object CustomerCommands {

  case class CreateCustomer(name: String, age: Int)

  case class ChangeCustomerName(name: String)

  case class ChangeCustomerAge(age: Int)
}
