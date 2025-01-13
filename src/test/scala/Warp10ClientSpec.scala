import java.time.*
import java.util.UUID
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration.Duration as Period
import scala.concurrent.duration.MILLISECONDS
import scala.concurrent.duration.SECONDS
import scala.util.{Failure, Success}
import org.apache.pekko
import pekko.actor.ActorSystem
import pekko.http.scaladsl.model.{HttpRequest, HttpResponse, StatusCodes}
import pekko.stream.Materializer
import pekko.stream.scaladsl.Flow
import com.clevercloud.warp10client.*
import com.clevercloud.warp10client.models.*
import com.clevercloud.warp10client.models.gts_module.*
import org.specs2.*
import org.specs2.matcher.MatchResult

class Warp10ClientSpec extends Specification with Warp10TestContainer {

  def is = s2"""
    This is a specification to check the Warp10 client

    The Warp10 client should

      Unit -> on sending simple data                                  $p1
      WarpException -> on invalid token                               $p2
      Unit -> on sending full data field and string value             $p4

      WarpException -> on fetch fail                                  $f1
      Seq[GTS] -> on fetch success                                    $f2

      Seq[GTS] empty -> on fetch success on empty data                $e1

      Unit -> on sending 3000GTS to real Warp10                       $p5
      WarpException -> on invalid data                                $p6

      Seq[GTS] contains data -> on fetch success                      $e2

      invalid WS throw comprehensive error                            $r3
  """

  val zonedNow: ZonedDateTime = ZonedDateTime.now
  val utcNow: LocalDateTime = LocalDateTime.ofInstant(zonedNow.toInstant, ZoneId.of("UTC"))
  val utcNowMilli: Long = utcNow.atZone(ZoneId.of("UTC")).toInstant.toEpochMilli
  val utcNowStartMicro: Long = s"${utcNowMilli}000".toLong
  val writeToken: String = warp10_write_token
  val readToken: String = warp10_read_token

  given actorSystem: ActorSystem = ActorSystem()
  given executionContext: ExecutionContext = actorSystem.dispatcher
  given warpConfiguration: WarpConfiguration = WarpConfiguration(warp10_url)

  // PUSH TESTS
  private def pushContext(
    )(implicit
      actorMaterializer: Materializer,
      executionContext: ExecutionContext
    ) = {
    WarpClientContext(
      poolClientFlow = {
        Flow[(HttpRequest, UUID)].mapAsync(1) {
          case (httpRequest, requestKey) => {
            WarpClientUtils
              .readAllDataBytes(httpRequest.entity.dataBytes)
              .map {
                // case x: String => println(x) ; Success(HttpResponse(StatusCodes.OK)) // use it to debug test
                case "1// testClass{} 73346576"                => Success(HttpResponse(StatusCodes.OK))
                case "3// testMultiple{lbl1=test,lbl2=test} 2" => Success(HttpResponse(StatusCodes.OK))
                case "4/1.0:-0.1/1 testFullDataField{lbl1=test,lbl2=test} 'string'" =>
                  Success(HttpResponse(StatusCodes.OK))
                case _ => Success(HttpResponse(StatusCodes.NotImplemented))
              }
              .map(httpResponse => (httpResponse, requestKey))
          }
        }
      },
      actorMaterializer = actorMaterializer,
      configuration = warpConfiguration
    )
  }

  val wPushClient = new Warp10Client(pushContext())

  val gtsPointSeq1: Seq[GTSPoint] = Seq(GTSPoint(Some(1.toLong), None, None, GTSLongValue(73346576)))
  val validSend_f: Future[Either[WarpException, Unit]] = wPushClient.push(GTS("testClass", Map.empty[String, String], gtsPointSeq1), writeToken)
  def p1 = Await.result(validSend_f, Period(1000, MILLISECONDS)) must beAnInstanceOf[Right[?, ?]]

  val gtsPointSeq2: Seq[GTSPoint] = Seq(GTSPoint(None, None, None, GTSLongValue(7)))

  val invalidTokenSend_f: Future[Either[WarpException, Unit]] = wPushClient.push(
    GTS("testFailClass{}", Map("label1" -> "dsfF3", "label2" -> "dsfg"), gtsPointSeq2),
    "invalid_write_token"
  )
  def p2 = Await.result(invalidTokenSend_f, Period(1000, MILLISECONDS)) must beAnInstanceOf[Left[?, ?]]

  val gtsPointSeq4: Seq[GTSPoint] = Seq(
    GTSPoint(Some(4.toLong), Some(Coordinates(1.0.toDouble, -0.1.toDouble)), Some(1.toLong), GTSStringValue("string"))
  )

  val fullDataFieldSend_f: Future[Either[WarpException, Unit]] =
    wPushClient.push(GTS("testFullDataField", Map("lbl1" -> "test", "lbl2" -> "test"), gtsPointSeq4), writeToken)
  def p4 = Await.result(fullDataFieldSend_f, Period(1000, MILLISECONDS)) must beAnInstanceOf[Right[?, ?]]

