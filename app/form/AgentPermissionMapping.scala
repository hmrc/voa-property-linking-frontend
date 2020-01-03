/*
 * Copyright 2020 HM Revenue & Customs
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

import models.{AgentPermission, NotPermitted}
import play.api.data.validation.Constraint
import play.api.data.{FormError, Mapping}

case class AgentPermissionMapping(other: String, key: String = "", constraints: Seq[Constraint[AgentPermission]] = Nil) extends Mapping[AgentPermission] {
  override val mappings = Seq(this)
  private val wrapped = EnumMapping(AgentPermission)

  override def bind(data: Map[String, String]) = {
    (wrapped.withPrefix(key).bind(data), wrapped.withPrefix(other).bind(data)) match {
      case (e@Left(err), _) => e
      case (Right(p1), Right(p2)) if p1 == NotPermitted && p2 == NotPermitted => Left(Seq(FormError(key, "error.invalidPermissions")))
      case (r@Right(_), _) => r
    }
  }

  override def unbind(value: AgentPermission) = {
    wrapped.withPrefix(key).unbind(value)
  }

  override def unbindAndValidate(value: AgentPermission) = {
    wrapped.withPrefix(key).unbindAndValidate(value)
  }

  override def withPrefix(prefix: String) = copy(key = prefix + key)

  override def verifying(cs: Constraint[AgentPermission]*) = copy(constraints = constraints ++ cs.toSeq)
}
