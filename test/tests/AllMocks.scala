/*
 * Copyright 2019 HM Revenue & Customs
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

package tests

import actions.AuthenticatedAction
import auditing.AuditingService
import org.mockito.Mockito
import org.scalatest.BeforeAndAfterEach
import org.scalatest.mockito.MockitoSugar
import repositories.SessionRepository
import session.WithLinkingSession
import uk.gov.voa.propertylinking.errorhandler.CustomErrorHandler

trait AllMocks { self: MockitoSugar with BeforeAndAfterEach =>

  val mockAuditingService = mock[AuditingService]

  val mockCustomErrorHandler = mock[CustomErrorHandler]

  val mockAuthenticationAction = mock[AuthenticatedAction]

  val mockWithLinkingSession = mock[WithLinkingSession]

  val mockSessionRepository = mock[SessionRepository]

  override protected def beforeEach(): Unit =
    Seq(
      mockAuditingService,
      mockAuthenticationAction,
      mockCustomErrorHandler,
      mockWithLinkingSession,
      mockSessionRepository
    ).foreach(Mockito.reset(_))

}
