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
import models.identityVerificationProxy.IvResult.IvFailure
import models.registration._
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.scalacheck.Arbitrary._
import org.scalatest.prop.Tables.Table
import org.scalatest.prop.{TableDrivenPropertyChecks, TableFor2}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.twirl.api.Html
import repositories.PersonalDetailsSessionRepository
import resources._
import services.RegistrationService
import services.iv.IvService
import uk.gov.hmrc.auth.core.AffinityGroup._
import utils._

import scala.concurrent.Future

class IdentityVerificationSpec extends VoaPropertyLinkingSpec {

  "Successfully verifying identity when an organisation does not have a CCA account" must
    "register and enrol the user then redirect to the registration success page" in new TestCase {
    StubIdentityVerification.stubSuccessfulJourney("successfuljourney")
    when(mockRegistrationService.create(any(), any(), any())(any())(any(), any())).thenReturn(Future.successful(RegistrationSuccess(1L)))

    val res = testIdentityVerification(userDetails(Organisation)).success()(requestWithJourneyId("successfuljourney"))
    status(res) mustBe SEE_OTHER
    redirectLocation(res) mustBe Some(controllers.registration.routes.RegistrationController.success(1L).url)
  }

  "Successfully verifying identity when an individual does not have a CCA account" must
    "register and enrol the user then redirect to the registration success page" in new TestCase {
    override lazy val mockSessionRepoOrgDetails = mockSessionRepoIndDetails
    StubIdentityVerification.stubSuccessfulJourney("successfuljourney")
    when(mockRegistrationService.create(any(), any(), any())(any())(any(), any())).thenReturn(Future.successful(RegistrationSuccess(1L)))

    val res = testIdentityVerification(userDetails(Individual)).success()(requestWithJourneyId("successfuljourney"))
    status(res) mustBe SEE_OTHER
    redirectLocation(res) mustBe Some(controllers.registration.routes.RegistrationController.success(1L).url)
  }

  "Successfully verifying identity" must
    "return internal server error when the registration or enrolment fails" in new TestCase {
    when(mockCustomErrorHandler.internalServerErrorTemplate(any()))
      .thenReturn(Html("INTERNAL SERVER ERROR"))
    StubIdentityVerification.stubSuccessfulJourney("successfuljourney")
    when(mockRegistrationService.create(any(), any(), any())(any())(any(), any())).thenReturn(Future.successful(EnrolmentFailure))

    val res = testIdentityVerification(userDetails(Organisation)).success()(requestWithJourneyId("successfuljourney"))

    status(res) mustBe INTERNAL_SERVER_ERROR
  }

  "Successfully verifying identity" must
    "return internal server error when there are details missing" in new TestCase {
    when(mockCustomErrorHandler.internalServerErrorTemplate(any()))
      .thenReturn(Html("INTERNAL SERVER ERROR"))
    StubIdentityVerification.stubSuccessfulJourney("successfuljourney")
    when(mockRegistrationService.create(any(), any(), any())(any())(any(), any())).thenReturn(Future.successful(DetailsMissing))

    val res = testIdentityVerification(userDetails(Organisation)).success()(requestWithJourneyId("successfuljourney"))

    status(res) mustBe INTERNAL_SERVER_ERROR
  }

  "Manually navigating to the iv success page after failing identity verification" must "return a 401 Unauthorised response" in new TestCase {
    StubIdentityVerification.stubFailedJourney("somejourneyid")
    val res = testIdentityVerification(userDetails(Organisation)).success()(requestWithJourneyId("somejourneyid"))
    status(res) mustBe UNAUTHORIZED
  }

