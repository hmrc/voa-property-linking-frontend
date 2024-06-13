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

package controllers.registration

import base.{HtmlComponentHelpers, ISpecBase}
import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import models.{Address, GroupAccount}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.http.HeaderNames
import play.api.http.Status.OK
import play.api.libs.json.{JsObject, Json}
import play.api.test.Helpers._
import utils.ListYearsHelpers

class RegisterAssistantISpec extends ISpecBase with HtmlComponentHelpers with ListYearsHelpers {

  val titleText = "Complete your contact details - Valuation Office Agency - GOV.UK"
  val headingText = "Complete your contact details"
  val weUseYourText = "We use your contact details to send you correspondence related to the service and your account."
  val youHaveText = "You have been added as a user to your organisation, please confirm your details below"
  val yourOrgText = "Your organisation details"
  val orgNameText = "Organisation name"
  val postalAddressText = "Postal address"
  val phoneNumText = "Telephone number"
  val emailText = "Correspondence email address"
  val yourDetailsText = "Your details"
  val firstNameText = "First name"
  val lastNameText = "Last name"
  val confirmText = "Confirm"
  val errorText = "Error: "
  val firstNameErrorText = "This field must be 100 characters or less"
  val lastNameErrorText = "This field must be 100 characters or less"

  val titleTextWelsh = "Cwblhewch eich manylion cyswllt - Valuation Office Agency - GOV.UK"
  val headingTextWelsh = "Cwblhewch eich manylion cyswllt"
  val weUseYourTextWelsh =
    "Rydym yn defnyddio’ch manylion cyswllt i anfon gohebiaeth atoch sy’n ymwneud â’ch cyfrif a’r gwasanaeth."
  val youHaveTextWelsh = "Rydych wedi cael eich ychwanegu fel defnyddiwr i’ch sefydliad, cadarnhewch eich manylion isod"
  val yourOrgTextWelsh = "Manylion eich sefydliad"
  val orgNameTextWelsh = "Enw sefydliad"
  val postalAddressTextWelsh = "Cyfeiriad post"
  val phoneNumTextWelsh = "Rhif ffôn"
  val emailTextWelsh = "Cyfeiriad gohebiaeth e-bost"
  val yourDetailsTextWelsh = "Eich manylion"
  val firstNameTextWelsh = "Enw cyntaf"
  val lastNameTextWelsh = "Cyfenw"
  val confirmTextWelsh = "Cadarnhau"
  val errorTextWelsh = "Gwall: "
  val firstNameErrorTextWelsh = "Mae’n rhaid i’r maes hwn fod yn 100 o gymeriadau neu lai"
  val lastNameErrorTextWelsh = "Mae’n rhaid i’r maes hwn fod yn 100 o gymeriadau neu lai"

  val headingSelector = "#main-content > div > div > h1"
  val weUseYourSelector = "#main-content > div > div > p.govuk-body"
  val youHaveSelector = "#main-content > div > div > p.govuk-hint"
  val yourOrgSelector = "#main-content > div > div > h2"
  val orgNameSelector = "#main-content > div > div > table > tbody > tr:nth-child(1) > th"
  val orgNameValueSelector = "#main-content > div > div > table > tbody > tr:nth-child(1) > td"
  val postalAddressSelector = "#main-content > div > div > table > tbody > tr:nth-child(2) > th"
  val postalAddressValueSelector = "#main-content > div > div > table > tbody > tr:nth-child(2) > td"
  val phoneNumSelector = "#main-content > div > div > table > tbody > tr:nth-child(3) > th"
  val phoneNumValueSelector = "#main-content > div > div > table > tbody > tr:nth-child(3) > td"
  val emailSelector = "#main-content > div > div > table > tbody > tr:nth-child(4) > th"
  val emailValueSelector = "#main-content > div > div > table > tbody > tr:nth-child(4) > td"
  val yourDetailsSelector = "#main-content > div > div > form > h2"
  val firstNameTextSelector = "label[for='firstName']"
  val firstNameInputSelector = "input[id='firstName']"
  val lastNameTextSelector = "label[for='lastName']"
  val lastNameInputSelector = "input[id='lastName']"
  val confirmSelector = "button.govuk-button"
  val firstNameSummaryErrorSelector = "#main-content > div > div > div > div > div > ul > li:nth-child(1) > a"
  val lastNameSummaryErrorSelector = "#main-content > div > div > div > div > div > ul > li:nth-child(2) > a"
  val firstNameFieldErrorSelector = "#firstName-error"
  val lastNameFieldErrorSelector = "#lastName-error"

