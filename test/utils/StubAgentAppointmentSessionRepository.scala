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

package utils

import controllers.agentAppointment.AppointAgent
import models.PropertyLink
import session.{AgentAppointmentSession, AgentAppointmentSessionRepository}
import uk.gov.hmrc.http.cache.client.SessionCache
import uk.gov.hmrc.play.http.HeaderCarrier

import scala.concurrent.Future

//FIXME: not needed
//class StubAgentAppointmentSessionRepository(cache: SessionCache) extends AgentAppointmentSessionRepository(cache) {
//
//  override def start(agent: AppointAgent, agentOrgId: Long, propertyLink: PropertyLink)(implicit hc: HeaderCarrier): Future[Unit] = {
//    Future.successful(())
//  }
//
//  override def saveOrUpdate(session: AgentAppointmentSession)(implicit hc: HeaderCarrier): Future[Unit] = {
//    Future.successful(())
//  }
//
//  override def get()(implicit hc: HeaderCarrier): Future[Option[AgentAppointmentSession]] = {
//    Future.successful(None)
//  }
//
//  override def remove()(implicit hc: HeaderCarrier): Future[Unit] = {
//    Future.successful(())
//  }
//}
