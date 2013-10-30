name := """hello-akka"""

version := "1.0"

scalaVersion := "2.10.2"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % "2.3-M1",
  "com.typesafe.akka" %% "akka-cluster" % "2.3-M1",
  "com.typesafe.akka" %% "akka-contrib" % "2.3-M1",
  "com.typesafe.akka" %% "akka-persistence-experimental" % "2.3-M1",
  "com.typesafe.akka" %% "akka-testkit" % "2.3-M1",
  "org.scalatest" %% "scalatest" % "1.9.1" % "test",
  "junit" % "junit" % "4.11" % "test",
  "com.novocode" % "junit-interface" % "0.7" % "test->default"
)

testOptions += Tests.Argument(TestFrameworks.JUnit, "-v")

