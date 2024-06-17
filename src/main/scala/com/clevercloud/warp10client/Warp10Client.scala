package com.clevercloud.warp10client

import java.util.UUID
import org.apache.pekko
import pekko.NotUsed
import pekko.actor.ActorSystem
import pekko.http.scaladsl.Http
import pekko.stream.Materializer
import pekko.stream.scaladsl.{ Flow, Sink, Source }
import com.clevercloud.warp10client.Runner.{ WarpScript, jsonToGTSSeq, jsonToStack }
import com.clevercloud.warp10client.models.*
import com.clevercloud.warp10client.models.gts_module.GTS

import scala.concurrent.Future

object WarpClient {
  import WarpClientUtils._

  def apply(
      host: String,
      port: Int,
      scheme: String = "http"
    )(implicit
      warpConfiguration: WarpConfiguration,
      actorSystem: ActorSystem,
      actorMaterializer: Materializer
    ): Warp10Client = {
    if (scheme.equals("http")) {
      WarpClient(Http().cachedHostConnectionPool[UUID](host, port))
    } else {
      WarpClient(Http().cachedHostConnectionPoolHttps[UUID](host, port))
    }
  }

  def apply(
      poolClientFlow: PoolClientFlow
    )(implicit
      warpConfiguration: WarpConfiguration,
      actorMaterializer: Materializer
    ): Warp10Client = {
    new Warp10Client(
      WarpClientContext(
        warpConfiguration,
        poolClientFlow,
        actorMaterializer
      )
    )
  }

  def closePool(
    )(implicit
      actorSystem: ActorSystem
    ): Future[Unit] = {
    Http().shutdownAllConnectionPools()
  }
}

class Warp10Client(warpContext: WarpClientContext) {
  import warpContext._

  def fetch(readToken: String): Flow[Query[FetchRange], Future[Either[WarpException, Seq[GTS]]], NotUsed] =
    Fetcher.fetch(readToken)(warpContext)

  def fetch(readToken: String, query: Query[FetchRange]): Future[Either[WarpException, Seq[GTS]]] = {
    Source
      .single(query)
      .via(fetch(readToken))
      .runWith(
        Sink.fold[Future[Either[WarpException, Seq[GTS]]], Future[Either[WarpException, Seq[GTS]]]](
          Future.successful(Right(Seq.empty[GTS]))
        )((a, b) => a.flatMap(_ => b))
      )
      .flatten
  }

  def pushSeq(writeToken: String): Flow[Seq[GTS], Future[Either[WarpException, Unit]], NotUsed] =
    Pusher.pushSeq(writeToken)(warpContext)

  def push(writeToken: String): Flow[GTS, Future[Either[WarpException, Unit]], NotUsed] =
    Pusher.push(writeToken)(warpContext)

  def push(gts: GTS, writeToken: String): Future[Either[WarpException, Unit]] = {
    Source
      .single(gts)
      .via(push(writeToken))
      .runWith(
        Sink.fold[Future[Either[WarpException, Unit]], Future[Either[WarpException, Unit]]](
          Future.successful(Right(()))
        )((a, b) => a.flatMap(_ => b))
      )
      .flatten
  }

  def push(gtsSeq: Seq[GTS], writeToken: String, batchSize: Int = 100): Future[Either[WarpException, Unit]] = {
    Source
      .fromIterator(() => gtsSeq.grouped(batchSize))
      .via(Pusher.pushSeq(writeToken)(warpContext))
      .runWith(
        Sink.fold[Future[Either[WarpException, Unit]], Future[Either[WarpException, Unit]]](
          Future.successful(Right(()))
        )((a, b) => a.flatMap(_ => b))
      )
      .flatten
  }

  def exec: Flow[WarpScript, Seq[GTS], NotUsed] = Runner.exec()(warpContext).via(jsonToGTSSeq())
  def execStack: Flow[WarpScript, Warp10Stack, NotUsed] = Runner.exec()(warpContext).via(jsonToStack())

  def execStack(script: WarpScript): Future[Warp10Stack] = Source.single(script).via(execStack).runWith(Sink.head)

  def exec(script: WarpScript): Future[Seq[GTS]] = {
    Source.single(script).via(exec).runWith(Sink.head)
  }
}
