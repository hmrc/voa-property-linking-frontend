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

package controllers.manageDetails

import connectors.{Addresses, GroupAccounts, IndividualAccounts}
import controllers.VoaPropertyLinkingSpec
import models.{Address, DetailedIndividualAccount}
import org.jsoup.Jsoup
import org.mockito.ArgumentMatchers.{eq => matching, _}
import org.mockito.Mockito._
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.{ManageDetails, Success}
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future
import scala.util.Random

class UpdatePersonalDetailsSpec extends VoaPropertyLinkingSpec {

  "The edit email page" should "require the updated email to be valid" in {
    val invalidEmail = Seq(
      "email"          -> "not an email",
      "confirmedEmail" -> "not an email"
    )

    val res = TestUpdatePersonalDetails.updateEmail()(request.withFormUrlEncodedBody(invalidEmail: _*))
    status(res) shouldBe BAD_REQUEST

    val html = Jsoup.parse(contentAsString(res))
    html.select("p.govuk-error-message").text shouldBe "Error: Enter a valid email address"
  }

  it should "require the confirmed email to match" in {
    val mismatchedEmails = Seq(
      "email"          -> "email@example.com",
      "confirmedEmail" -> "anotherEmail@example.com"
    )

    val res = TestUpdatePersonalDetails.updateEmail()(request.withFormUrlEncodedBody(mismatchedEmails: _*))
    status(res) shouldBe BAD_REQUEST

    val html = Jsoup.parse(contentAsString(res))
    html
      .select("p.govuk-error-message")
      .text shouldBe "Error: Email addresses must match. Check them and try again"
  }

  it should "update the user's email when they make a valid submission" in {

    val updatedEmail = "email@example.com"

    val validData = Seq(
      "email"          -> updatedEmail,
      "confirmedEmail" -> updatedEmail
    )

    val res = TestUpdatePersonalDetails.updateEmail()(request.withFormUrlEncodedBody(validData: _*))
    status(res) shouldBe SEE_OTHER
    redirectLocation(res) shouldBe Some(viewDetailsPage)

    val updatedDetails = detailedIndividualAccount.details.copy(email = updatedEmail)
    verify(mockIndividualAccounts).update(matching(detailedIndividualAccount.copy(details = updatedDetails)))(
      any[HeaderCarrier])
  }

  "The edit name page" should "require a non-empty first name" in {
    val missingFirstName = Seq(
      "lastName" -> "Person"
    )

    val res = TestUpdatePersonalDetails.updateName()(request.withFormUrlEncodedBody(missingFirstName: _*))
    status(res) shouldBe BAD_REQUEST

    val html = Jsoup.parse(contentAsString(res))
    html.select("p.govuk-error-message").text shouldBe "Error: This must be filled in"
  }

  it should "require a non-empty last name" in {
    val missingLastName = Seq(
      "firstName" -> "Mr"
    )

    val res = TestUpdatePersonalDetails.updateName()(request.withFormUrlEncodedBody(missingLastName: _*))
    status(res) shouldBe BAD_REQUEST

    val html = Jsoup.parse(contentAsString(res))
    html.select("p.govuk-error-message").text shouldBe "Error: This must be filled in"
  }

  it should "update the user's name when they make a valid submission" in {
    val validData = Seq(
      "firstName" -> "Mr",
      "lastName"  -> "Person"
    )

    val res = TestUpdatePersonalDetails.updateName()(request.withFormUrlEncodedBody(validData: _*))
    status(res) shouldBe SEE_OTHER
    redirectLocation(res) shouldBe Some(viewDetailsPage)

    val updatedDetails = detailedIndividualAccount.details.copy(firstName = "Mr", lastName = "Person")
    verify(mockIndividualAccounts).update(matching(detailedIndividualAccount.copy(details = updatedDetails)))(
      any[HeaderCarrier])
  }

