/*
 * Copyright 2024 HM Revenue & Customs
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
import models.registration.{RegistrationSuccess, User, UserDetails}
import models.{DetailedIndividualAccount, GroupAccount}
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.scalacheck.Arbitrary.arbitrary
import org.scalatestplus.mockito.MockitoSugar
import play.api.mvc.{AnyContent, AnyContentAsFormUrlEncoded}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import repositories.SessionRepo
import services.RegistrationService
import services.iv.IdentityVerificationService
import uk.gov.hmrc.auth.core.AffinityGroup.{Agent, Organisation}
import uk.gov.hmrc.auth.core.{AffinityGroup, Assistant, ConfidenceLevel, User}
import utils.{StubGroupAccountConnector, _}

import scala.concurrent.Future

class RegistrationControllerSpec extends VoaPropertyLinkingSpec with MockitoSugar {

  lazy val mockSessionRepo: SessionRepo = {
    val f = mock[SessionRepo]
    when(f.start(any())(any(), any())).thenReturn(Future.successful(()))
    when(f.saveOrUpdate(any())(any(), any())).thenReturn(Future.successful(()))
    f
  }

  val mockIdentityVerificationService: IdentityVerificationService = mock[IdentityVerificationService]

  val mockRegistrationService: RegistrationService = mock[RegistrationService]

  val mockSessionUserDetailsAction: SessionUserDetailsAction = mock[SessionUserDetailsAction]

  private def testRegistrationController(userDetails: UserDetails): RegistrationController =
    new RegistrationController(
      errorHandler = mockCustomErrorHandler,
      ggAuthenticated = ggPreauthenticated(userDetails),
      authenticated = preAuthenticatedActionBuilders(),
      sessionUserDetailsAction = sessionUserDetailsAction,
      groupAccounts = StubGroupAccountConnector,
      individualAccounts = StubIndividualAccountConnector,
      addresses = StubAddresses,
      registrationService = mockRegistrationService,
      invalidAccountTypeView = invalidAccountTypeView,
      invalidAccountCreationView = invalidAccountCreationView,
      registerIndividualView = registerIndividualView,
      registerOrganisationView = registerOrganisationView,
      registerAssistantAdminView = registerAssistantAdminView,
      registerAssistantView = registerAssistantView,
      registerConfirmationView = registerConfirmationView,
      confirmationView = confirmationView,
      personalDetailsSessionRepo = mockSessionRepo
    )

  "Going directly to the complete-contact-details page, when logged in with an already registered VOA account" should
    "redirect the user to the dashboard" in {
    StubIndividualAccountConnector.stubAccount(
      arbitrary[DetailedIndividualAccount].sample.get.copy(externalId = ggExternalId))

    val res = testRegistrationController(userDetails()).show()(FakeRequest())

    status(res) shouldBe SEE_OTHER
    redirectLocation(res) shouldBe Some("http://localhost:9542/business-rates-dashboard/home")
  }

  "Going to the create account page, when logged in with an account that has not registered and has an Individual affinity group" should
    "display the create individual account form" in {

    val user = userDetails(affinityGroup = AffinityGroup.Individual)

    val res = testRegistrationController(user).show()(FakeRequest())
    status(res) shouldBe OK

    val html = HtmlPage(res)
    html.shouldContainText("Mobile number")
    html.inputShouldContain("email", user.email)
    html.inputShouldContain("confirmedEmail", user.email)
    html.inputShouldContain("phone", "")
    html.inputShouldContain("mobilePhone", "")
    html.inputShouldContain("tradingName", "")
    html.html.getElementsByClass("manualAddress").hasClass("govuk-!-display-block") shouldBe true
    html.html.getElementsByClass("lookupAddressCancel").hasClass("govuk-!-display-none") shouldBe true
  }

  "Going to the create account page, when logged in with an account that is an Agent" should
    "display the invalid account type page" in {
    val res = testRegistrationController(userDetails(affinityGroup = Agent)).show()(FakeRequest())
    status(res) shouldBe OK

    val html = HtmlPage(res)
    html.shouldContainText("You’ve tried to register using an existing Agent Government Gateway account")
  }

  "Going to the create account page, when logged in with an account that has not registered and has an Organisation affinity group" should
    "display the create organisation account form" in {
    val user = userDetails(Organisation)
    val res = testRegistrationController(user).show()(FakeRequest())
    status(res) shouldBe OK

    val html = HtmlPage(res)
    html.inputShouldContain("postcodeSearch", user.postcode.get)

    html.shouldContainText("Business name")
    html.inputShouldContain("email", user.email)
    html.inputShouldContain("confirmedBusinessEmail", user.email)
    html.inputShouldContain("companyName", "")
    html.inputShouldContain("phone", "")
    html.html.getElementsByClass("manualAddress").hasClass("govuk-!-display-block") shouldBe true
    html.html.getElementsByClass("lookupAddressCancel").hasClass("govuk-!-display-none") shouldBe true
  }

  "Going to the create account page when logged in as a new assistant user registering with an existing group account" should
    "display the complete your contact details form for an assistant" in {
    val user: UserDetails = userDetails(affinityGroup = Organisation, credentialRole = Assistant)
    val groupAccount = arbitrary[GroupAccount].sample.get.copy(groupId = user.groupIdentifier)
    StubGroupAccountConnector.stubAccount(groupAccount)

    val res = testRegistrationController(user).show()(FakeRequest())
    status(res) shouldBe OK

    val html = HtmlPage(res)
    html.shouldContainTextInput("#firstName")
    html.shouldContainTextInput("#lastName")
    html.shouldContainText("You have been added as a user to your organisation, please confirm your details below")
  }

  "Submitting an invalid assistant form" should "return a bad request response" in {
    StubGroupAccountConnector.stubAccount(groupAccount(agent = true))

    val data = Map(
      "firstName" -> Seq("first")
    )
    val fakeRequest: FakeRequest[AnyContent] = FakeRequest().withBody(AnyContentAsFormUrlEncoded(data))
    val res = testRegistrationController(userDetails()).submitAssistant()(fakeRequest)
    status(res) shouldBe BAD_REQUEST

    val html = HtmlPage(res)
    html.shouldContainText("Last name - This must be filled in")
    html.shouldNotContainText("First Name - This must be filled in")
  }

  "Going to the create account page when logged in as a new admin user registering with an existing group account" should
    "display the complete your contact details form for an admin" in {
    val user: UserDetails = userDetails(affinityGroup = Organisation, credentialRole = User)
    val ga: GroupAccount = arbitrary[GroupAccount].sample.get.copy(groupId = user.groupIdentifier)
    StubGroupAccountConnector.stubAccount(ga)

    val res = testRegistrationController(user).show()(FakeRequest())
    status(res) shouldBe OK

    val html = HtmlPage(res)
    html.shouldContainText(
      "We use your organisation details to send you correspondence related to the service and your account. ")
  }

  "Going to the create account page when logged in as a new assistant user registering without an existing group account" should
    "display the invalid account creation page" in {
    val res = testRegistrationController(userDetails(affinityGroup = Organisation, credentialRole = Assistant))
      .show()(FakeRequest())
    status(res) shouldBe OK

    val html = HtmlPage(res)
    html.shouldContainText(
      "Registration failed You can’t register until the Administrator from your organisation registers first.")
  }

  "Call confirmation" should "return an valid page" in {

    val personId = 123L

    val data = Map(
      "companyName"            -> Seq("company"),
      "firstName"              -> Seq("first"),
      "lastName"               -> Seq("second"),
      "address.line1"          -> Seq("123456"),
      "address.line2"          -> Seq(""),
      "address.line3"          -> Seq(""),
      "address.line4"          -> Seq(""),
      "address.postcode"       -> Seq("BN1 2CD"),
      "phone"                  -> Seq("01234 555 555"),
      "email"                  -> Seq("x@x.com"),
      "confirmedBusinessEmail" -> Seq("x@x.com"),
      "isAgent"                -> Seq("false"),
      "nino"                   -> Seq("AA000001B"),
      "dob.day"                -> Seq("11"),
      "dob.month"              -> Seq("02"),
      "dob.year"               -> Seq("1980")
    )

    val fakeRequest: FakeRequest[AnyContent] = FakeRequest().withBody(AnyContentAsFormUrlEncoded(data))

    when(mockRegistrationService.continue(any(), any())(any(), any()))
      .thenReturn(Future.successful(Some(RegistrationSuccess(personId))))

    val res =
      testRegistrationController(userDetails(confidenceLevel = ConfidenceLevel.L200)).confirmation(100)(fakeRequest)
    status(res) shouldBe OK
    val html = HtmlPage(res)
    //Page title
    html.titleShouldMatch("Registration successful - Valuation Office Agency - GOV.UK")
    //Page should contains VOA Person ID value 100
    html.verifyElementText("personal-id", "100")
    //Page should contains VOA Agent code value 300
    html.verifyElementText("agent-code", "300")

    html.verifyElementText("email-sent", "We have sent these details to")
    html.verifyElementText("what-next", "What happens next")
    html.verifyElementText("terms-of-use", "Terms of use")

  }

  override protected def beforeEach(): Unit = {
    StubIndividualAccountConnector.reset()
    StubGroupAccountConnector.reset()
    StubIdentityVerification.reset()
    StubPropertyLinkConnector.reset()
    StubSubmissionIdConnector.reset()
    StubPropertyRepresentationConnector.reset()
  }
}
