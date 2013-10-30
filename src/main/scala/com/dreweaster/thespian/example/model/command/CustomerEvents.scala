package com.dreweaster.thespian.example.model.command

object CustomerEvents {

  case class CustomerCreated(name: String, age: Int)

  case class CustomerNameChanged(name: String)

  case class CustomerAgeChanged(age: Int)
}
