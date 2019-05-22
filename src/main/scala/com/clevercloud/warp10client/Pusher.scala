package com.clevercloud.warp10client

import java.util.UUID

import scala.concurrent.Future
import scala.util.{Failure, Success}

import akka.NotUsed
import akka.http.scaladsl.model._
import akka.stream.scaladsl.Flow

import com.clevercloud.warp10client.models.gts_module.GTS

object Pusher {
  def push(writeToken: String)(
    implicit warpClientContext: WarpClientContext
  ): Flow[GTS, Unit, NotUsed] = {
    val uuid = UUID.randomUUID
    Flow[GTS]
      .map(gts => pushRequest(gts, writeToken))
      .map(request => (request -> uuid)) // cf. https://doc.akka.io/docs/akka-http/current/client-side/host-level.html
      .via(warpClientContext.poolClientFlow)
      .filter({ case (_, key) => key == uuid })
      .map({ case (responseTry, _) => responseTry })
      .mapAsync(1) {
        case Success(response) => processResponse(response)
        case Failure(e) => Future.failed(e)
      }
  }

  def pushSeq(writeToken: String)(
    implicit warpClientContext: WarpClientContext
  ): Flow[Seq[GTS], Unit, NotUsed] = {
    val uuid = UUID.randomUUID
    Flow[Seq[GTS]]
      .map(gtsSeq => pushSeqRequest(gtsSeq, writeToken))
      .map(request => (request -> uuid)) // cf. https://doc.akka.io/docs/akka-http/current/client-side/host-level.html
      .via(warpClientContext.poolClientFlow)
      .filter({ case (_, key) => key == uuid })
      .map({ case (responseTry, _) => responseTry })
      .mapAsync(1) {
        case Success(response) => processResponse(response)
        case Failure(e) => Future.failed(e)
      }
  }

  def pushSeqRequest(gtsSeq: Seq[GTS], writeToken: String)(
    implicit warpClientContext: WarpClientContext
  ) = {
    HttpRequest(
      method = HttpMethods.POST,
      uri = warpClientContext.configuration.pushUrl,
      headers = List(`X-Warp10-Token`(writeToken)),
      entity = HttpEntity(gtsSeq.map(_.serialize).mkString("\n"))
    )
  }

  def pushRequest(gts: GTS, writeToken: String)(
    implicit warpClientContext: WarpClientContext
  ) = {
    HttpRequest(
      method = HttpMethods.POST,
      uri = warpClientContext.configuration.pushUrl,
      headers = List(`X-Warp10-Token`(writeToken)),
      entity = HttpEntity(gts.serialize)
    )
  }

  def processResponse(httpResponse: HttpResponse)(
    implicit warpClientContext: WarpClientContext
  ): Future[Unit] = {
    import warpClientContext._

    if (httpResponse.status == StatusCodes.OK) {
      Future.successful(httpResponse.discardEntityBytes())
    } else {
      WarpClientUtils
        .readAllDataBytes(httpResponse.entity.dataBytes)
        .map(WarpException(httpResponse.status.intValue, _))
        .map(throw _)
    }
  }
}
