package com.dreweaster.akka3d.example.customer

object CustomerEvents {

  case class CustomerCreated(name: String, age: Int)

  case class CustomerNameChanged(name: String)

  case class CustomerAgeChanged(age: Int)
}
