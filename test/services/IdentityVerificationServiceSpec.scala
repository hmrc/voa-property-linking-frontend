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
import connectors.identityVerificationProxy.IdentityVerificationProxyConnector
import controllers.ControllerSpec
import models._
import models.enrolment._
import models.identityVerificationProxy.{Journey, Link}
import org.mockito.ArgumentMatchers.{eq => matching, _}
import org.mockito.Mockito._
import org.scalacheck.Arbitrary.arbitrary
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{BeforeAndAfterEach, FlatSpec, MustMatchers}
import play.api.libs.json.Reads
import play.api.{Configuration, Environment}
import repositories.SessionRepo
import resources.{shortString, _}
import services.iv.IdentityVerificationServiceNonEnrolment
import uk.gov.hmrc.auth.core.{AffinityGroup, User}
import uk.gov.hmrc.auth.core.AffinityGroup.Individual
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.config.inject.DefaultRunMode
import utils._

import scala.concurrent.Future

class IdentityVerificationServiceSpec extends ServiceSpec {

//  override val additionalAppConfig: Seq[(String, String)] = Seq("microservice.services.auth.host" -> "test.protected.mdtp", "microservice.services.auth.port" -> "90", "microservice.services.identity-verification.host" -> "test.protected.mdtp", "microservice.services.identity-verification.port" -> "90")
  "create" should "return enrolment success" in new TestCase {
    StubIndividualAccountConnector.stubAccount(DetailedIndividualAccount(externalId, "", 1l, 2l, IndividualDetails("", "", "", "", None, 12)))

    when(ivProxy.start(any[Journey])(any[HeaderCarrier])).thenReturn(Future.successful(Link("")))

    val res: Future[Option[GroupAccount]] = identityVerification.continue("")(UserDetails("", UserInfo(None, None, "", None, "", "", Individual, User)), hc, ec)

    res.futureValue must be(None)
  }

  "create" should "return enrolment failure" in new TestCase {
    StubIndividualAccountConnector.stubAccount(DetailedIndividualAccount(externalId, "", 1l, 2l, IndividualDetails("", "", "", "", None, 12)))

    StubGroupAccountConnector.stubAccount(GroupAccount(1l, groupId, "", 12, "", "", false, 1l))

    when(ivProxy.start(any[Journey])(any[HeaderCarrier])).thenReturn(Future.successful(Link("")))

    val res: Future[Option[GroupAccount]] = identityVerification.continue("")(UserDetails("", UserInfo(None, None, "", None, "", "", Individual, User)), hc, ec)

    res.futureValue must be(Some(GroupAccount(1l, groupId, "", 12, "", "", false, 1l)))
  }

  trait TestCase {
    val (groupId, externalId): (String, String) = (shortString, shortString)
    StubAuthConnector.stubGroupId(groupId)
    StubAuthConnector.stubExternalId(externalId)
    StubAuthConnector.stubUserDetails(externalId, testOrganisationInfo)


    lazy val mockSessionRepo = {
      val f = mock[SessionRepo]
      when(f.start(any())(any(), any())
      ).thenReturn(Future.successful(()))
      when(f.saveOrUpdate(any())(any(), any())
      ).thenReturn(Future.successful(()))
      when(f.get[PersonalDetails](any(), any())).thenReturn(Future.successful(arbitrary[PersonalDetails].sample))
      f
    }

    implicit val ec = play.api.libs.concurrent.Execution.Implicits.defaultContext
    implicit val hc = HeaderCarrier()

    def testOrganisationInfo = UserInfo(firstName = Some("Bob"),
      lastName = Some("Smith"),
      email = "bob@smith.com",
      postcode = Some("AB12 3CD"),
      groupIdentifier = "GroupIdenfifier",
      affinityGroup = AffinityGroup.Organisation,
      gatewayId = "",
      credentialRole = User)

    protected val ivProxy = mock[IdentityVerificationProxyConnector]

    protected val identityVerification = new IdentityVerificationServiceNonEnrolment(
      StubAuthConnector,
      StubIndividualAccountConnector,
      ivProxy,
      mockSessionRepo,
      app.injector.instanceOf[ApplicationConfig],
      StubGroupAccountConnector,
      StubAddresses)
  }

  override protected def beforeEach(): Unit = {
    StubIndividualAccountConnector.reset()
    StubGroupAccountConnector.reset()
    StubAuthConnector.reset()
    StubIdentityVerification.reset()
    StubPropertyLinkConnector.reset()
    StubAuthentication.reset()
    StubBusinessRatesValuation.reset()
    StubSubmissionIdConnector.reset()
    StubPropertyRepresentationConnector.reset()
  }
}