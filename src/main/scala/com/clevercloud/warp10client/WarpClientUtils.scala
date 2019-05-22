package com.clevercloud.warp10client

import java.util.UUID

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

import akka.NotUsed
import akka.actor.ActorSystem
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.HttpHeader.ParsingResult
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Flow, Source}
import akka.util.ByteString

import com.clevercloud.warp10client.models.WarpConfiguration
import com.clevercloud.warp10client.WarpClientUtils.PoolClientFlow

object WarpClientUtils {
  type PoolClientFlow = Flow[(HttpRequest, UUID), (Try[HttpResponse], UUID), _]

  def readAllDataBytes(dataBytesSource: Source[ByteString, _])(implicit actorMaterializer: ActorMaterializer): Future[String] = {
    implicit val executionContext = actorMaterializer.system.dispatcher
    dataBytesSource
      .runFold(ByteString.empty) {
        case (seq, item) => seq ++ item
      }
      .map(_.decodeString("UTF-8"))
  }

  def decode: Flow[ByteString, String, NotUsed] = {
    Flow[ByteString]
      .map(_.decodeString("UTF-8"))
  }
}

case class WarpClientContext(
  configuration: WarpConfiguration,
  poolClientFlow: WarpClientUtils.PoolClientFlow,
  actorMaterializer: ActorMaterializer
) {
  implicit def implicitActorMaterializer: ActorMaterializer = actorMaterializer
  implicit def implicitActorSystem: ActorSystem = actorMaterializer.system
  implicit def implicitExecutionContext: ExecutionContext = actorMaterializer.system.dispatcher
  implicit def implicitPoolClientFlow: PoolClientFlow = poolClientFlow
  implicit def implicitWarpConfiguration: WarpConfiguration = configuration
}

case class WarpException(statusCode: Int, error: String) extends Exception(s"HTTP $statusCode: $error")

object `X-Warp10-Token` {
  def apply(value: String): HttpHeader = {
    HttpHeader.parse("X-Warp10-Token", value) match {
      case ParsingResult.Ok(httpHeader, _) => httpHeader
      case ParsingResult.Error(error) => throw WarpException(-1, s"${error.summary}: ${error.detail}.")
    }
  }
}
