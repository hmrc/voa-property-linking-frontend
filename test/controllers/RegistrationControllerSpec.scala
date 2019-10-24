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

import actions.registration.SessionUserDetailsAction
import controllers.registration.RegistrationController
import models.identityVerificationProxy.Link
import models.registration.{RegistrationSuccess, User, UserDetails}
import models.{DetailedIndividualAccount, GroupAccount, IndividualDetails}
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.scalacheck.Arbitrary.arbitrary
import org.scalatestplus.mockito.MockitoSugar
import play.api.mvc.{AnyContent, AnyContentAsFormUrlEncoded}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import repositories.SessionRepo
import resources._
import services.iv.IdentityVerificationService
import services.{RegistrationService, Success}
import uk.gov.hmrc.auth.core.AffinityGroup.{Agent, Organisation}
import uk.gov.hmrc.auth.core.{AffinityGroup, Assistant, User}
import utils.{StubGroupAccountConnector, _}

import scala.concurrent.Future

class RegistrationControllerSpec extends VoaPropertyLinkingSpec with MockitoSugar {

  lazy val mockSessionRepo = {
    val f = mock[SessionRepo]
    when(f.start(any())(any(), any())
    ).thenReturn(Future.successful(()))
    when(f.saveOrUpdate(any())(any(), any())
    ).thenReturn(Future.successful(()))
    f
  }


  val mockIdentityVerificationService = mock[IdentityVerificationService]

  val mockRegistrationService = mock[RegistrationService]

  val mockSessionUserDetailsAction = mock[SessionUserDetailsAction]

  private def testRegistrationController(userDetails: UserDetails, sessionUserDetails: User = adminOrganisationAccountDetails): RegistrationController = new RegistrationController(
    mockCustomErrorHandler,
    ggPreauthenticated(userDetails),
    sessionUserDetailsAction(sessionUserDetails),
    StubGroupAccountConnector,
    StubIndividualAccountConnector,
    StubAddresses,
    mockRegistrationService,
    mockSessionRepo
  )

  "Going directly to the complete-contact-details page, when logged in with an already registered VOA account" should
    "redirect the user to the dashboard" in {
    StubIndividualAccountConnector.stubAccount(arbitrary[DetailedIndividualAccount].sample.get.copy(externalId = ggExternalId))

    val res = testRegistrationController(userDetails()).show()(FakeRequest())

    status(res) mustBe SEE_OTHER
    redirectLocation(res) mustBe Some(controllers.routes.Dashboard.home().url)
  }

  "Going to the create account page, when logged in with an account that has not registered and has an Individual affinity group" should
    "display the create individual account form" in {

    val user = userDetails(affinityGroup = AffinityGroup.Individual)

    val res = testRegistrationController(user, individualUserAccountDetails).show()(FakeRequest())
    status(res) mustBe OK

    val html = HtmlPage(res)
    html.mustContainText("Mobile number")
    html.inputMustContain("email", user.email)
    html.inputMustContain("confirmedEmail", user.email)
    html.inputMustContain("firstName", user.firstName.get)
    html.inputMustContain("lastName", user.lastName.get)
    html.inputMustContain("addresspostcode", user.postcode.get)
  }

  "Going to the create account page, when logged in with an account that is an Agent" should
    "display the invalid account type page" in {
    val res = testRegistrationController(userDetails(affinityGroup = Agent)).show()(FakeRequest())
    status(res) mustBe OK

    val html = HtmlPage(res)
    html.mustContainText("You’ve tried to register using an existing Agent Government Gateway account")
  }

  "Going to the create account page, when logged in with an account that has not registered and has an Organisation affinity group" should
    "display the create organisation account form" in {
    val user = userDetails(Organisation)
    val res = testRegistrationController(user).show()(FakeRequest())
    status(res) mustBe OK

    val html = HtmlPage(res)
    html.inputMustContain("addresspostcode", user.postcode.get)

    html.mustContainText("Business name")
    html.inputMustContain("email", user.email)
    html.inputMustContain("confirmedBusinessEmail", user.email)
    html.inputMustContain("firstName", user.firstName.get)
    html.inputMustContain("lastName", user.lastName.get)
  }

  "Going to the create account page when logged in as a new assistant user registering with an existing group account" should
    "display the complete your contact details form for an assistant" in {
    val user: UserDetails = userDetails(affinityGroup = Organisation, credentialRole = Assistant)
    val groupAccount = arbitrary[GroupAccount].sample.get.copy(groupId = user.groupIdentifier)
    StubGroupAccountConnector.stubAccount(groupAccount)

    val res = testRegistrationController(user, adminInExistingOrganisationAccountDetails).show()(FakeRequest())
    status(res) mustBe OK

    val html = HtmlPage(res)
    html.mustContainTextInput("#firstName")
    html.mustContainTextInput("#lastName")
    html.mustContainText("You have been added as a user to your organisation, please confirm your details below")
  }

  "Submitting an invalid assistant form" should "return a bad request response" in {
    StubGroupAccountConnector.stubAccount(groupAccount(agent = true))

    val data = Map(
      "firstName" -> Seq("first")
    )
    val fakeRequest: FakeRequest[AnyContent] = FakeRequest().withBody(AnyContentAsFormUrlEncoded(data))
    val res = testRegistrationController(userDetails()).submitAssistant()(fakeRequest)
    status(res) mustBe BAD_REQUEST

    val html = HtmlPage(res)
    html.mustContainText("Last Name - This must be filled in")
    html.mustNotContainText("First Name - This must be filled in")
  }

