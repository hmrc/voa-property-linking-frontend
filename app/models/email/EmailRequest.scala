/*
 * Copyright 2018 HM Revenue & Customs
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

package models.email

import play.api.libs.json.Json

case class EmailRequest(to: String, templateId: String, parameters: Map[String, String])

object EmailRequest {

  implicit val format = Json.format[EmailRequest]

  def registration(to: String, personId: String, name: String): EmailRequest =
    EmailRequest(to, "cca_enrolment_confirmation", Map("personId" -> personId, "name" -> name))

  def migration(to: String, personId: String, name: String): EmailRequest =
    EmailRequest(to, "cca_enrolment_migration_confirmation", Map("personId" -> personId, "name" -> name))

}