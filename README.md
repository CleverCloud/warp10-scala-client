[![Build Status](https://travis-ci.org/CleverCloud/akka-warp10-scala-client.svg?branch=master)](https://travis-ci.org/CleverCloud/akka-warp10-scala-client) [![Bintray Version](https://img.shields.io/bintray/v/clevercloud/maven/akka-warp10-scala-client.svg)](https://bintray.com/clevercloud/maven/akka-warp10-scala-client#)

# Scala client for [Warp10 Geo/time series DB](http://www.warp10.io/).

## Use it

First, add this resolver to your `build.sbt`.

```scala
"clevercloud-bintray" at "http://dl.bintray.com/clevercloud/maven",
```

Second, add the library dependency:

```scala
"com.clevercloud" %% "akka-warp10-scala-client" % "1.3.0"
```

## Configuration

```scala
import akka.actor._
import akka.stream.ActorMaterializer

import com.clevercloud.warp10client._
import com.clevercloud.warp10client.models._
import com.clevercloud.warp10client.models.gts_module._

implicit val executionContext = system.dispatchers.lookup("yourContext")
implicit val actorMaterializer = ActorMaterializer()
implicit val warpConfiguration = WarpConfiguration("www.clever-cloud.com")
val warpClient = WarpClient("clever-cloud.com", 80)
```

## Classical usage

```scala
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