  "Going to the create account page when logged in as a new admin user registering with an existing group account" should
    "display the complete your contact details form for an admin" in {
    val user: UserDetails = userDetails(affinityGroup = Organisation, credentialRole = User)
    val ga: GroupAccount = arbitrary[GroupAccount].sample.get.copy(groupId = user.groupIdentifier)
    StubGroupAccountConnector.stubAccount(ga)

    val res = testRegistrationController(user).show()(FakeRequest())
    status(res) mustBe OK

    val html = HtmlPage(res)
    html.mustContainText("You have been added as a user to your organisation, please confirm your details below")
    html.mustContainTextInput("#firstName")
    html.mustContainTextInput("#lastName")
    html.mustContainTextInput("#dobday")
    html.mustContainTextInput("#dobmonth")
    html.mustContainTextInput("#dobyear")
    html.mustContainTextInput("#nino")
  }

  "Going to the create account page when logged in as a new assistant user registering without an existing group account" should
    "display the invalid account creation page" in {
    val res = testRegistrationController(userDetails(affinityGroup = Organisation, credentialRole = Assistant)).show()(FakeRequest())
    status(res) mustBe OK

    val html = HtmlPage(res)
    html.mustContainText("Registration failed You can’t register until the Administrator from your organisation registers first.")
  }

  "Submitting an invalid individual form" should "return a bad request response" in {
    val res = testRegistrationController(userDetails()).submitIndividual()(FakeRequest())
    status(res) mustBe BAD_REQUEST
  }

  "Submitting a valid individual form" should "return a redirect" in {
    when(mockEnrolmentService.enrol(any(), any())(any(), any())).thenReturn(Future.successful(Success))
    when(mockRegistrationService.create(any(), any(), any())(any())(any(), any())).thenReturn(Future.successful(RegistrationSuccess(1L)))
    when(mockIdentityVerificationService.start(any())(any(), any())).thenReturn(Future.successful(Link("")))
    val (groupId, externalId): (String, String) = (shortString, shortString)
    StubIndividualAccountConnector.stubAccount(DetailedIndividualAccount(externalId, "", 1L, 2l, IndividualDetails("", "", "", "", None, 12)))

    val data = Map(
      "firstName" -> Seq("first"),
      "lastName" -> Seq("second"),
      "email" -> Seq("x@x.com"),
      "confirmedEmail" -> Seq("x@x.com"),
      "phone" -> Seq("1234567891"),
      "mobilePhone" -> Seq("123456"),
      "address.line1" -> Seq("1234567"),
      "address.line2" -> Seq(""),
      "address.line3" -> Seq(""),
      "address.line4" -> Seq(""),
      "address.postcode" -> Seq("12345"),
      "nino" -> Seq("AA000001B"),
      "dob.day" -> Seq("11"),
      "dob.month" -> Seq("02"),
      "dob.year" -> Seq("1980")
    )

    val fakeRequest: FakeRequest[AnyContent] = FakeRequest().withBody(AnyContentAsFormUrlEncoded(data))
    val res = testRegistrationController(userDetails()).submitIndividual()(fakeRequest)
    status(res) mustBe SEE_OTHER
  }

  "Submitting an invalid organisation form" should "return a bad request response" in {
    val res = testRegistrationController(userDetails()).submitOrganisation()(FakeRequest())
    status(res) mustBe BAD_REQUEST
  }

  "Submitting a valid organisation form" should "return a redirect" in {
    when(mockIdentityVerificationService.start(any())(any(), any())).thenReturn(Future.successful(Link("")))
    when(mockEnrolmentService.enrol(any(), any())(any(), any())).thenReturn(Future.successful(Success))
    when(mockRegistrationService.create(any(), any(), any())(any())(any(), any())).thenReturn(Future.successful(RegistrationSuccess(1L)))
    val externalId: String = shortString
    StubIndividualAccountConnector.stubAccount(DetailedIndividualAccount(externalId, "", 1L, 2l, IndividualDetails("", "", "", "", None, 12)))

    val data = Map(
      "companyName" -> Seq("company"),
      "firstName" -> Seq("first"),
      "lastName" -> Seq("second"),
      "address.line1" -> Seq("123456"),
      "address.line2" -> Seq(""),
      "address.line3" -> Seq(""),
      "address.line4" -> Seq(""),
      "address.postcode" -> Seq("post"),
      "phone" -> Seq("12345"),
      "email" -> Seq("x@x.com"),
      "confirmedBusinessEmail" -> Seq("x@x.com"),
      "isAgent" -> Seq("false"),
      "nino" -> Seq("AA000001B"),
      "dob.day" -> Seq("11"),
      "dob.month" -> Seq("02"),
      "dob.year" -> Seq("1980")
    )
    val fakeRequest: FakeRequest[AnyContent] = FakeRequest().withBody(AnyContentAsFormUrlEncoded(data))
    val res = testRegistrationController(userDetails()).submitOrganisation()(fakeRequest)
    status(res) mustBe SEE_OTHER
  }
}
