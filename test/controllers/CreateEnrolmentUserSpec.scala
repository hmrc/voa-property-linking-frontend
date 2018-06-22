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

import auth.GgAction
import controllers.enrolment.CreateEnrolmentUser
import models.enrolment.{EnrolmentSuccess, UserInfo}
import models.identityVerificationProxy.Link
import models.{DetailedIndividualAccount, GroupAccount, IndividualDetails}
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.scalacheck.Arbitrary.arbitrary
import org.scalatest.mockito.MockitoSugar
import play.api.mvc.{AnyContent, AnyContentAsFormUrlEncoded}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import repositories.SessionRepo
import resources._
import services.iv.IdentityVerificationService
import services.{EnrolmentService, RegistrationService, Success}
import uk.gov.hmrc.auth.core._
import utils.{StubGroupAccountConnector, _}

import scala.concurrent.Future

class CreateEnrolmentUserSpec extends VoaPropertyLinkingSpec with MockitoSugar {

  override val additionalAppConfig = Seq("featureFlags.enrolment" -> "true")

  lazy val mockEnrolmentService = mock[EnrolmentService]

  lazy val mockSessionRepo = {
    val f = mock[SessionRepo]
    when(f.start(any())(any(), any())
    ).thenReturn(Future.successful(()))
    when(f.saveOrUpdate(any())(any(), any())
    ).thenReturn(Future.successful(()))
    f
  }

  val testIndividualInfo = UserInfo(firstName = Some("Bob"),
    lastName = Some("Smith"),
    email = "bob@smith.com",
    postcode = Some("AB12 3CD"),
    groupIdentifier = "GroupIdenfifier",
    affinityGroup = AffinityGroup.Individual,
    gatewayId = "",
    credentialRole = User)

  val testOrganisationInfo = UserInfo(firstName = Some("Bob"),
    lastName = Some("Smith"),
    email = "bob@smith.com",
    postcode = Some("AB12 3CD"),
    groupIdentifier = "GroupIdenfifier",
    affinityGroup = AffinityGroup.Organisation,
    gatewayId = "",
    credentialRole = Admin)

  val testAgentInfo = UserInfo(firstName = Some("Bob"),
    lastName = Some("Smith"),
    email = "bob@smith.com",
    postcode = Some("AB12 3CD"),
    groupIdentifier = "GroupIdenfifier",
    affinityGroup = AffinityGroup.Agent,
    gatewayId = "",
    credentialRole = Admin)

  val mockIdentityVerificationService = mock[IdentityVerificationService]

  val mockRegistrationService = mock[RegistrationService]

  private object TestCreateEnrolmentUser extends CreateEnrolmentUser(
    StubGgAction,
    StubGroupAccountConnector,
    StubIndividualAccountConnector,
    mockEnrolmentService,
    StubVplAuthConnector,
    StubAddresses,
    mockRegistrationService,
    StubEmailService,
    StubAuthentication,
    mockIdentityVerificationService,
    mockSessionRepo
  )

  "Invoking the app held CreateEnrolmentUser controller" should "result in correct dependency injection" in {
    app.injector.instanceOf[CreateEnrolmentUser]
  }

  "Going directly to the complete-contact-details page, when logged in with an already registered VOA account" should
    "redirect the user to the dashboard" in {
    val (groupId, externalId): (String, String) = (shortString, shortString)
    StubAuthConnector.stubGroupId(groupId)
    StubAuthConnector.stubExternalId(externalId)
    StubAuthConnector.stubUserDetails(externalId, testIndividualInfo)
    StubIndividualAccountConnector.stubAccount(arbitrary[DetailedIndividualAccount].sample.get.copy(externalId = externalId))

    val res = TestCreateEnrolmentUser.show()(FakeRequest())
    status(res) mustBe SEE_OTHER
    redirectLocation(res) mustBe Some(controllers.routes.Dashboard.home().url)
  }

