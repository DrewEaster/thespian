akka3d
======

A DDD, CQRS and event sourcing nanoframework based on Akka

TODO
----

* Add Distributed PubSub style to event publishing - topic per aggregate root type. Read models subscribe to "feed" for particular aggregate root type
* Support for become/unbecome without breaking snapshoting
* Snapshot scheduling
* Add Akka cluster support for fully distributed aggregates - might take a while :-)
* Support with ProcessManagers
* Support for FSMs
* Java API
