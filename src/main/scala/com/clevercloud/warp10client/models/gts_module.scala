package com.clevercloud.warp10client.models

import java.net.URLDecoder
import java.time._

import io.circe._

import scala.collection.immutable.ListMap
import scala.util.{Failure, Success, Try}

object gts_module {
  case object gts_errors {
    sealed trait InvalidGTSFormat
    case object InvalidGTSStructureFormat extends InvalidGTSFormat
    case class ListInvalidGTSFormat(errors: Seq[InvalidGTSFormat]) extends InvalidGTSFormat
    case object InvalidGTSclassnameFormat extends InvalidGTSFormat
    case object InvalidGTSLabelsFormat extends InvalidGTSFormat

    sealed trait InvalidGTSPointFormat extends InvalidGTSFormat
    case object InvalidGTSPointStructureFormat extends InvalidGTSPointFormat
    case class ListInvalidGTSPointFormat(errors: Seq[InvalidGTSPointFormat]) extends InvalidGTSPointFormat
    case object InvalidGTSPointValueFormat extends InvalidGTSPointFormat
    case object InvalidGTSPointTimestampFormat extends InvalidGTSPointFormat
    case object InvalidGTSPointCoordinatesFormat extends InvalidGTSPointFormat
    case object InvalidGTSPointElevationFormat extends InvalidGTSPointFormat
    case object CantParseAsBoolean extends InvalidGTSPointFormat
    case object CantParseAsDouble extends InvalidGTSPointFormat
    case object CantParseAsString extends InvalidGTSPointFormat
    case object CantParseAsMacro extends InvalidGTSPointFormat
    case object MacroValueParseNotImplemented extends InvalidGTSPointFormat
  }
  import gts_errors._

  implicit object gtsPointOrdering extends Ordering[GTSPoint] {
    def compare(x: GTSPoint, y: GTSPoint): Int = x.ts.get.compare(y.ts.get)
  }

  case class GTS(
    classname: String,
    labels: Map[String, String],
    points: Seq[GTSPoint]
  ) {
    def toSelector: String = s"~$classname{${labels.map { case (key,value) => s"$key=$value" }.mkString(",")}}"
    def serialize: String = (Seq(points.head.serializeWith(classname, labels)) ++ points.drop(1).map(point => point.serializeWithoutMetadata())).mkString("\n=")
    def filter(maxDate: Long): GTS = {
      val filteredPoints: Seq[GTSPoint] = points.filter(_.ts.isDefined).filter(_.ts.get >= maxDate)
      this.copy(points = filteredPoints)
    }
    def mostRecentPoint: GTSPoint = points.filter(_.ts.isDefined).maxBy(_.ts.get)
  }

  object GTS {
    private val gtsRegex = "([^/]*)/([^/]*:[^/]*)?/([^ ]*) ([^ ]*)\\{([^}]*)\\} (.*)".r

    def parse(input: String): Either[List[InvalidGTSFormat], List[GTS]] = {
      val unparsedGTSList: List[String] = input.split("\n(?=[^=])").toList // split in GTS List

      unparsedGTSList.map(parseGTS(_)).partition(_.isLeft) match {
        case (Nil,  gts) => Right(for(Right(i) <- gts) yield i)
        case (err, _) => Left(for(Left(s) <- err) yield s)
      }
    }

    def parseGTS(input: String): Either[InvalidGTSFormat, GTS] = {
      val unparsedGTSPointList: List[String] = input.split("\n").toList
      val firstPointWithLabelsAndclassname: String = unparsedGTSPointList.head

      firstPointWithLabelsAndclassname match {
        case gtsRegex(tsAsString, coordinatesAsString, elevAsString, classnameAsString, labelsAsString, valueAsString) => {
          val tsEither = parseLong(notNullString(tsAsString), InvalidGTSPointTimestampFormat)
          val coordinatesEither = parseCoordinates(notNullString(coordinatesAsString))
          val elevEither = parseLong(notNullString(elevAsString), InvalidGTSPointElevationFormat)
          val classnameEither = if (notNullString(classnameAsString).nonEmpty) Right(classnameAsString) else Left(InvalidGTSclassnameFormat)
          val labelsEither = parseLabels(notNullString(labelsAsString))
          val valueEither = GTSValue.parse(notNullString(valueAsString))

          (tsEither, coordinatesEither, elevEither, classnameEither, labelsEither, valueEither) match {
            case (Right(ts), Right(coordinates), Right(elev), Right(classname), Right(labels), Right(value)) => {
              val firstPoint = GTSPoint(ts, coordinates, elev, value)
              unparsedGTSPointList
                .drop(1) // drop first point which is already parsed
                .map(GTSPoint.parse)
                .foldRight(Right(Nil): Either[InvalidGTSFormat, List[GTSPoint]])((e, acc) => for (xs <- acc; x <- e) yield x :: xs) match {
                  case Left(e) => Left(e)
                  case Right(points) => Right(GTS(classname = classname, labels = labels, points = firstPoint :: points))
                }
            }
            case _ =>
              Left(ListInvalidGTSFormat(
                Seq(tsEither, coordinatesEither, elevEither, classnameEither, labelsEither, valueEither)
                  .filter(_.isLeft)
                  .map(_.left)
                  .map(_.get)
              ))
          }
        }
        case _ => Left(InvalidGTSStructureFormat)
      }
    }
  }

