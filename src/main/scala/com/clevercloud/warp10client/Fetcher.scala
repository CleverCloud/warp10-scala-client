package com.clevercloud.warp10client

import java.util.UUID
import scala.concurrent.Future
import scala.util.{ Failure, Success }

import org.apache.pekko
import pekko.NotUsed
import pekko.http.scaladsl.model._
import pekko.stream.scaladsl.Flow
import com.typesafe.scalalogging.Logger
import org.apache.commons.text.StringEscapeUtils
import org.slf4j.LoggerFactory

import com.clevercloud.warp10client.models._
import com.clevercloud.warp10client.models.gts_module.GTS

object Fetcher {
  val log = Logger(LoggerFactory.getLogger("Fetcher"))

  def fetch(
      readToken: String
    )(implicit
      warpClientContext: WarpClientContext
    ): Flow[Query[FetchRange], Future[Either[WarpException, Seq[GTS]]], NotUsed] = {
    val uuid = UUID.randomUUID
    Flow[Query[FetchRange]]
      .map(query => fetchRequest(readToken, query))
      .map(request => (request -> uuid)) // cf. https://doc.pekko.io/docs/pekko-http/current/client-side/host-level.html
      .via(warpClientContext.poolClientFlow)
      .filter({ case (_, key) => key == uuid })
      .map({ case (responseTry, _) => responseTry })
      .map {
        case Success(response) => processResponse(response)
        case Failure(e)        => Future.successful(Left(WarpException(s"Error: $e")))
      }
  }

  def fetchRequest(
      readToken: String,
      query: Query[FetchRange]
    )(implicit
      warpClientContext: WarpClientContext
    ) = {
    log.debug(s"[FETCHER] sending ${warpClientContext.configuration.fetchUrl}?${query.serialize}")
    HttpRequest(
      method = HttpMethods.GET,
      uri = warpClientContext.configuration.fetchUrl + "?" + query.serialize,
      headers = List(`X-Warp10-Token`(readToken))
    )
  }

  def processResponse(
      httpResponse: HttpResponse
    )(implicit
      warpClientContext: WarpClientContext
    ): Future[Either[WarpException, List[GTS]]] = {
    import warpClientContext._

    if (httpResponse.status == StatusCodes.OK) {
      WarpClientUtils.readAllDataBytes(httpResponse.entity.dataBytes).map { data =>
        if (data.size > 0) {
          log.debug(s"[FETCHER] data provided, let's parse them")
          GTS.parse(data) match {
            case Left(e) => {
              log.error(s"[FETCHER] can't parse GTS due to: ${e.toString()}")
              throw WarpException(s"[FETCHER] can't parse GTS due to: $e")
            }
            case Right(gtsList) => Right(gtsList)
          }
        } else {
          log.debug(s"[FETCHER] empty data provided, let's return empty List()")
          Right(List())
        }
      }
    } else {
      WarpClientUtils.readAllDataBytes(httpResponse.entity.dataBytes).map { content =>
        val escapedContent = StringEscapeUtils.unescapeXml(content)
        log.error(s"[FETCHER] HTTP status: ${httpResponse.status.intValue.toString}: $escapedContent")
        Left(WarpException(s"[FETCHER] HTTP status: ${httpResponse.status.intValue.toString}: $escapedContent"))
      }
    }
  }

  def stringToGTSSeq: Flow[String, Seq[GTS], NotUsed] = {
    Flow[String].map { data =>
      if (data.size > 0) {
        log.debug(s"Data provided, let's parse them")
        GTS.parse(data) match {
          case Left(e) => {
            log.error(s"Can't parse GTS due to: ${e.toString()}")
            throw WarpException(s"Can't parse GTS due to: $e")
          }
          case Right(gtsList) => gtsList
        }
      } else {
        log.debug(s"Empty data provided, let's return empty Seq()")
        Seq()
      }
    }
  }
}
