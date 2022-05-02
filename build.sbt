organization := """com.clevercloud"""

name := """akka-warp10-scala-client"""

version := "1.6.2"

scalaVersion := "2.13.8"

crossScalaVersions := Seq("2.12.15", "2.13.8")

versionScheme := Some("early-semver")

lazy val scalatestVersion = "3.2.10"

libraryDependencies ++= Seq(
  "org.apache.commons" % "commons-text" % "1.9",
  "com.typesafe.akka" %% "akka-actor" % "2.6.19",
  "com.typesafe.akka" %% "akka-http" % "10.2.7",
  "com.typesafe.akka" %% "akka-stream" % "2.6.19",
  "io.circe" %% "circe-core" % "0.14.1",
  "io.circe" %% "circe-generic" % "0.14.1",
  "io.circe" %% "circe-parser" % "0.14.1",
  "ch.qos.logback" % "logback-classic" % "1.2.11",
  "com.typesafe.scala-logging" %% "scala-logging" % "3.9.4",
  "org.specs2" %% "specs2-core" % "4.13.3" % Test,
  "com.clever-cloud" %% "testcontainers-scala-warp10" % "2.0.0" % Test
)

scalacOptions ++= Seq(
  "-deprecation",
  "-feature",
  "-unchecked",
  "-Xlint",
  "-Xcheckinit",
  "-language:postfixOps"
)

Test / parallelExecution := false

enablePlugins(GhpagesPlugin)

git.remoteRepo := "git@github.com:clevercloud/akka-warp10-scala-client.git"

enablePlugins(SiteScaladocPlugin)

publishTo := Some("Sonatype Snapshots Nexus" at "https://oss.sonatype.org/content/repositories/snapshots")

publishTo := Some(
  if (isSnapshot.value)
    Opts.resolver.sonatypeSnapshots
  else
    Opts.resolver.sonatypeStaging
)

licenses += ("Apache-2.0", url("https://www.apache.org/licenses/LICENSE-2.0"))
scmInfo := Some(
  ScmInfo(
    url("https://github.com/clevercloud/akka-warp10-scala-client"),
    "git@github.com:clevercloud/akka-warp10-scala-client.git"
  )
)
developers := List(Developer("kannarfr", "Alexandre DUVAL", "kannarfr@gmail.com", url("https://alexandre-duval.fr")))

resolvers += Resolver.mavenLocal
resolvers += Resolver.defaultLocal
resolvers += Resolver.jcenterRepo

ThisBuild / githubWorkflowTargetTags ++= Seq("v*")
ThisBuild / githubWorkflowPublishTargetBranches := Seq(RefPredicate.StartsWith(Ref.Tag("v")))
ThisBuild / githubWorkflowPublish := Seq(WorkflowStep.Sbt(List("ci-release")))
ThisBuild / githubWorkflowPublishTargetBranches += RefPredicate.StartsWith(Ref.Tag("v"))
ThisBuild / githubWorkflowPublish := Seq(
  WorkflowStep.Sbt(
    List("ci-release"),
    env = Map(
      "PGP_PASSPHRASE" -> "${{ secrets.OSSRH_GPG_SECRET_KEY_PASSWORD }}",
      "PGP_SECRET" -> "${{ secrets.OSSRH_GPG_SECRET_KEY }}",
      "SONATYPE_USERNAME" -> "${{ secrets.OSSRH_USERNAME }}",
      "SONATYPE_PASSWORD" -> "${{ secrets.OSSRH_TOKEN }}"
    )
  )
)
