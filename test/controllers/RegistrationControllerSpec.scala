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
import services.RegistrationService
import services.iv.IdentityVerificationService
import uk.gov.hmrc.auth.core.AffinityGroup.{Agent, Organisation}
import uk.gov.hmrc.auth.core.{AffinityGroup, Assistant, ConfidenceLevel, User}
import utils.{StubGroupAccountConnector, _}

import scala.concurrent.Future

class RegistrationControllerSpec extends VoaPropertyLinkingSpec with MockitoSugar {

  lazy val mockSessionRepo = {
    val f = mock[SessionRepo]
    when(f.start(any())(any(), any())).thenReturn(Future.successful(()))
    when(f.saveOrUpdate(any())(any(), any())).thenReturn(Future.successful(()))
    f
  }

  val mockIdentityVerificationService = mock[IdentityVerificationService]

  val mockRegistrationService = mock[RegistrationService]

  val mockSessionUserDetailsAction = mock[SessionUserDetailsAction]

  private def testRegistrationController(
        userDetails: UserDetails,
        sessionUserDetails: User = adminOrganisationAccountDetails): RegistrationController =
    new RegistrationController(
      errorHandler = mockCustomErrorHandler,
      ggAuthenticated = ggPreauthenticated(userDetails),
      authenticated = preAuthenticatedActionBuilders(true),
      sessionUserDetailsAction = sessionUserDetailsAction(sessionUserDetails),
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

    val res = testRegistrationController(user, individualUserAccountDetails).show()(FakeRequest())
    status(res) shouldBe OK

    val html = HtmlPage(res)
    html.shouldContainText("Mobile number")
    html.inputShouldContain("email", user.email)
    html.inputShouldContain("confirmedEmail", user.email)
    html.inputShouldContain("firstName", user.firstName.get)
    html.inputShouldContain("lastName", user.lastName.get)
    html.inputShouldContain("address.postcode", user.postcode.get)
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
    html.inputShouldContain("address.postcode", user.postcode.get)

    html.shouldContainText("Business name")
    html.inputShouldContain("email", user.email)
    html.inputShouldContain("confirmedBusinessEmail", user.email)
    html.inputShouldContain("firstName", user.firstName.get)
    html.inputShouldContain("lastName", user.lastName.get)
  }

