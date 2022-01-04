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

package services

import config.ApplicationConfig
import connectors.identityVerificationProxy.IdentityVerificationProxyConnector
import models.identityVerificationProxy.{Journey, Link}
import models.registration._
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.scalacheck.Arbitrary.arbitrary
import repositories.SessionRepo
import services.iv.IdentityVerificationService
import uk.gov.hmrc.auth.core.AffinityGroup._
import uk.gov.hmrc.http.HeaderCarrier
import utils._

import scala.concurrent.Future

class IvServiceSpec extends ServiceSpec {

  "continue" should {
    "return a successful registration result if registration was successful for a new organisation" in new TestCase {
      StubGroupAccountConnector.stubAccount(groupAccount(agent = true))
      when(mockRegistrationService.continue(any(), any())(any(), any()))
        .thenReturn(Future.successful(Some(RegistrationSuccess(1L))))
      when(ivProxy.start(any[Journey])(any[HeaderCarrier])).thenReturn(Future.successful(Link("")))

      val res: Future[Option[RegistrationResult]] = identityVerification.continue(None, userDetails(Organisation))
      res.futureValue should be(Some(RegistrationSuccess(1L)))
    }

    "return a failed registration result if registration failed for a new organisation" in new TestCase {
      StubGroupAccountConnector.stubAccount(groupAccount(agent = true))
      when(mockRegistrationService.continue(any(), any())(any(), any()))
        .thenReturn(Future.successful(Some(EnrolmentFailure)))
      when(ivProxy.start(any[Journey])(any[HeaderCarrier])).thenReturn(Future.successful(Link("")))

      val res: Future[Option[RegistrationResult]] = identityVerification.continue(None, userDetails(Organisation))
      res.futureValue should be(Some(EnrolmentFailure))
    }

    "return a successful registration result if registration was successful for a new individual" in new TestCase {
      override lazy val mockSessionRepoOrgDetails = mockSessionRepoIndDetails
      StubGroupAccountConnector.stubAccount(groupAccount(agent = true))
      StubIndividualAccountConnector.stubAccount(detailedIndividualAccount)
      when(mockRegistrationService.continue(any(), any())(any(), any()))
        .thenReturn(Future.successful(Some(RegistrationSuccess(1L))))
      when(ivProxy.start(any[Journey])(any[HeaderCarrier])).thenReturn(Future.successful(Link("")))

      val res: Future[Option[RegistrationResult]] = identityVerification.continue(None, userDetails())
      res.futureValue should be(Some(RegistrationSuccess(1L)))
    }

    "return a failed registration result if registration failed for a new individual" in new TestCase {
      override lazy val mockSessionRepoOrgDetails = mockSessionRepoIndDetails
      StubGroupAccountConnector.stubAccount(groupAccount(agent = true))
      StubIndividualAccountConnector.stubAccount(detailedIndividualAccount)
      when(mockRegistrationService.continue(any(), any())(any(), any()))
        .thenReturn(Future.successful(Some(EnrolmentFailure)))
      when(ivProxy.start(any[Journey])(any[HeaderCarrier])).thenReturn(Future.successful(Link("")))

      val res: Future[Option[RegistrationResult]] = identityVerification.continue(None, userDetails())
      res.futureValue should be(Some(EnrolmentFailure))
    }
  }

  trait TestCase {
    implicit val headerCarrier: HeaderCarrier = HeaderCarrier()

    lazy val mockSessionRepoOrgDetails: SessionRepo = {
      val f = mock[SessionRepo]
      when(f.start(any())(any(), any())).thenReturn(Future.successful(()))
      when(f.saveOrUpdate(any())(any(), any())).thenReturn(Future.successful(()))
      when(f.get[AdminOrganisationAccountDetails](any(), any()))
        .thenReturn(Future.successful(arbitrary[AdminOrganisationAccountDetails].sample))
      f
    }

    lazy val mockSessionRepoIndDetails: SessionRepo = {
      val f = mock[SessionRepo]
      when(f.start(any())(any(), any())).thenReturn(Future.successful(()))
      when(f.saveOrUpdate(any())(any(), any())).thenReturn(Future.successful(()))
      when(f.get[IndividualUserAccountDetails](any(), any()))
        .thenReturn(Future.successful(arbitrary[IndividualUserAccountDetails].sample))
      f
    }

    lazy val mockRegistrationService = mock[RegistrationService]

    protected val ivProxy = mock[IdentityVerificationProxyConnector]

    protected val identityVerification = new IdentityVerificationService(
      errorHandler = mockCustomErrorHandler,
      registrationService = mockRegistrationService,
      proxyConnector = ivProxy,
      config = app.injector.instanceOf[ApplicationConfig]
    )
  }

  override protected def beforeEach(): Unit = {
    StubIndividualAccountConnector.reset()
    StubGroupAccountConnector.reset()
  }
}
