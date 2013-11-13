package com.dreweaster.thespian.domain

import com.dreweaster.thespian.example.model.command.Customer
import java.util.UUID

/**
class DomainBuilder {

  def withAggregateRoot(art: AggregateRootType): DomainBuilder

  def withProcessManager(pmt: ProcesssManagerType, art: AggregateRootType): DomainBuilder

  def withSubscriber()

  def build: DomainModel
}*/

object DomainBuilder

object Example {

/*  val domainModel =
    DomainBuilder {
      aggregateRoots {
        Invoice,
        Payment
      },
      processManagers {
        processManager {
          factory = InvoiceProgress
          createdBy = Invoice >> InvoiceCreated
          updatedBy = {
            Payment >> PaymentCompleted ? _.correlationId, // def ?(f: T => UUID): UUID
            Payment >> PaymentFailed ? _.correlationId // CorrelationId is the unique id of the ProcessManager
          } // Still perfectly valid to send a message directly to a process manager - e.g. pm ! PaymentFailed
        },
        processManager {
          ...
        }
      },
      services {
        // TODO: What is this?
      },
      subscribers {
        Invoice >> MQPublisher("aggregate.invoice"),
        Payment >> MQPublisher("aggregate.payment")
      }
    }.build

  // Example of process manager where payment is a separate bounded context - i.e. programmatic event passing
  case pf@PaymentFailed(_, correlationId, _, _) => notifyProcessManager(correlationId, pf)
  case pc@PaymentCompleted(_, correlationId, _, _) => notifyProcessManager(correlationId, pc)

  def notifyProcessManager(processManagerId: UUID, event:Any) = {
    val invoiceProgress = domainModel.processManagerOf(InvoiceProgress, processManagerId)
    invoiceProgress ! event
  }*/
}