package com.clevercloud.warp10client.models

import io.circe.{ Decoder, Json }
import cats.syntax.either.catsSyntaxEither

case class Warp10Stack(json: Json) {

  def head[T](
      implicit
      d: Decoder[T]
    ): Either[String, T] = get(0)

  def size: Int = json.asArray.knownSize

  def get[T](
      index: Int
    )(implicit
      d: Decoder[T]
    ): Either[String, T] = json.asArray
    .map(Right(_))
    .getOrElse(Left("invalid stack"))
    .map { array => array.apply(index) }
    .map { o => d.decodeJson(o) }
    .flatMap {
      case Left(err) => Left(err.getMessage)
      case Right(t)  => Right(t)
    }
}
