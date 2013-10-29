akka3d (pre-alpha)
==================

A DDD, CQRS and event sourcing framework built on top of Akka

Why the name?
-------------

Because 3d is short for Domain Driven Design (DDD).

What is it?
-----------

It's an actor based framework for building DDD, CQRS and event sourcing applications. It's built on top of Akka and makes use of the simply awesome akka-persistence extension. The design is influenced by a vision to have a 1:1 relationship between an Aggregate Root (AR) and an actor. And, importantly, it promotes a simplified concurrency model whereby only one actor per aggregate root instance can exist at a time. 

What is it not?
---------------

Close to being ready to use in anything other than prototyping contexts :-) And it only has single node support for now which is quite limiting.

TODO
----

* Add Distributed PubSub style to event publishing - topic per aggregate root type. Read models subscribe to "feed" for particular aggregate root type. Subscribe via DomainModel.
* Support for become/unbecome without breaking snapshoting
* Aggregate root timeouts (clear cache) - ReceiveTimeout
* Snapshot scheduling
* DI integration (subcut?)
* Add Akka cluster support for fully distributed aggregates - might take a while :-) But this could be of limited use without it
* Support for services (what are these exactly?). How do aggregates access them. Subcut/DI? Services as actors?
* Support with process managers (not sure what I really mean by this yet!). Actor that receives events and sends commands (and is persistent)...
* Scheduling support - linked with process managers, e.g. timing out a ticket reservation
* Support for FSMs
* What the hell to do about errors, e.g. trying to send a Create to an existing entity. Error stream?! This is a wider CQRS topic
* Java API
* Play! integration
* Spray integration
* Build on TravisCI and deploy artifact to some maven repository
* Create Typesafe Activator template that uses akka3d
