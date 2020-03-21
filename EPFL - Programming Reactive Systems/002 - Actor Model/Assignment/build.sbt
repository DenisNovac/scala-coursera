course := "reactive"
assignment := "actorbintree"

testOptions in Test += Tests.Argument(TestFrameworks.JUnit, "-a", "-v", "-s")
parallelExecution in Test := false

val akkaVersion = "2.6.0"

scalaVersion := "2.13.1"

scalacOptions ++= Seq(
  "-feature",
  "-deprecation",
  "-encoding", "UTF-8",
  "-unchecked",
  "-Xlint",
)

libraryDependencies ++= Seq(
  "com.typesafe.akka"        %% "akka-actor"         % akkaVersion,
  "com.typesafe.akka"        %% "akka-testkit"       % akkaVersion % Test,
  "com.novocode"             % "junit-interface"     % "0.11"      % Test
)
libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.2.3"
libraryDependencies += "com.typesafe.scala-logging" %% "scala-logging" % "3.9.2"