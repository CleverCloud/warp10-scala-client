organization := """com.clevercloud"""

name := """akka-warp10-scala-client"""

version := "0.1.0-SNAPSHOT"

scalaVersion := "2.12.6"

crossScalaVersions := Seq("2.11.7", "2.12.6")


val ARTIFACTORY_ADDON_HOST = sys.env.get("ARTIFACTORY_ADDON_HOST").getOrElse(throw new RuntimeException("Environment variable ARTIFACTORY_ADDON_HOST missing"))
val ARTIFACTORY_SBT_RELEASE_REPOSITORY = sys.env.get("ARTIFACTORY_SBT_RELEASE_REPOSITORY").getOrElse(throw new RuntimeException("Environment variable ARTIFACTORY_SBT_RELEASE_REPOSITORY missing"))
val ARTIFACTORY_SBT_RELEASE_USER = sys.env.get("ARTIFACTORY_SBT_RELEASE_USER").getOrElse(throw new RuntimeException("Environment variable ARTIFACTORY_SBT_RELEASE_USER missing"))
val ARTIFACTORY_SBT_RELEASE_PASSWORD = sys.env.get("ARTIFACTORY_SBT_RELEASE_PASSWORD").getOrElse(throw new RuntimeException("Environment variable ARTIFACTORY_SBT_RELEASE_PASSWORD missing"))

resolvers += "Artifactory Realm" at "https://" + ARTIFACTORY_ADDON_HOST + "/" + ARTIFACTORY_SBT_RELEASE_REPOSITORY

publishTo := Some("Artifactory Realm" at "https://" + ARTIFACTORY_ADDON_HOST + ARTIFACTORY_SBT_RELEASE_REPOSITORY + ";build.timestamp=" + new java.util.Date().getTime)

credentials += Credentials("Artifactory Realm", ARTIFACTORY_ADDON_HOST, ARTIFACTORY_SBT_RELEASE_USER, ARTIFACTORY_SBT_RELEASE_PASSWORD)

val circeVersion = "0.9.3"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % "2.5.12",
  "com.typesafe.akka" %% "akka-http" % "10.1.1",
  "com.typesafe.akka" %% "akka-stream" % "2.5.12",
  "com.typesafe.play" %% "play-json" % "2.6.7",
  "io.circe" %% "circe-core" % "0.10.0",
  "io.circe" %% "circe-generic" % "0.10.0",
  "io.circe" %% "circe-parser" % "0.10.0",
  "org.specs2" %% "specs2-core" % "3.8.8" % Test
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
