import com.clevercloud.testcontainers.scala.Warp10Container
import org.scalatest.{ BeforeAndAfterAll, Suite }

trait Warp10TestContainer extends Suite with BeforeAndAfterAll {

  private val warp10Version = "2.7.5"

  private val warp10_container: Warp10Container = new Warp10Container(tag = warp10Version)

  warp10_container.start()

  val warp10_url: String = s"${warp10_container.protocol}://${warp10_container.httpHostAddress}/api/v0"

  val warp10_admin_token: String = warp10_container.writeToken

  override protected def afterAll(): Unit = {
    warp10_container.stop()
    super.afterAll()
  }
}
