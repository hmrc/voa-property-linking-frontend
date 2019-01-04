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

package services

import config.ApplicationConfig
import connectors.identityVerificationProxy.IdentityVerificationProxyConnector
import models._
import models.registration._
import models.identityVerificationProxy.{Journey, Link}
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.scalacheck.Arbitrary.arbitrary
import repositories.SessionRepo
import resources.{shortString, _}
import services.iv.IvService
import uk.gov.hmrc.auth.core.AffinityGroup.Individual
import uk.gov.hmrc.auth.core.{Admin, AffinityGroup, User}
import uk.gov.hmrc.http.HeaderCarrier
import utils._

import scala.concurrent.Future

class IvServiceSpec extends ServiceSpec {

  "continue" should "return a successful registration result if registration was successful for a new organisation" in new TestCase {
    StubGroupAccountConnector.stubAccount(GroupAccount(1l, groupId, "", 12, "", "", false, 1l))
    StubIndividualAccountConnector.stubAccount(DetailedIndividualAccount(externalId, "", 1l, 2l, IndividualDetails("", "", "", "", None, 12)))
    when(mockRegistrationService.create(any(), any())(any())(any(), any())).thenReturn(Future.successful(RegistrationSuccess(1l)))
    when(ivProxy.start(any[Journey])(any[HeaderCarrier])).thenReturn(Future.successful(Link("")))
    val res: Future[Option[RegistrationResult]] = identityVerification.continue("")(UserDetails("", UserInfo(None, None, "", None, "", "", Individual, User)), hc, ec)
    res.futureValue must be(Some(RegistrationSuccess(1l)))
  }

  "continue" should "return a failed registration result if registration failed for a new organisation" in new TestCase {
    StubGroupAccountConnector.stubAccount(GroupAccount(1l, groupId, "", 12, "", "", false, 1l))
    StubIndividualAccountConnector.stubAccount(DetailedIndividualAccount(externalId, "", 1l, 2l, IndividualDetails("", "", "", "", None, 12)))
    when(mockRegistrationService.create(any(), any())(any())(any(), any())).thenReturn(Future.successful(EnrolmentFailure))
    when(ivProxy.start(any[Journey])(any[HeaderCarrier])).thenReturn(Future.successful(Link("")))
    val res: Future[Option[RegistrationResult]] = identityVerification.continue("")(UserDetails("", UserInfo(None, None, "", None, "", "", Individual, User)), hc, ec)
    res.futureValue must be(Some(EnrolmentFailure))
  }

  "continue" should "return a successful registration result if registration was successful for a new individual" in new TestCase {
    override lazy val mockSessionRepoOrgDetails = mockSessionRepoIndDetails
    StubVplAuthConnector.stubUserDetails(externalId, testIndividualInfo)
    StubGroupAccountConnector.stubAccount(GroupAccount(1l, groupId, "", 12, "", "", false, 1l))
    StubIndividualAccountConnector.stubAccount(DetailedIndividualAccount(externalId, "", 1l, 2l, IndividualDetails("", "", "", "", None, 12)))
    when(mockRegistrationService.create(any(), any())(any())(any(), any())).thenReturn(Future.successful(RegistrationSuccess(1l)))
    when(ivProxy.start(any[Journey])(any[HeaderCarrier])).thenReturn(Future.successful(Link("")))
    val res: Future[Option[RegistrationResult]] = identityVerification.continue("")(UserDetails("", UserInfo(None, None, "", None, "", "", Individual, User)), hc, ec)
    res.futureValue must be(Some(RegistrationSuccess(1l)))
  }

  "continue" should "return a failed registration result if registration failed for a new individual" in new TestCase {
    override lazy val mockSessionRepoOrgDetails = mockSessionRepoIndDetails
    StubVplAuthConnector.stubUserDetails(externalId, testIndividualInfo)
    StubGroupAccountConnector.stubAccount(GroupAccount(1l, groupId, "", 12, "", "", false, 1l))
    StubIndividualAccountConnector.stubAccount(DetailedIndividualAccount(externalId, "", 1l, 2l, IndividualDetails("", "", "", "", None, 12)))
    when(mockRegistrationService.create(any(), any())(any())(any(), any())).thenReturn(Future.successful(EnrolmentFailure))
    when(ivProxy.start(any[Journey])(any[HeaderCarrier])).thenReturn(Future.successful(Link("")))
    val res: Future[Option[RegistrationResult]] = identityVerification.continue("")(UserDetails("", UserInfo(None, None, "", None, "", "", Individual, User)), hc, ec)
    res.futureValue must be(Some(EnrolmentFailure))
  }

  trait TestCase {
    val (groupId, externalId): (String, String) = (shortString, shortString)
    StubVplAuthConnector.stubGroupId(groupId)
    StubVplAuthConnector.stubExternalId(externalId)
    StubVplAuthConnector.stubUserDetails(externalId, testOrganisationInfo)

    lazy val mockSessionRepoOrgDetails = {
      val f = mock[SessionRepo]
      when(f.start(any())(any(), any())
      ).thenReturn(Future.successful(()))
      when(f.saveOrUpdate(any())(any(), any())
      ).thenReturn(Future.successful(()))
      when(f.get[AdminOrganisationAccountDetails](any(), any())).thenReturn(Future.successful(arbitrary[AdminOrganisationAccountDetails].sample))
      f
    }

    lazy val mockSessionRepoIndDetails = {
      val f = mock[SessionRepo]
      when(f.start(any())(any(), any())
      ).thenReturn(Future.successful(()))
      when(f.saveOrUpdate(any())(any(), any())
      ).thenReturn(Future.successful(()))
      when(f.get[IndividualUserAccountDetails](any(), any())).thenReturn(Future.successful(arbitrary[IndividualUserAccountDetails].sample))
      f
    }

    lazy val mockRegistrationService = mock[RegistrationService]

    implicit val ec = play.api.libs.concurrent.Execution.Implicits.defaultContext
    implicit val hc = HeaderCarrier()

    def testOrganisationInfo = UserInfo(firstName = Some("Bob"),
      lastName = Some("Smith"),
      email = "bob@smith.com",
      postcode = Some("AB12 3CD"),
      groupIdentifier = "GroupIdenfifier",
      affinityGroup = AffinityGroup.Organisation,
      gatewayId = "",
      credentialRole = Admin)

    def testIndividualInfo = UserInfo(firstName = Some("Bob"),
      lastName = Some("Smith"),
      email = "bob@smith.com",
      postcode = Some("AB12 3CD"),
      groupIdentifier = "GroupIdenfifier",
      affinityGroup = AffinityGroup.Individual,
      gatewayId = "",
      credentialRole = Admin)

    protected val ivProxy = mock[IdentityVerificationProxyConnector]

    protected val identityVerification = new IvService(
      StubVplAuthConnector,
      mockRegistrationService,
      mockSessionRepoOrgDetails,
      ivProxy,
      app.injector.instanceOf[ApplicationConfig])
  }

  override protected def beforeEach(): Unit = {
    StubIndividualAccountConnector.reset()
    StubGroupAccountConnector.reset()
    StubVplAuthConnector.reset()
    StubIdentityVerification.reset()
    StubPropertyLinkConnector.reset()
    StubAuthentication.reset()
    StubBusinessRatesValuation.reset()
    StubSubmissionIdConnector.reset()
    StubPropertyRepresentationConnector.reset()
  }
}