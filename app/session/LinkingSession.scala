/*
 * Copyright 2016 HM Revenue & Customs
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

package session

import config.Wiring
import connectors.CapacityDeclaration
import models.Property
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import serialization.JsonFormats.sessionFormat
import uk.gov.hmrc.http.cache.client.SessionCache
import uk.gov.hmrc.play.http.HeaderCarrier

import scala.concurrent.Future

case class LinkingSession(claimedProperty: Property, envelopeId: String ,
                          declaration: Option[CapacityDeclaration] = None, selfCertifyComplete: Option[Boolean] = None) {
  def withDeclaration(d: CapacityDeclaration) = this.copy(declaration = Some(d))
}

class LinkingSessionRepository(cache: SessionCache) {
  private val sessionDocument = "sessiondocument"

  def start(p: Property, envelopeId:String)(implicit hc: HeaderCarrier): Future[Unit] = {
      cache.cache(sessionDocument, LinkingSession(p, envelopeId)).map(_ => ())
  }

  def saveOrUpdate(session: LinkingSession)(implicit hc: HeaderCarrier): Future[Unit] =
    cache.cache(sessionDocument, session).map(_ => ())

  def get()(implicit hc: HeaderCarrier): Future[Option[LinkingSession]] =
    cache.fetchAndGetEntry(sessionDocument)

  def remove()(implicit hc:HeaderCarrier): Future[Unit] =
    cache.remove().map(_ => ())

}
