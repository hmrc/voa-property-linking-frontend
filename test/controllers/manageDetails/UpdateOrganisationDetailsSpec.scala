/*
 * Copyright 2020 HM Revenue & Customs
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

import connectors.GroupAccounts
import controllers.VoaPropertyLinkingSpec
import models._
import org.jsoup.Jsoup
import org.mockito.ArgumentMatchers.{eq => matching, _}
import org.mockito.Mockito._
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.{ManageDetails, Success}
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future

class UpdateOrganisationDetailsSpec extends VoaPropertyLinkingSpec  {
  "The update business name page" must "require a non-empty business name" in new Setup {
    val emptyName = Seq("businessName" -> "")

    val res = testController.updateBusinessName()(FakeRequest().withFormUrlEncodedBody(emptyName: _*))
    status(res) mustBe BAD_REQUEST

    val html = Jsoup.parse(contentAsString(res))
    html.select("label[for=businessName] span.error-message").text mustBe "This must be filled in"
  }

  it must "update the business name on a valid submission" in new Setup {
    when(mockGroups.update(anyLong, any[UpdatedOrganisationAccount])(any[HeaderCarrier])).thenReturn(Future.successful(()))

    val validData = Seq(
      "businessName" -> "My Cool Business"
    )

    val res = testController.updateBusinessName()(FakeRequest().withFormUrlEncodedBody(validData: _*))
    status(res) mustBe SEE_OTHER
    redirectLocation(res) mustBe Some(viewDetailsPage)
  }

  "The update business address page" must "update the business address ID if the postcode lookup is used" in new Setup {
    when(mockGroups.update(any(), any())(any())).thenReturn(Future.successful(()))
    when(mockManageDetails.updatePostcode(any(), any(), any())(any(), any())).thenReturn(Future.successful(Success))

    val validData = Seq(
      "address.addressId" -> "1234567890",
      "address.line1" -> "1, The Place",
      "address.line2" -> "",
      "address.line3" -> "",
      "address.line4" -> "",
      "address.postcode" -> "AA11 1AA"
    )

    val res = testController.updateBusinessAddress()(FakeRequest().withFormUrlEncodedBody(validData: _*))
    status(res) mustBe SEE_OTHER
    redirectLocation(res) mustBe Some(viewDetailsPage)

    verify(mockGroups).update(matching(ga.id), matching(updatedDetails(ga, ggExternalId, addressId = Some(1234567890L))))(any[HeaderCarrier])
    verify(mockManageDetails).updatePostcode(any(), any(), matching(1234567890L))(any(), any())
  }

  it must "create an address record, and update the business address ID to the created ID, if the address is entered manually" in new Setup {
    when(mockGroups.update(anyLong, any[UpdatedOrganisationAccount])(any[HeaderCarrier])).thenReturn(Future.successful(()))
    when(mockAddresses.create(any[Address])(any[HeaderCarrier])).thenReturn(Future.successful(1L))
    when(mockManageDetails.updatePostcode(any(), any(), any())(any(), any())).thenReturn(Future.successful(Success))

    val validData = Seq(
      "address.line1" -> "1, The Place",
      "address.line2" -> "",
      "address.line3" -> "",
      "address.line4" -> "",
      "address.postcode" -> "AA11 1AA"
    )

    val res = testController.updateBusinessAddress()(FakeRequest().withFormUrlEncodedBody(validData: _*))
    status(res) mustBe SEE_OTHER
    redirectLocation(res) mustBe Some(viewDetailsPage)

    verify(mockAddresses).create(matching(Address(None, "1, The Place", "", "", "", "AA11 1AA")))(any[HeaderCarrier])
    verify(mockGroups).update(matching(ga.id), matching(updatedDetails(ga, ggExternalId, addressId = Some(1L))))(any[HeaderCarrier])
    verify(mockManageDetails).updatePostcode(any(), any(), matching(1L))(any(), any())
  }

  "The update business phone page" must "require a non-empty phone number" in new Setup {
    when(mockGroups.update(anyLong, any[UpdatedOrganisationAccount])(any[HeaderCarrier])).thenReturn(Future.successful(()))
    val emptyPhoneNumber = Seq("phone" -> "")

    val res = testController.updateBusinessPhone()(FakeRequest().withFormUrlEncodedBody(emptyPhoneNumber: _*))
    status(res) mustBe BAD_REQUEST

    val html = Jsoup.parse(contentAsString(res))
    html.select("label[for=phone] span.error-message").text mustBe "This must be filled in"
  }

  it must "update the business phone number on a valid submission" in new Setup {
    when(mockGroups.update(anyLong, any[UpdatedOrganisationAccount])(any[HeaderCarrier])).thenReturn(Future.successful(()))

    val validData = Seq("phone" -> "999")

    val res = testController.updateBusinessPhone()(FakeRequest().withFormUrlEncodedBody(validData: _*))
    status(res) mustBe SEE_OTHER
    redirectLocation(res) mustBe Some(viewDetailsPage)

    verify(mockGroups).update(matching(ga.id), matching(updatedDetails(ga, ggExternalId, phone = Some("999"))))(any[HeaderCarrier])
  }

  "The update business email page" must "require a valid email address" in new Setup {
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

  it must "require the confirmed email to match" in new Setup {
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

  it must "update the business email address on a valid submission" in new Setup {
    when(mockGroups.update(anyLong, any[UpdatedOrganisationAccount])(any[HeaderCarrier])).thenReturn(Future.successful(()))

    val validData = Seq(
      "email" -> "email@example.com",
      "confirmedEmail" -> "email@example.com"
    )

    val res = testController.updateBusinessEmail()(FakeRequest().withFormUrlEncodedBody(validData: _*))
    status(res) mustBe SEE_OTHER
    redirectLocation(res) mustBe Some(viewDetailsPage)

    verify(mockGroups).update(matching(ga.id), matching(updatedDetails(ga, ggExternalId, email = Some("email@example.com"))))(any[HeaderCarrier])
  }

  "viewBusinessName" should "display the business name" in new Setup {
    val res = testController.viewBusinessName()(FakeRequest())

    status(res) mustBe OK

    val html = Jsoup.parse(contentAsString(res))
    html.title mustBe "Update business name"
  }

  "viewBusinessAddress" should "display the business address" in new Setup {
    val res = testController.viewBusinessAddress()(FakeRequest())

    status(res) mustBe OK

    val html = Jsoup.parse(contentAsString(res))
    html.title mustBe "Update business address"
  }

  "viewBusinessPhone" should "display the business phone number" in new Setup {
    val res = testController.viewBusinessPhone()(FakeRequest())

    status(res) mustBe OK

    val html = Jsoup.parse(contentAsString(res))
    html.title mustBe "Update business telephone number"
  }

  "viewBusinessEmail" should "display the users email" in new Setup {
    val res = testController.viewBusinessEmail()(FakeRequest())

    status(res) mustBe OK

    val html = Jsoup.parse(contentAsString(res))
    html.title mustBe "Update business email"
  }

  trait Setup {
    protected def updatedDetails(org: GroupAccount,
                                 personId: String,
                                 addressId: Option[Long] = None,
                                 name: Option[String] = None,
                                 email: Option[String] = None,
                                 phone: Option[String] = None) = {
      UpdatedOrganisationAccount(
        governmentGatewayGroupId = org.groupId,
        addressUnitId = addressId.getOrElse(org.addressId),
        representativeFlag = org.isAgent,
        organisationName = name.getOrElse(org.companyName),
        organisationEmailAddress = email.getOrElse(org.email),
        organisationTelephoneNumber = phone.getOrElse(org.phone),
        effectiveFrom = Instant.now(clock),
        changedByGGExternalId = personId)
    }

    val ga = groupAccount(agent = true)

    val clock = Clock.fixed(Instant.now, ZoneId.systemDefault)
    val mockGroups = mock[GroupAccounts]
    val mockManageDetails = mock[ManageDetails]
    val viewDetailsPage = controllers.manageDetails.routes.ViewDetails.show().url
    val testController = new UpdateOrganisationDetails(
      mockCustomErrorHandler,
      preAuthenticatedActionBuilders(),
      mockGroups,
      mockAddresses,
      mockManageDetails
    )(
      executionContext,
      clock,
      stubMessagesControllerComponents(),
      applicationConfig
    )
  }

}
