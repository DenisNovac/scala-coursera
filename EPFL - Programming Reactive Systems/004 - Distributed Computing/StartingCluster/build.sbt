name := "ClusterMain"

version := "0.1"

scalaVersion := "2.13.1"



libraryDependencies += "com.typesafe.akka" %% "akka-actor" % "2.6.3"
libraryDependencies += "com.typesafe.akka" %% "akka-cluster" % "2.6.3"
libraryDependencies += "com.typesafe.akka" %% "akka-testkit" % "2.6.3" % Test
libraryDependencies += "org.scalatest" %% "scalatest" % "3.1.1" % "test"

//javaOptions += s"-Dconfig.file=${sourceDirectory.value}/application.conf"

libraryDependencies += "com.ning" % "async-http-client" % "1.7.19"
libraryDependencies += "org.jsoup" % "jsoup" % "1.8.1"
