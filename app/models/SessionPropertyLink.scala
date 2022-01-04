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

package models

import models.searchApi.{OwnerAuthAgent, OwnerAuthResult, OwnerAuthorisation}
import play.api.libs.json.{Format, Json}

case class SessionPropertyLink(authorisationId: Long, submissionId: String, agents: Seq[OwnerAuthAgent])

object SessionPropertyLink {
  implicit val format: Format[SessionPropertyLink] = Json.format[SessionPropertyLink]

  def apply(propertyLink: OwnerAuthorisation): SessionPropertyLink =
    SessionPropertyLink(propertyLink.authorisationId, propertyLink.submissionId, agents = propertyLink.agents.toList)
}

case class SessionPropertyLinks(links: Seq[SessionPropertyLink])

object SessionPropertyLinks {
  implicit val format: Format[SessionPropertyLinks] = Json.format[SessionPropertyLinks]

  def apply(propertyLinks: OwnerAuthResult): SessionPropertyLinks =
    SessionPropertyLinks(propertyLinks.authorisations.map(link => SessionPropertyLink(link)).toList)
}
