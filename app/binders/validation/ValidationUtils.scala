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

import binders.Params
import cats.data.Validated.Valid
import cats.data.{Validated, ValidatedNel}
import play.api.data.validation.ValidationError
import utils.Cats

import scala.language.implicitConversions


trait ValidationUtils extends Cats {

  def readOption(implicit key: String, params: Params): ValidatedNel[ValidationError, Option[String]] =
    params.get(key).flatMap(_.headOption).validNel

  def maxLength(maxBound: Int)(value: String)(implicit key: String): ValidatedNel[ValidationError, String] =
    Validated.condNel(value.length <= maxBound, value, ValidationError(key, maxBound))

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
