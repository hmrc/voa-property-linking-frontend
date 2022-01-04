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

package utils

import play.api.data.{FormError, Forms, Mapping}
import play.api.data.validation.Constraint
import uk.gov.voa.play.form.Condition

object Conditionals {
  case class IfCondition[T](
        condition: Condition,
        mapping: Mapping[T],
        defaultValue: Option[T] = None,
        orElse: Seq[IfCondition[T]] = Nil,
        constraints: Seq[Constraint[T]] = Nil)
      extends Mapping[T] {
    override val format: Option[(String, Seq[Any])] = mapping.format

    val alwaysTrue: Map[String, String] => Boolean = { _ =>
      true
    }
    val key = mapping.key
    val mappings: Seq[Mapping[_]] = mapping.mappings :+ this
    def verifying(addConstraints: Constraint[T]*): Mapping[T] =
      this.copy(constraints = constraints ++ addConstraints.toSeq)
    def unbind(value: T): Map[String, String] = mapping.unbind(value)
    def unbindAndValidate(value: T): (Map[String, String], Seq[FormError]) = mapping.unbindAndValidate(value)
    def withPrefix(prefix: String): IfCondition[T] = copy(mapping = mapping.withPrefix(prefix))

    def bind(data: Map[String, String]): Either[Seq[FormError], T] =
      if (condition(data)) Forms.default(mapping, defaultValue.get).bind(data)
      else
        orElse
          .find { m =>
            m.condition(data)
          }
          .map(_.withDefault(defaultValue).withPrefix(key).bind(data))
          .getOrElse(Right(defaultValue.get))

    def elseIf(condition: Condition, mapping: Mapping[T]): IfCondition[T] =
      this.copy(orElse = orElse :+ IfCondition(condition, mapping))

    def default(mapping: Mapping[T], value: T): IfCondition[T] =
      this.copy(orElse = orElse :+ IfCondition(alwaysTrue, mapping)).withDefault(Some(value))

    private def withDefault: Option[T] => IfCondition[T] = default => this.copy(defaultValue = default)
  }
}
