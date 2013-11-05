[![Build Status](https://travis-ci.org/DrewEaster/thespian.png?branch=master)](https://travis-ci.org/DrewEaster/thespian)

Thespian
========

A DDD, CQRS and event sourcing framework built on top of Akka.

Why the name?
-------------

Because thespians are actors, Akka is an actor based framework, and I was trying to be clever with word play.

What is it?
-----------

It's an actor based framework for building DDD, CQRS and event sourcing applications. It's built on top of Akka and makes use of the simply awesome [akka-persistence](http://doc.akka.io/docs/akka/snapshot/scala/persistence.html) extension (and many other Akka goodies). The design is influenced by a vision to have a 1:1 relationship between an Aggregate Root (AR) and an actor. And, importantly, it promotes a simplified concurrency model whereby only one actor per aggregate root instance can exist at a time. 

Initially, Thespian is more an exploration of ideas and best practices for combining the actor model with DDD, CQRS and event sourcing. It does kind of represent a framework, but it's possibly a bit generous to call it one in its current form.

This project aims to provide a viable alternative to traditional database backed CRUD solutions. No longer must we fight against business logic saturated services, anaemic domain models and bloated ORMs.

In the good old days, OO programming was all about message passing and, somewhere along the way, these concepts got lost in translation under a mountain of getters and setters. The actor model brings us right back to the true concepts of OO design - encapsulation of state, and manipulation of state via message passing. This all contributes towards a much cleaner, fully object-oriented domain layer. The aim of Thespian is to evangelise a return to better OO design using Eric Evans' Domain Driven Design (DDD) principles as its guiding light. Combining DDD principles, CQRS and event sourcing with the actor model (Akka), is what brings the vision to life. 

A potentially overlooked benefit of CQRS and eventsourcing is how compatible these patterns are with TDD. When viewing business logic as simply 'Command in' -> 'Events out', it becomes clear very quickly how testable your code can be. Thespian plans to maximize this benefit, allowing engineers to write not just more robust software, but much cleaner tests as well.

Thespian is the materialisation and extension of some ideas I first wrote about [here](http://www.dreweaster.com/blog/2013/10/27/Akka-DDD-CQRS-Event-Sourcing-And-Me/).

What is it not?
---------------

Close to being ready to use in anything other than prototyping contexts :-) And it only has single node support for now which could be quite limiting.

Thespian does not currently have a Java implementation - it is planned!

A short example
---------------

This is just a very basic example of Thespian in use:
```scala
  val domainModel = DomainModel("my-domain") {
    Customer
  }

  val readModel = domainModel.subscribe(Customer, CustomerReadModel.props)

  val customerId = UUID.randomUUID
  val customer = domainModel.aggregateRootOf(Customer, customerId)

  customer ! CreateCustomer("Andrew", 33)
  customer ! ChangeCustomerName("Andy")
  customer ! ChangeCustomerAge(34)

  (readModel ? GetCustomer(customerId)).map {
    case Some(customer: CustomerDTO) => println(customer)
  }
```
Credits
-------

Some of the concepts within Thespian were inspired by some code shared by Vaughn Vernon on his [blog](http://vaughnvernon.co). Many thanks to him for the inspiration.

As always, none of this would be possible without the true awesomeness of the Akka Team. Thanks to Roland, Patrik, Martin et al.

TODO
----

* Add Distributed PubSub style to event publishing - topic per aggregate root type. Read models subscribe to "feed" for particular aggregate root type. Subscribe via DomainModel.
* Support for become/unbecome without breaking snapshoting
* Aggregate root timeouts (clear cache) - ReceiveTimeout
* Snapshot scheduling
* Support for intermediate state recovery (consideration that people might want to do this "offline").
* Need to think about how people can rebuild read models easily
* Testing support - testing command handling without persistence
* DI integration (subcut?)
* Avoid missing published events in read models?
* Add Akka cluster support for fully distributed aggregates - might take a while :-) But this could be of limited use without it
* Support for services (what are these exactly?). How do aggregates access them. Subcut/DI? Services as actors?
* Support with process managers (not sure what I really mean by this yet!). Actor that receives events and sends commands (and is persistent)...
* Scheduling support - linked with process managers, e.g. timing out a ticket reservation. Must support durable scheduling - i.e. scheduled jobs must survive restarts/crashes
* Support for FSMs
* Support for publishing events direct to external message queues
* What the hell to do about errors, e.g. trying to send a Create to an existing entity. Error stream?! This is a wider CQRS topic
* I'd like to see whether it's possible to introduce a short-term subscription (with timeout) api - to listen for read model being updated following command submission
* Java API
* Play! integration
* Spray integration
* Build on TravisCI and deploy artifact to some maven repository
* Create Typesafe Activator template that uses Thespian

License
-------

This software is licensed under the Apache 2 license, quoted below.

Copyright &copy; 2013 **[Andrew Easter](http://www.dreweaster.com/)**.

Licensed under the Apache License, Version 2.0 (the "License"); you may not use this project except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0.

Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.

