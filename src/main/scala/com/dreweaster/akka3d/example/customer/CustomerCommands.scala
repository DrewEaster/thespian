package com.dreweaster.akka3d.example.customer


object CustomerCommands {

  case class CreateCustomer(name: String, age: Int)

  case class ChangeCustomerName(name: String)

  case class ChangeCustomerAge(age: Int)
}