  val firstNameErrorHref = "#firstName"
  val lastNameErrorHref = "#lastName"

  "RegistrationController show method for a new assistant to the organisation" should {
    "Show an English registration contact details screen with the correct text" which {

      lazy val document: Document = getRegisterAssistantPage(English)

      s"has a title of $titleText" in {
        document.title() shouldBe titleText
      }

      s"has a header of $headingText" in {
        document.select(headingSelector).text() shouldBe headingText
      }

      s"has text on the screen of $weUseYourText" in {
        document.select(weUseYourSelector).text() shouldBe weUseYourText
      }

      s"has text on the screen of $youHaveText" in {
        document.select(youHaveSelector).text() shouldBe youHaveText
      }

      s"has a small header on the screen of $yourOrgText" in {
        document.select(yourOrgSelector).text() shouldBe yourOrgText
      }

      s"has a table row for the $orgNameText" in {
        document.select(orgNameSelector).text() shouldBe orgNameText
        document.select(orgNameValueSelector).text() shouldBe "Test name"
      }

      s"has a table row for the $postalAddressText" in {
        document.select(postalAddressSelector).text() shouldBe postalAddressText
        document.select(postalAddressValueSelector).text() shouldBe "Test 1 Test 2 Test 3 Test 4 TF4 9ER"
      }

      s"has a table row for the $phoneNumText" in {
        document.select(phoneNumSelector).text() shouldBe phoneNumText
        document.select(phoneNumValueSelector).text() shouldBe "1234567890"
      }

      s"has a table row for the $emailText" in {
        document.select(emailSelector).text() shouldBe emailText
        document.select(emailValueSelector).text() shouldBe "test@email.com"
      }

      s"has text on the screen of $yourDetailsText" in {
        document.select(yourDetailsSelector).text() shouldBe yourDetailsText
      }

      s"has a text input field for the $firstNameText" in {
        document.select(firstNameTextSelector).text() shouldBe firstNameText
        document.select(firstNameInputSelector).attr("type") shouldBe "text"
      }

      s"has a text input field for the $lastNameText" in {
        document.select(lastNameTextSelector).text() shouldBe lastNameText
        document.select(lastNameInputSelector).attr("type") shouldBe "text"
      }

      s"has a $confirmText button" in {
        document.select(confirmSelector).text() shouldBe confirmText
      }
    }

    "Show a Welsh registration contact details screen with the correct text" which {

      lazy val document: Document = getRegisterAssistantPage(Welsh)

      s"has a title of $titleText in welsh" in {
        document.title() shouldBe titleTextWelsh
      }

      s"has a header of $headingText in welsh" in {
        document.select(headingSelector).text() shouldBe headingTextWelsh
      }

      s"has text on the screen of $weUseYourText in welsh" in {
        document.select(weUseYourSelector).text() shouldBe weUseYourTextWelsh
      }

      s"has text on the screen of $youHaveText in welsh" in {
        document.select(youHaveSelector).text() shouldBe youHaveTextWelsh
      }

      s"has a small header on the screen of $yourOrgText in welsh" in {
        document.select(yourOrgSelector).text() shouldBe yourOrgTextWelsh
      }

      s"has a table row for the $orgNameText in welsh" in {
        document.select(orgNameSelector).text() shouldBe orgNameTextWelsh
        document.select(orgNameValueSelector).text() shouldBe "Test name"
      }

      s"has a table row for the $postalAddressText in welsh" in {
        document.select(postalAddressSelector).text() shouldBe postalAddressTextWelsh
        document.select(postalAddressValueSelector).text() shouldBe "Test 1 Test 2 Test 3 Test 4 TF4 9ER"
      }

      s"has a table row for the $phoneNumText in welsh" in {
        document.select(phoneNumSelector).text() shouldBe phoneNumTextWelsh
        document.select(phoneNumValueSelector).text() shouldBe "1234567890"
      }

      s"has a table row for the $emailText in welsh" in {
        document.select(emailSelector).text() shouldBe emailTextWelsh
        document.select(emailValueSelector).text() shouldBe "test@email.com"
      }

      s"has text on the screen of $yourDetailsText in welsh" in {
        document.select(yourDetailsSelector).text() shouldBe yourDetailsTextWelsh
      }

      s"has a text input field for the $firstNameText in welsh" in {
        document.select(firstNameTextSelector).text() shouldBe firstNameTextWelsh
        document.select(firstNameInputSelector).attr("type") shouldBe "text"
      }

      s"has a text input field for the $lastNameText in welsh" in {
        document.select(lastNameTextSelector).text() shouldBe lastNameTextWelsh
        document.select(lastNameInputSelector).attr("type") shouldBe "text"
      }

      s"has a $confirmText button in welsh" in {
        document.select(confirmSelector).text() shouldBe confirmTextWelsh
      }
    }
  }

