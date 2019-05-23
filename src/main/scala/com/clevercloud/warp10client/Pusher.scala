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
  ): Flow[GTS, Future[Either[WarpException, Unit]], NotUsed] = {
    val uuid = UUID.randomUUID
    Flow[GTS]
      .map(gts => pushRequest(gts, writeToken))
      .map(request => (request -> uuid)) // cf. https://doc.akka.io/docs/akka-http/current/client-side/host-level.html
      .via(warpClientContext.poolClientFlow)
      .filter({ case (_, key) => key == uuid })
      .map({ case (responseTry, _) => responseTry })
      .map {
        case Success(response) => processResponse(response)
        case Failure(e) => Future.successful(Left(WarpException(s"Error: $e")))
      }
  }

  def pushSeq(writeToken: String)(
    implicit warpClientContext: WarpClientContext
  ): Flow[Seq[GTS], Future[Either[WarpException, Unit]], NotUsed] = {
    val uuid = UUID.randomUUID
    Flow[Seq[GTS]]
      .map(gtsSeq => pushSeqRequest(gtsSeq, writeToken))
      .map(request => (request -> uuid)) // cf. https://doc.akka.io/docs/akka-http/current/client-side/host-level.html
      .via(warpClientContext.poolClientFlow)
      .filter({ case (_, key) => key == uuid })
      .map({ case (responseTry, _) => responseTry })
      .map {
        case Success(response) => processResponse(response)
        case Failure(e) => Future.successful(Left(WarpException(s"Error: $e")))
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
  ): Future[Either[WarpException, Unit]] = {
    import warpClientContext._

    if (httpResponse.status == StatusCodes.OK) {
      Future.successful(Right(httpResponse.discardEntityBytes()))
    } else {
      WarpClientUtils
        .readAllDataBytes(httpResponse.entity.dataBytes)
        .map(content => Left(WarpException(s"HTTP status: ${httpResponse.status.intValue.toString}: $content")))
    }
  }
}
