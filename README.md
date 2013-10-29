akka3d
======

A DDD, CQRS and event sourcing framework based on Akka

TODO
----

* Add Distributed PubSub style to event publishing - topic per aggregate root type. Read models subscribe to "feed" for particular aggregate root type
* Support for become/unbecome without breaking snapshoting
* Aggregate root timeouts (clear cache) - ReceiveTimeout
* Snapshot scheduling
* DI integration (subcut?)
* Add Akka cluster support for fully distributed aggregates - might take a while :-)
* Support for services (what are these exactly?). How do aggregates access them. Subcut/DI? Services as actors?
* Support with process managers (not sure what I really mean by this yet!)
* Scheduling support - linked with process managers, e.g. timing out a ticket reservation
* Support for FSMs
* What the hell to do about errors, e.g. trying to send a Create to an existing entity. Error stream?! This is a wider CQRS topic
* Java API
* Play! integration
* Spray integration
* Build on TravisCI and deploy artifact to some maven repository
* Create Typesafe Activator template that uses akka3d
