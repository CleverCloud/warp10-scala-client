package com.clevercloud.warp10client

import java.util.UUID
import scala.concurrent.Future
import scala.util.{ Failure, Success }
import org.apache.pekko
import pekko.NotUsed
import pekko.http.scaladsl.model._
import pekko.stream.scaladsl.Flow
import org.apache.commons.text.StringEscapeUtils
import com.clevercloud.warp10client.models.gts_module.GTS

object Pusher {

  def push(
      writeToken: String
    )(using
      warpClientContext: WarpClientContext
    ): Flow[GTS, Future[Either[WarpException, Unit]], NotUsed] = {
    val uuid = UUID.randomUUID
    Flow[GTS]
      .map { gts => pushRequest(gts, writeToken) }
      .map{ request => (request -> uuid) } // cf. https://doc.pekko.io/docs/pekko-http/current/client-side/host-level.html
      .via(warpClientContext.poolClientFlow)
      .filter({ case (_, key) => key == uuid })
      .map({ case (responseTry, _) => responseTry })
      .map {
        case Success(response) => processResponse(response)
        case Failure(e)        => Future.successful(Left(WarpException(s"Error: $e")))
      }
  }

  def pushSeq(writeToken: String)(using warpClientContext: WarpClientContext): Flow[Seq[GTS], Future[Either[WarpException, Unit]], NotUsed] = {
    val uuid = UUID.randomUUID
    Flow[Seq[GTS]]
      .map { gtsSeq => pushSeqRequest(gtsSeq, writeToken) }
      .map {request => (request -> uuid) } // cf. https://doc.pekko.io/docs/pekko-http/current/client-side/host-level.html
      .via(warpClientContext.poolClientFlow)
      .filter({ case (_, key) => key == uuid })
      .map({ case (responseTry, _) => responseTry })
      .map {
        case Success(response) => processResponse(response)
        case Failure(e)        => Future.successful(Left(WarpException(s"Error: $e")))
      }
  }

  def pushSeqRequest(gtsSeq: Seq[GTS], writeToken: String)(using warpClientContext: WarpClientContext) = HttpRequest(
      method = HttpMethods.POST,
      uri = warpClientContext.configuration.pushUrl,
      headers = List(`X-Warp10-Token`(writeToken)),
      entity = HttpEntity(gtsSeq.map(_.serialize).mkString("\n"))
    )

  def pushRequest(gts: GTS, writeToken: String)(using warpClientContext: WarpClientContext) = HttpRequest(
      method = HttpMethods.POST,
      uri = warpClientContext.configuration.pushUrl,
      headers = List(`X-Warp10-Token`(writeToken)),
      entity = HttpEntity(gts.serialize)
    )

  def processResponse(httpResponse: HttpResponse)(using warpClientContext: WarpClientContext): Future[Either[WarpException, Unit]] = {
    import warpClientContext._

    if (httpResponse.status == StatusCodes.OK) {
      Future.successful(Right(httpResponse.discardEntityBytes()))
    } else {
      WarpClientUtils.readAllDataBytes(httpResponse.entity.dataBytes).map { content =>
        val escapedContent = StringEscapeUtils.unescapeXml(content)
        Left(WarpException(s"HTTP status: ${httpResponse.status.intValue.toString}: $escapedContent"))
      }
    }
  }
}
