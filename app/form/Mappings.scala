/*
 * Copyright 2016 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package form

import models.{NamedEnum, NamedEnumSupport, SimpleAddress}
import org.joda.time.DateTime
import play.api.data.Forms._
import play.api.data.format.{Formats, Formatter}
import play.api.data.validation.{Constraint, Constraints}
import play.api.data.{FormError, Forms, Mapping}
import views.helpers.Errors

import scala.util.Try

object Mappings extends DateMappings {

  def trueOnly(error: String): Mapping[Boolean] =
    text.verifying(error, _ == "true").transform[Boolean](_.toBoolean, _.toString)

  def mandatoryBoolean: Mapping[Boolean] = optional(boolean).verifying("error.required", _.isDefined).transform(_.get, Some.apply)

  def address: Mapping[SimpleAddress] = mapping(
    "addressId" -> addressId,
    "line1" -> nonEmptyText,
    "line2" -> default(text, ""),
    "line3" -> default(text, ""),
    "line4" -> default(text, ""),
    "postcode" -> nonEmptyText
  )(SimpleAddress.apply)(SimpleAddress.unapply)

  private def addressId: Mapping[Option[Int]] = default(text, "").transform(t => Try { t.toInt }.toOption, _.map(_.toString).getOrElse(""))
}

trait DateMappings {
  def dmyDate: Mapping[DateTime] = toDateTime(dmyDateTuple)

  def dmyDateAfterMarch2017 = toDateTime(dmyDateTupleAfterMarch2017)

  def dmyPastDate: Mapping[DateTime] = toDateTime(dmyPastDateTuple)

  private def toDateTime(m: Mapping[(Int, Int, Int)]): Mapping[DateTime] = m.transform[DateTime](
    x => new DateTime(x._3, x._2, x._1, 0, 0, 0, 0),
    x => (x.getDayOfMonth, x.getMonthOfYear, x.getYear)
  )

  private def dmyPastDateTuple = dmyDateTuple.verifying(
    Errors.dateMustBeInPast,
    x => new DateTime(x._3, x._2, x._1, 0, 0, 0, 0).isBefore(DateTime.now.withTimeAtStartOfDay)
  )

  private def dmyDateTupleAfterMarch2017 = dmyDateTuple.verifying(
    Errors.dateMustBeAfterMarch2017,
    x => new DateTime(x._3, x._2, x._1, 0, 0).isAfter(new DateTime(2017, 3, 31, 0, 0))
  )

  private def dmyDateTuple: Mapping[(Int, Int, Int)] = mapping(
    "day" -> number(1, 31),
    "month" -> number(1, 12),
    "year" -> number(1900, 3000)
  )((d, m, y) => (d, m, y))(Some(_)).verifying(Errors.invalidDate, x => Try(new DateTime(x._3, x._2, x._1, 0, 0, 0, 0)).isSuccess)


  private def number(min: Int, max: Int) = Forms.of[Int](trimmingNumberFormatter).verifying(Constraints.min(min)).verifying(Constraints.max(max))

  implicit lazy val trimmingNumberFormatter = new Formatter[Int] {
    override val format: Option[(String, Seq[Any])] = Formats.intFormat.format

    override def bind(key: String, data: Map[String, String]): Either[Seq[FormError], Int] =
      Formats.intFormat.bind(key, data.map(x => (x._1, x._2.trim)))

    override def unbind(key: String, value: Int): Map[String, String] = Formats.intFormat.unbind(key, value)
  }
}

object EnumMapping {
  def apply[T <: NamedEnum](named:NamedEnumSupport[T]) = Forms.of[T](new Formatter[T] {

    override val format = Some((Errors.noValueSelected, Nil))

    def bind(key: String, data: Map[String, String]): Either[Seq[FormError], T] = {
      val resOpt = for {
        keyVal <- data.get(key)
        enumTypeValue <- named.fromName(keyVal)
      } yield {
        Right(enumTypeValue)
      }
      resOpt.getOrElse(Left(Seq(FormError(key, Errors.noValueSelected, Nil))))
    }

    def unbind(key: String, value: T): Map[String, String] = Map(key -> value.name)
  })
}

case class DateAfter(afterField: String, key: String = "", constraints: Seq[Constraint[DateTime]] = Nil) extends Mapping[DateTime] {
  import Mappings._

  override val mappings = Nil

  override def bind(data: Map[String, String]) = (dmyDate.withPrefix(afterField).bind(data), dmyDate.withPrefix(key).bind(data)) match {
    case (_, errs@Left(_)) => errs
    case (Left(_), r@Right(_)) => r
    case (Right(after), r@Right(d)) if d.isAfter(after) => r
    case (Right(_), Right(_)) => Left(Seq(FormError(key, Errors.dateMustBeAfterOtherDate)))
  }

  override def unbind(value: DateTime) = dmyDate.withPrefix(key).unbind(value)

  override def unbindAndValidate(value: DateTime) = dmyDate.withPrefix(key).unbindAndValidate(value)

  override def withPrefix(prefix: String) = copy(key = prefix + key)

  override def verifying(c: Constraint[DateTime]*) = copy(constraints = constraints ++ c.toSeq)
}

case class TextMatching(other: String, errorKey: String, key: String = "", constraints: Seq[Constraint[String]] = Nil) extends Mapping[String] {
  override val mappings = Nil

  override def bind(data: Map[String, String]) = (text.withPrefix(key).bind(data), text.withPrefix(other).bind(data)) match {
    case (l@Left(_), _) => l
    case (r@Right(_), Left(_)) => r
    case (r@Right(a), Right(b)) if a == b => r
    case (Right(_), Right(_)) => Left(Seq(FormError(key, errorKey)))
  }

  override def unbind(value: String) = text.unbind(value)

  override def unbindAndValidate(value: String) = text.unbindAndValidate(value)

  override def withPrefix(prefix: String) = copy(key = prefix + key)

  override def verifying(c: Constraint[String]*) = copy(constraints = constraints ++ c.toSeq)
}