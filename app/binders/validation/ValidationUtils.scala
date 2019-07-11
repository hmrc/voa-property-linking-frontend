/*
 * Copyright 2019 HM Revenue & Customs
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

package binders.validation

import java.time.LocalDate

import binders.Params
import cats.data.Validated.Valid
import cats.data.{Validated, ValidatedNel}
import org.apache.commons.lang3.StringUtils
import utils.Cats

import scala.language.implicitConversions
import scala.util.Try
import scala.util.matching.Regex

trait ValidationUtils extends Cats {

  def read(implicit key: String, params: Params): ValidatedNel[MissingError, String] =
    Validated.fromOption(params.get(key).flatMap(_.headOption), MissingError(key)).toValidatedNel

  def readWithDefault(
                       default: => String)(implicit key: String, params: Params): ValidatedNel[ValidationError, String] =
    params.get(key).fold(default)(_.headOption.getOrElse(default)).validNel

  def readOption(implicit key: String, params: Params): ValidatedNel[ValidationError, Option[String]] =
    params.get(key).flatMap(_.headOption).validNel

  def asInt(value: String)(implicit key: String): ValidatedNel[InvalidTypeError[Int], Int] =
    Validated.fromTry(Try(value.toInt)).leftMap(_ => InvalidTypeError(key, classOf[Int])(value)).toValidatedNel

  def asLong(value: String)(implicit key: String): ValidatedNel[InvalidTypeError[Long], Long] =
    Validated.fromTry(Try(value.toLong)).leftMap(_ => InvalidTypeError(key, classOf[Long])(value)).toValidatedNel

  def nonBlankString(value: String)(implicit key: String): ValidatedNel[BlankQueryParameterError, String] =
    Valid(value).ensure(BlankQueryParameterError(key))(StringUtils.isNotBlank(_)).toValidatedNel

  def asBoolean(value: String)(implicit key: String): ValidatedNel[InvalidTypeError[Boolean], Boolean] =
    Validated.fromTry(Try(value.toBoolean)).leftMap(_ => InvalidTypeError(key, classOf[Boolean])(value)).toValidatedNel

  def asLocalDate(value: String)(implicit key: String): ValidatedNel[InvalidTypeError[LocalDate], LocalDate] =
    Validated
      .fromTry(Try(LocalDate.parse(value)))
      .leftMap(_ => InvalidTypeError(key, classOf[LocalDate])(value))
      .toValidatedNel

  def min[T](minBound: T)(value: T)(implicit key: String, ordT: Ordering[T]): ValidatedNel[UnderLimitError[T], T] =
    Valid(value).ensure(UnderLimitError(key, minBound)(value))(ordT.gteq(_, minBound)).toValidatedNel

  def max[T](maxBound: T)(value: T)(implicit key: String, ordT: Ordering[T]): ValidatedNel[OverLimitError[T], T] =
    Valid(value).ensure(OverLimitError(key, maxBound)(value))(ordT.lteq(_, maxBound)).toValidatedNel

  def maxLength(maxBound: Int)(value: String)(implicit key: String): ValidatedNel[OverMaxLengthError, String] =
    Validated.condNel(value.length <= maxBound, value, OverMaxLengthError(key, maxBound)(value))

  def minLength(minBound: Int)(value: String)(implicit key: String): ValidatedNel[UnderMinLengthError, String] =
    Validated.condNel(value.length >= minBound, value, UnderMinLengthError(key, minBound)(value))

  def enumValue[T <: Enumeration](t: T)(value: String)(implicit key: String): ValidatedNel[NotAnEnumError, t.Value] =
    Validated
      .fromTry(Try(t.withName(value)))
      .leftMap(_ => NotAnEnumError(key, t.values.map(_.toString).toList)(value))
      .toValidatedNel

  def regex(r: Regex)(value: String)(implicit key: String): ValidatedNel[InvalidFormat, String] =
    Validated.fromOption(r.findFirstIn(value), InvalidFormat(key)).toValidatedNel

  lazy val ukPostCodeFormat: Regex =
    """^(([A-Za-z]\d{1,2})|(([A-Za-z]{2}\d{1,2})|(([A-Za-z]\d[A-Za-z])|([A-Za-z]{2}\d[A-Za-z]))))( \d[A-Za-z]{2}){0,1}$""".r

  def validPostcode(implicit key: String): String => ValidatedNel[InvalidFormat, String] = regex(ukPostCodeFormat)

  private[validation] class ValidatedOptional[E, A](validated: Validated[E, Option[A]]) {

    def ifPresent[B](f: A => Validated[E, B]): Validated[E, Option[B]] =
      validated.andThen {
        case Some(value) => f(value).map(Option.apply)
        case None        => Valid(Option.empty[B])
      }
  }

  implicit def wrapValidated[E, A](v: Validated[E, Option[A]]): ValidatedOptional[E, A] = new ValidatedOptional(v)

}

object ValidationUtils extends ValidationUtils