  "Going to the create account page when logged in as a new assistant user registering with an existing group account" should
    "display the complete your contact details form for an assistant" in {
    val user: UserDetails = userDetails(affinityGroup = Organisation, credentialRole = Assistant)
    val groupAccount = arbitrary[GroupAccount].sample.get.copy(groupId = user.groupIdentifier)
    StubGroupAccountConnector.stubAccount(groupAccount)

    val res = testRegistrationController(user, adminInExistingOrganisationAccountDetails).show()(FakeRequest())
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
    html.shouldContainText("You have been added as a user to your organisation, please confirm your details below")
    html.shouldContainTextInput("#firstName")
    html.shouldContainTextInput("#lastName")
    html.shouldContainDateSelect("dob")
    html.shouldContainTextInput("#nino")
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

  trait SubmitIndividual {
    val personId = 123L

    when(mockRegistrationService.create(any(), any(), any())(any())(any(), any()))
      .thenReturn(Future.successful(RegistrationSuccess(personId)))

    StubIndividualAccountConnector.stubAccount(
      account = DetailedIndividualAccount(
        externalId = "externalId",
        trustId = None,
        organisationId = 1L,
        individualId = personId,
        details =
          IndividualDetails(firstName = "", lastName = "", email = "", phone1 = "", phone2 = None, addressId = 12)
      ))

    val data = Map(
      "firstName"        -> Seq("first"),
      "lastName"         -> Seq("second"),
      "email"            -> Seq("x@x.com"),
      "confirmedEmail"   -> Seq("x@x.com"),
      "phone"            -> Seq("01234 555 555"),
      "mobilePhone"      -> Seq("07554 555 555"),
      "address.line1"    -> Seq("1234567"),
      "address.line2"    -> Seq(""),
      "address.line3"    -> Seq(""),
      "address.line4"    -> Seq(""),
      "address.postcode" -> Seq("BN1 2CD"),
      "nino"             -> Seq("AA000001B"),
      "dob.day"          -> Seq("11"),
      "dob.month"        -> Seq("02"),
      "dob.year"         -> Seq("1980")
    )

    val fakeRequest: FakeRequest[AnyContent] = FakeRequest().withBody(AnyContentAsFormUrlEncoded(data))

  }

  "Submitting a valid individual with low confidence level" should "return an IV redirect" in new SubmitIndividual {
    when(mockIdentityVerificationService.start(any())(any())).thenReturn(Future.successful(Link("")))

    val res =
      testRegistrationController(userDetails(confidenceLevel = ConfidenceLevel.L50)).submitIndividual()(fakeRequest)
    status(res) shouldBe SEE_OTHER
    redirectLocation(res) shouldBe Some("/business-rates-property-linking/identity-verification/start")
  }

  "Submitting a valid individual with high confidence level" should "return a create-success redirect" in new SubmitIndividual {
    when(mockRegistrationService.continue(any(), any())(any(), any()))
      .thenReturn(Future.successful(Some(RegistrationSuccess(personId))))

    val res =
      testRegistrationController(userDetails(confidenceLevel = ConfidenceLevel.L200)).submitIndividual()(fakeRequest)
    status(res) shouldBe SEE_OTHER
    redirectLocation(res) shouldBe Some(s"/business-rates-property-linking/create-confirmation?personId=$personId")
  }

  "Call confirmation" should "return an valid page" in new SubmitOrganisation {
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

  "Submitting an individual form with invalid email, mobilePhone, phone, nino" should "return a bad request response" in {
    val data = Map(
      "phone"       -> Seq("01"),
      "mobilePhone" -> Seq("11111222111"),
      "nino"        -> Seq("0AQ"),
      "email"       -> Seq("invalidEmail!!.com"))

    val fakeRequest: FakeRequest[AnyContent] = FakeRequest().withBody(AnyContentAsFormUrlEncoded(data))

    val res = testRegistrationController(userDetails()).submitIndividual()(fakeRequest)
    status(res) shouldBe BAD_REQUEST
    val html = HtmlPage(res)
    html.shouldContainText("Enter a valid email address")
    html.shouldContainText("Enter a valid National Insurance number")
    html.shouldContainText("Telephone number must be between 11 and 20 characters")
    html.shouldContainText("Enter a telephone number, like 01623 960 001 or +44 0808 157 0192")
  }

  "Submitting an individual form with non-matching emails" should "return a bad request response" in {
    val data = Map("email" -> Seq("x@x.com"), "confirmedEmail" -> Seq("1@1.com"))

    val fakeRequest: FakeRequest[AnyContent] = FakeRequest().withBody(AnyContentAsFormUrlEncoded(data))

    val res = testRegistrationController(userDetails()).submitIndividual()(fakeRequest)
    status(res) shouldBe BAD_REQUEST
    val html = HtmlPage(res)
    html.shouldContainText("Email addresses must match. Check them and try again")
  }

  "Submitting an empty individual form" should "return a bad request response" in {
    val res = testRegistrationController(userDetails()).submitIndividual()(FakeRequest())
    status(res) shouldBe BAD_REQUEST
    val html = HtmlPage(res)
    html.shouldContainText("Last name - This must be filled in")
    html.shouldContainText("Date of birth - Enter a valid date")
    html.shouldContainText("Address line 1 - This must be filled in")
    html.shouldContainText("Postcode - This must be filled in")
    html.shouldContainText("NINO - This must be filled in")
    html.shouldContainText("Email - This must be filled in")
    html.shouldContainText("Mobile number - This must be filled in")
    html.shouldContainText("Phone - This must be filled in")
  }

  "Submitting an invalid organisation admin form" should "return a bad request response" in {
    val user: UserDetails = userDetails(affinityGroup = Organisation, credentialRole = User)
    val ga: GroupAccount = arbitrary[GroupAccount].sample.get.copy(groupId = user.groupIdentifier)
    StubGroupAccountConnector.stubAccount(ga)

    val res = testRegistrationController(user).submitAdminToExistingOrganisation()(FakeRequest())
    status(res) shouldBe BAD_REQUEST
    val html = HtmlPage(res)
    html.shouldContainText("First name - This must be filled in")
    html.shouldContainText("Last name - This must be filled in")
    html.shouldContainText("Date of birth - Enter a valid date")
    html.shouldContainText("NINO - This must be filled in")

  }

  trait SubmitOrganisation {
    val personId = 123L

    when(mockRegistrationService.create(any(), any(), any())(any())(any(), any()))
      .thenReturn(Future.successful(RegistrationSuccess(personId)))

    StubIndividualAccountConnector.stubAccount(
      account = DetailedIndividualAccount(
        externalId = "externalId",
        trustId = None,
        organisationId = 1L,
        individualId = personId,
        details =
          IndividualDetails(firstName = "", lastName = "", email = "", phone1 = "", phone2 = None, addressId = 12)
      ))

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
  }

  "Submitting a valid organisation with low confidence level" should "return an IV redirect" in new SubmitOrganisation {
    when(mockIdentityVerificationService.start(any())(any())).thenReturn(Future.successful(Link("")))

    val res =
      testRegistrationController(userDetails(confidenceLevel = ConfidenceLevel.L50)).submitOrganisation()(fakeRequest)
    status(res) shouldBe SEE_OTHER
    redirectLocation(res) shouldBe Some("/business-rates-property-linking/identity-verification/start")
  }

  "Submitting a valid organisation with high confidence level" should "return an create-success redirect" in new SubmitOrganisation {
    when(mockRegistrationService.continue(any(), any())(any(), any()))
      .thenReturn(Future.successful(Some(RegistrationSuccess(personId))))

    val res =
      testRegistrationController(userDetails(confidenceLevel = ConfidenceLevel.L200)).submitOrganisation()(fakeRequest)
    status(res) shouldBe SEE_OTHER
    redirectLocation(res) shouldBe Some(s"/business-rates-property-linking/create-confirmation?personId=$personId")

  }

  "Submitting an invalid organisation form" should "return a bad request response" in {
    val res = testRegistrationController(userDetails()).submitOrganisation()(FakeRequest())
    status(res) shouldBe BAD_REQUEST
    val html = HtmlPage(res)
    html.shouldContainText("Last name - This must be filled in")
    html.shouldContainText("Date of birth - Enter a valid date")
    html.shouldContainText("Address line 1 - This must be filled in")
    html.shouldContainText("Postcode - This must be filled in")
    html.shouldContainText("NINO - This must be filled in")
    html.shouldContainText("Email - This must be filled in")
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
