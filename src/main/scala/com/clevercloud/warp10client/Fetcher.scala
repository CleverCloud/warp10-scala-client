package com.clevercloud.warp10client

import java.util.UUID

import scala.util.{Failure, Success, Try}

import akka.NotUsed
import akka.http.scaladsl.model._
import akka.stream.scaladsl.{Flow, Source}

import com.clevercloud.warp10client.models._
import com.clevercloud.warp10client.models.gts_module.GTS

object Fetcher {
  def fetch(readToken: String)(
    implicit warpClientContext: WarpClientContext
  ): Flow[Query[FetchRange], Seq[GTS], NotUsed] = {
    val uuid = UUID.randomUUID
    Flow[Query[FetchRange]]
      .map(query => fetchRequest(readToken, query))
      .map(request => (request -> uuid)) // cf. https://doc.akka.io/docs/akka-http/current/client-side/host-level.html
      .via(warpClientContext.poolClientFlow)
      .filter({ case (_, key) => key == uuid })
      .map({ case (responseTry, _) => responseTry })
      .via(processResponseTry)
      .via(stringToGTSSeq)
  }

  def fetchRequest(readToken: String, query: Query[FetchRange])(
    implicit warpClientContext: WarpClientContext
  ) = {
    HttpRequest(
      method = HttpMethods.GET,
      uri = warpClientContext.configuration.fetchUrl + "?" + query.serialize,
      headers = List(`X-Warp10-Token`(readToken))
    )
  }

  def processResponseTry(
    implicit warpClientContext: WarpClientContext
  ): Flow[Try[HttpResponse], String, NotUsed] = {
    import warpClientContext._

    Flow[Try[HttpResponse]]
      .flatMapConcat {
        case Success(httpResponse) => {
          if (httpResponse.status == StatusCodes.OK) {
            Source.fromFuture(
              WarpClientUtils
                .readAllDataBytes(httpResponse.entity.dataBytes)
            )
          } else {
            Source.fromFuture(
              WarpClientUtils
                .readAllDataBytes(httpResponse.entity.dataBytes)
                .map(content => WarpException(s"HTTP status: ${httpResponse.status.intValue.toString}: $content"))
                .map(throw _)
            )
          }
        }
        case Failure(e) => throw e
      }
  }

  def stringToGTSSeq: Flow[String, Seq[GTS], NotUsed] = {
    Flow[String]
      .map {
        GTS.parse(_) match {
          case Left(e) => throw WarpException(s"Can't parse GTS due to: $e")
          case Right(gtsList) => gtsList
        }
      }
  }
}
