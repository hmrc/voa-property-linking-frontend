/*
 * Copyright 2018 HM Revenue & Customs
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

import java.time.LocalDate

import config.ApplicationConfig
import controllers.GroupAccountDetails
import models._
import models.enrolment._
import models.identityVerificationProxy.Link
import org.mockito.ArgumentMatchers.{eq => matching, _}
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{FlatSpec, MustMatchers}
import resources.{shortString, _}
import services.iv.IdentityVerificationService
import uk.gov.hmrc.auth.core.{AffinityGroup, User}
import uk.gov.hmrc.auth.core.AffinityGroup.Individual
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.HeaderCarrier
import utils._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}

class RegistrationServiceSpec extends ServiceSpec {

  "create" should "return enrolment success" in new TestCase {
    when(mockEnrolmentService.enrol(any(), any())(any(), any())).thenReturn(Future.successful(Success))
    StubIndividualAccountConnector.stubAccount(DetailedIndividualAccount(externalId, "", 1l, 2l, IndividualDetails("", "", "", "", None, 12)))

    when(mockIdentityVerficationService.start(any[IVDetails])(any[HeaderCarrier], any[ExecutionContext])).thenReturn(Future.successful(Link("some/link")))
    val res: Future[RegistrationResult] = registrationService
      .create(
        GroupAccountDetails("", Address(None, "", "", "", "", ""), "", "", "", false),
        IVDetails("", "", LocalDate.now(), Nino("AA012345A")),
        UserDetails("", UserInfo(None, None, "", None, "", "", Individual, User))
      )(userDetails => int => opt => IndividualAccountSubmission("", "", opt, IndividualDetails("", "", "", "", None, 12)))

    res.futureValue must be(EnrolmentSuccess(Link("some/link"), 2l))
  }

  "create" should "return enrolment failure" in new TestCase {
    when(mockEnrolmentService.enrol(any(), any())(any(), any())).thenReturn(Future.successful(Failure))
    StubIndividualAccountConnector.stubAccount(DetailedIndividualAccount(externalId, "", 1l, 2l, IndividualDetails("", "", "", "", None, 12)))

    when(mockIdentityVerficationService.start(any[IVDetails])(any[HeaderCarrier], any[ExecutionContext])).thenReturn(Future.successful(Link("some/link")))
    val res: Future[RegistrationResult] = registrationService
      .create(
        GroupAccountDetails("", Address(None, "", "", "", "", ""), "", "", "", false),
        IVDetails("", "", LocalDate.now(), Nino("AA012345A")),
        UserDetails("", UserInfo(None, None, "", None, "", "", Individual, User))
      )(userDetails => int => opt => IndividualAccountSubmission("", "", opt, IndividualDetails("", "", "", "", None, 12)))

    res.futureValue must be(EnrolmentFailure)
  }

  "create" should "return missing details" in new TestCase {

    when(mockIdentityVerficationService.start(any[IVDetails])(any[HeaderCarrier], any[ExecutionContext])).thenReturn(Future.successful(Link("some/link")))
    val res: Future[RegistrationResult] = registrationService
      .create(
        GroupAccountDetails("", Address(None, "", "", "", "", ""), "", "", "", false),
        IVDetails("", "", LocalDate.now(), Nino("AA012345A")),
        UserDetails("", UserInfo(None, None, "", None, "", "", Individual, User))
      )(userDetails => int => opt => IndividualAccountSubmission("", "", opt, IndividualDetails("", "", "", "", None, 12)))

    res.futureValue must be(DetailsMissing)
  }

  trait TestCase {
    val (groupId, externalId): (String, String) = (shortString, shortString)
    StubAuthConnector.stubGroupId(groupId)
    StubAuthConnector.stubExternalId(externalId)
    StubAuthConnector.stubUserDetails(externalId, testOrganisationInfo)

    implicit val hc = HeaderCarrier()

    def testOrganisationInfo = UserInfo(firstName = Some("Bob"),
      lastName = Some("Smith"),
      email = "bob@smith.com",
      postcode = Some("AB12 3CD"),
      groupIdentifier = "GroupIdenfifier",
      affinityGroup = AffinityGroup.Organisation,
      gatewayId = "",
      credentialRole = User)


    protected val mockEnrolmentService = mock[EnrolmentService]
    protected val mockIdentityVerficationService = mock[IdentityVerificationService]

    protected val registrationService = new RegistrationService(
      groupAccounts = StubGroupAccountConnector,
      individualAccounts = StubIndividualAccountConnector,
      enrolmentService = mockEnrolmentService,
      auth = StubAuthConnector,
      addresses = StubAddresses,
      StubEmailService,
      mockIdentityVerficationService,
      app.injector.instanceOf[ApplicationConfig]
    )
  }
}
