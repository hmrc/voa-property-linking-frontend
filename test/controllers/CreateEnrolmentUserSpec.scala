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

import controllers.enrolment.CreateEnrolmentUser
import models.enrolment.{EnrolmentSuccess, UserInfo}
import models.identityVerificationProxy.Link
import models.{DetailedIndividualAccount, IndividualDetails}
import org.mockito.ArgumentMatchers.{eq => matching, _}
import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar
import play.api.mvc.{AnyContent, AnyContentAsFormUrlEncoded}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import resources._
import services.iv.IdentityVerificationService
import services.{EnrolmentService, RegistrationService, Success}
import uk.gov.hmrc.auth.core.{Admin, AffinityGroup, User}
import utils._

import scala.concurrent.Future

class CreateEnrolmentUserSpec extends VoaPropertyLinkingSpec with MockitoSugar {

  lazy val mockEnrolmentService = mock[EnrolmentService]

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
    StubGGAction,
    StubGroupAccountConnector,
    StubIndividualAccountConnector,
    mockEnrolmentService,
    StubAuthConnector,
    StubAddresses,
    mockRegistrationService,
    StubEmailService,
    StubAuthentication,
    mockIdentityVerificationService
  )

  "Invoking the app held CreateEnrolmentUser controller" should "result in correct dependency injection" in {
    app.injector.instanceOf[CreateEnrolmentUser]
  }

  "Going to the create account page, when logged in with an account that has not registered and has an Individual affinity group" should
    "display the create individual account form" in {
    val (groupId, externalId): (String, String) = (shortString, shortString)
    StubAuthConnector.stubGroupId(groupId)
    StubAuthConnector.stubExternalId(externalId)
    StubAuthConnector.stubUserDetails(externalId, testIndividualInfo)

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
    StubAuthConnector.stubGroupId(groupId)
    StubAuthConnector.stubExternalId(externalId)
    StubAuthConnector.stubUserDetails(externalId, testAgentInfo)

    val res = TestCreateEnrolmentUser.show()(FakeRequest())
    status(res) mustBe OK

    val html = HtmlPage(res)
    html.mustContainText("You’ve tried to register using an existing Individual or Agent Government Gateway account")
  }

  "Going to the create account page, when logged in with an account that has not registered and has an Organisation affinity group" should
    "display the create organisation account form" in {
    val (groupId, externalId): (String, String) = (shortString, shortString)
    StubAuthConnector.stubGroupId(groupId)
    StubAuthConnector.stubExternalId(externalId)
    StubAuthConnector.stubUserDetails(externalId, testOrganisationInfo)

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

  "Submitting an invalid individual form" should "return a bad request response" in {
    val (groupId, externalId): (String, String) = (shortString, shortString)
    StubAuthConnector.stubGroupId(groupId)
    StubAuthConnector.stubExternalId(externalId)
    StubAuthConnector.stubUserDetails(externalId, testIndividualInfo)

    val res = TestCreateEnrolmentUser.submitIndividual()(FakeRequest())
    status(res) mustBe BAD_REQUEST
  }

  "Submitting a valid individual form" should "return a redirect" in {
    when(mockEnrolmentService.enrol(any(), any())(any(), any())).thenReturn(Future.successful(Success))
    when(mockRegistrationService.create(any(), any(), any())(any())(any(), any())).thenReturn(Future.successful(EnrolmentSuccess(Link(""), 1l)))
    when(mockIdentityVerificationService.start(any())(any(), any())).thenReturn(Future.successful(Link("")))
    val (groupId, externalId): (String, String) = (shortString, shortString)
    StubAuthConnector.stubGroupId(groupId)
    StubAuthConnector.stubExternalId(externalId)
    StubAuthConnector.stubUserDetails(externalId, testIndividualInfo)
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
    StubAuthConnector.stubGroupId(groupId)
    StubAuthConnector.stubExternalId(externalId)
    StubAuthConnector.stubUserDetails(externalId, testOrganisationInfo)

    val res = TestCreateEnrolmentUser.submitOrganisation()(FakeRequest())
    status(res) mustBe BAD_REQUEST
  }

  "Submitting a valid organisation form" should "return a redirect" in {
    when(mockIdentityVerificationService.start(any())(any(), any())).thenReturn(Future.successful(Link("")))
    when(mockEnrolmentService.enrol(any(), any())(any(), any())).thenReturn(Future.successful(Success))
    when(mockRegistrationService.create(any(), any(), any())(any())(any(), any())).thenReturn(Future.successful(EnrolmentSuccess(Link(""), 1l)))
    val (groupId, externalId): (String, String) = (shortString, shortString)
    StubAuthConnector.stubGroupId(groupId)
    StubAuthConnector.stubExternalId(externalId)
    StubAuthConnector.stubUserDetails(externalId, testOrganisationInfo)
    StubIndividualAccountConnector.stubAccount(DetailedIndividualAccount(externalId, "", 1l, 2l, IndividualDetails("", "", "", "", None, 12)))

    CreateGroupAccount.form
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