  "Navigating to the iv failed page" must "return the failed page" in new TestCase {
    StubIdentityVerification.stubFailedJourney("somejourneyid")
    val res = testIdentityVerification(userDetails(Organisation)).fail()(requestWithJourneyId("failed journey"))
    status(res) mustBe OK
    contentAsString(res) must include("Identity verification failed")
  }
  "Navigating to restoreSession" must "redirect to the iv success page" in new TestCase {
    val res = testIdentityVerification(userDetails(Organisation)).restoreSession()(requestWithJourneyId("somejourneyid"))
    status(res) mustBe SEE_OTHER
    redirectLocation(res) mustBe Some(routes.IdentityVerification.success().url)
  }

  "fail" must "redirect the user to the identity verification failure page with appropriate error message detailing the cause of the IV failure" in new TestCase {
    val scenarios: TableFor2[IvFailure, String] = Table(
      ("IV Failure", "expected text"),
      IvFailure.Incomplete -> "you have not answered all the questions",
      IvFailure.FailedMatching -> "the details you have given do not match our records",
      IvFailure.FailedDirectorCheck -> "the details you have given do not match the director records from Companies House",
      IvFailure.InsufficientEvidence -> "you have not given us enough information",
      IvFailure.LockedOut -> "you answered questions incorrectly too many times. You will need to wait 24 hours before you can try again",
      IvFailure.UserAborted -> "you have chosen not to continue",
      IvFailure.Timeout -> "you have been timed out due to inactivity",
      IvFailure.TechnicalIssue -> "of a technical issue with this service",
      IvFailure.PreconditionFailed -> "of a technical issue with this service",
      IvFailure.Deceased -> "of a technical issue with this service",
      IvFailure.FailedIV -> "of a technical issue with this service"
    )

    TableDrivenPropertyChecks.forAll(scenarios) {
      case (failure, expectedText) =>
        StubIdentityVerification.stubFailedJourney("somejourneyid", failure)
        val res = testIdentityVerification(userDetails(Organisation)).fail()(requestWithJourneyId("failed journey"))
        status(res) mustBe OK
        val html = contentAsString(res)
        html must include(s"We’re unable to verify your identity because $expectedText.")
    }

  }


  trait TestCase {
    def mockSessionRepoOrgDetails: PersonalDetailsSessionRepository = {
      val f = mock[PersonalDetailsSessionRepository]
      when(f.start(any())(any(), any())).thenReturn(Future.successful(()))
      when(f.saveOrUpdate(any())(any(), any())).thenReturn(Future.successful(()))
      when(f.get[AdminOrganisationAccountDetails](any(), any())).thenReturn(Future.successful(arbitrary[AdminOrganisationAccountDetails].sample))
      f
    }

    def mockSessionRepoIndDetails: PersonalDetailsSessionRepository = {
      val f = mock[PersonalDetailsSessionRepository]
      when(f.start(any())(any(), any())).thenReturn(Future.successful(()))
      when(f.saveOrUpdate(any())(any(), any())).thenReturn(Future.successful(()))
      when(f.get[IndividualUserAccountDetails](any(), any())).thenReturn(Future.successful(arbitrary[IndividualUserAccountDetails].sample))
      f
    }

    lazy val mockRegistrationService = mock[RegistrationService]

    lazy val stubIdentityVerificationServiceEnrolmentOrg =
      new IvService(
        errorHandler = mockCustomErrorHandler,
        registrationService = mockRegistrationService,
        personalDetailsSessionRepo = mockSessionRepoOrgDetails,
        proxyConnector = mock[IdentityVerificationProxyConnector],
        config = applicationConfig
      )

    def testIdentityVerification(userDetails: UserDetails): IdentityVerification =
      new IdentityVerification(
        errorHandler = mockCustomErrorHandler,
        ggAction = ggPreauthenticated(userDetails),
        identityVerificationConnector = StubIdentityVerification,
        identityVerificationService = stubIdentityVerificationServiceEnrolmentOrg,
        personalDetailsSessionRepo = mockSessionRepoOrgDetails
      )

    def requestWithJourneyId(id: String) = FakeRequest(GET, s"/?journeyId=$id").withSession("journeyId" -> id)
  }

}
