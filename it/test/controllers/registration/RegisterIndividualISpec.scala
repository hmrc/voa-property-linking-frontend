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
import com.github.tomakehurst.wiremock.client.WireMock.{aResponse, _}
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import models.GroupAccount
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.http.HeaderNames
import play.api.http.Status.OK
import play.api.libs.json.{JsObject, Json}
import play.api.libs.ws.WSResponse
import play.api.test.Helpers._
import utils.ListYearsHelpers

class RegisterIndividualISpec extends ISpecBase with HtmlComponentHelpers with ListYearsHelpers {

  val titleText = "Complete your contact details - Valuation Office Agency - GOV.UK"
  val headingText = "Complete your contact details"
  val weUseYourText = "We use your contact details to send you correspondence related to the service and your account."
  val enterAddressManuallyText = "Enter address manually"
  val findAddressByPostcodeText = "Find address by postcode"
  val findAddressButtonText = "Find address"
  val addressText = "Address"
  val postcodeText = "Postcode"
  val emailAddressText = "Email address"
  val confirmEmailAddressText = "Confirm email address"
  val phoneNumText = "Telephone number"
  val mobileNumText = "Mobile number"
  val tradingNameText = "Trading name (Optional)"
  val optionalText = "Optional"
  val saveAndContinueText = "Save and continue"
  val errorText = "Error: "
  val addressLengthLine1ErrorText = "This field must be 80 characters or less"
  val addressLengthErrorText = "This field must be 30 characters or less"

  val titleTextWelsh = "Cwblhewch eich manylion cyswllt - Valuation Office Agency - GOV.UK"
  val headingTextWelsh = "Cwblhewch eich manylion cyswllt"
  val weUseYourTextWelsh =
    "Rydym yn defnyddio’ch manylion cyswllt i anfon gohebiaeth atoch sy’n ymwneud â’ch cyfrif a’r gwasanaeth."
  val enterAddressManuallyTextWelsh = "Nodwchy cyfeiriad â llaw"
  val findAddressByPostcodeTextWelsh = "Canfod cyfeiriad yn ôl cod post"
  val findAddressButtonTextWelsh = "Dod o hyd i gyfeiriad"
  val addressTextWelsh = "Cyfeiriad"
  val postcodeTextWelsh = "Cod post"
  val emailAddressTextWelsh = "Cyfeiriad e-bost"
  val confirmEmailAddressTextWelsh = "Cadarnhau cyfeiriad e-bost"
  val phoneNumTextWelsh = "Rhif ffôn"
  val mobileNumTextWelsh = "Rhif ffôn symudol"
  val tradingNameTextWelsh = "Enw masnachu (Dewisol)"
  val optionalTextWelsh = "Dewisol"
  val saveAndContinueTextWelsh = "Arbed a pharhau"
  val errorTextWelsh = "Gwall: "
  val addressLengthErrorTextWelsh = "Mae’n rhaid i’r maes hwn fod yn 30 o gymeriadau neu lai"
  val addressLengthLine1ErrorTextWelsh = "Mae’n rhaid i’r maes hwn fod yn 80 o gymeriadau neu lai"

  val headingSelector = "#main-content > div > div > h1"
  val weUseYourSelector = "#contactDetailsUse"
  val enterAddressManuallySelector = "#addressGroup > p:nth-child(4) > a"
  val findAddressByPostcodeSelector = "#backLookup"
  val findAddressButtonSelector = "#postcodeLookupButton"
  val addressSelector = "label[for='address.line1']"
  val addressLineOneInputSelector = "input[id='address.line1']"
  val addressLineTwoInputSelector = "input[id='address.line2']"
  val addressLineThreeInputSelector = "input[id='address.line3']"
  val addressLineFourInputSelector = "input[id='address.line4']"
  val addressPostcodeSelector = "label[for='address.postcode']"
  val addressPostcodeInputSelector = "input[id='address.postcode']"
  val postcodeSelector = "#postcodeSearchOnly > div.postcodeSearchGroup.govuk-body > div > label"
  val postcodeInputSelector = "input[id='postcodeSearch']"
  val emailSelector = "#main-content > div > div > form > div:nth-child(3) > label"
  val addressLine1ErrorSelector = "p[id='address.line1-error']"
  val addressLine2ErrorSelector = "p[id='address.line2-error']"
  val addressLine3ErrorSelector = "p[id='address.line3-error']"
  val addressLine4ErrorSelector = "p[id='address.line4-error']"

