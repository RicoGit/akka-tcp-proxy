name := "akka-tcp-proxy"

version := "1.0"

scalaVersion := "2.11.8"


/* Dependencies */

val akkaVersion = "2.5.3"
val log4jVersion = "2.8.2"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-stream" % akkaVersion,
  "org.json4s" %% "json4s-jackson" % "3.5.2",
  "com.github.kardapoltsev" %% "json4s-java-time" % "1.0.1",
  "net.ceedubs" %% "ficus" % "1.1.2",

  // logging

  "com.typesafe.akka" %% "akka-slf4j" % akkaVersion,
  "org.apache.logging.log4j" % "log4j-api" % log4jVersion,
  "org.apache.logging.log4j" % "log4j-slf4j-impl" % log4jVersion,

  // tests

  "com.typesafe.akka" %% "akka-stream-testkit" % akkaVersion % "test",
  "org.scalatest" %% "scalatest" % "3.0.0" % "test"
)
