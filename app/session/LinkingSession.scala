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

import config.Keystore
import controllers.CapacityDeclaration
import models.Property
import uk.gov.hmrc.play.http.HeaderCarrier
import serialization.JsonFormats.sessionFormat
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import scala.concurrent.Future

case class LinkingSession(claimedProperty: Property, declaration: Option[CapacityDeclaration] = None) {
  def withDeclaration(d: CapacityDeclaration) = this.copy(declaration = Some(d))
}

object LinkingSessionRepository {
  def start(p: Property)(implicit hc: HeaderCarrier): Future[Unit] =
    Keystore.cache("sessiondocument", LinkingSession(p)).map(_ => ())

  def saveOrUpdate(session: LinkingSession)(implicit hc: HeaderCarrier): Future[Unit] =
    Keystore.cache("sessiondocument", session).map(_ => ())

  def get()(implicit hc: HeaderCarrier): Future[Option[LinkingSession]] =
    Keystore.fetchAndGetEntry("sessiondocument")
}
