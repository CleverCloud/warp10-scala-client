master: [![Build Status](https://travis-ci.org/CleverCloud/akka-warp10-scala-client.svg?branch=master)](https://travis-ci.org/CleverCloud/akka-warp10-scala-client)

# This is a scala client for [Warp10 Geo/time series DB](http://www.warp10.io/)

It is based on AKKA Stream.

It is a test project.

# Code examples

```
import akka.actor._
import akka.stream.ActorMaterializer

import com.clevercloud.warp10client._
import com.clevercloud.warp10client.models._
import com.clevercloud.warp10client.models.gts_module._

implicit val executionContext = system.dispatchers.lookup("yourContext")
implicit val actorMaterializer = ActorMaterializer()
implicit val warpConfiguration = WarpConfiguration("www.clever-cloud.com")
val warpClient = WarpClient("clever-cloud.com", 80)

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
    println(Json.toJson(gts)) // using play-json writers
}
```

Have a look at the test directory for functional tests and code examples
