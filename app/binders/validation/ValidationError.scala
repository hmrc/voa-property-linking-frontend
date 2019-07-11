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

import cats.Show
import org.apache.commons.lang3.text.WordUtils


sealed trait ValidationError extends Product with Serializable

object ValidationError {

  def translateClass(expectedClass: Class[_]): String = expectedClass.getSimpleName match {
    case "int" | "long" => "Integer"
    case simpleName     => WordUtils.capitalize(simpleName) //to uppercase first letter of primitive types
  }

  implicit val showInstance: Show[ValidationError] = Show.show[ValidationError] {
    case MissingError(key) =>
      s"""Missing value for parameter "$key""""
    case e @ InvalidTypeError(key, expectedClass) =>
      s"""Invalid type for parameter "$key". "${e.value}" is not a valid ${translateClass(expectedClass)}"""
    case e @ NotAnEnumError(key, acceptableValues) =>
      s"""Invalid value "${e.value}" for parameter "$key". Allowed values: [${acceptableValues.mkString(", ")}]"""
    case e @ OverLimitError(key, acceptableLimit) =>
      s"""Value "${e.value}" for parameter "$key" is over the acceptable limit: $acceptableLimit"""
    case e @ UnderLimitError(key, acceptableLimit) =>
      s"""Value "${e.value}" for parameter "$key" is under the acceptable limit: $acceptableLimit"""
    case e @ OverMaxLengthError(key, acceptableLimit) =>
      s"""Value for parameter "$key" is longer than the the acceptable maximum: $acceptableLimit"""
    case e @ UnderMinLengthError(key, acceptableLimit) =>
      s"""Value for parameter "$key" is shorter than the acceptable minimum: $acceptableLimit"""
    case e @ AllMissingError(key, keys @ _ *) =>
      s"""At least one of these parameters must be provided: [${(key :: keys.toList).mkString(", ")}]"""
    case InvalidFormat(key) =>
      s"""Invalid format for parameter "$key""""
    case BlankQueryParameterError(key) =>
      s"""Missing value for parameter "$key""""
  }
}

final case class MissingError(key: String) extends ValidationError

final case class BlankQueryParameterError(key: String) extends ValidationError

final case class InvalidTypeError[T](key: String, expectedClass: Class[T])(val value: String) extends ValidationError

final case class NotAnEnumError(key: String, acceptableValues: Seq[String])(val value: String) extends ValidationError

final case class OverMaxLengthError(key: String, acceptableLimit: Int)(val value: String) extends ValidationError

final case class UnderMinLengthError(key: String, acceptableLimit: Int)(val value: String) extends ValidationError

final case class OverLimitError[T: Ordering](key: String, acceptableLimit: T)(val value: T) extends ValidationError

final case class UnderLimitError[T: Ordering](key: String, acceptableLimit: T)(val value: T) extends ValidationError

final case class AllMissingError(key: String, keys: String*) extends ValidationError

final case class InvalidFormat(key: String) extends ValidationError
