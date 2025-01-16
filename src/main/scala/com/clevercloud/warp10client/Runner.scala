package com.clevercloud.warp10client

import com.clevercloud.warp10client.models.Warp10Stack

import java.util.UUID
import scala.util.{ Failure, Success, Try }
import org.apache.pekko
import pekko.NotUsed
import pekko.http.scaladsl.model.*
import pekko.http.scaladsl.model.StatusCodes.{OK, InternalServerError}
import pekko.stream.scaladsl.{ Flow, Source }
import io.circe.*
import io.circe.parser.*
import com.clevercloud.warp10client.models.gts_module.*

import scala.concurrent.Future

object Runner {
  type WarpScript = String

  def exec(
    )(implicit
      warpClientContext: WarpClientContext
    ): Flow[WarpScript, String, NotUsed] = {
    val uuid = UUID.randomUUID
    Flow[WarpScript]
      .map(script => execRequest(script))
      .map(request => (request -> uuid))
      .via(warpClientContext.poolClientFlow)
      .filter({ case (_, key) => key == uuid })
      .map({ case (responseTry, _) => responseTry })
      .via(processResponseTry)
  }

  private def execRequest(
      script: WarpScript
    )(implicit
      warpClientContext: WarpClientContext
    ) = HttpRequest(
    method = HttpMethods.POST,
    uri = warpClientContext.configuration.execUrl,
    entity = HttpEntity(script)
  )

  def processResponseTry(
      implicit
      warpClientContext: WarpClientContext
    ): Flow[Try[HttpResponse], String, NotUsed] = {
    import warpClientContext._

    Flow[Try[HttpResponse]].flatMapConcat {
      case Success(httpResponse) =>
        Source.future(
          httpResponse.status match
            case OK => WarpClientUtils.readAllDataBytes(httpResponse.entity.dataBytes)
            case InternalServerError =>
              // In case of 500, we try to read headers which are human-readable and fallback on response body which is HTML
              val error: Option[String] = httpResponse.headers.findLast(_.is("x-warp10-error-message")).map(_.value)
              val line: Option[Int] = httpResponse
                .headers
                .findLast(_.is("x-warp10-error-line"))
                .map(_.value)
                .flatMap(_.toIntOption)

              error match
                case Some(err) => Future.successful(throw WarpException(err, line))
                case None => WarpClientUtils
                  .readAllDataBytes(httpResponse.entity.dataBytes)
                  .map(WarpException(_, line))
                  .map(throw _)
            case status => WarpClientUtils
                .readAllDataBytes(httpResponse.entity.dataBytes)
                .map(content => WarpException(s"HTTP status: ${status.intValue.toString}: $content"))
                .map(throw _)
          )
      case Failure(e) => throw e
    }
  }

  private def parseJson: Flow[String, Json, NotUsed] = {
    Flow[String].map { s =>
      parse(s) match {
        case Right(json) => json
        case Left(e)     => throw WarpException(s"Error on parsing: $e")
      }
    }
  }

  def jsonToGTSSeq(): Flow[String, Seq[GTS], NotUsed] = {
    Flow[String]
      .via(parseJson)
      .map { json => json.hcursor.downArray } // warp response contains [[]] or [{}] so we drop an array level
      .map { array => // global array with all matching script
        array.values
          .getOrElse(throw WarpException(s"No data to parse."))
          .map { series => // http://www.warp10.io/apis/gts-output-format/
            val `class` =
              series.hcursor.getOrElse[String]("c")("").getOrElse(throw WarpException("Can't parse `class` value."))
            val labels = series.hcursor
              .getOrElse[Map[String, String]]("l")(Map.empty[String, String])
              .getOrElse(throw WarpException("Cant parse `labels` value."))
            GTS(
              classname = `class`,
              labels = labels,
              points = (series \\ "v").flatMap { seriesContentArrays => // [[point_1], [point_2], ...]
                seriesContentArrays.asArray.get.map { point => // [point_i]
                  point.asArray.get match { // [timestamp, lat, lon, elev, value] is point's content
                    case Vector(timestamp: Json, value: Json) => {
                      GTSPoint(
                        timestamp.asNumber.get.toLong,
                        None,
                        None,
                        GTSValue.parse(value) match {
                          case Right(gtsPoint) => gtsPoint
                          case Left(e) => GTSStringValue(s"${e.toString}: ${value.toString}.")
                        }
                      )
                    }
                    case Vector(timestamp: Json, elevation: Json, value: Json) => {
                      GTSPoint(
                        timestamp.asNumber.get.toLong,
                        None,
                        elevation.asNumber.get.toLong,
                        GTSValue.parse(value) match {
                          case Right(gtsPoint) => gtsPoint
                          case Left(e) => GTSStringValue(s"${e.toString}: ${value.toString}.")
                        }
                      )
                    }
                    case Vector(timestamp: Json, latitude: Json, longitude: Json, value: Json) => {
                      GTSPoint(
                        timestamp.asNumber.get.toLong,
                        Some(Coordinates(latitude.asNumber.get.toDouble, longitude.asNumber.get.toDouble)),
                        None,
                        GTSValue.parse(value) match {
                          case Right(gtsPoint) => gtsPoint
                          case Left(e) => GTSStringValue(s"${e.toString}: ${value.toString}.")
                        }
                      )
                    }
                    case Vector(timestamp: Json, latitude: Json, longitude: Json, elevation: Json, value: Json) => {
                      GTSPoint(
                        timestamp.asNumber.get.toLong,
                        Some(Coordinates(latitude.asNumber.get.toDouble, longitude.asNumber.get.toDouble)),
                        elevation.asNumber.get.toLong,
                        GTSValue.parse(value) match {
                          case Right(gtsPoint) => gtsPoint
                          case Left(e) => GTSStringValue(s"${e.toString}: ${value.toString}.")
                        }
                      )
                    }
                    case _ => throw WarpException("invalid datapoint")
                  }
                }
              }
            )
          }
          .toSeq
      }
  }

  def jsonToStack(): Flow[String, Warp10Stack, NotUsed] = Flow[String].via(parseJson).map(Warp10Stack.apply)
}