  "RegistrationController submit assistant method for a new assistant to the organisation" should {

    "Show an english error page when the user inputs nothing for the first name and last name" which {

      val postBody: JsObject = Json.obj(
        "firstName" -> "",
        "lastName"  -> ""
      )

      lazy val document: Document = postRegisterAssistantPage(English, postBody)

      s"has a title of ${errorText + titleText}" in {
        document.title() shouldBe errorText + titleText
      }

      s"has an error at the summary for the $firstNameText" in {
        document.select(firstNameSummaryErrorSelector).text() shouldBe firstNameText + " - " + firstNameErrorText
        document.select(firstNameSummaryErrorSelector).attr("href") shouldBe firstNameErrorHref
      }

      s"has an error at the summary for the $lastNameText" in {
        document.select(lastNameSummaryErrorSelector).text() shouldBe lastNameText + " - " + lastNameErrorText
        document.select(lastNameSummaryErrorSelector).attr("href") shouldBe lastNameErrorHref
      }

      s"has a header of $headingText" in {
        document.select(headingSelector).text() shouldBe headingText
      }

      s"has text on the screen of $weUseYourText" in {
        document.select(weUseYourSelector).text() shouldBe weUseYourText
      }

      s"has text on the screen of $youHaveText" in {
        document.select(youHaveSelector).text() shouldBe youHaveText
      }

      s"has a small header on the screen of $yourOrgText" in {
        document.select(yourOrgSelector).text() shouldBe yourOrgText
      }

      s"has a table row for the $orgNameText" in {
        document.select(orgNameSelector).text() shouldBe orgNameText
        document.select(orgNameValueSelector).text() shouldBe "Test name"
      }

      s"has a table row for the $postalAddressText" in {
        document.select(postalAddressSelector).text() shouldBe postalAddressText
        document.select(postalAddressValueSelector).text() shouldBe "Test 1 Test 2 Test 3 Test 4 TF4 9ER"
      }

      s"has a table row for the $phoneNumText" in {
        document.select(phoneNumSelector).text() shouldBe phoneNumText
        document.select(phoneNumValueSelector).text() shouldBe "1234567890"
      }

      s"has a table row for the $emailText" in {
        document.select(emailSelector).text() shouldBe emailText
        document.select(emailValueSelector).text() shouldBe "test@email.com"
      }

      s"has text on the screen of $yourDetailsText" in {
        document.select(yourDetailsSelector).text() shouldBe yourDetailsText
      }

      s"has an error at the $firstNameText input field" in {
        document.select(firstNameFieldErrorSelector).text() shouldBe errorText + firstNameErrorText
      }

      s"has a text input field for the $firstNameText" in {
        document.select(firstNameTextSelector).text() shouldBe firstNameText
        document.select(firstNameInputSelector).attr("type") shouldBe "text"
      }

      s"has an error at the $lastNameText input field" in {
        document.select(lastNameFieldErrorSelector).text() shouldBe errorText + lastNameErrorText
      }

      s"has a text input field for the $lastNameText" in {
        document.select(lastNameTextSelector).text() shouldBe lastNameText
        document.select(lastNameInputSelector).attr("type") shouldBe "text"
      }

      s"has a $confirmText button" in {
        document.select(confirmSelector).text() shouldBe confirmText
      }
    }

    "Show an english error page when the user inputs over 100 characters for the first name and last name" which {

      val postBody: JsObject = Json.obj(
        "firstName" -> "Long first nameeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeee",
        "lastName"  -> "Long last nameeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeee"
      )

      lazy val document: Document = postRegisterAssistantPage(English, postBody)

      s"has a title of ${errorText + titleText}" in {
        document.title() shouldBe errorText + titleText
      }

      s"has an error at the summary for the $firstNameText" in {
        document.select(firstNameSummaryErrorSelector).text() shouldBe firstNameText + " - " + firstNameErrorText
        document.select(firstNameSummaryErrorSelector).attr("href") shouldBe firstNameErrorHref
      }

      s"has an error at the summary for the $lastNameText" in {
        document.select(lastNameSummaryErrorSelector).text() shouldBe lastNameText + " - " + lastNameErrorText
        document.select(lastNameSummaryErrorSelector).attr("href") shouldBe lastNameErrorHref
      }

      s"has a header of $headingText" in {
        document.select(headingSelector).text() shouldBe headingText
      }

      s"has text on the screen of $weUseYourText" in {
        document.select(weUseYourSelector).text() shouldBe weUseYourText
      }

      s"has text on the screen of $youHaveText" in {
        document.select(youHaveSelector).text() shouldBe youHaveText
      }

      s"has a small header on the screen of $yourOrgText" in {
        document.select(yourOrgSelector).text() shouldBe yourOrgText
      }

      s"has a table row for the $orgNameText" in {
        document.select(orgNameSelector).text() shouldBe orgNameText
        document.select(orgNameValueSelector).text() shouldBe "Test name"
      }

      s"has a table row for the $postalAddressText" in {
        document.select(postalAddressSelector).text() shouldBe postalAddressText
        document.select(postalAddressValueSelector).text() shouldBe "Test 1 Test 2 Test 3 Test 4 TF4 9ER"
      }

      s"has a table row for the $phoneNumText" in {
        document.select(phoneNumSelector).text() shouldBe phoneNumText
        document.select(phoneNumValueSelector).text() shouldBe "1234567890"
      }

      s"has a table row for the $emailText" in {
        document.select(emailSelector).text() shouldBe emailText
        document.select(emailValueSelector).text() shouldBe "test@email.com"
      }

      s"has text on the screen of $yourDetailsText" in {
        document.select(yourDetailsSelector).text() shouldBe yourDetailsText
      }

      s"has an error at the $firstNameText input field" in {
        document.select(firstNameFieldErrorSelector).text() shouldBe errorText + firstNameErrorText
      }

      s"has a text input field for the $firstNameText" in {
        document.select(firstNameTextSelector).text() shouldBe firstNameText
        document.select(firstNameInputSelector).attr("type") shouldBe "text"
      }

      s"has an error at the $lastNameText input field" in {
        document.select(lastNameFieldErrorSelector).text() shouldBe errorText + lastNameErrorText
      }

      s"has a text input field for the $lastNameText" in {
        document.select(lastNameTextSelector).text() shouldBe lastNameText
        document.select(lastNameInputSelector).attr("type") shouldBe "text"
      }

      s"has a $confirmText button" in {
        document.select(confirmSelector).text() shouldBe confirmText
      }
    }

    "Show a welsh error page when the user inputs nothing for the first name and last name" which {

      val postBody: JsObject = Json.obj(
        "firstName" -> "",
        "lastName"  -> ""
      )

      lazy val document: Document = postRegisterAssistantPage(Welsh, postBody)

      s"has a title of ${errorText + titleText} in welsh" in {
        document.title() shouldBe errorTextWelsh + titleTextWelsh
      }

      s"has an error at the summary for the $firstNameText in welsh" in {
        document
          .select(firstNameSummaryErrorSelector)
          .text() shouldBe firstNameTextWelsh + " - " + firstNameErrorTextWelsh
        document.select(firstNameSummaryErrorSelector).attr("href") shouldBe firstNameErrorHref
      }

      s"has an error at the summary for the $lastNameText in welsh" in {
        document.select(lastNameSummaryErrorSelector).text() shouldBe lastNameTextWelsh + " - " + lastNameErrorTextWelsh
        document.select(lastNameSummaryErrorSelector).attr("href") shouldBe lastNameErrorHref
      }

      s"has a header of $headingText in welsh" in {
        document.select(headingSelector).text() shouldBe headingTextWelsh
      }

      s"has text on the screen of $weUseYourText in welsh" in {
        document.select(weUseYourSelector).text() shouldBe weUseYourTextWelsh
      }

      s"has text on the screen of $youHaveText in welsh" in {
        document.select(youHaveSelector).text() shouldBe youHaveTextWelsh
      }

      s"has a small header on the screen of $yourOrgText in welsh" in {
        document.select(yourOrgSelector).text() shouldBe yourOrgTextWelsh
      }

      s"has a table row for the $orgNameText in welsh" in {
        document.select(orgNameSelector).text() shouldBe orgNameTextWelsh
        document.select(orgNameValueSelector).text() shouldBe "Test name"
      }

      s"has a table row for the $postalAddressText in welsh" in {
        document.select(postalAddressSelector).text() shouldBe postalAddressTextWelsh
        document.select(postalAddressValueSelector).text() shouldBe "Test 1 Test 2 Test 3 Test 4 TF4 9ER"
      }

      s"has a table row for the $phoneNumText in welsh" in {
        document.select(phoneNumSelector).text() shouldBe phoneNumTextWelsh
        document.select(phoneNumValueSelector).text() shouldBe "1234567890"
      }

      s"has a table row for the $emailText in welsh" in {
        document.select(emailSelector).text() shouldBe emailTextWelsh
        document.select(emailValueSelector).text() shouldBe "test@email.com"
      }

      s"has text on the screen of $yourDetailsText in welsh" in {
        document.select(yourDetailsSelector).text() shouldBe yourDetailsTextWelsh
      }

//      TODO: The below doesnt translate the Error part to welsh
      s"has an error at the $firstNameText input field in welsh" in {
        document.select(firstNameFieldErrorSelector).text() shouldBe errorText + firstNameErrorTextWelsh
      }

      s"has a text input field for the $firstNameText in welsh" in {
        document.select(firstNameTextSelector).text() shouldBe firstNameTextWelsh
        document.select(firstNameInputSelector).attr("type") shouldBe "text"
      }

//      TODO: The below doesnt translate the Error part to welsh
      s"has an error at the $lastNameText input field in welsh" in {
        document.select(lastNameFieldErrorSelector).text() shouldBe errorText + lastNameErrorTextWelsh
      }

      s"has a text input field for the $lastNameText in welsh" in {
        document.select(lastNameTextSelector).text() shouldBe lastNameTextWelsh
        document.select(lastNameInputSelector).attr("type") shouldBe "text"
      }

      s"has a $confirmText button in welsh" in {
        document.select(confirmSelector).text() shouldBe confirmTextWelsh
      }
    }

    "Show a welsh error page when the user inputs over 100 characters for the first name and last name" which {

      val postBody: JsObject = Json.obj(
        "firstName" -> "Long first nameeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeee",
        "lastName"  -> "Long last nameeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeee"
      )

      lazy val document: Document = postRegisterAssistantPage(Welsh, postBody)

      s"has a title of ${errorText + titleText} in welsh" in {
        document.title() shouldBe errorTextWelsh + titleTextWelsh
      }

      s"has an error at the summary for the $firstNameText in welsh" in {
        document
          .select(firstNameSummaryErrorSelector)
          .text() shouldBe firstNameTextWelsh + " - " + firstNameErrorTextWelsh
        document.select(firstNameSummaryErrorSelector).attr("href") shouldBe firstNameErrorHref
      }

      s"has an error at the summary for the $lastNameText in welsh" in {
        document.select(lastNameSummaryErrorSelector).text() shouldBe lastNameTextWelsh + " - " + lastNameErrorTextWelsh
        document.select(lastNameSummaryErrorSelector).attr("href") shouldBe lastNameErrorHref
      }

      s"has a header of $headingText in welsh" in {
        document.select(headingSelector).text() shouldBe headingTextWelsh
      }

      s"has text on the screen of $weUseYourText in welsh" in {
        document.select(weUseYourSelector).text() shouldBe weUseYourTextWelsh
      }

      s"has text on the screen of $youHaveText in welsh" in {
        document.select(youHaveSelector).text() shouldBe youHaveTextWelsh
      }

      s"has a small header on the screen of $yourOrgText in welsh" in {
        document.select(yourOrgSelector).text() shouldBe yourOrgTextWelsh
      }

      s"has a table row for the $orgNameText in welsh" in {
        document.select(orgNameSelector).text() shouldBe orgNameTextWelsh
        document.select(orgNameValueSelector).text() shouldBe "Test name"
      }

      s"has a table row for the $postalAddressText in welsh" in {
        document.select(postalAddressSelector).text() shouldBe postalAddressTextWelsh
        document.select(postalAddressValueSelector).text() shouldBe "Test 1 Test 2 Test 3 Test 4 TF4 9ER"
      }

      s"has a table row for the $phoneNumText in welsh" in {
        document.select(phoneNumSelector).text() shouldBe phoneNumTextWelsh
        document.select(phoneNumValueSelector).text() shouldBe "1234567890"
      }

      s"has a table row for the $emailText in welsh" in {
        document.select(emailSelector).text() shouldBe emailTextWelsh
        document.select(emailValueSelector).text() shouldBe "test@email.com"
      }

      s"has text on the screen of $yourDetailsText in welsh" in {
        document.select(yourDetailsSelector).text() shouldBe yourDetailsTextWelsh
      }

//      TODO: The below doesnt translate the Error part to welsh
      s"has an error at the $firstNameText input field in welsh" in {
        document.select(firstNameFieldErrorSelector).text() shouldBe errorText + firstNameErrorTextWelsh
      }

      s"has a text input field for the $firstNameText in welsh" in {
        document.select(firstNameTextSelector).text() shouldBe firstNameTextWelsh
        document.select(firstNameInputSelector).attr("type") shouldBe "text"
      }

//      TODO: The below doesnt translate the Error part to welsh
      s"has an error at the $lastNameText input field in welsh" in {
        document.select(lastNameFieldErrorSelector).text() shouldBe errorText + lastNameErrorTextWelsh
      }

      s"has a text input field for the $lastNameText in welsh" in {
        document.select(lastNameTextSelector).text() shouldBe lastNameTextWelsh
        document.select(lastNameInputSelector).attr("type") shouldBe "text"
      }

      s"has a $confirmText button in welsh" in {
        document.select(confirmSelector).text() shouldBe confirmTextWelsh
      }
    }

  }

