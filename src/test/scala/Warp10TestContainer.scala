import com.clevercloud.testcontainers.scala.Warp10Container
import org.specs2.specification.BeforeAfterAll

trait Warp10TestContainer extends BeforeAfterAll {
  private val version = "2.7.5"
  private val container: Warp10Container = new Warp10Container(tag = version)

  container.start()

  val warp10_host: String = container.host
  val warp10_port: Int = container.httpPort
  val warp10_url: String = s"${container.protocol}://${container.httpHostAddress}"
  val warp10_write_token: String = container.writeToken
  val warp10_read_token: String = container.readToken

  override def beforeAll(): Unit = {}

  override def afterAll(): Unit = container.stop()
}
