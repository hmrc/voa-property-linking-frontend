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

package controllers

import connectors.identityVerificationProxy.IdentityVerificationProxyConnector
import models.registration._
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.scalacheck.Arbitrary._
import org.scalatest.mockito.MockitoSugar
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.twirl.api.Html
import repositories.PersonalDetailsSessionRepository
import resources._
import services.RegistrationService
import services.iv.IvService
import tests.AllMocks
import uk.gov.hmrc.auth.core.AffinityGroup._
import utils._

import scala.concurrent.Future

class IdentityVerificationSpec extends VoaPropertyLinkingSpec with MockitoSugar with AllMocks {

  "Successfully verifying identity when an organisation does not have a CCA account" must
    "register and enrol the user then redirect to the registration success page" in new TestCase {
    StubIdentityVerification.stubSuccessfulJourney("successfuljourney")
    when(mockRegistrationService.create(any(), any(), any())(any())(any(), any())).thenReturn(Future.successful(RegistrationSuccess(1L)))

    val u: UserDetails = userDetails(Organisation)
    val res = testIdentityVerification(u).success()(requestWithJourneyId("successfuljourney"))
    status(res) mustBe SEE_OTHER
    redirectLocation(res) mustBe Some(controllers.registration.routes.RegistrationController.success(1L).url)
  }

  "Successfully verifying identity when an individual does not have a CCA account" must
    "register and enrol the user then redirect to the registration success page" in new TestCase {
    override lazy val mockSessionRepoOrgDetails = mockSessionRepoIndDetails
    StubIdentityVerification.stubSuccessfulJourney("successfuljourney")
    when(mockRegistrationService.create(any(), any(), any())(any())(any(), any())).thenReturn(Future.successful(RegistrationSuccess(1L)))

    val u = userDetails(Individual)
    val res = testIdentityVerification(u).success()(requestWithJourneyId("successfuljourney"))
    status(res) mustBe SEE_OTHER
    redirectLocation(res) mustBe Some(controllers.registration.routes.RegistrationController.success(1L).url)
  }

  "Successfully verifying identity" must
    "return internal server error when the registration or enrolment fails" in new TestCase {
    when(mockCustomErrorHandler.internalServerErrorTemplate(any()))
      .thenReturn(Html("INTERNAL SERVER ERROR"))
    StubIdentityVerification.stubSuccessfulJourney("successfuljourney")
    when(mockRegistrationService.create(any(), any(), any())(any())(any(), any())).thenReturn(Future.successful(EnrolmentFailure))

    val u = userDetails(Organisation)
    val res = testIdentityVerification(u).success()(requestWithJourneyId("successfuljourney"))

    status(res) mustBe INTERNAL_SERVER_ERROR
  }

  "Successfully verifying identity" must
    "return internal server error when there are details missing" in new TestCase {
    when(mockCustomErrorHandler.internalServerErrorTemplate(any()))
      .thenReturn(Html("INTERNAL SERVER ERROR"))
    StubIdentityVerification.stubSuccessfulJourney("successfuljourney")
    when(mockRegistrationService.create(any(), any(), any())(any())(any(), any())).thenReturn(Future.successful(DetailsMissing))

    val u = userDetails(Organisation)
    val res = testIdentityVerification(u).success()(requestWithJourneyId("successfuljourney"))

    status(res) mustBe INTERNAL_SERVER_ERROR
  }

  "Manually navigating to the iv success page after failing identity verification" must "return a 401 Unauthorised response" in new TestCase {
    StubIdentityVerification.stubFailedJourney("somejourneyid")
    val u = userDetails(Organisation)

    val res = testIdentityVerification(u).success()(request.withSession("journey-id" -> "somejourneyid"))
    status(res) mustBe UNAUTHORIZED
  }

  "Navigating to the iv failed page" must "return the failed page" in new TestCase {
    val u = userDetails(Organisation)

    val res = testIdentityVerification(u).fail()(request)
    status(res) mustBe OK
    contentAsString(res) must include("Identity verification failed")
  }
  "Navigating to restoreSession" must "redirect to the iv success page" in new TestCase {
    val u = userDetails(Organisation)

    val res = testIdentityVerification(u).restoreSession()(request)
    status(res) mustBe SEE_OTHER
    redirectLocation(res) mustBe Some(routes.IdentityVerification.success.url)
  }

  "fail" must "redirect the user to the identity verification failure page" in new TestCase {
    val u = userDetails(Organisation)

    val res = testIdentityVerification(u).fail()(FakeRequest())

    status(res) mustBe OK

    val html = contentAsString(res)
    html must include("We’re unable to verify your identity.")
  }


  trait TestCase {
    def mockSessionRepoOrgDetails: PersonalDetailsSessionRepository = {
      val f = mock[PersonalDetailsSessionRepository]
      when(f.start(any())(any(), any())
      ).thenReturn(Future.successful(()))
      when(f.saveOrUpdate(any())(any(), any())
      ).thenReturn(Future.successful(()))
      when(f.get[AdminOrganisationAccountDetails](any(), any())).thenReturn(Future.successful(arbitrary[AdminOrganisationAccountDetails].sample))
      f
    }

    def mockSessionRepoIndDetails: PersonalDetailsSessionRepository = {
      val f = mock[PersonalDetailsSessionRepository]
      when(f.start(any())(any(), any())
      ).thenReturn(Future.successful(()))
      when(f.saveOrUpdate(any())(any(), any())
      ).thenReturn(Future.successful(()))
      when(f.get[IndividualUserAccountDetails](any(), any())).thenReturn(Future.successful(arbitrary[IndividualUserAccountDetails].sample))
      f
    }


    lazy val mockRegistrationService = mock[RegistrationService]

    lazy val stubIdentityVerificationServiceEnrolmentOrg = new IvService(mockCustomErrorHandler, mockRegistrationService, mockSessionRepoOrgDetails, app.injector.instanceOf[IdentityVerificationProxyConnector], applicationConfig)

    def testIdentityVerification(userDetails: UserDetails) =
      new IdentityVerification(mockCustomErrorHandler,
        ggPreauthenticated(userDetails), StubIdentityVerification, StubAddresses,
        StubIndividualAccountConnector, stubIdentityVerificationServiceEnrolmentOrg, StubGroupAccountConnector, mockSessionRepoOrgDetails)

    val request = FakeRequest()

    def requestWithJourneyId(id: String) = request.withSession("journeyId" -> id)
  }

}