  private def getRegisterAssistantPage(language: Language): Document = {

    stubsSetup

    val res = await(
      ws.url(s"http://localhost:$port/business-rates-property-linking/complete-contact-details")
        .withCookies(languageCookie(language), getSessionCookie(testSessionId))
        .withFollowRedirects(follow = false)
        .get()
    )

    res.status shouldBe OK
    Jsoup.parse(res.body)
  }
  private def postRegisterAssistantPage(language: Language, postBody: JsObject): Document = {

    stubsSetup

    val res = await(
      ws.url(s"http://localhost:$port/business-rates-property-linking/complete-business-contact-details-assistant")
        .withCookies(languageCookie(language), getSessionCookie(testSessionId))
        .withFollowRedirects(follow = false)
        .withHttpHeaders(HeaderNames.COOKIE -> "sessionId", "Csrf-Token" -> "nocheck")
        .post(body = postBody)
    )

    res.status shouldBe BAD_REQUEST
    Jsoup.parse(res.body)
  }

  private def stubsSetup: StubMapping = {

    stubFor {
      get("/business-rates-authorisation/authenticate")
        .willReturn {
          aResponse.withStatus(OK).withBody(Json.toJson(testAccounts).toString())
        }
    }

    val authResponseBody =
      """{ "affinityGroup": "Organisation", "credentialRole": "Assistant", "optionalName": {"name": "Test First Name", "lastName": "Test Last Name"}, "email": "test@test.com", "groupIdentifier": "1", "externalId": "3", "confidenceLevel": 200}"""

    stubFor {
      post("/auth/authorise")
        .willReturn {
          aResponse.withStatus(OK).withBody(authResponseBody)
        }
    }

    val testGroup = GroupAccount(
      id = 1L,
      groupId = "1L",
      companyName = "Test name",
      addressId = 1L,
      email = "test@email.com",
      phone = "1234567890",
      isAgent = false,
      agentCode = Some(1L))

    stubFor(
      get("/property-linking/groups?groupId=1")
        .willReturn {
          aResponse.withStatus(OK).withBody(Json.toJson(testGroup).toString())
        }
    )

    stubFor(
      get("/property-linking/individuals?externalId=3")
        .willReturn(notFound)
    )

    val testAddress = Address(
      addressUnitId = Some(1L),
      line1 = "Test 1",
      line2 = "Test 2",
      line3 = "Test 3",
      line4 = "Test 4",
      postcode = "TF4 9ER")

    stubFor(
      get("/property-linking/address/1")
        .willReturn {
          aResponse.withStatus(OK).withBody(Json.toJson(testAddress).toString())
        }
    )

    stubFor {
      post("/property-linking/individuals")
        .willReturn {
          aResponse.withStatus(OK).withBody(Json.obj("id" -> 3L).toString())
        }
    }

  }

}
