package com.clevercloud.warp10client.models

import java.time._

import scala.concurrent.duration.Duration

case class Query[+A <: FetchRange](
    selector: Selector,
    range: A,
    dedup: Boolean = false) {
  def serialize = s"selector=${selector.serialize}&format=text&dedup=$dedup&${range.serialize}"
}

trait Serializable {
  def serialize: String
}

sealed trait Selector extends Serializable

object Selector {
  def apply(name: String, labels: Seq[LabelSelector]): Selector = LabelledSelector(name, labels)
  def apply(raw: String): Selector = RawSelector(raw)

  def apply(name: String, labels: Map[String, String]): Selector = {
    LabelledSelector(
      name = name,
      labels = labels.map { case (key, value) => LabelSelector(key, value) } toSeq
    )
  }
}

case class LabelledSelector(name: String, labels: Seq[LabelSelector]) extends Selector {
  override def serialize: String = s"${name.toString}{${labels.map(_.serialize).mkString(",")}}"
}

case class RawSelector(selector: String) extends Selector {
  override def serialize: String = s"$selector{}"
}

case class LabelSelector(key: String, value: String) extends Serializable {
  override def serialize: String = s"$key$value"
}

sealed trait FetchRange extends Serializable

object FetchRange {
  def apply(start: LocalDateTime, end: LocalDateTime) = StartStopRange(start, end)
  def apply(now: LocalDateTime, range: Duration) = RangeBefore(now, range)
  def apply(now: LocalDateTime, limit: Long) = RecordsSince(now, limit)
  def apply(now: Long, limit: Long) = StartStopRangeMicros(now, limit)
}

case class StartStopRange(start: LocalDateTime, stop: LocalDateTime) extends FetchRange {
  override def serialize: String = s"start=${oldestDate}Z&stop=${newestDate}Z"
  def oldestDate = if (start isBefore stop) start else stop
  def newestDate = if (start isBefore stop) stop else start
}

case class StartStopRangeMicros(start: Long, stop: Long) extends FetchRange {
  override def serialize: String = s"start=${oldestDate}&stop=${newestDate}"
  def oldestDate = if (start < stop) start else stop
  def newestDate = if (start < stop) stop else start
}

case class RangeBefore(now: LocalDateTime, range: Duration) extends FetchRange {
  override def serialize: String = s"now=${now.toEpochSecond(ZoneOffset.UTC)}000000&timespan=${range.toMicros}"
}

case class RecordsSince(now: LocalDateTime, limit: Long) extends FetchRange {

  override def serialize: String = {
    val nowMilli = now.atZone(ZoneId.of("UTC")).toInstant.toEpochMilli
    s"now=${nowMilli}000&timespan=-${limit.abs}"
  }
}

case class RecordsSinceMicros(now: Long, limit: Long) extends FetchRange {

  override def serialize: String = {
    s"now=${now}&timespan=-${limit.abs}"
  }
}