  // FETCH TESTS
  private def fetchContext(
    )(implicit
      actorMaterializer: Materializer
    ) = {
    WarpClientContext(
      poolClientFlow = Flow[(HttpRequest, UUID)].map {
        case (httpRequest, requestKey) => {
          (
            httpRequest.uri.rawQueryString match {
              // case Some(x) => println(x.toString) ; Success(HttpResponse(StatusCodes.OK)) // use it to debug
              case Some(query) if query.contains("selector=test") => {
                Success(
                  HttpResponse(
                    StatusCodes.OK,
                    entity = """1434590504// test{steps=101} -0.6133061918698982
                  |=1434590288// 0.9228427144511169
                  |=1434590072// -0.1301889411087915
                  |1434590504// test{steps=102} -0.6133061918698982
                  |=1434590288// 0.9228427144511169
                  |=1434590072// -0.1301889411087915
                  |1434590504// test{steps=103} -0.6133061918698982
                  |=1434590288// 0.9228427144511169
                  |=1434590072// -0.1301889411087915""".stripMargin
                  )
                )
              }
              case Some(query) if query.contains("selector=fail") =>
                Failure(new WarpException("No data for `selector=fail`."))
              case _ => Success(HttpResponse(StatusCodes.BadRequest, entity = "The request is invalid"))
            },
            requestKey
          )
        }
      },
      actorMaterializer = actorMaterializer,
      configuration = warpConfiguration
    )
  }

  val wFetchClient = new Warp10Client(fetchContext())

  val gtsPointForSeq: Seq[GTSPoint] = Seq(
    GTSPoint(Some(1434590504.toLong), None, None, GTSDoubleValue(-0.6133061918698982.toDouble)),
    GTSPoint(Some(1434590288.toLong), None, None, GTSDoubleValue(0.9228427144511169.toDouble)),
    GTSPoint(Some(1434590072.toLong), None, None, GTSDoubleValue(-0.1301889411087915.toDouble))
  )

  val realSeq: Seq[GTS] = Seq(
    GTS("test", Map("steps" -> "101"), gtsPointForSeq),
    GTS("test", Map("steps" -> "102"), gtsPointForSeq),
    GTS("test", Map("steps" -> "103"), gtsPointForSeq)
  )

  val failFetch_f: Future[Either[WarpException, Seq[GTS]]] = wFetchClient.fetch(readToken, Query(Selector("fail"), FetchRange(utcNow, 1000.toLong)))
  def f1: MatchResult[Either[WarpException, Seq[GTS]]] = Await.result(failFetch_f, Period(1000, MILLISECONDS)) must beAnInstanceOf[Left[?, ?]]

  val validFetch_f: Future[Either[WarpException, Seq[GTS]]] = wFetchClient.fetch(readToken, Query(Selector("test"), FetchRange(utcNow, 1000.toLong)))
  def f2: MatchResult[Either[WarpException, Seq[GTS]]] = Await.result(validFetch_f, Period(1000, MILLISECONDS)) must beAnInstanceOf[Right[?, ?]]

  // PUSH 10 000 GTS to real Warp10
  val realWarpClient: Warp10Client = WarpClient(warp10_host, warp10_port)

  // check no data
  def e1: MatchResult[Either[WarpException, Seq[GTS]]] = Await.result(
    realWarpClient.fetch(
      readToken,
      Query(
        Selector("accessLogs", Map(".app=" -> "test")),
        FetchRange(LocalDateTime.now.minusSeconds(20), LocalDateTime.now)
      )
    ),
    Period(10000, MILLISECONDS)
  ) must beAnInstanceOf[Right[?, ?]]

  // push 3000 points
  val realSeqRangedFetch: Seq[GTS] = (1 to 3000) map { i =>
    GTS(
      "rangedFetchTest",
      Map(".app=" -> "test"),
      Seq(GTSPoint(Some(utcNowStartMicro - (i * 1L)), None, None, GTSStringValue(s"J$i")))
    )
  }

  val validHugePush_p: Future[Either[WarpException, Unit]] = realWarpClient.push(realSeqRangedFetch, writeToken)
  def p5: MatchResult[Either[WarpException, Unit]] = Await.result(validHugePush_p, Period(100000, MILLISECONDS)) must beAnInstanceOf[Right[?, ?]]

  val gtsPointSeq6: Seq[GTSPoint] = Seq(
    GTSPoint(Some(1.toLong), Some(Coordinates(lat = 3.333, lon = 4.444)), None, GTSLongValue(73346576))
  )
  val invalidSend6: Future[Either[WarpException, Unit]] = realWarpClient.push(GTS("testClass", Map.empty[String, String], gtsPointSeq6), writeToken)
  def p6: MatchResult[Either[WarpException, Unit]] = Await.result(invalidSend6, Period(10000, MILLISECONDS)) must beAnInstanceOf[Right[?, ?]]

  def e2: MatchResult[Either[WarpException, Seq[GTS]]] = Await.result(
    realWarpClient.fetch(
      readToken,
      Query(
        Selector("rangedFetchTest", Map(".app=" -> "test")),
        FetchRange(utcNowStartMicro - 10000000L, utcNowStartMicro)
      )
    ),
    Period(10000, MILLISECONDS)
  ) must beAnInstanceOf[Right[?, ?]]

  def e3: Warp10Stack = Await.result(realWarpClient.execStack("fdsfds"), Period(10, SECONDS))
  val r3: MatchResult[Warp10Stack] = e3 must throwA[WarpException].like {
    case e: WarpException =>
      e.error must beEqualTo("Exception at '=>fdsfds<=' in section [TOP] (Unknown function 'fdsfds')")
      e.line.getOrElse(0) must beEqualTo(1)

  }

  // private def getNbGTSPoints(gtsSeq: Seq[GTS]): Int = gtsSeq.map(_.points.size).sum

  // close http pool
  WarpClient.closePool()
}
