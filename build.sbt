lazy val circeVersion = "0.14.9"
lazy val pekkoVersion = "1.0.3"
lazy val pekkoHttpVersion = "1.0.1"

ThisBuild / organization := "com.clever-cloud"
ThisBuild / homepage := Some(url("https://github.com/clevercloud/warp10-scala-client"))
ThisBuild / licenses := List("Apache-2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0"))
ThisBuild / developers := List(
  Developer(
    "kannarfr",
    "Alexandre DUVAL",
    "kannarfr@gmail.com",
    url("https://alexandre-duval.fr")
  )
)
ThisBuild / version := "2.1.0"
ThisBuild / scalaVersion := "3.4.2"
ThisBuild / versionScheme := Some("early-semver")
ThisBuild / scmInfo := Some(
  ScmInfo(
    url("https://github.com/clevercloud/warp10-scala-client"),
    "git@github.com:clevercloud/warp10-scala-client.git"
  )
)
ThisBuild / libraryDependencies ++= Seq(
  "org.apache.commons" % "commons-text" % "1.10.0",
  "org.apache.pekko" %% "pekko-actor" % pekkoVersion,
  "org.apache.pekko" %% "pekko-stream" % pekkoVersion,
  "org.apache.pekko" %% "pekko-http" % pekkoHttpVersion,
  "io.circe" %% "circe-core" % circeVersion,
  "io.circe" %% "circe-generic" % circeVersion,
  "io.circe" %% "circe-parser" % circeVersion,
  "ch.qos.logback" % "logback-classic" % "1.5.14",
  "com.typesafe.scala-logging" %% "scala-logging" % "3.9.5",
  "org.specs2" %% "specs2-core" % "4.20.2" % Test,
  "com.clever-cloud" %% "testcontainers-scala-warp10" % "2.1.0" % Test
)
ThisBuild / scalacOptions ++= Seq(
  "-deprecation",
  "-feature",
  "-unchecked",
  "-language:postfixOps"
)
ThisBuild / Test / parallelExecution := false
ThisBuild / git.remoteRepo := "git@github.com:clevercloud/warp10-scala-client.git"

enablePlugins(GhpagesPlugin)
enablePlugins(SiteScaladocPlugin)

ThisBuild / sonatypeCredentialHost := "s01.oss.sonatype.org"
ThisBuild / sonatypeRepository := "https://s01.oss.sonatype.org/service/local"

ThisBuild / githubWorkflowJavaVersions := Seq(JavaSpec(JavaSpec.Distribution.Temurin, "21"))
ThisBuild / githubWorkflowTargetTags ++= Seq("v*")
ThisBuild / githubWorkflowPublishTargetBranches := Seq(RefPredicate.StartsWith(Ref.Tag("v")))
ThisBuild / githubWorkflowPublish := Seq(
  WorkflowStep.Sbt(
    List("ci-release"),
    env = Map(
      "PGP_PASSPHRASE" -> "${{ secrets.PGP_PASSPHRASE }}",
      "PGP_SECRET" -> "${{ secrets.PGP_SECRET }}",
      "SONATYPE_PASSWORD" -> "${{ secrets.OSSRH_TOKEN }}",
      "SONATYPE_USERNAME" -> "${{ secrets.OSSRH_USERNAME }}"
    )
  )
)

lazy val root = (project in file(".")).settings(
  name := "warp10-scala-client"
)
