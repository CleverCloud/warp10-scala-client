import com.clevercloud.warp10client.models.gts_module._
import org.specs2._

import scala.collection.immutable.ListMap

class GTSSpec extends Specification {
  def is = s2"""
    This is a specification to check the GTS module

    The GTS module should
      Serialize GTSMacroValue $g1
      Parse GTSMacroValue $g2
  """

  val gts = GTSMacroValue(
    "m",
    "macro",
    ListMap(
      "s" -> "12.12.12.12",
      "i1" -> 10,
      "l" -> 2L,
      "i2" -> 20,
      "d" -> 3.2,
      "b1" -> false,
      "b2" -> true,
    )
  )
  val serialized = ":m:macro:{'s' '12.12.12.12' 'i1' 10 'l' 2 'i2' 20 'd' 3.2 'b1' false 'b2' true}"

  def g1 = gts.serialize must beEqualTo(serialized)

  // not implemented
  def g2 = GTSValue.parse(serialized) must beAnInstanceOf[Left[_, _]]
}
