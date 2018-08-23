package com.clevercloud.warp10client

import java.util.UUID

import scala.concurrent.Future

import akka.{Done, NotUsed}
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Flow, Sink, Source}

import com.clevercloud.warp10client.Executioner.WarpScript
import com.clevercloud.warp10client.models._
import com.clevercloud.warp10client.models.gts_module.GTS

object WarpClient {
  import WarpClientUtils._

  def apply(host: String, port: Int, scheme: String = "http")(
    implicit warpConfiguration: WarpConfiguration,
    actorSystem: ActorSystem,
    actorMaterializer: ActorMaterializer
  ): WarpClient = {
    if (scheme.equals("http")) {
      WarpClient(Http().cachedHostConnectionPool[UUID](host, port))
    } else {
      WarpClient(Http().cachedHostConnectionPoolHttps[UUID](host, port))
    }
  }

  def apply(poolClientFlow: PoolClientFlow)(
    implicit warpConfiguration: WarpConfiguration,
    actorMaterializer: ActorMaterializer
  ): WarpClient = {
    new WarpClient(WarpClientContext(
      warpConfiguration,
      poolClientFlow,
      actorMaterializer
    ))
  }
  
  def closePool()(implicit actorSystem: ActorSystem) = {
    Http().shutdownAllConnectionPools
  }
}

class WarpClient(warpContext: WarpClientContext) {
  import warpContext._

  def fetch(readToken: String): Flow[Query[FetchRange], Seq[GTS], NotUsed] = Fetcher.fetch(readToken)(warpContext)
  def fetch(readToken: String, query: Query[FetchRange]): Future[Seq[GTS]] = {
    Source
      .single(query)
      .via(fetch(readToken))
      .runWith(Sink.head)
  }

  def push(writeToken: String): Flow[GTS, Unit, NotUsed] = Pusher.push(writeToken)(warpContext)
  def push(gts: GTS, writeToken: String): Future[Done] = {
    Source
      .single(gts)
      .via(push(writeToken))
      .runForeach(_ => ())
  }
  def push(gtsSeq: Seq[GTS], writeToken: String, batchSize: Int = 100): Future[Done] = {
    Source
      .fromIterator(() => gtsSeq.grouped(batchSize))
      .via(Pusher.pushSeq(writeToken)(warpContext))
      .runForeach(_ => ())
  }

  def exec: Flow[WarpScript, Seq[GTS], NotUsed] = Executioner.exec()(warpContext)
  def exec(script: WarpScript): Future[Seq[GTS]] = {
    Source
      .single(script)
      .via(exec)
      .runWith(Sink.head)
  }
}
