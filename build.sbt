organization := """com.clevercloud"""

name := """akka-warp10-scala-client"""

version := "1.4.2-SNAPSHOT"

scalaVersion := "2.12.10"

crossScalaVersions := Seq("2.12.10", "2.13.1")

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % "2.6.4",
  "com.typesafe.akka" %% "akka-http" % "10.1.11",
  "com.typesafe.akka" %% "akka-stream" % "2.6.4",
  "com.typesafe.play" %% "play-json" % "2.8.1",
  "io.circe" %% "circe-core" % "0.13.0",
  "io.circe" %% "circe-generic" % "0.13.0",
  "io.circe" %% "circe-parser" % "0.13.0",
  "org.apache.commons" % "commons-lang3" % "3.9",
  "org.specs2" %% "specs2-core" % "4.9.2" % Test
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
