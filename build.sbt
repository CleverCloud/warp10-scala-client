organization := """com.clevercloud"""

name := """akka-warp10-scala-client"""

version := "1.4.1-SNAPSHOT"

scalaVersion := "2.12.8"

crossScalaVersions := Seq("2.12.8")

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % "2.5.25",
  "com.typesafe.akka" %% "akka-http" % "10.1.10",
  "com.typesafe.akka" %% "akka-stream" % "2.5.25",
  "com.typesafe.play" %% "play-json" % "2.7.3",
  "io.circe" %% "circe-core" % "0.12.2",
  "io.circe" %% "circe-generic" % "0.12.2",
  "io.circe" %% "circe-parser" % "0.12.2",
  "org.apache.commons" % "commons-lang3" % "3.9",
  "org.specs2" %% "specs2-core" % "4.8.0" % Test
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

licenses += ("Apache-2.0", url("https://www.apache.org/licenses/LICENSE-2.0"))

enablePlugins(GhpagesPlugin)

git.remoteRepo := "git@github.com:clevercloud/akka-warp10-scala-client.git"

enablePlugins(SiteScaladocPlugin)
