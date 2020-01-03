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

package models

import play.api.libs.json.{Format, Json}

case class ClientProperty(ownerOrganisationId: Long,
                          ownerOrganisationName: String,
                          billingAuthorityReference: String,
                          authorisedPartyId: Long,
                          authorisationId: Long,
                          authorisationStatus: Boolean,
                          authorisedPartyStatus: RepresentationStatus,
                          checkPermission: String,
                          challengePermission: String,
                          address: String)

object ClientProperty {
  implicit val format = Json.format[ClientProperty]
}

case class ClientPropertyResponse(resultCount: Option[Int], properties: Seq[ClientProperty])

object ClientPropertyResponse {
  implicit val format: Format[ClientPropertyResponse] = Json.format[ClientPropertyResponse]
}