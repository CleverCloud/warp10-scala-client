inThisBuild(
  List(
    organization := "com.clevercloud",
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
    name := """akka-warp10-scala-client""",
    scalaVersion := "2.13.8",
    versionScheme := Some("early-semver"),
    scmInfo := Some(
      ScmInfo(
        url("https://github.com/clevercloud/akka-warp10-scala-client"),
        "git@github.com:clevercloud/akka-warp10-scala-client.git"
      )
    ),
    libraryDependencies ++= Seq(
      "org.apache.commons" % "commons-text" % "1.9",
      "com.typesafe.akka" %% "akka-actor" % "2.6.19",
      "com.typesafe.akka" %% "akka-http" % "10.2.9",
      "com.typesafe.akka" %% "akka-stream" % "2.6.19",
      "io.circe" %% "circe-core" % "0.14.1",
      "io.circe" %% "circe-generic" % "0.14.1",
      "io.circe" %% "circe-parser" % "0.14.1",
      "ch.qos.logback" % "logback-classic" % "1.2.11",
      "com.typesafe.scala-logging" %% "scala-logging" % "3.9.4",
      "org.specs2" %% "specs2-core" % "4.15.0" % Test,
      "com.clever-cloud" %% "testcontainers-scala-warp10" % "2.0.0" % Test
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
    git.remoteRepo := "git@github.com:clevercloud/akka-warp10-scala-client.git",
  )
)
enablePlugins(GhpagesPlugin)
enablePlugins(SiteScaladocPlugin)
