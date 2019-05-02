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

import play.api.mvc.QueryStringBindable

import utils.QueryParamUtils.toQueryString

import scala.reflect.ClassTag
import scala.reflect.runtime.universe._

abstract class ValidatingBinder[T: TypeTag: ClassTag] extends ValidationUtils {

  def validate(params: Params): ValidationResult[T]

  implicit val binder: QueryStringBindable[T] = new QueryStringBindable[T] {

    override def bind(key: String, params: Params): Option[Either[String, T]] =
      Some(validate(params).leftMap(_.map(_.show).toList.mkString(", ")).toEither)

    override def unbind(key: String, value: T): String =
      toQueryString(value)
  }

}
