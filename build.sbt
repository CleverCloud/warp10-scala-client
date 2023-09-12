inThisBuild(
  List(
    organization := "com.clever-cloud",
    homepage := Some(url("https://github.com/clevercloud/akka-warp10-scala-client")),
    licenses := List("Apache-2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0")),
    developers := List(
      Developer(
        "kannarfr",
        "Alexandre DUVAL",
        "kannarfr@gmail.com",
        url("https://alexandre-duval.fr")
      )
    ),
    version := "1.6.14",
    name := """akka-warp10-scala-client""",
    scalaVersion := "2.13.12",
    versionScheme := Some("early-semver"),
    scmInfo := Some(
      ScmInfo(
        url("https://github.com/clevercloud/akka-warp10-scala-client"),
        "git@github.com:clevercloud/akka-warp10-scala-client.git"
      )
    ),
    libraryDependencies ++= Seq(
      "org.apache.commons" % "commons-text" % "1.10.0",
      "com.typesafe.akka" %% "akka-actor" % "2.6.19",
      "com.typesafe.akka" %% "akka-http" % "10.2.9",
      "com.typesafe.akka" %% "akka-stream" % "2.6.19",
      "io.circe" %% "circe-core" % "0.14.5",
      "io.circe" %% "circe-generic" % "0.14.5",
      "io.circe" %% "circe-parser" % "0.14.5",
      "ch.qos.logback" % "logback-classic" % "1.4.11",
      "com.typesafe.scala-logging" %% "scala-logging" % "3.9.5",
      "org.specs2" %% "specs2-core" % "4.20.2" % Test,
      "com.clever-cloud" %% "testcontainers-scala-warp10" % "2.0.2" % Test
    ),
    scalacOptions ++= Seq(
      "-deprecation",
      "-feature",
      "-unchecked",
      "-Xlint",
      "-Xcheckinit",
      "-language:postfixOps"
    ),
    Test / parallelExecution := false,
    git.remoteRepo := "git@github.com:clevercloud/akka-warp10-scala-client.git"
  )
)
enablePlugins(GhpagesPlugin)
enablePlugins(SiteScaladocPlugin)

sonatypeCredentialHost := "s01.oss.sonatype.org"
sonatypeRepository := "https://s01.oss.sonatype.org/service/local"

ThisBuild / githubWorkflowTargetTags ++= Seq("v*")
ThisBuild / githubWorkflowPublishTargetBranches := Seq(RefPredicate.StartsWith(Ref.Tag("v")))
ThisBuild / githubWorkflowPublishTargetBranches += RefPredicate.StartsWith(Ref.Tag("v"))
ThisBuild / githubWorkflowPublish := Seq(WorkflowStep.Sbt(List("ci-release")))
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
