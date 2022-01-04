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

package session

import actions.propertylinking.WithLinkingSession
import actions.propertylinking.requests.LinkingSessionRequest
import controllers.VoaPropertyLinkingSpec
import models.LinkingSession
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import play.api.test.FakeRequest
import repositories.SessionRepo
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future

class LinkingSessionRequestSpec extends VoaPropertyLinkingSpec {

  implicit val request = FakeRequest()
  implicit val hc = HeaderCarrier()

  val mockLinkingSession = mock[LinkingSession]
  val mockSessionRepo = mock[SessionRepo]

  val linkingSessionRequest =
    LinkingSessionRequest(mockLinkingSession, 1234l, mockDetailedIndividualAccount, mockGroupAccount, request)

  object TestWithLinkingSession extends WithLinkingSession(mockCustomErrorHandler, mockSessionRepo)

  "apply" should "invoke the wrapped if a session exists" in {
    when(mockSessionRepo.get[LinkingSession](any(), any())).thenReturn(Future.successful(Some(mockLinkingSession)))

    //    val res = TestWithLinkingSession.refine { _ =>
    //      Future.successful(Ok("Test"))
    //    }(messages)(FakeRequest()) //TODO testing nothing
  }

  "apply" should "return not found if a session doesn't exist" in {
    when(mockSessionRepo.get[LinkingSession]).thenReturn(Future.successful(Some(mockLinkingSession)))

    //    val res = TestWithLinkingSession { _ =>
    //      Future.successful(NotFound)
    //    }(messages)(FakeRequest()) //TODO testing nothing
  }

}