  case class Coordinates(lat: Double, lon: Double) {
    def serialize: String = s"$lat:$lon"
  }

  case class GTSPoint(
    ts: Option[Long],
    coordinates: Option[Coordinates],
    elev: Option[Long],
    value: GTSValue
  ) {
    def serializeWith(classname: String, labels: Map[String, String]): String = s"$serializeTs/$serializeCoordinates/$serializeElev ${serializeclassname(classname)}${serializeLabels(labels)} $serializeValue"
    def serializeWithoutMetadata(): String = s"$serializeTs/$serializeCoordinates/$serializeElev $serializeValue"
    private def serializeTs = ts.map(_.toString).getOrElse("")
    private def serializeCoordinates = coordinates.map(_.serialize).getOrElse("")
    private def serializeElev = elev.map(_.toString).getOrElse("")
    private def serializeclassname(classname: String) = classname
    private def serializeLabels(labels: Map[String, String]) = labels.map(pair => pair._1 + "=" + pair._2).mkString("{", ",", "}")
    private def serializeValue = value.serialize
  }

  object GTSPoint {
    def apply(
      ts: ZonedDateTime,
      coordinates: Option[Coordinates],
      elev: Option[Long],
      value: GTSValue
    ): GTSPoint = {
      val utc = LocalDateTime.ofInstant(ts.toInstant, ZoneId.of("UTC"))
      val utcMilli = utc.atZone(ZoneId.of("UTC")).toInstant.toEpochMilli
      val utcMicro = s"${utcMilli}000".toLong
      GTSPoint(Some(utcMicro), coordinates, elev, value)
    }

    def apply(
      ts: LocalDateTime,
      coordinates: Option[Coordinates],
      elev: Option[Long],
      value: GTSValue
    ): GTSPoint = {
      val utcMilli = ts.toInstant(ZoneOffset.UTC).toEpochMilli
      val utcMicro = s"${utcMilli}000".toLong
      GTSPoint(Some(utcMicro), coordinates, elev, value)
    }

    private val gtsPointRegex = "([^/]*)/([^/]*:[^/]*)?/(.*)? (.*)".r

    def parse(input: String): Either[InvalidGTSPointFormat, GTSPoint] = {
      input.substring(1) match { // remove "=" in pattern `=timestamp// 'value'`
        case gtsPointRegex(tsAsString, coordinatesAsString, elevAsString, valueAsString) => {
          val tsEither = parseLong(notNullString(tsAsString), InvalidGTSPointTimestampFormat)
          val coordinatesEither = parseCoordinates(notNullString(coordinatesAsString))
          val elevEither = parseLong(notNullString(elevAsString), InvalidGTSPointElevationFormat)
          val valueEither = GTSValue.parse(notNullString(valueAsString))

          (tsEither, coordinatesEither, elevEither, valueEither) match {
            case (Right(ts), Right(coordinates), Right(elev), Right(value)) => {
              Right(GTSPoint(ts = ts, coordinates = coordinates, elev = elev, value = value))
            }
            case _ =>
              Left(ListInvalidGTSPointFormat(
                Seq(tsEither, coordinatesEither, elevEither, valueEither)
                  .filter(_.isLeft)
                  .map(_.left)
                  .map(_.get)
              ))
          }
        }
        case _ => Left(InvalidGTSPointStructureFormat)
      }
    }
  }

  private def notNullString(input: String) = {
    if (input == null) ""
    else URLDecoder.decode(input, "UTF-8")
  }

  private def parseCoordinates(input: String): Either[InvalidGTSPointFormat, Option[Coordinates]] = {
    if (input.nonEmpty) {
      val coordinatesParts = input.split(":")
      if (coordinatesParts.length == 2) {
        val latEither = parseDouble(coordinatesParts(0), InvalidGTSPointCoordinatesFormat)
        val lonEither = parseDouble(coordinatesParts(1), InvalidGTSPointCoordinatesFormat)
        (latEither, lonEither) match {
          case (Right(Some(lat)), Right(Some(lon))) => Right(Some(Coordinates(lat, lon)))
          case _ => Left(InvalidGTSPointCoordinatesFormat)
        }
      } else {
        Left(InvalidGTSPointCoordinatesFormat)
      }
    } else {
      Right(None)
    }
  }

