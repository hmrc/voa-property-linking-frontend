/*
 * Copyright 2022 HM Revenue & Customs
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

import java.time.LocalDate
import models.{Address, NamedEnum, NamedEnumSupport}
import play.api.data.Forms._
import play.api.data.format.{Formats, Formatter}
import play.api.data.validation._
import play.api.data.{FormError, Forms, Mapping}
import uk.gov.voa.play.form.Condition
import views.helpers.Errors
import org.apache.commons.lang3.StringUtils.{isNotBlank, isNumeric}
import play.api.data.validation.Constraints._

import scala.util.Try
import uk.gov.voa.play.form.ConditionalMappings._
import utils.Formatters.formatDate
import views.helpers.Errors.{dateMustBeAfter, dateMustBeAfter1stApril2017, dateMustBeInPast}

import scala.util.matching.Regex

object Mappings extends DateMappings {

  def trueOnly(error: String): Mapping[Boolean] =
    text.verifying(error, _ == "true").transform[Boolean](_.toBoolean, _.toString)

  val mandatoryBoolean: Mapping[Boolean] =
    optional(boolean).verifying("error.boolean", _.isDefined).transform(_.get, Some.apply)

  val addressEnteredManually: Condition = paramMap => !paramMap.get("address.addressId").exists(isNumeric)
  val postcodeRegex: Regex =
    """^([A-Za-z][A-Za-z]\d\d|[A-Za-z][A-Za-z]\d|[A-Za-z]\d|[A-Za-z]\d\d|[A-Za-z]\d[A-Za-z]|[A-Za-z]{2}\d[A-Za-z]) {0,1}\d[A-Za-z]{2}$|.{0}""".r

  val addressMapping: Mapping[Address] = mapping(
    "addressId" -> addressId,
    "line1"     -> onlyIf(addressEnteredManually, text.verifying(nonEmpty, maxLength(36)))(""),
    "line2"     -> text(maxLength = 36),
    "line3"     -> text(maxLength = 36),
    "line4"     -> text(maxLength = 36),
    "postcode" -> onlyIf(
      addressEnteredManually,
      text
        .verifying(
          nonEmpty,
          pattern(postcodeRegex, error = "error.invalidPostcode")
        )
        .transform[String](_.toUpperCase, identity)
    )("")
  )(Address.apply)(Address.unapply).verifying("error.required", address => {
    address.addressUnitId.isDefined || (isNotBlank(address.postcode) && isNotBlank(address.line1))
  })

  lazy val addressId: Mapping[Option[Long]] =
    default(text, "").transform(t => Try { t.toLong }.toOption, _.map(_.toString).getOrElse(""))

  lazy val agentCode: Mapping[Long] =
    nonEmptyText.verifying("error.agentCode", s => s.trim.forall(_.isDigit)).transform(_.trim.toLong, _.toString)
}

trait DateMappings {

  val dmyDate: Mapping[LocalDate] = tuple(
    "day"   -> number(1, 31),
    "month" -> number(1, 12),
    "year"  -> number(1900, 3000)
  ).verifying(Errors.invalidDate, x => x match { case (d, m, y) => Try(LocalDate.of(y, m, d)).isSuccess })
    .transform({ case (d, m, y) => LocalDate.of(y, m, d) }, d => (d.getDayOfMonth, d.getMonthValue, d.getYear))

  val dmyDateAfterThreshold: Mapping[LocalDate] =
    dmyDate.verifying(dateMustBeAfter1stApril2017, d => d.isAfter(LocalDate.of(2017, 4, 1)))

  def dmyDateAfterThreshold(thresholdDate: LocalDate): Mapping[LocalDate] = {
    val constraint =
      Constraint[LocalDate](dateMustBeAfter, formatDate(thresholdDate)) { date =>
        if (date.isAfter(thresholdDate)) Valid
        else Invalid(dateMustBeAfter, formatDate(thresholdDate))
      }
    dmyDate.verifying(constraint)
  }

  val dmyPastDate: Mapping[LocalDate] = dmyDate.verifying(dateMustBeInPast, d => d.isBefore(LocalDate.now))

  private def number(min: Int, max: Int) =
    Forms.of[Int](trimmingNumberFormatter).verifying(Constraints.min(min)).verifying(Constraints.max(max))

  implicit lazy val trimmingNumberFormatter = new Formatter[Int] {
    override val format: Option[(String, Seq[Any])] = Formats.intFormat.format

    override def bind(key: String, data: Map[String, String]): Either[Seq[FormError], Int] =
      Formats.intFormat.bind(key, data.map(x => (x._1, x._2.trim)))

    override def unbind(key: String, value: Int): Map[String, String] = Formats.intFormat.unbind(key, value)
  }
}

object EnumMapping {
  def apply[T <: NamedEnum](named: NamedEnumSupport[T], defaultErrorMessageKey: String = Errors.noValueSelected) =
    Forms.of[T](new Formatter[T] {

      override val format = Some((defaultErrorMessageKey, Nil))

      def bind(key: String, data: Map[String, String]): Either[Seq[FormError], T] = {
        val resOpt = for {
          keyVal        <- data.get(key)
          enumTypeValue <- named.fromName(keyVal)
        } yield {
          Right(enumTypeValue)
        }
        resOpt.getOrElse(Left(Seq(FormError(key, defaultErrorMessageKey, Nil))))
      }

      def unbind(key: String, value: T): Map[String, String] = Map(key -> value.name)
    })
}

case class ConditionalDateAfter(
      disableField: String,
      afterField: String,
      key: String = "",
      constraints: Seq[Constraint[LocalDate]] = Nil)
    extends Mapping[LocalDate] {

  import Mappings._

  override val mappings = Nil

  override def bind(data: Map[String, String]) =
    (
      boolean.withPrefix(disableField).bind(data),
      dmyDate.withPrefix(afterField).bind(data),
      dmyDate.withPrefix(key).verifying(constraints: _*).bind(data)) match {
      case (_, _, errs @ Left(_))                                         => errs
      case (Left(_), Left(_), r @ Right(_))                               => r
      case (Right(true), _, r @ Right(_))                                 => r
      case (Right(false), Right(after), r @ Right(d)) if d.isAfter(after) => r
      case (Right(false), _, Right(_))                                    => Left(Seq(FormError(key, Errors.dateMustBeAfterOtherDate)))
    }

  override def unbind(value: LocalDate) = dmyDate.withPrefix(key).unbind(value)

  override def unbindAndValidate(value: LocalDate) = dmyDate.withPrefix(key).unbindAndValidate(value)

  override def withPrefix(prefix: String) = copy(key = prefix + key)

  override def verifying(c: Constraint[LocalDate]*) = copy(constraints = constraints ++ c.toSeq)
}

case class TextMatching(other: String, errorKey: String, key: String = "", constraints: Seq[Constraint[String]] = Nil)
    extends Mapping[String] {
  override val mappings = Nil

  override def bind(data: Map[String, String]) =
    (text.withPrefix(key).bind(data), text.withPrefix(other).bind(data)) match {
      case (l @ Left(_), _)                   => l
      case (r @ Right(_), Left(_))            => r
      case (r @ Right(a), Right(b)) if a == b => r
      case (Right(_), Right(_))               => Left(Seq(FormError(key, errorKey)))
    }

  override def unbind(value: String) = text.unbind(value)

  override def unbindAndValidate(value: String) = text.unbindAndValidate(value)

  override def withPrefix(prefix: String) = copy(key = prefix + key)

  override def verifying(c: Constraint[String]*) = copy(constraints = constraints ++ c.toSeq)
}

object FormValidation {
  def nonEmptyList[T]: Constraint[List[T]] = Constraint[List[T]]("constraint.required") { list =>
    if (list.nonEmpty) Valid else Invalid(ValidationError("error.required"))
  }
}
