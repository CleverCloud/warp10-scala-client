package com.clevercloud.warp10client

import com.clevercloud.warp10client.models.gts_module.*
import org.specs2.*
import org.specs2.matcher.MatchResult

import scala.collection.immutable.ListMap

class GTSSpec extends Specification {

  def is = s2"""
    This is a specification to check the GTS module

    The GTS module should
      Serialize GTSMacroValue $g1
      Parse GTSMacroValue $g2
  """

  val gts: GTSMacroValue = GTSMacroValue(
    "m",
    "macro",
    ListMap(
      "s" -> "12.12.12.12",
      "i1" -> 10,
      "l" -> 2L,
      "i2" -> 20,
      "d" -> 3.2,
      "b1" -> false,
      "b2" -> true
    )
  )
  val serialized = ":m:macro:{'s' '12.12.12.12' 'i1' 10 'l' 2 'i2' 20 'd' 3.2 'b1' false 'b2' true}"

  def g1: MatchResult[String] = gts.serialize must beEqualTo(serialized)

  // not implemented
  def g2: MatchResult[Either[gts_errors.InvalidGTSPointFormat, GTSValue]] =
    GTSValue.parse(serialized) must beAnInstanceOf[Left[?, ?]]
}