  private def parseLabels(input: String): Either[InvalidGTSFormat, Map[String, String]] = {
    if (input.nonEmpty) {
      val keyAndValueAsStringSeq = input.split(",")
      if (keyAndValueAsStringSeq.forall(_.contains("="))) {
        val encodedKeyAndValuAsPairSeq = keyAndValueAsStringSeq.map(_.split("=")).map(array => array(0) -> array(1))
        val optionalDecodedKeyAndValueAsPairSeq = encodedKeyAndValuAsPairSeq.map {
          case (key, value) =>
            try {
              Some((
                URLDecoder.decode(key, "UTF-8"),
                URLDecoder.decode(value, "UTF-8")
              ))
            } catch {
              case _: IllegalArgumentException => None
            }
        }
        if (optionalDecodedKeyAndValueAsPairSeq.contains(None)) {
          Left(InvalidGTSLabelsFormat)
        } else {
          Right(Map(optionalDecodedKeyAndValueAsPairSeq.map(_.get): _*))
        }
      } else {
        Left(InvalidGTSLabelsFormat)
      }
    } else {
      Right(Map())
    }
  }

  private def parseLong = parseWithExceptionCatching(_.toLong) _
  private def parseDouble = parseWithExceptionCatching(_.toDouble) _

  private def parseWithExceptionCatching[A](map: String => A)(input: String, resultOnError: InvalidGTSPointFormat): Either[InvalidGTSPointFormat, Option[A]] = {
    if (input.nonEmpty) {
      Try {
        map(input)
      } match {
        case Failure(_) => Left(resultOnError)
        case Success(s) => Right(Some(s))
      }
    } else {
      Right(None)
    }
  }

  sealed trait GTSValue {
    def serialize: String
  }
  case class GTSLongValue(value: Long) extends GTSValue {
    override def serialize: String = value.toString
  }
  case class GTSDoubleValue(value: Double) extends GTSValue {
    override def serialize: String = value.toString
  }
  case class GTSBooleanValue(value: Boolean) extends GTSValue {
    override def serialize: String = value.toString
  }
  case class GTSStringValue(value: String) extends GTSValue {
    override def serialize: String = s"'$value'"
  }
  case class GTSMacroValue(
                            prefix: String,
                            `macro`: String,
                            values: ListMap[String, Any], //list map to keep order
                          ) extends GTSValue {
    override def serialize: String = s":$prefix:${`macro`}:${this.printValues}"

    def printValues = {
      this.values.map({ case (key, value) => {
        value match {
          case _: String => s"'$key' '$value'"
          case _: Double => s"'$key' $value"
          case _: Long => s"'$key' $value"
          case _: Boolean => s"'$key' $value"
          case _: Int => s"'$key' $value"
          case _: Array[Byte] => s"'$key' $value"
        }
      }}).mkString("{", " ", "}")
    }
  }

  object GTSValue {
    def apply(value: Long) = GTSLongValue(value)
    def apply(value: Double) = GTSDoubleValue(value)
    def apply(value: Boolean) = GTSBooleanValue(value)
    def apply(value: String) = GTSStringValue(value)
    def apply(prefix: String, `macro`: String, values: ListMap[String, Any]) = GTSMacroValue(prefix, `macro`, values)

    def parse(string: String): Either[InvalidGTSPointFormat, GTSValue] = {
      def isStringValue = string.startsWith("'") && string.endsWith("'")
      def isTrueValue = (string == "true" || string == "T")
      def isFalseValue = (string == "false" || string == "F")
      def isLongValue = string.matches("(\\+|-)?\\d+")
      def isDoubleValue = string.matches("(\\+|-)?\\d+(\\.\\d*)?")
      def isMacroValue = string.matches(":\\w.*:\\w.*:\\{.+\\}")

      if (isMacroValue) {
        Left(MacroValueParseNotImplemented)
      } else if (isStringValue) {
        Right(GTSValue(string.substring(1, string.length - 1)))
      } else if (isTrueValue) {
        Right(GTSValue(true))
      } else if (isFalseValue) {
        Right(GTSValue(false))
      } else if (isLongValue) {
        Right(GTSValue(string.toLong))
      } else if (isDoubleValue) {
        Right(GTSValue(string.toDouble))
      } else {
        Left(InvalidGTSPointValueFormat)
      }
    }

    def parse(value: Json): Either[InvalidGTSPointFormat, GTSValue] = {
      if (value.isString) {
        value.asString match {
          case Some(string) => Right(GTSValue(string.substring(1, string.length - 1)))
          case None => Left(CantParseAsString)
        }
      } else if (value.isBoolean) {
        value.asBoolean match {
          case Some(boolean) => Right(GTSValue(boolean))
          case None => Left(CantParseAsBoolean)
        }
      } else if (value.isNumber) {
        value.asNumber match {
          case Some(number) => Right(GTSValue(number.toDouble))
          case None => Left(CantParseAsDouble)
        }
      } else {
        Left(InvalidGTSPointValueFormat)
      }
    }
  }
}
