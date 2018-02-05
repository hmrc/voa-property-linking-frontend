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

import java.time.LocalDate

import akka.util.ByteString
import controllers.enrolment.CreateEnrolmentUser
import models.{Address, DetailedIndividualAccount, IndividualDetails, PersonalDetails}
import models.enrolment.UserInfo
import org.mockito.ArgumentMatchers.{eq => matching}
import org.scalatest.mockito.MockitoSugar
import play.api.libs.json.{JsValue, Json}
import play.api.libs.streams.Accumulator
import play.api.mvc.{AnyContent, AnyContentAsFormUrlEncoded, Result}
import play.api.test.{FakeHeaders, FakeRequest}
import play.api.test.Helpers._
import resources._
import services.{EnrolmentResult, EnrolmentService, Success}
import uk.gov.hmrc.auth.core.AffinityGroup
import uk.gov.hmrc.domain.Nino
import utils._
import connectors.{Addresses, TaxEnrolmentConnector}
import org.mockito.ArgumentMatchers.{eq => matching, _}
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{FlatSpec, MustMatchers}
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.Future

class CreateEnrolmentUserSpec extends ControllerSpec with MockitoSugar {

  lazy val mockEnrolmentService = mock[EnrolmentService]

  val testIndividualInfo = UserInfo(firstName = Some("Bob"),
    lastName = Some("Smith"),
    email = "bob@smith.com",
    postcode = Some("AB12 3CD"),
    groupIdentifier = "GroupIdenfifier",
    affinityGroup = AffinityGroup.Individual)

  val testOrganisationInfo = UserInfo(firstName = Some("Bob"),
    lastName = Some("Smith"),
    email = "bob@smith.com",
    postcode = Some("AB12 3CD"),
    groupIdentifier = "GroupIdenfifier",
    affinityGroup = AffinityGroup.Organisation)

  private object TestCreateEnrolmentUser extends CreateEnrolmentUser(
    StubGGAction,
    StubGroupAccountConnector,
    StubIndividualAccountConnector,
    mockEnrolmentService,
    StubAuthConnector,
    StubAddresses,
    StubEmailService,
    StubAuthentication
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

  "Going to the create account page, when logged in with an account that has not registered and has an Organisation affinity group" should
    "display the create organisation account form" in {
    val (groupId, externalId): (String, String) = (shortString, shortString)
    StubAuthConnector.stubGroupId(groupId)
    StubAuthConnector.stubExternalId(externalId)
    StubAuthConnector.stubUserDetails(externalId, testOrganisationInfo)

    val res = TestCreateEnrolmentUser.show()(FakeRequest())
    status(res) mustBe OK

    val html = HtmlPage(res)
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
    when(mockEnrolmentService.enrol(any(), any())(any(), any(), any())).thenReturn(Future.successful(Success))
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
      "address.postcode" -> Seq("12345")
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
      "isAgent" -> Seq("false")
    )
    val fakeRequest: FakeRequest[AnyContent] = FakeRequest().withBody(AnyContentAsFormUrlEncoded(data))
    val res = TestCreateEnrolmentUser.submitOrganisation()(fakeRequest)
    status(res) mustBe SEE_OTHER
  }
}
