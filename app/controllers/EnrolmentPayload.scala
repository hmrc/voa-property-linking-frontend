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

package controllers

import play.api.libs.json.Json

case class EnrolmentPayload(identifiers: List[KeyValuePair], verifiers: List[KeyValuePair])

object EnrolmentPayload {
  implicit val keyValue = Json.format[KeyValuePair]
  implicit val previous = Json.format[Previous]
  implicit val format = Json.format[PayLoad]
  implicit val enrolmentPayload = Json.format[EnrolmentPayload]
}

case class PayLoad(verifiers: Seq[KeyValuePair], legacy: Option[Previous] = None)

object PayLoad {
  implicit val keyValue = Json.format[KeyValuePair]
  implicit val previous = Json.format[Previous]
  implicit val format = Json.format[PayLoad]
}

case class KeyValuePair(key: String, value: String)

case class Previous(previousVerifiers: List[KeyValuePair])
