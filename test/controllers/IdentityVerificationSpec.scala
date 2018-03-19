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

package controllers
import config.ApplicationConfig
import connectors.identityVerificationProxy.IdentityVerificationProxyConnector
import models.{GroupAccount, PersonalDetails}
import org.jsoup.Jsoup
import org.mockito.ArgumentMatchers.{eq => matching, _}
import org.mockito.Mockito._
import org.scalacheck.Arbitrary._
import org.scalatest.mockito.MockitoSugar
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.FakeRequest
import play.api.test.Helpers._
import repositories.PersonalDetailsSessionRepository
import resources._
import services.iv.{IdentityVerificationService, IdentityVerificationServiceNonEnrolment}
import utils.{StubAddresses, StubAuthConnector, StubGGAction, StubGroupAccountConnector, StubIdentityVerification, StubIndividualAccountConnector, _}

import scala.concurrent.Future
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.HeaderCarrierConverter

class IdentityVerificationSpec extends ControllerSpec with MockitoSugar {

  lazy val mockSessionRepo = {
    val f = mock[PersonalDetailsSessionRepository]
    when(f.start(any())(any(), any())
    ).thenReturn(Future.successful(()))
    when(f.saveOrUpdate(any())(any(), any())
    ).thenReturn(Future.successful(()))
    when(f.get[PersonalDetails](any(), any())).thenReturn(Future.successful(arbitrary[PersonalDetails].sample))
    f
  }

  lazy val stubIdentityVerificationService = new IdentityVerificationServiceNonEnrolment(StubAuthConnector, StubIndividualAccountConnector, app.injector.instanceOf[IdentityVerificationProxyConnector], mockSessionRepo, applicationConfig, StubGroupAccountConnector, StubAddresses)

  private object TestIdentityVerification extends IdentityVerification(StubGGAction, StubIdentityVerification, StubAddresses,
    StubIndividualAccountConnector, stubIdentityVerificationService, StubGroupAccountConnector,
    StubAuthConnector, mockSessionRepo)

  val request = FakeRequest()
  private def requestWithJourneyId(id: String) = request.withSession("journeyId" -> id)

  "Successfully verifying identity when the group does not have a CCA account" must
    "display the successful iv confirmation page, and not create an individual account" in {
    StubAuthConnector.stubExternalId("externalId")
    StubAuthConnector.stubGroupId("groupwithoutaccount")
    StubIdentityVerification.stubSuccessfulJourney("successfuljourney")

    val res = TestIdentityVerification.success()(requestWithJourneyId("successfuljourney"))
    status(res) mustBe OK

    val content = contentAsString(res)
    val html = Jsoup.parse(content)
    html.select("h1").html must equal ("We’ve verified your identity") withClue "Page did not contain success summary"
    html.select(s"a.button[href=${routes.CreateGroupAccount.show.url}]").size must equal (1) withClue "Page did not contain link to create group account"

    implicit val hc = HeaderCarrierConverter.fromHeadersAndSession(request.headers, Some(request.session))

    await(StubIndividualAccountConnector.withExternalId("externalId")) mustBe None
  }

  "Successfully verifying identity when the group does have a CCA account" must "display a confirmation page, and create the individual account" in {
    StubAuthConnector.stubExternalId("individualwithoutaccount")
    StubAuthConnector.stubGroupId("groupwithaccount")
    val groupAccount = arbitrary[GroupAccount].sample.get
    StubGroupAccountConnector.stubAccount(groupAccount.copy(groupId = "groupwithaccount"))
    StubIdentityVerification.stubSuccessfulJourney("anothersuccess")

    val res = TestIdentityVerification.success()(requestWithJourneyId("anothersuccess"))
    status(res) mustBe OK

    val html = Jsoup.parse(contentAsString(res))
    html.select("h1").html must equal (s"${groupAccount.companyName} has already registered.") withClue "Page did not contain success summary"
    html.select(s"a.button[href=${routes.Dashboard.home.url}]").size must equal (1) withClue "Page did not contain dashboard link"

    implicit val hc = HeaderCarrierConverter.fromHeadersAndSession(request.headers, Some(request.session))

    StubIndividualAccountConnector.withExternalId("individualwithoutaccount") must not be None
  }

  "Manually navigating to the iv success page after failing identity verification" must "return a 401 Unauthorised response" in {
    StubIdentityVerification.stubFailedJourney("somejourneyid")

    val res = TestIdentityVerification.success()(request.withSession("journey-id" -> "somejourneyid"))
    status(res) mustBe UNAUTHORIZED
  }
}
