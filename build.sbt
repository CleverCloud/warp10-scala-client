organization := """com.clevercloud"""

name := """akka-warp10-scala-client"""

version := "1.5.2"

scalaVersion := "2.13.6"

crossScalaVersions := Seq("2.12.15", "2.13.6")

libraryDependencies ++= Seq(
  "org.apache.commons" % "commons-text" % "1.9",
  "com.typesafe.akka" %% "akka-actor" % "2.6.17",
  "com.typesafe.akka" %% "akka-http" % "10.2.6",
  "com.typesafe.akka" %% "akka-stream" % "2.6.17",
  "io.circe" %% "circe-core" % "0.14.1",
  "io.circe" %% "circe-generic" % "0.14.1",
  "io.circe" %% "circe-parser" % "0.14.1",

  "ch.qos.logback" % "logback-classic" % "1.2.6",
  "com.typesafe.scala-logging" %% "scala-logging" % "3.9.4",

  "org.specs2" %% "specs2-core" % "4.12.4-js-ec" % Test
)

bintrayOrganization := Some("clevercloud")

scalacOptions ++= Seq(
  "-deprecation",
  "-feature",
  "-unchecked",
  "-Xlint",
  "-Xcheckinit",
  "-language:postfixOps"
)

parallelExecution in Test := false

licenses += ("Apache-2.0", url("https://www.apache.org/licenses/LICENSE-2.0"))

enablePlugins(GhpagesPlugin)

git.remoteRepo := "git@github.com:clevercloud/akka-warp10-scala-client.git"

enablePlugins(SiteScaladocPlugin)
