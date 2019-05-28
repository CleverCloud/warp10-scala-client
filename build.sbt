organization := """com.clevercloud"""

name := """akka-warp10-scala-client"""

version := "1.2.1"

scalaVersion := "2.12.8"

crossScalaVersions := Seq("2.12.8")

val circeVersion = "0.9.3"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % "2.5.23",
  "com.typesafe.akka" %% "akka-http" % "10.1.8",
  "com.typesafe.akka" %% "akka-stream" % "2.5.23",
  "com.typesafe.play" %% "play-json" % "2.7.3",
  "io.circe" %% "circe-core" % "0.10.1",
  "io.circe" %% "circe-generic" % "0.10.1",
  "io.circe" %% "circe-parser" % "0.10.1",
  "org.specs2" %% "specs2-core" % "4.5.1" % Test
)

bintrayOrganization := Some("clevercloud")

scalacOptions ++= Seq(
  "-deprecation",
  "-feature",
  "-unchecked",
  "-Xlint",
  "-Xcheckinit",
  "-Ywarn-adapted-args",
  "-Ywarn-dead-code",
  "-Ywarn-inaccessible",
  "-Ywarn-dead-code",
  "-Ywarn-nullary-override",
  "-Ywarn-nullary-unit",
  "-Ywarn-numeric-widen",
  "-Ywarn-unused",
  "-Ywarn-unused-import",
  "-Ywarn-value-discard",
  "-language:postfixOps"
)

parallelExecution in Test := false

licenses += ("Apache-2.0", url("http://www.apache.org/licenses/LICENSE-2.0"))