  val emailInputSelector = "input[id='email']"
  val confirmEmailSelector = "#main-content > div > div > form > div:nth-child(4) > label"
  val confirmEmailInputSelector = "input[id='confirmedEmail']"
  val phoneNumSelector = "#main-content > div > div > form > div:nth-child(5) > label"
  val phoneNumInputSelector = "input[id='phone']"
  val mobileNumSelector = "#main-content > div > div > form > div:nth-child(6) > label"
  val mobileNumInputSelector = "input[id='mobilePhone']"
  val tradingNameSelector = "#main-content > div > div > form > div:nth-child(7) > label"
  val optionalSelector = "#tradingName-hint"
  val tradingNameInputSelector = "input[id='tradingName']"
  val saveAndContinueSelector = "button[id='save-and-continue']"

  "RegistrationController show method for a new individual" should {
    "Show an English registration contact details screen with the correct text" which {

      lazy val document: Document = getSuccessPage(English)

      s"has a title of $titleText" in {
        document.title() shouldBe titleText
      }

      s"has a header of $headingText" in {
        document.select(headingSelector).text() shouldBe headingText
      }

      s"has text on the screen of $weUseYourText" in {
        document.select(weUseYourSelector).text() shouldBe weUseYourText
      }

      s"has a link to $enterAddressManuallyText" in {
        document.select(enterAddressManuallySelector).text() shouldBe enterAddressManuallyText
      }

      s"has a link to $findAddressByPostcodeText" in {
        document.select(findAddressByPostcodeSelector).text() shouldBe findAddressByPostcodeText
      }

      s"has a $findAddressButtonText button" in {
        document.select(findAddressButtonSelector).text() shouldBe findAddressButtonText
      }

      s"has text input fields for the $addressText" in {
        document.select(addressSelector).text() shouldBe addressText
        document.select(addressLineOneInputSelector).attr("type") shouldBe "text"
        document.select(addressLineTwoInputSelector).attr("type") shouldBe "text"
        document.select(addressLineThreeInputSelector).attr("type") shouldBe "text"
        document.select(addressLineFourInputSelector).attr("type") shouldBe "text"
        document.select(addressPostcodeSelector).text() shouldBe postcodeText
        document.select(addressPostcodeInputSelector).attr("type") shouldBe "text"
      }

      s"has text input fields for the $postcodeText" in {
        document.select(postcodeSelector).text() shouldBe postcodeText
        document.select(postcodeInputSelector).attr("type") shouldBe "text"
      }

      s"has a text input field for the $emailAddressText" in {
        document.select(emailSelector).text() shouldBe emailAddressText
        document.select(emailInputSelector).attr("type") shouldBe "text"
      }

      s"has a text input field for the $confirmEmailAddressText" in {
        document.select(confirmEmailSelector).text() shouldBe confirmEmailAddressText
        document.select(confirmEmailInputSelector).attr("type") shouldBe "text"
      }

      s"has a text input field for the $phoneNumText" in {
        document.select(phoneNumSelector).text() shouldBe phoneNumText
        document.select(phoneNumInputSelector).attr("type") shouldBe "text"
      }

      s"has a text input field for the $mobileNumText" in {
        document.select(mobileNumSelector).text() shouldBe mobileNumText
        document.select(mobileNumInputSelector).attr("type") shouldBe "text"
      }

      s"has a text input field for the $tradingNameText" in {
        document.select(tradingNameSelector).text() shouldBe tradingNameText
        document.select(tradingNameInputSelector).attr("type") shouldBe "text"
      }

      s"has text on the screen of $optionalText" in {
        document.select(optionalSelector).text() shouldBe optionalText
      }

      s"has a $saveAndContinueText button" in {
        document.select(saveAndContinueSelector).text() shouldBe saveAndContinueText
      }
    }

    "Show a Welsh registration contact details screen with the correct text" which {

      lazy val document: Document = getSuccessPage(Welsh)

      s"has a title of $titleText in Welsh" in {
        document.title() shouldBe titleTextWelsh
      }

      s"has a header of $headingText in Welsh" in {
        document.select(headingSelector).text() shouldBe headingTextWelsh
      }

      s"has text on the screen of $weUseYourText in Welsh" in {
        document.select(weUseYourSelector).text() shouldBe weUseYourTextWelsh
      }

      s"has a link to $enterAddressManuallyText in Welsh" in {
        document.select(enterAddressManuallySelector).text() shouldBe enterAddressManuallyTextWelsh
      }

      s"has a link to $findAddressByPostcodeText in Welsh" in {
        document.select(findAddressByPostcodeSelector).text() shouldBe findAddressByPostcodeTextWelsh
      }

      s"has a $findAddressButtonText button in Welsh" in {
        document.select(findAddressButtonSelector).text() shouldBe findAddressButtonTextWelsh
      }

      s"has text input fields for the $addressText in Welsh" in {
        document.select(addressSelector).text() shouldBe addressTextWelsh
        document.select(addressLineOneInputSelector).attr("type") shouldBe "text"
        document.select(addressLineTwoInputSelector).attr("type") shouldBe "text"
        document.select(addressLineThreeInputSelector).attr("type") shouldBe "text"
        document.select(addressLineFourInputSelector).attr("type") shouldBe "text"
        document.select(addressPostcodeSelector).text() shouldBe postcodeTextWelsh
        document.select(addressPostcodeInputSelector).attr("type") shouldBe "text"
      }

      s"has text input fields for the $postcodeText in Welsh" in {
        document.select(postcodeSelector).text() shouldBe postcodeTextWelsh
        document.select(postcodeInputSelector).attr("type") shouldBe "text"
      }

      s"has a text input field for the $emailAddressText in Welsh" in {
        document.select(emailSelector).text() shouldBe emailAddressTextWelsh
        document.select(emailInputSelector).attr("type") shouldBe "text"
      }

      s"has a text input field for the $confirmEmailAddressText in Welsh" in {
        document.select(confirmEmailSelector).text() shouldBe confirmEmailAddressTextWelsh
        document.select(confirmEmailInputSelector).attr("type") shouldBe "text"
      }

      s"has a text input field for the $phoneNumText in Welsh" in {
        document.select(phoneNumSelector).text() shouldBe phoneNumTextWelsh
        document.select(phoneNumInputSelector).attr("type") shouldBe "text"
      }

      s"has a text input field for the $mobileNumText in Welsh" in {
        document.select(mobileNumSelector).text() shouldBe mobileNumTextWelsh
        document.select(mobileNumInputSelector).attr("type") shouldBe "text"
      }

      s"has a text input field for the $tradingNameText in Welsh" in {
        document.select(tradingNameSelector).text() shouldBe tradingNameTextWelsh
        document.select(tradingNameInputSelector).attr("type") shouldBe "text"
      }

      s"has text on the screen of $optionalText in Welsh" in {
        document.select(optionalSelector).text() shouldBe optionalTextWelsh
      }

      s"has a $saveAndContinueText button in Welsh" in {
        document.select(saveAndContinueSelector).text() shouldBe saveAndContinueTextWelsh
      }
    }
  }