  "The update phone number page" should "require the phone number to be non-empty" in {
    val emptyPhoneNumber = Seq(
      "phone" -> ""
    )

    val res = TestUpdatePersonalDetails.updatePhone()(request.withFormUrlEncodedBody(emptyPhoneNumber: _*))
    status(res) shouldBe BAD_REQUEST

    val html = Jsoup.parse(contentAsString(res))
    html.select("p.govuk-error-message").text shouldBe "Error: Enter a telephone number"
  }

  it should "update the user's phone number when they make a valid submission" in {
    val res = TestUpdatePersonalDetails.updatePhone()(request.withFormUrlEncodedBody("phone" -> "01234567890"))

    status(res) shouldBe SEE_OTHER
    redirectLocation(res) shouldBe Some(viewDetailsPage)

    val updatedDetails = detailedIndividualAccount.details.copy(phone1 = "01234567890")
    verify(mockIndividualAccounts).update(matching(detailedIndividualAccount.copy(details = updatedDetails)))(
      any[HeaderCarrier])
  }

  "The update address page" should "require a postcode" in {
    val missingPostcode = Seq(
      "address.line1"    -> "Some place",
      "address.postcode" -> ""
    )

    val res = TestUpdatePersonalDetails.updateAddress()(request.withFormUrlEncodedBody(missingPostcode: _*))
    status(res) shouldBe BAD_REQUEST

    val html = Jsoup.parse(contentAsString(res))
    html.select("p.govuk-error-message").text contains "Error: This must be filled in"
  }

  it should "update the user's address ID if they use the lookup" in {
    when(mockManageDetails.updatePostcode(any(), any(), any())(any(), any())).thenReturn(Future.successful(Success))

    val validData = Seq(
      "address.addressId" -> "1234567890",
      "address.line1"     -> "Some place",
      "address.line2"     -> "",
      "address.line3"     -> "",
      "address.line4"     -> "",
      "address.postcode"  -> "AA11 1AA"
    )

    val res = TestUpdatePersonalDetails.updateAddress()(request.withFormUrlEncodedBody(validData: _*))
    status(res) shouldBe SEE_OTHER
    redirectLocation(res) shouldBe Some(viewDetailsPage)

    val updatedDetails = detailedIndividualAccount.details.copy(addressId = 1234567890)
    verify(mockIndividualAccounts).update(matching(detailedIndividualAccount.copy(details = updatedDetails)))(
      any[HeaderCarrier])
    verify(mockManageDetails)
      .updatePostcode(matching(detailedIndividualAccount.individualId), any(), matching(1234567890L))(any(), any())
  }

  it should "create an address record, and update the user's record with the generated address ID, if they enter the address manually" in {
    val addressId = Random.nextLong()
    val address: Address = utils.addressGen.sample.get.copy(addressUnitId = None)

    when(mockAddressConnector.create(any[Address])(any[HeaderCarrier])).thenReturn(Future.successful(addressId))
    when(mockManageDetails.updatePostcode(any(), any(), any())(any(), any())).thenReturn(Future.successful(Success))

    val validFormData: Seq[(String, String)] = Seq(
      "address.line1"    -> address.line1,
      "address.line2"    -> address.line2,
      "address.line3"    -> address.line3,
      "address.line4"    -> address.line4,
      "address.postcode" -> address.postcode
    )
    val res = TestUpdatePersonalDetails.updateAddress()(request.withFormUrlEncodedBody(validFormData: _*))
    status(res) should be(SEE_OTHER)
    redirectLocation(res) should be(Some(viewDetailsPage))

    val updatedDetails = detailedIndividualAccount.details.copy(addressId = addressId)

    verify(mockAddressConnector).create(matching(address))(any[HeaderCarrier])
    verify(mockIndividualAccounts).update(matching(detailedIndividualAccount.copy(details = updatedDetails)))(
      any[HeaderCarrier])
    verify(mockManageDetails)
      .updatePostcode(matching(detailedIndividualAccount.individualId), any(), matching(addressId))(any(), any())
  }

