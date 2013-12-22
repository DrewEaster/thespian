package com.dreweaster.thespian.example.model.command

import java.util.UUID

/**
 */
object CustomerExceptions {

  class CustomerNotCreatedException(id: UUID) extends RuntimeException

  class CustomerAlreadyCreatedException(id: UUID) extends RuntimeException
}
