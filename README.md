[![Tests](https://github.com/clevercloud/warp10-scala-client/actions/workflows/ci.yml/badge.svg)](https://github.com/CleverCloud/warp10-scala-client/actions/workflows/ci.yml)

[![Central Version](https://img.shields.io/maven-central/v/com.clever-cloud/warp10-scala-client_3)](https://mvnrepository.com/artifact/com.clever-cloud/warp10-scala-client)

# Scala client for [Warp10 Geo/time series DB](http://www.warp10.io/).


## Documentation

Scaladoc is available [here](https://clevercloud.github.io/warp10-scala-client/latest/api/index.html).

```scala
// to generate documentation on gh-pages branch
sbt ghpagesPushSite
```

## Use it

Add the library dependency:

```scala
"com.clever-cloud" %% "warp10-scala-client" % "<version>"
```

## Configuration

```scala
import scala.concurrent.ExecutionContext

import org.apache.pekko
import pekko.actor._
import pekko.stream.Materializer

import com.clevercloud.warp10client._
import com.clevercloud.warp10client.models._
import com.clevercloud.warp10client.models.gts_module._

given executionContext: ExecutionContext = system.dispatchers.lookup("yourContext")
given warpConfiguration: WarpConfiguration = WarpConfiguration("www.clever-cloud.com")
val warpClient = Warp10Client("clever-cloud.com", 80)
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

## Pekko Streams usage

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
