package com.clevercloud.warp10client

import java.util.UUID

import org.apache.pekko
import pekko.NotUsed
import pekko.actor.ActorSystem
import pekko.http.scaladsl.model.HttpHeader.ParsingResult
import pekko.http.scaladsl.model._
import pekko.stream.Materializer
import pekko.stream.scaladsl.{ Flow, Source }
import pekko.util.ByteString
import com.clevercloud.warp10client.WarpClientUtils.PoolClientFlow
import com.clevercloud.warp10client.models.WarpConfiguration

import scala.concurrent.{ ExecutionContext, Future }
import scala.util.Try

object WarpClientUtils {
  type PoolClientFlow = Flow[(HttpRequest, UUID), (Try[HttpResponse], UUID), ?]

  def readAllDataBytes(
      dataBytesSource: Source[ByteString, ?]
    )(implicit
      actorMaterializer: Materializer
    ): Future[String] = {
    given executionContext: ExecutionContext = actorMaterializer.system.dispatcher
    dataBytesSource.runFold(ByteString.empty)({ case (seq, item) => seq ++ item }).map(_.decodeString("UTF-8"))
  }

  def decode: Flow[ByteString, String, NotUsed] = {
    Flow[ByteString].map(_.decodeString("UTF-8"))
  }
}

case class WarpClientContext(
    configuration: WarpConfiguration,
    poolClientFlow: WarpClientUtils.PoolClientFlow,
    actorMaterializer: Materializer) {
  implicit def implicitActorMaterializer: Materializer = actorMaterializer
  implicit def implicitActorSystem: ActorSystem = actorMaterializer.system
  implicit def implicitExecutionContext: ExecutionContext = actorMaterializer.system.dispatcher
  implicit def implicitPoolClientFlow: PoolClientFlow = poolClientFlow
  implicit def implicitWarpConfiguration: WarpConfiguration = configuration
}

case class WarpException(error: String, line: Option[Int] = None) extends Exception(error)

object `X-Warp10-Token` {

  def apply(value: String): HttpHeader = {
    HttpHeader.parse("X-Warp10-Token", value) match {
      case ParsingResult.Ok(httpHeader, _) => httpHeader
      case ParsingResult.Error(error)      => throw WarpException(s"${error.summary}: ${error.detail}.")
    }
  }
}
