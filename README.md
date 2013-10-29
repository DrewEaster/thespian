akka3d
======

A DDD, CQRS and event sourcing framework based on Akka

TODO
----

* Add Distributed PubSub style to event publishing - topic per aggregate root type. Read models subscribe to "feed" for particular aggregate root type
* Support for become/unbecome without breaking snapshoting
* Snapshot scheduling
* DI integration (subcut?)
* Add Akka cluster support for fully distributed aggregates - might take a while :-)
* Support for services (what are these exactly?)
* Support with process managers (not sure what I really mean by this yet!)
* Scheduling support - linked with process managers. e.g. timing out a ticket reservation
* Support for FSMs
* Java API
* Play! integration
* Spray integration
