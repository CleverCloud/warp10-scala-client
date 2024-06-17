package com.clevercloud.warp10client

import com.clevercloud.warp10client.models.Warp10Stack
import io.circe.Decoder
import io.circe.parser
import io.circe.generic.semiauto.deriveDecoder
import org.specs2.*

class Warp10StackTest extends mutable.Specification {
  "This is a specification for the Stack".txt

  "The Stack should" >> {
    "contain a A element with str" >> {
      case class A(str: String, n: Int)

      given Decoder[A] = deriveDecoder[A]

      val stack = Warp10Stack.apply(parser.parse("[{\"str\": \"toto\", \"n\": 30}]").toOption.get)
      val result = stack.get[A](0).toOption.get

      stack.size must equalTo(1 )
      result.str must equalTo ("toto")
      result.n must equalTo (30)
    }
  }
}