  "Going to the create account page, when logged in with an account that has not registered and has an Individual affinity group" should
    "display the create individual account form" in {
    val (groupId, externalId): (String, String) = (shortString, shortString)
    StubVplAuthConnector.stubGroupId(groupId)
    StubVplAuthConnector.stubExternalId(externalId)
    StubVplAuthConnector.stubUserDetails(externalId, testIndividualInfo)

    val res = TestCreateEnrolmentUser.show()(FakeRequest())
    status(res) mustBe OK

    val html = HtmlPage(res)
    html.mustContainText("Mobile number")
    html.inputMustContain("email", testIndividualInfo.email)
    html.inputMustContain("confirmedEmail", testIndividualInfo.email)
    html.inputMustContain("firstName", testIndividualInfo.firstName.get)
    html.inputMustContain("lastName", testIndividualInfo.lastName.get)
    html.inputMustContain("addresspostcode", testIndividualInfo.postcode.get)
  }

  "Going to the create account page, when logged in with an account that is an Agent" should
    "display the invalid account type page" in {
    val (groupId, externalId): (String, String) = (shortString, shortString)
    StubVplAuthConnector.stubGroupId(groupId)
    StubVplAuthConnector.stubExternalId(externalId)
    StubVplAuthConnector.stubUserDetails(externalId, testAgentInfo)

    val res = TestCreateEnrolmentUser.show()(FakeRequest())
    status(res) mustBe OK

    val html = HtmlPage(res)
    html.mustContainText("You’ve tried to register using an existing Agent Government Gateway account")
  }

  "Going to the create account page, when logged in with an account that has not registered and has an Organisation affinity group" should
    "display the create organisation account form" in {
    val (groupId, externalId): (String, String) = (shortString, shortString)
    StubVplAuthConnector.stubGroupId(groupId)
    StubVplAuthConnector.stubExternalId(externalId)
    StubVplAuthConnector.stubUserDetails(externalId, testOrganisationInfo)

    val res = TestCreateEnrolmentUser.show()(FakeRequest())
    status(res) mustBe OK

    val html = HtmlPage(res)
    html.inputMustContain("addresspostcode", testOrganisationInfo.postcode.get)

    html.mustContainText("Business name")
    html.inputMustContain("email", testOrganisationInfo.email)
    html.inputMustContain("confirmedBusinessEmail", testOrganisationInfo.email)
    html.inputMustContain("firstName", testOrganisationInfo.firstName.get)
    html.inputMustContain("lastName", testOrganisationInfo.lastName.get)
    html.inputMustContain("addresspostcode", testOrganisationInfo.postcode.get)
  }

  "Going to the create account page when logged in as a new assistant user registering with an existing group account" should
    "display the complete your contact details form for an assistant" in {

    val groupAccount = arbitrary[GroupAccount].sample.get
    val individualAccount = arbitrary[DetailedIndividualAccount].sample.get.copy(organisationId = groupAccount.id)

    StubVplAuthConnector.stubGroupId(groupAccount.groupId)
    StubVplAuthConnector.stubExternalId(individualAccount.externalId)
    StubGroupAccountConnector.stubAccount(groupAccount)
    StubVplAuthConnector.stubUserDetails(individualAccount.externalId, testOrganisationInfo.copy(credentialRole = Assistant))

    val res = TestCreateEnrolmentUser.show()(FakeRequest())
    status(res) mustBe OK

    val html = HtmlPage(res)
    html.mustContainTextInput("#firstName")
    html.mustContainTextInput("#lastName")
    html.mustContainText("You have been added as a user to your organisation, please confirm your details below")
  }