  it should "reject an address record, if the manually entered postcode is blank" in {
    val addressId = Random.nextLong()
    val address: Address = utils.addressGen.sample.get.copy(addressUnitId = None)

    when(mockAddressConnector.create(any[Address])(any[HeaderCarrier])).thenReturn(Future.successful(addressId))
    when(mockManageDetails.updatePostcode(any(), any(), any())(any(), any())).thenReturn(Future.successful(Success))

    val validFormData: Seq[(String, String)] = Seq(
      "address.line1"    -> address.line1,
      "address.line2"    -> address.line2,
      "address.line3"    -> address.line3,
      "address.line4"    -> address.line4,
      "address.postcode" -> "   "
    )

    val res = TestUpdatePersonalDetails.updateAddress()(request.withFormUrlEncodedBody(validFormData: _*))
    status(res) should be(BAD_REQUEST)
  }

  "The update mobile number page" should "update the user's mobile number if they submit a valid form" in {
    val res = TestUpdatePersonalDetails.updateMobile()(request.withFormUrlEncodedBody("phone" -> "01234567890"))

    status(res) shouldBe SEE_OTHER
    redirectLocation(res) shouldBe Some(viewDetailsPage)

    val updatedDetails = detailedIndividualAccount.details.copy(phone2 = Some("01234567890"))
    verify(mockIndividualAccounts).update(matching(detailedIndividualAccount.copy(details = updatedDetails)))(
      any[HeaderCarrier])
  }

  "The update mobile number page" should "throw BAD_REQUEST if they submit an invalid form" in {
    val res = TestUpdatePersonalDetails.updateMobile()(FakeRequest())
    status(res) shouldBe BAD_REQUEST
  }

  "viewEmail" should "display the users email" in {
    val res = TestUpdatePersonalDetails.viewEmail()(FakeRequest())

    status(res) shouldBe OK

    val html = Jsoup.parse(contentAsString(res))
    html.title shouldBe "Update email - Valuation Office Agency - GOV.UK"
  }

  "viewAddress" should "display the users address" in {
    val res = TestUpdatePersonalDetails.viewAddress()(FakeRequest())

    status(res) shouldBe OK

    val html = Jsoup.parse(contentAsString(res))
    html.title shouldBe "Update address - Valuation Office Agency - GOV.UK"
  }

  "viewPhone" should "display the users phone number" in {
    val res = TestUpdatePersonalDetails.viewPhone()(FakeRequest())

    status(res) shouldBe OK

    val html = Jsoup.parse(contentAsString(res))
    html.title shouldBe "Update telephone number - Valuation Office Agency - GOV.UK"
  }

  "viewName" should "display the users name" in {
    val res = TestUpdatePersonalDetails.viewName()(FakeRequest())

    status(res) shouldBe OK

    val html = Jsoup.parse(contentAsString(res))
    html.title shouldBe "Update your name - Valuation Office Agency - GOV.UK"
  }

  "viewMobile" should "display the users name" in {
    val res = TestUpdatePersonalDetails.viewMobile()(FakeRequest())

    status(res) shouldBe OK

    val html = Jsoup.parse(contentAsString(res))
    html.title shouldBe "Update mobile number - Valuation Office Agency - GOV.UK"
  }

  private lazy val viewDetailsPage = controllers.manageDetails.routes.ViewDetails.show.url

  private object TestUpdatePersonalDetails
      extends UpdatePersonalDetails(
        mockCustomErrorHandler,
        preAuthenticatedActionBuilders(),
        mockAddressConnector,
        mockIndividualAccounts,
        mockManageDetails,
        mockGroupAccounts,
        updateAddressView,
        updatePhoneView,
        updateMobileView,
        updateEmailView,
        updateNameView
      )

  lazy val mockIndividualAccounts: IndividualAccounts = {
    val m = mock[IndividualAccounts]
    when(m.update(any[DetailedIndividualAccount])(any[HeaderCarrier])).thenReturn(Future.successful(()))
    m
  }

  lazy val mockAddressConnector = mock[Addresses]
  lazy val mockManageDetails = mock[ManageDetails]
  lazy val mockGroupAccounts = mock[GroupAccounts]

  lazy val request = FakeRequest()

}
