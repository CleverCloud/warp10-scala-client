import org.apache.pekko
import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.http.scaladsl.Http
import org.apache.pekko.http.scaladsl.model.{ HttpMethod, HttpRequest, HttpResponse, Uri }
import org.apache.pekko.stream.scaladsl.{ Sink, Source }

import scala.concurrent.Future

/**
 * Mock Akka HTTP Server, will handle requests which is provided with handle request
 */
object MockServer {

  val interface = "localhost"
  val port = 8888

  def handleRequest(
      method: HttpMethod,
      uri: Uri,
      response: HttpResponse
      // httpRequest: HttpRequest
    )(implicit
      system: ActorSystem
    ): Future[Http.ServerBinding] = {

    val serverSource: Source[Http.IncomingConnection, Future[Http.ServerBinding]] =
      Http().newServerAt(interface, port).connectionSource()

    // val requestPath = httpRequest.uri.path.toString()

    val requestHandler: HttpRequest => HttpResponse = {
      case HttpRequest(method, uri, _, _, _) => // Uri.Path(`requestPath`)
        response
      case _: HttpRequest =>
        HttpResponse(404, entity = "Unknown resource!")
    }

    serverSource
      .to(Sink.foreach { connection =>
        println("Mock Server accepted new connection from " + connection.remoteAddress)
        connection handleWithSyncHandler requestHandler
      })
      .run()
  }
}