  "Going to the create account page when logged in as a new admin user registering with an existing group account" should
    "display the complete your contact details form for an admin" in {

    val groupAccount = arbitrary[GroupAccount].sample.get
    val individualAccount = arbitrary[DetailedIndividualAccount].sample.get.copy(organisationId = groupAccount.id)

    StubVplAuthConnector.stubGroupId(groupAccount.groupId)
    StubVplAuthConnector.stubExternalId(individualAccount.externalId)
    StubGroupAccountConnector.stubAccount(groupAccount)
    StubVplAuthConnector.stubUserDetails(individualAccount.externalId, testOrganisationInfo.copy(credentialRole = Admin))

    val res = TestCreateEnrolmentUser.show()(FakeRequest())
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

    val groupAccount = arbitrary[GroupAccount].sample.get
    val individualAccount = arbitrary[DetailedIndividualAccount].sample.get.copy(organisationId = groupAccount.id)

    StubVplAuthConnector.stubGroupId(groupAccount.groupId)
    StubVplAuthConnector.stubExternalId(individualAccount.externalId)
    StubVplAuthConnector.stubUserDetails(individualAccount.externalId, testOrganisationInfo.copy(credentialRole = Assistant))

    val res = TestCreateEnrolmentUser.show()(FakeRequest())
    status(res) mustBe OK

    val html = HtmlPage(res)
    html.mustContainText("Registration failed You can’t register until the Administrator from your organisation registers first.")
  }

  "Submitting an invalid individual form" should "return a bad request response" in {
    val (groupId, externalId): (String, String) = (shortString, shortString)
    StubVplAuthConnector.stubGroupId(groupId)
    StubVplAuthConnector.stubExternalId(externalId)
    StubVplAuthConnector.stubUserDetails(externalId, testIndividualInfo)

    val res = TestCreateEnrolmentUser.submitIndividual()(FakeRequest())
    status(res) mustBe BAD_REQUEST
  }

  "Submitting a valid individual form" should "return a redirect" in {
    when(mockEnrolmentService.enrol(any(), any())(any(), any())).thenReturn(Future.successful(Success))
    when(mockRegistrationService.create(any(), any())(any())(any(), any())).thenReturn(Future.successful(EnrolmentSuccess(1l)))
    when(mockIdentityVerificationService.start(any())(any(), any())).thenReturn(Future.successful(Link("")))
    val (groupId, externalId): (String, String) = (shortString, shortString)
    StubVplAuthConnector.stubGroupId(groupId)
    StubVplAuthConnector.stubExternalId(externalId)
    StubVplAuthConnector.stubUserDetails(externalId, testIndividualInfo)
    StubIndividualAccountConnector.stubAccount(DetailedIndividualAccount(externalId, "", 1l, 2l, IndividualDetails("", "", "", "", None, 12)))

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
    val res = TestCreateEnrolmentUser.submitIndividual()(fakeRequest)
    status(res) mustBe SEE_OTHER
  }

  "Submitting an invalid organisation form" should "return a bad request response" in {
    val (groupId, externalId): (String, String) = (shortString, shortString)
    StubVplAuthConnector.stubGroupId(groupId)
    StubVplAuthConnector.stubExternalId(externalId)
    StubVplAuthConnector.stubUserDetails(externalId, testOrganisationInfo)

    val res = TestCreateEnrolmentUser.submitOrganisation()(FakeRequest())
    status(res) mustBe BAD_REQUEST
  }

  "Submitting a valid organisation form" should "return a redirect" in {
    when(mockIdentityVerificationService.start(any())(any(), any())).thenReturn(Future.successful(Link("")))
    when(mockEnrolmentService.enrol(any(), any())(any(), any())).thenReturn(Future.successful(Success))
    when(mockRegistrationService.create(any(), any())(any())(any(), any())).thenReturn(Future.successful(EnrolmentSuccess(1l)))
    val (groupId, externalId): (String, String) = (shortString, shortString)
    StubVplAuthConnector.stubGroupId(groupId)
    StubVplAuthConnector.stubExternalId(externalId)
    StubVplAuthConnector.stubUserDetails(externalId, testOrganisationInfo)
    StubIndividualAccountConnector.stubAccount(DetailedIndividualAccount(externalId, "", 1l, 2l, IndividualDetails("", "", "", "", None, 12)))

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
    val res = TestCreateEnrolmentUser.submitOrganisation()(fakeRequest)
    status(res) mustBe SEE_OTHER
  }
}
