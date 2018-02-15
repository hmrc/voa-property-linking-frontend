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

import java.time.{Clock, Instant, ZoneId}

import connectors.{Addresses, Authenticated, GroupAccounts}
import controllers.ControllerSpec
import models._
import org.jsoup.Jsoup
import org.scalatest.mockito.MockitoSugar
import org.mockito.Mockito._
import org.mockito.ArgumentMatchers.{eq => matching, _}
import play.api.test.FakeRequest
import resources._
import utils.StubAuthentication
import play.api.test.Helpers._
import services.{ManageDetails, Success}

import scala.concurrent.Future
import uk.gov.hmrc.http.HeaderCarrier

class UpdateOrganisationDetailsSpec extends ControllerSpec with MockitoSugar {
  "The update business name page" must "require a non-empty business name" in {
    stubLoggedInUser()

    val emptyName = Seq(
      "businessName" -> ""
    )

    val res = testController.updateBusinessName()(FakeRequest().withFormUrlEncodedBody(emptyName: _*))
    status(res) mustBe BAD_REQUEST

    val html = Jsoup.parse(contentAsString(res))
    html.select("label[for=businessName] span.error-message").text mustBe "This must be filled in"
  }

  it must "update the business name on a valid submission" in {
    val (org, person) = stubLoggedInUser()
    when(mockGroups.update(anyLong, any[UpdatedOrganisationAccount])(any[HeaderCarrier])).thenReturn(Future.successful(()))

    val validData = Seq(
      "businessName" -> "My Cool Business"
    )

    val res = testController.updateBusinessName()(FakeRequest().withFormUrlEncodedBody(validData: _*))
    status(res) mustBe SEE_OTHER
    redirectLocation(res) mustBe Some(viewDetailsPage)

    verify(mockGroups, once).update(matching(org.id), matching(updatedDetails(org, person.externalId, name = Some("My Cool Business"))))(any[HeaderCarrier])
  }

  "The update business address page" must "update the business address ID if the postcode lookup is used" in {
    val (org, person) = stubLoggedInUser()
    when(mockGroups.update(anyLong, any[UpdatedOrganisationAccount])(any[HeaderCarrier])).thenReturn(Future.successful(()))
    when(mockManageDetails.updatePostcode(any(), any(), any())(any())(any(), any())).thenReturn(Future.successful(Success))

    val validData = Seq(
      "address.addressId" -> "1234567890",
      "address.line1" -> "1, The Place",
      "address.postcode" -> "AA11 1AA"
    )

    val res = testController.updateBusinessAddress()(FakeRequest().withFormUrlEncodedBody(validData: _*))
    status(res) mustBe SEE_OTHER
    redirectLocation(res) mustBe Some(viewDetailsPage)

    verify(mockGroups, once).update(matching(org.id), matching(updatedDetails(org, person.externalId, addressId = Some(1234567890))))(any[HeaderCarrier])
    verify(mockManageDetails, once).updatePostcode(matching(person.individualId), any(), matching(1234567890))(any())(any(), any())
  }

  it must "create an address record, and update the business address ID to the created ID, if the address is entered manually" in {
    val (org, person) = stubLoggedInUser()
    when(mockGroups.update(anyLong, any[UpdatedOrganisationAccount])(any[HeaderCarrier])).thenReturn(Future.successful(()))
    when(mockAddresses.create(any[Address])(any[HeaderCarrier])).thenReturn(Future.successful(1))
    when(mockManageDetails.updatePostcode(any(), any(), any())(any())(any(), any())).thenReturn(Future.successful(Success))

    val validData = Seq(
      "address.line1" -> "1, The Place",
      "address.postcode" -> "AA11 1AA"
    )

    val res = testController.updateBusinessAddress()(FakeRequest().withFormUrlEncodedBody(validData: _*))
    status(res) mustBe SEE_OTHER
    redirectLocation(res) mustBe Some(viewDetailsPage)

    verify(mockAddresses, once).create(matching(Address(None, "1, The Place", "", "", "", "AA11 1AA")))(any[HeaderCarrier])
    verify(mockGroups, once).update(matching(org.id), matching(updatedDetails(org, person.externalId, addressId = Some(1))))(any[HeaderCarrier])
    verify(mockManageDetails, once).updatePostcode(matching(person.individualId), any(), matching(1))(any())(any(), any())
  }

