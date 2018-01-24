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

package controllers.manageDetails

import actions.BasicAuthenticatedRequest
import config.ApplicationConfig
import connectors.{Addresses, Authenticated, IndividualAccounts}
import controllers.ControllerSpec
import models.{Accounts, Address, DetailedIndividualAccount}
import org.jsoup.Jsoup
import org.mockito.ArgumentMatchers.{eq => matching, _}
import org.mockito.Mockito._
import org.mockito.invocation.InvocationOnMock
import org.mockito.stubbing.Answer
import org.scalatest.mockito.MockitoSugar
import play.api.mvc.{Action, AnyContent, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import resources._
import services.ManageDetails
import utils.{StubAuthentication, StubGroupAccountConnector}

import scala.concurrent.Future
import scala.util.Random
import uk.gov.hmrc.http.HeaderCarrier

class UpdatePersonalDetailsSpec extends ControllerSpec with MockitoSugar {

  "The edit email page" must "require the updated email to be valid" in {
    stubLoggedInUser()

    val invalidEmail = Seq(
      "email" -> "not an email",
      "confirmedEmail" -> "not an email"
    )

    val res = TestUpdatePersonalDetails.updateEmail()(request.withFormUrlEncodedBody(invalidEmail:_*))
    status(res) mustBe BAD_REQUEST

    val html = Jsoup.parse(contentAsString(res))
    html.select("label[for=email] span.error-message").text mustBe "Enter a valid email address"
  }

  it must "require the confirmed email to match" in {
    stubLoggedInUser()

    val mismatchedEmails = Seq(
      "email" -> "email@example.com",
      "confirmedEmail" -> "anotherEmail@example.com"
    )

    val res = TestUpdatePersonalDetails.updateEmail()(request.withFormUrlEncodedBody(mismatchedEmails:_*))
    status(res) mustBe BAD_REQUEST

    val html = Jsoup.parse(contentAsString(res))
    html.select("label[for=confirmedEmail] span.error-message").text mustBe "Email addresses must match. Check them and try again"
  }

  it must "update the user's email when they make a valid submission" in {
    val (_, current) = stubLoggedInUser()

    val updatedEmail = "email@example.com"

    val validData = Seq(
      "email" -> updatedEmail,
      "confirmedEmail" -> updatedEmail
    )

    val res = TestUpdatePersonalDetails.updateEmail()(request.withFormUrlEncodedBody(validData:_*))
    status(res) mustBe SEE_OTHER
    redirectLocation(res) mustBe Some(viewDetailsPage)

    val updatedDetails = current.details.copy(email = updatedEmail)
    verify(mockIndividualAccounts, once).update(matching(current.copy(details = updatedDetails)))(any[HeaderCarrier])
  }

  "The edit name page" must "require a non-empty first name" in {
    stubLoggedInUser()

    val missingFirstName = Seq(
      "lastName" -> "Person"
    )

    val res = TestUpdatePersonalDetails.updateName()(request.withFormUrlEncodedBody(missingFirstName:_*))
    status(res) mustBe BAD_REQUEST

    val html = Jsoup.parse(contentAsString(res))
    html.select("label[for=firstName] span.error-message").text mustBe "This must be filled in"
  }

  it must "require a non-empty last name" in {
    stubLoggedInUser()

    val missingLastName = Seq(
      "firstName" -> "Mr"
    )

    val res = TestUpdatePersonalDetails.updateName()(request.withFormUrlEncodedBody(missingLastName:_*))
    status(res) mustBe BAD_REQUEST

    val html = Jsoup.parse(contentAsString(res))
    html.select("label[for=lastName] span.error-message").text mustBe "This must be filled in"
  }

  it must "update the user's name when they make a valid submission" in {
    val (_, current) = stubLoggedInUser()

    val validData = Seq(
      "firstName" -> "Mr",
      "lastName" -> "Person"
    )

    val res = TestUpdatePersonalDetails.updateName()(request.withFormUrlEncodedBody(validData:_*))
    status(res) mustBe SEE_OTHER
    redirectLocation(res) mustBe Some(viewDetailsPage)

    val updatedDetails = current.details.copy(firstName = "Mr", lastName = "Person")
    verify(mockIndividualAccounts, once).update(matching(current.copy(details = updatedDetails)))(any[HeaderCarrier])
  }

  "The update phone number page" must "require the phone number to be non-empty" in {
    stubLoggedInUser()

    val emptyPhoneNumber = Seq(
      "phone" -> ""
    )

    val res = TestUpdatePersonalDetails.updatePhone()(request.withFormUrlEncodedBody(emptyPhoneNumber:_*))
    status(res) mustBe BAD_REQUEST

    val html = Jsoup.parse(contentAsString(res))
    html.select("label[for=phone] span.error-message").text mustBe "This must be filled in"
  }

  it must "update the user's phone number when they make a valid submission" in {
    val (_, current) = stubLoggedInUser()

    val validData = Seq(
      "phone" -> "01234567890"
    )

    val res = TestUpdatePersonalDetails.updatePhone()(request.withFormUrlEncodedBody(validData:_*))
    status(res) mustBe SEE_OTHER
    redirectLocation(res) mustBe Some(viewDetailsPage)

    val updatedDetails = current.details.copy(phone1 = "01234567890")
    verify(mockIndividualAccounts, once).update(matching(current.copy(details = updatedDetails)))(any[HeaderCarrier])
  }

  "The update address page" must "require a postcode" in {
    stubLoggedInUser()

    val missingPostcode = Seq(
      "address.line1" -> "Some place",
      "address.postcode" -> ""
    )

    val res = TestUpdatePersonalDetails.updateAddress()(request.withFormUrlEncodedBody(missingPostcode:_*))
    status(res) mustBe BAD_REQUEST

    val html = Jsoup.parse(contentAsString(res))
    html.select("label[for=addresspostcode] span.error-message").text mustBe "This must be filled in"
  }

  it must "update the user's address ID if they use the lookup" in {
    val (_, current) = stubLoggedInUser()

    when(mockManageDetails.updatePostcode(any(),any(),any())(any())(any(), any())).thenReturn(Future.successful(()))

    val validData = Seq(
      "address.addressId" -> "1234567890",
      "address.line1" -> "Some place",
      "address.postcode" -> "AA11 1AA"
    )

    val res = TestUpdatePersonalDetails.updateAddress()(request.withFormUrlEncodedBody(validData:_*))
    status(res) mustBe SEE_OTHER
    redirectLocation(res) mustBe Some(viewDetailsPage)

    val updatedDetails = current.details.copy(addressId = 1234567890)
    verify(mockIndividualAccounts, once).update(matching(current.copy(details = updatedDetails)))(any[HeaderCarrier])
    verify(mockManageDetails, once).updatePostcode(matching(current.individualId),any(),matching(1234567890))(any())(any(), any())
  }

  it must "create an address record, and update the user's record with the generated address ID, if they enter the address manually" in {
    val (_, current) = stubLoggedInUser()
    val address: Address = addressGen.copy(addressUnitId = None)
    val addressId = Random.nextInt()

    when(mockAddressConnector.create(any[Address])(any[HeaderCarrier])).thenReturn(Future.successful(addressId))
    when(mockManageDetails.updatePostcode(any(),any(),any())(any())(any(), any())).thenReturn(Future.successful(()))

    val validFormData: Seq[(String, String)] = Seq(
      "address.line1" -> address.line1,
      "address.line2" -> address.line2,
      "address.line3" -> address.line3,
      "address.line4" -> address.line4,
      "address.postcode" -> address.postcode
    )

    val res = TestUpdatePersonalDetails.updateAddress()(request.withFormUrlEncodedBody(validFormData: _*))
    status(res) must be(SEE_OTHER)
    redirectLocation(res) must be(Some(viewDetailsPage))

    val updatedDetails = current.details.copy(addressId = addressId)

    verify(mockAddressConnector, once).create(matching(address))(any[HeaderCarrier])
    verify(mockIndividualAccounts, once).update(matching(current.copy(details = updatedDetails)))(any[HeaderCarrier])
    verify(mockManageDetails, once).updatePostcode(matching(current.individualId),any(),matching(addressId))(any())(any(), any())
  }

  "The update mobile number page" must "update the user's mobile number if they submit a valid form" in {
    val (_, current) = stubLoggedInUser()

    val validData = Seq(
      "phone" -> "01234567890"
    )

    val res = TestUpdatePersonalDetails.updateMobile()(request.withFormUrlEncodedBody(validData:_*))
    status(res) mustBe SEE_OTHER
    redirectLocation(res) mustBe Some(viewDetailsPage)

    val updatedDetails = current.details.copy(phone2 = Some("01234567890"))
    verify(mockIndividualAccounts, once).update(matching(current.copy(details = updatedDetails)))(any[HeaderCarrier])
  }

  private lazy val viewDetailsPage = controllers.manageDetails.routes.ViewDetails.show().url

  private object TestUpdatePersonalDetails extends UpdatePersonalDetails(
    app.injector.instanceOf[ApplicationConfig],
    StubAuthentication,
    mockAddressConnector,
    mockIndividualAccounts,
    mockManageDetails
  )

  lazy val mockIndividualAccounts = {
    val m = mock[IndividualAccounts]
    when(m.update(any[DetailedIndividualAccount])(any[HeaderCarrier])).thenReturn(Future.successful(()))
    m
  }

  lazy val mockAddressConnector = mock[Addresses]
  lazy val mockManageDetails = mock[ManageDetails]

  lazy val request = FakeRequest().withSession(token)

  lazy val once = times(1)

  private def stubLoggedInUser() = {
    val groupAccount = groupAccountGen.sample.get
    val individual = individualGen.sample.get
    StubGroupAccountConnector.stubAccount(groupAccount)
    when(mockIndividualAccounts.get(matching(individual.individualId))(any[HeaderCarrier])).thenReturn(Future.successful(Some(individual)))
    StubAuthentication.stubAuthenticationResult(Authenticated(Accounts(groupAccount, individual)))
    (groupAccount, individual)
  }
}
