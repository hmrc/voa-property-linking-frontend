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

package session

import actions.AuthenticatedAction
import connectors.Authenticated
import controllers.VoaPropertyLinkingSpec
import models.{Accounts, DetailedIndividualAccount, GroupAccount, LinkingSession}
import org.mockito.Mockito.when
import org.scalacheck.Arbitrary.arbitrary
import play.api.i18n.Messages
import play.api.mvc.Results._
import play.api.test.FakeRequest
import repositories.SessionRepo
import resources._
import uk.gov.hmrc.http.HeaderCarrier
import utils.StubAuthentication

import scala.concurrent.Future

class LinkingSessionRequestSpec extends VoaPropertyLinkingSpec {

  implicit val request = FakeRequest()
  implicit val hc = HeaderCarrier()
  implicit lazy val messages: Messages = Messages.Implicits.applicationMessages


  val mockLinkingSession = mock[LinkingSession]
  val mockAuthenticatedAction = mock[AuthenticatedAction]
  val mockSessionRepo = mock[SessionRepo]

  val linkingSessionRequest = LinkingSessionRequest(mockLinkingSession, 1234l, mockDetailedIndividualAccount, mockGroupAccount, request)

  object TestWithLinkingSession extends WithLinkingSession(StubAuthentication, mockSessionRepo)

  "apply" should "invoke the wrapped if a session exists" in {
    val clientGroup = arbitrary[GroupAccount].sample.get.copy(isAgent = false)
    val clientPerson = arbitrary[DetailedIndividualAccount].sample.get.copy(organisationId = clientGroup.id)

    StubAuthentication.stubAuthenticationResult(Authenticated(Accounts(clientGroup, clientPerson)))

    when(mockSessionRepo.get[LinkingSession]).thenReturn(Future.successful(Some(mockLinkingSession)))

    val res = TestWithLinkingSession { _ =>
      Future.successful(Ok("Test"))
    }(messages)(FakeRequest())
  }

  "apply" should "return not found if a session doesn't exist" in {
    val clientGroup = arbitrary[GroupAccount].sample.get.copy(isAgent = false)
    val clientPerson = arbitrary[DetailedIndividualAccount].sample.get.copy(organisationId = clientGroup.id)

    StubAuthentication.stubAuthenticationResult(Authenticated(Accounts(clientGroup, clientPerson)))

    when(mockSessionRepo.get[LinkingSession]).thenReturn(Future.successful(Some(mockLinkingSession)))

    val res = TestWithLinkingSession { _ =>
      Future.successful(NotFound)
    }(messages)(FakeRequest())
  }

}
