[![Tests](https://github.com/clevercloud/akka-warp10-scala-client/actions/workflows/ci.yml/badge.svg)](https://github.com/CleverCloud/akka-warp10-scala-client/actions/workflows/ci.yml)

[![Central Version](https://img.shields.io/maven-central/v/com.clever-cloud/akka-warp10-scala-client_2.13)](https://mvnrepository.com/artifact/com.clever-cloud/akka-warp10-scala-client)
[![Nexus Version](https://img.shields.io/nexus/r/com.clever-cloud/akka-warp10-scala-client_2.13?server=https%3A%2F%2Fs01.oss.sonatype.org)](https://search.maven.org/artifact/com.clever-cloud/akka-warp10-scala-client)

# Scala client for [Warp10 Geo/time series DB](http://www.warp10.io/).


## Documentation

Scaladoc is available [here](https://clevercloud.github.io/akka-warp10-scala-client/latest/api/index.html).

```scala
// to generate documentation on gh-pages branch
sbt ghpagesPushSite
```

## Use it

Add the library dependency:

```scala
"com.clever-cloud" %% "akka-warp10-scala-client" % "<version>"
```

## Configuration

```scala
import akka.actor._
import akka.stream.Materializer

import com.clevercloud.warp10client._
import com.clevercloud.warp10client.models._
import com.clevercloud.warp10client.models.gts_module._

implicit val executionContext = system.dispatchers.lookup("yourContext")
implicit val actorMaterializer = Materializer.matFromSystem
implicit val warpConfiguration = WarpConfiguration("www.clever-cloud.com")
val warpClient = WarpClient("clever-cloud.com", 80)
```

## Classical usage

```scala
val labels = Map(
  "exactLabel=" -> "label1",
  "regexLabel~" -> "lab.*"
)

warpClient.fetch(
  "READ_TOKEN",
  Query(
    Selector("warpClass", labels),
    FetchRange(LocalDateTime.now.minusSeconds(100), LocalDateTime.now)
  )
).map { gts =>
  ...
}

warpClient.exec(s"""
  1 h 'duration' STORE
  NOW 'now' STORE
  [ '${token.token}' '~alert.http.status' { 'owner_id' '561bf859-b1ae-41bd-bd89-3421fbad0697' } $$now $$duration ] FETCH
  [ 0 1 ]
  SUBLIST
""").map { gtsList =>
  ...
}

warpClient.push(gts: GTS, "WRITE_TOKEN")
// or
warpClient.push(gts: Seq[GTS], "WRITE_TOKEN", batchSize = 300)
```

## Akka Streams usage

```scala
Flow[Query[FetchRange]]
  .map { gtsSequence =>
    ...
  }

Flow[WarpScript]
  .map { gtsSequence =>
    ...
  }

Flow[GTS]
  .via(warpClient.push("WRITE_TOKEN"))
```

Have a look at the test directory for functional tests and code examples
