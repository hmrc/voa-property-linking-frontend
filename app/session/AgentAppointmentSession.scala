/*
 * Copyright 2017 HM Revenue & Customs
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

import javax.inject.Inject

import controllers.agentAppointment.AppointAgent
import models.PropertyLink
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.Json
import reactivemongo.api.DB
import repositories.SessionRepository
import uk.gov.hmrc.http.cache.client.SessionCache
import uk.gov.hmrc.play.http.HeaderCarrier

import scala.concurrent.Future

case class AgentAppointmentSession(agent: AppointAgent, agentOrgId: Long, propertyLink: PropertyLink)
object AgentAppointmentSession {
  implicit val format = Json.format[AgentAppointmentSession]
}

  //FIXME
class AgentAppointmentSessionRepository @Inject() (db: DB) extends SessionRepository ("agentAppointmentDocument", db)
//  private val sessionDocument = "agentAppointmentDocument"
//  def start(agent: AppointAgent, agentOrgId: Long, propertyLink: PropertyLink)(implicit hc: HeaderCarrier): Future[Unit] = {
//    cache.cache(sessionDocument, AgentAppointmentSession(agent, agentOrgId, propertyLink)).map(_ => ())
//  }
//  def saveOrUpdate(session: AgentAppointmentSession)(implicit hc: HeaderCarrier): Future[Unit] =
//    cache.cache(sessionDocument, session).map(_ => ())
//  def get()(implicit hc: HeaderCarrier): Future[Option[AgentAppointmentSession]] =
//    cache.fetchAndGetEntry[AgentAppointmentSession](sessionDocument)
//  def remove()(implicit hc: HeaderCarrier): Future[Unit] =
//    cache.remove().map(_ => ())
//}