  "The update business phone page" must "require a non-empty phone number" in {
    stubLoggedInUser()
    when(mockGroups.update(anyLong, any[UpdatedOrganisationAccount])(any[HeaderCarrier])).thenReturn(Future.successful(()))

    val emptyPhoneNumber = Seq(
      "phone" -> ""
    )

    val res = testController.updateBusinessPhone()(FakeRequest().withFormUrlEncodedBody(emptyPhoneNumber: _*))
    status(res) mustBe BAD_REQUEST

    val html = Jsoup.parse(contentAsString(res))
    html.select("label[for=phone] span.error-message").text mustBe "This must be filled in"
  }

  it must "update the business phone number on a valid submission" in {
    val (org, person) = stubLoggedInUser()
    when(mockGroups.update(anyLong, any[UpdatedOrganisationAccount])(any[HeaderCarrier])).thenReturn(Future.successful(()))

    val validData = Seq(
      "phone" -> "999"
    )

    val res = testController.updateBusinessPhone()(FakeRequest().withFormUrlEncodedBody(validData: _*))
    status(res) mustBe SEE_OTHER
    redirectLocation(res) mustBe Some(viewDetailsPage)

    verify(mockGroups, once).update(matching(org.id), matching(updatedDetails(org, person.externalId, phone = Some("999"))))(any[HeaderCarrier])
  }

  "The update business email page" must "require a valid email address" in {
    stubLoggedInUser()
    when(mockGroups.update(anyLong, any[UpdatedOrganisationAccount])(any[HeaderCarrier])).thenReturn(Future.successful(()))

    val invalidEmail = Seq(
      "email" -> "not an email",
      "confirmedEmail" -> "not an email"
    )

    val res = testController.updateBusinessEmail()(FakeRequest().withFormUrlEncodedBody(invalidEmail: _*))
    status(res) mustBe BAD_REQUEST

    val html = Jsoup.parse(contentAsString(res))
    html.select("label[for=email] span.error-message").text mustBe "Enter a valid email address"
  }

  it must "require the confirmed email to match" in {
    stubLoggedInUser()
    when(mockGroups.update(anyLong, any[UpdatedOrganisationAccount])(any[HeaderCarrier])).thenReturn(Future.successful(()))

    val mismatchingEmails = Seq(
      "email" -> "email@example.com",
      "confirmedEmail" -> "anotherEmail@example.com"
    )

    val res = testController.updateBusinessEmail()(FakeRequest().withFormUrlEncodedBody(mismatchingEmails: _*))
    status(res) mustBe BAD_REQUEST

    val html = Jsoup.parse(contentAsString(res))
    html.select("label[for=confirmedEmail] span.error-message").text mustBe "Email addresses must match. Check them and try again"
  }

  it must "update the business email address on a valid submission" in {
    val (org, person) = stubLoggedInUser()
    when(mockGroups.update(anyLong, any[UpdatedOrganisationAccount])(any[HeaderCarrier])).thenReturn(Future.successful(()))

    val validData = Seq(
      "email" -> "email@example.com",
      "confirmedEmail" -> "email@example.com"
    )

    val res = testController.updateBusinessEmail()(FakeRequest().withFormUrlEncodedBody(validData: _*))
    status(res) mustBe SEE_OTHER
    redirectLocation(res) mustBe Some(viewDetailsPage)

    verify(mockGroups, once).update(matching(org.id), matching(updatedDetails(org, person.externalId, email = Some("email@example.com"))))(any[HeaderCarrier])
  }

  private def updatedDetails(org: GroupAccount,
                             personId: String,
                             addressId: Option[Int] = None,
                             name: Option[String] = None,
                             email: Option[String] = None,
                             phone: Option[String] = None) = {
    UpdatedOrganisationAccount(org.groupId, addressId.getOrElse(org.addressId), org.isAgent, name.getOrElse(org.companyName), email.getOrElse(org.email), phone.getOrElse(org.phone), Instant.now(clock), personId)
  }

  private lazy val testController = new UpdateOrganisationDetails(StubAuthentication, mockGroups, mockAddresses, mockManageDetails)(clock, messageApi, applicationConfig)

  private lazy val mockAddresses = mock[Addresses]
  private lazy val mockGroups = mock[GroupAccounts]
  private lazy val mockManageDetails = mock[ManageDetails]

  private def stubLoggedInUser() = {
    val org: GroupAccount = groupAccountGen
    val person: DetailedIndividualAccount = individualGen
    when(mockGroups.get(anyInt)(any[HeaderCarrier])).thenReturn(Future.successful(Some(org)))
    StubAuthentication.stubAuthenticationResult(Authenticated(Accounts(org, person)))
    (org, person)
  }

  private lazy val once = times(1)
  private lazy val viewDetailsPage = controllers.manageDetails.routes.ViewDetails.show().url

  private lazy val clock = Clock.fixed(Instant.now, ZoneId.systemDefault)
}
