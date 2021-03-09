package com.clevercloud.warp10client

import java.util.UUID
import scala.concurrent.Future
import scala.util.{Failure, Success}

import akka.NotUsed
import akka.http.scaladsl.model._
import akka.stream.scaladsl.Flow
import com.typesafe.scalalogging.Logger
import org.apache.commons.text.StringEscapeUtils
import org.slf4j.LoggerFactory

import com.clevercloud.warp10client.models._
import com.clevercloud.warp10client.models.gts_module.GTS

object Fetcher {
  val log = Logger(LoggerFactory.getLogger("Fetcher"))

  def fetch(readToken: String)(
    implicit warpClientContext: WarpClientContext
  ): Flow[Query[FetchRange], Future[Either[WarpException, Seq[GTS]]], NotUsed] = {
    val uuid = UUID.randomUUID
    Flow[Query[FetchRange]]
      .map(query => fetchRequest(readToken, query))
      .map(request => (request -> uuid)) // cf. https://doc.akka.io/docs/akka-http/current/client-side/host-level.html
      .via(warpClientContext.poolClientFlow)
      .filter({ case (_, key) => key == uuid })
      .map({ case (responseTry, _) => responseTry })
      .map {
        case Success(response) => processResponse(response)
        case Failure(e) => Future.successful(Left(WarpException(s"Error: $e")))
      }
  }

  def fetchRequest(readToken: String, query: Query[FetchRange])(
    implicit warpClientContext: WarpClientContext
  ) = {
    log.debug(s"[FETCHER] sending ${warpClientContext.configuration.fetchUrl}?${query.serialize}")
    HttpRequest(
      method = HttpMethods.GET,
      uri = warpClientContext.configuration.fetchUrl + "?" + query.serialize,
      headers = List(`X-Warp10-Token`(readToken))
    )
  }

  def processResponse(httpResponse: HttpResponse)(
    implicit warpClientContext: WarpClientContext
  ): Future[Either[WarpException, List[GTS]]] = {
    import warpClientContext._

    if (httpResponse.status == StatusCodes.OK) {
      WarpClientUtils
        .readAllDataBytes(httpResponse.entity.dataBytes)
        .map { data =>
          if (data.size > 0) {
            GTS.parse(data) match {
              case Left(e) => {
                log.error(s"[FETCHER] can't parse GTS due to: ${e.toString()}")
                throw WarpException(s"[FETCHER] can't parse GTS due to: $e")
              }
              case Right(gtsList) => Right(gtsList)
            }
          } else {
            Right(List())
          }
        }
    } else {
      WarpClientUtils
        .readAllDataBytes(httpResponse.entity.dataBytes)
        .map { content =>
          val escapedContent = StringEscapeUtils.unescapeXml(content)
          log.error(s"[FETCHER] HTTP status: ${httpResponse.status.intValue.toString}: $escapedContent")
          Left(WarpException(s"[FETCHER] HTTP status: ${httpResponse.status.intValue.toString}: $escapedContent"))
        }
    }
  }

  def stringToGTSSeq: Flow[String, Seq[GTS], NotUsed] = {
    Flow[String]
      .map {
        GTS.parse(_) match {
          case Left(e) => {
            log.error(s"[FETCHER] can't parse GTS due to: ${e.toString()}")
            throw WarpException(s"[FETCHER] can't parse GTS due to: $e")
          }
          case Right(gtsList) => gtsList
        }
      }
  }
}