  "RegistrationController show" should {
    "with confidence level 50 redirects to IV uplift start" in {
      stubsSetup

      val authResponseBody =
        """{ "affinityGroup": "Individual", "credentialRole": "User", "optionalName": {"name": "Test First Name", "lastName": "Test Last Name"}, "email": "test@test.com", "groupIdentifier": "1", "externalId": "3", "confidenceLevel": 50}"""

      stubFor {
        post("/auth/authorise")
          .willReturn {
            aResponse.withStatus(OK).withBody(authResponseBody)
          }
      }

      val result = await(
        ws.url(s"http://localhost:$port/business-rates-property-linking/complete-contact-details")
          .withCookies(languageCookie(English), getSessionCookie(testSessionId))
          .withFollowRedirects(follow = false)
          .get()
      )

      result.status shouldBe SEE_OTHER
      result.headers("Location").head shouldBe "/business-rates-property-linking/identity-verification/start-uplift"
    }
  }

  private def getSuccessPage(language: Language): Document = {

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

  private def stubsSetup: StubMapping = {

    stubFor {
      get("/business-rates-authorisation/authenticate")
        .willReturn {
          aResponse.withStatus(OK).withBody(Json.toJson(testAccounts).toString())
        }
    }

    val authResponseBody =
      """{ "affinityGroup": "Individual", "credentialRole": "User", "optionalItmpName": {"givenName": "Test First Name", "familyName": "Test Last Name"}, "email": "test@test.com", "groupIdentifier": "1", "externalId": "3", "confidenceLevel": 200}"""

    stubFor {
      post("/auth/authorise")
        .willReturn {
          aResponse.withStatus(OK).withBody(authResponseBody)
        }
    }

    stubFor(
      get("/property-linking/individuals?externalId=3")
        .willReturn(notFound)
    )
  }

  "RegistrationController post method for a new individual" should {
    "redirect to the confirmation page when all valid details are passed in" which {

      val requestBody: JsObject = Json.obj(
        "address" -> Json
          .obj("line1" -> "test street", "line2" -> "", "line3" -> "", "line4" -> "", "postcode" -> "LS1 3SP"),
        "phone"          -> "0177728837298",
        "mobilePhone"    -> "07829879332",
        "email"          -> "test@email.com",
        "confirmedEmail" -> "test@email.com",
        "tradingName"    -> "test trade name"
      )

      lazy val res = postContactDetailsPage(language = English, postBody = requestBody)

      val redirectUrl = "/business-rates-property-linking/create-confirmation?personId=2"

      "has a status of 303" in {
        res.status shouldBe SEE_OTHER
      }

      s"has a location header of $redirectUrl" in {
        res.header("Location") shouldBe Some(redirectUrl)
      }

    }

    "Return a bad request with the relevant errors when each address line is greater than 30 characters in English" which {

      val requestBody: JsObject = Json.obj(
        "address" -> Json.obj(
          "line1"    -> "Address line of 81 charssssssssssssssssssssssssssssssssssssssssssssssssssssssssss",
          "line2"    -> "Address line of 31 charssssssss",
          "line3"    -> "Address line of 31 charssssssss",
          "line4"    -> "Address line of 31 charssssssss",
          "postcode" -> "LS1 3SP"
        ),
        "phone"          -> "0177728837298",
        "mobilePhone"    -> "07829879332",
        "email"          -> "test@email.com",
        "confirmedEmail" -> "test@email.com",
        "tradingName"    -> "test trade name"
      )

      lazy val res = postContactDetailsPage(language = English, postBody = requestBody)

      "has a status of 400" in {
        res.status shouldBe BAD_REQUEST
      }

      lazy val document = Jsoup.parse(res.body)

      s"has a title of ${errorText + titleText}" in {
        document.title() shouldBe errorText + titleText
      }

      s"has an error above the address line 1 field of ${errorText + addressLengthLine1ErrorText}" in {
        document.select(addressLine1ErrorSelector).text() shouldBe errorText + addressLengthLine1ErrorText
      }

      s"has an error above the address line 2 field of ${errorText + addressLengthErrorText}" in {
        document.select(addressLine2ErrorSelector).text() shouldBe errorText + addressLengthErrorText
      }

      s"has an error above the address line 3 field of ${errorText + addressLengthErrorText}" in {
        document.select(addressLine3ErrorSelector).text() shouldBe errorText + addressLengthErrorText
      }

      s"has an error above the address line 4 field of ${errorText + addressLengthErrorText}" in {
        document.select(addressLine4ErrorSelector).text() shouldBe errorText + addressLengthErrorText
      }

    }

    "Return a bad request with the relevant errors when each address line is greater than 30 characters in Welsh" which {

      val requestBody: JsObject = Json.obj(
        "address" -> Json.obj(
          "line1"    -> "Address line of 81 charssssssssssssssssssssssssssssssssssssssssssssssssssssssssss",
          "line2"    -> "Address line of 31 charssssssss",
          "line3"    -> "Address line of 31 charssssssss",
          "line4"    -> "Address line of 31 charssssssss",
          "postcode" -> "LS1 3SP"
        ),
        "phone"          -> "0177728837298",
        "mobilePhone"    -> "07829879332",
        "email"          -> "test@email.com",
        "confirmedEmail" -> "test@email.com",
        "tradingName"    -> "test trade name"
      )

      lazy val res = postContactDetailsPage(language = Welsh, postBody = requestBody)

      "has a status of 400" in {
        res.status shouldBe BAD_REQUEST
      }

      lazy val document = Jsoup.parse(res.body)

      s"has a title of ${errorText + titleText} in welsh" in {
        document.title() shouldBe errorTextWelsh + titleTextWelsh
      }

      s"has an error above the address line 1 field of ${errorText + addressLengthLine1ErrorText} in welsh" in {
        // The below is a bug, it should translate the Error part to welsh too
        document.select(addressLine1ErrorSelector).text() shouldBe errorText + addressLengthLine1ErrorTextWelsh
      }

      s"has an error above the address line 2 field of ${errorText + addressLengthErrorText} in welsh" in {
        // The below is a bug, it should translate the Error part to welsh too
        document.select(addressLine2ErrorSelector).text() shouldBe errorText + addressLengthErrorTextWelsh
      }

      s"has an error above the address line 3 field of ${errorText + addressLengthErrorText} in welsh" in {
        // The below is a bug, it should translate the Error part to welsh too
        document.select(addressLine3ErrorSelector).text() shouldBe errorText + addressLengthErrorTextWelsh
      }

      s"has an error above the address line 4 field of ${errorText + addressLengthErrorText} in welsh" in {
        // The below is a bug, it should translate the Error part to welsh too
        document.select(addressLine4ErrorSelector).text() shouldBe errorText + addressLengthErrorTextWelsh
      }

    }

  }

  private def postContactDetailsPage(language: Language, postBody: JsObject): WSResponse = {

    val authResponseBody =
      """{ "affinityGroup": "Individual", "credentialRole": "User", "optionalItmpName": {"givenName": "Test First Name", "familyName": "Test Last Name"}, "email": "test@test.com", "groupIdentifier": "1", "externalId": "3", "confidenceLevel": 200}"""

    stubFor {
      post("/auth/authorise")
        .willReturn {
          aResponse.withStatus(OK).withBody(authResponseBody)
        }
    }

    stubFor {
      post("/property-linking/address")
        .willReturn {
          aResponse.withStatus(OK).withBody(Json.obj("id" -> 1L).toString())
        }
    }

    stubFor {
      get("/property-linking/individuals?externalId=3")
        .willReturn {
          aResponse.withStatus(OK).withBody(Json.toJson(detailedIndividualAccount).toString())
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
      agentCode = None)

    stubFor {
      get("/property-linking/groups?groupId=1")
        .willReturn {
          aResponse.withStatus(OK).withBody(Json.toJson(testGroup).toString())
        }
    }

    stubFor {
      post("/property-linking/groups")
        .willReturn {
          aResponse.withStatus(OK).withBody(Json.obj("id" -> 1L).toString())
        }
    }

    stubFor {
      post("/property-linking/individuals")
        .willReturn {
          aResponse.withStatus(OK).withBody(Json.obj("id" -> 1L).toString())
        }
    }

    await(
      ws.url(s"http://localhost:$port/business-rates-property-linking/complete-your-contact-details")
        .withCookies(languageCookie(language), getSessionCookie(testSessionId))
        .withFollowRedirects(follow = false)
        .withHttpHeaders(HeaderNames.COOKIE -> "sessionId", "Csrf-Token" -> "nocheck")
        .post(body = postBody))

  }
}
