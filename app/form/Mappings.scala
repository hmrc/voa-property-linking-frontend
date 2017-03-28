/*
 * Copyright 2017 HM Revenue & Customs
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

import config.ApplicationConfig
import models.{Address, NamedEnum, NamedEnumSupport}
import org.joda.time.LocalDate
import play.api.data.Forms._
import play.api.data.format.{Formats, Formatter}
import play.api.data.validation.{Constraint, Constraints}
import play.api.data.{FormError, Forms, Mapping}
import views.helpers.Errors

import scala.util.Try

object Mappings extends DateMappings {

  def trueOnly(error: String): Mapping[Boolean] =
    text.verifying(error, _ == "true").transform[Boolean](_.toBoolean, _.toString)

  def mandatoryBoolean: Mapping[Boolean] = optional(boolean).verifying("error.boolean", _.isDefined).transform(_.get, Some.apply)

  def address: Mapping[Address] = mapping(
    "addressId" -> addressId,
    "line1" -> nonEmptyText(maxLength = 100),
    "line2" -> default(text(maxLength = 100), ""),
    "line3" -> default(text(maxLength = 100), ""),
    "line4" -> default(text(maxLength = 100), ""),
    "postcode" -> nonEmptyText(maxLength = 100).transform[String](_.toUpperCase, identity)
  )(Address.apply)(Address.unapply)

  private def addressId: Mapping[Option[Int]] = default(text, "").transform(t => Try { t.toInt }.toOption, _.map(_.toString).getOrElse(""))
}

trait DateMappings {

  def dmyDate: Mapping[LocalDate] = tuple(
    "day" -> number(1, 31),
    "month" -> number(1, 12),
    "year" -> number(1900, 3000)
  ).verifying(Errors.invalidDate, x => x match { case (d, m, y) => Try(new LocalDate(y, m, d)).isSuccess } )
    .transform({ case (d, m, y) => new LocalDate(y, m, d) }, d => (d.getDayOfMonth, d.getMonthOfYear, d.getYear))

  def dmyDateAfterThreshold: Mapping[LocalDate] = dmyDate.verifying(Errors.dateMustBeAfter1stApril2017,
    d => d.isAfter(ApplicationConfig.propertyLinkingDateThreshold))

  def dmyPastDate: Mapping[LocalDate] = dmyDate.verifying(Errors.dateMustBeAfter1stApril2017, d => d.isBefore(LocalDate.now))

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

case class DateAfter(afterField: String, key: String = "", constraints: Seq[Constraint[LocalDate]] = Nil) extends Mapping[LocalDate] {
  import Mappings._

  override val mappings = Nil

  override def bind(data: Map[String, String]) = (dmyDate.withPrefix(afterField).bind(data),
    dmyDate.withPrefix(key).verifying(constraints:_*).bind(data)) match {
    case (_, errs@Left(_)) => errs
    case (Left(_), r@Right(_)) => r
    case (Right(after), r@Right(d)) if d.isAfter(after) => r
    case (Right(_), Right(_)) => Left(Seq(FormError(key, Errors.dateMustBeAfterOtherDate)))
  }

  override def unbind(value: LocalDate) = dmyDate.withPrefix(key).unbind(value)

  override def unbindAndValidate(value: LocalDate) = dmyDate.withPrefix(key).unbindAndValidate(value)

  override def withPrefix(prefix: String) = copy(key = prefix + key)

  override def verifying(c: Constraint[LocalDate]*) = copy(constraints = constraints ++ c.toSeq)
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
