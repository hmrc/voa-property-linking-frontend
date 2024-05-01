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
  val firstNameText = "First name"
  val lastNameText = "Last name"
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
  val dateOfBirthText = "Date of birth"
  val forExampleText = "For example, 28 4 2017"
  val dayText = "Day"
  val monthText = "Month"
  val yearText = "Year"
  val ninoText = "National Insurance number"
  val itsOnYourText = "It’s on your National Insurance card, benefit letter, payslip or P60. For example, QQ123456C."
  val noNinoText = "I don’t have a National Insurance number"
  val expandedNoNinoText = "If you don’t have these details you’ll need to contact the Valuation Office Agency (VOA)."
  val saveAndContinueText = "Save and continue"
  val errorText = "Error: "
  val addressLengthLine1ErrorText = "This field must be 80 characters or less"
  val addressLengthErrorText = "This field must be 30 characters or less"

  val titleTextWelsh = "Cwblhewch eich manylion cyswllt - Valuation Office Agency - GOV.UK"
  val headingTextWelsh = "Cwblhewch eich manylion cyswllt"
  val weUseYourTextWelsh =
    "Rydym yn defnyddio’ch manylion cyswllt i anfon gohebiaeth atoch sy’n ymwneud â’ch cyfrif a’r gwasanaeth."
  val firstNameTextWelsh = "Enw cyntaf"
  val lastNameTextWelsh = "Cyfenw"
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
  val dateOfBirthTextWelsh = "Dyddiad geni"
  val forExampleTextWelsh = "Er enghraifft, 28 4 2017"
  val dayTextWelsh = "Diwrnod"
  val monthTextWelsh = "Mis"
  val yearTextWelsh = "Blwyddyn"
  val ninoTextWelsh = "Rhif Yswiriant Gwladol"
  val itsOnYourTextWelsh =
    "Mae ar eich cerdyn Yswiriant Gwladol, llythyr budd-dal, slip cyflog neu P60. Er enghraifft, QQ123456C."
  val noNinoTextWelsh = "Does gen i ddim rhif Yswiriant Gwladol"
  val expandedNoNinoTextWelsh =
    "Os nad oes gennych y manylion hyn bydd angen i chi gysylltu ag Asiantaeth y Swyddfa Brisio (VOA)."
  val saveAndContinueTextWelsh = "Arbed a pharhau"
  val errorTextWelsh = "Gwall: "
  val addressLengthErrorTextWelsh = "Mae’n rhaid i’r maes hwn fod yn 30 o gymeriadau neu lai"
  val addressLengthLine1ErrorTextWelsh = "Mae’n rhaid i’r maes hwn fod yn 80 o gymeriadau neu lai"

  val headingSelector = "#main-content > div > div > h1"
  val weUseYourSelector = "#contactDetailsUse"
  val firstNameTextSelector = "#main-content > div > div > form > div:nth-child(2) > label"
  val firstNameInputSelector = "input[id='firstName']"
  val lastNameTextSelector = "#main-content > div > div > form > div:nth-child(3) > label"
  val lastNameInputSelector = "input[id='lastName']"
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
  val emailSelector = "#main-content > div > div > form > div:nth-child(5) > label"
  val emailInputSelector = "input[id='email']"
  val confirmEmailSelector = "#main-content > div > div > form > div:nth-child(6) > label"
  val confirmEmailInputSelector = "input[id='confirmedEmail']"
  val phoneNumSelector = "#main-content > div > div > form > div:nth-child(7) > label"
  val phoneNumInputSelector = "input[id='phone']"
  val mobileNumSelector = "#main-content > div > div > form > div:nth-child(8) > label"
  val mobileNumInputSelector = "input[id='mobilePhone']"
  val tradingNameSelector = "#main-content > div > div > form > div:nth-child(9) > label"
  val optionalSelector = "#tradingName-hint"
  val tradingNameInputSelector = "input[id='tradingName']"
  val dateOfBirthSelector = "#dob > div > fieldset > legend > h1"
  val forExampleSelector = "#dob_dates-hint"
  val daySelector = "#dob_dates > div:nth-child(1) > div > label"
  val dayInputSelector = "input[id='dob-day']"
  val monthSelector = "#dob_dates > div:nth-child(2) > div > label"
  val monthInputSelector = "input[id='dob-month']"
  val yearSelector = "#dob_dates > div:nth-child(3) > div > label"
  val yearInputSelector = "input[id='dob-year']"
  val ninoSelector = "#main-content > div > div > form > div:nth-child(11) > label"
  val ninoInputSelector = "input[id='nino']"
  val itsOnYourSelector = "#nino-hint"
  val noNinoSelector = "#main-content > div > div > form > details > summary > span"
  val noNinoExpandedSelector = "#main-content > div > div > form > details > div"
  val saveAndContinueSelector = "button[id='save-and-continue']"
  val addressLine1ErrorSelector = "p[id='address.line1-error']"
  val addressLine2ErrorSelector = "p[id='address.line2-error']"
  val addressLine3ErrorSelector = "p[id='address.line3-error']"
  val addressLine4ErrorSelector = "p[id='address.line4-error']"

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

      s"has a text input field for the $firstNameText" in {
        document.select(firstNameTextSelector).text() shouldBe firstNameText
        document.select(firstNameInputSelector).attr("type") shouldBe "text"
      }

      s"has a text input field for the $lastNameText" in {
        document.select(lastNameTextSelector).text() shouldBe lastNameText
        document.select(lastNameInputSelector).attr("type") shouldBe "text"
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

      s"has text on the screen of $dateOfBirthText" in {
        document.select(dateOfBirthSelector).text() shouldBe dateOfBirthText
      }

      s"has text on the screen of $forExampleText" in {
        document.select(forExampleSelector).text() shouldBe forExampleText
      }

      s"has a text input field for the $dayText" in {
        document.select(daySelector).text() shouldBe dayText
        document.select(dayInputSelector).attr("type") shouldBe "text"
      }

      s"has a text input field for the $monthText" in {
        document.select(monthSelector).text() shouldBe monthText
        document.select(monthInputSelector).attr("type") shouldBe "text"
      }

      s"has a text input field for the $yearText" in {
        document.select(yearSelector).text() shouldBe yearText
        document.select(yearInputSelector).attr("type") shouldBe "text"
      }

      s"has a text input field for the $ninoText" in {
        document.select(ninoSelector).text() shouldBe ninoText
        document.select(itsOnYourSelector).text() shouldBe itsOnYourText
        document.select(ninoInputSelector).attr("type") shouldBe "text"
      }

      s"has text on the screen of $noNinoText" in {
        document.select(noNinoSelector).text() shouldBe noNinoText
      }

      s"has text on the screen of $expandedNoNinoText" in {
        document.select(noNinoExpandedSelector).text() shouldBe expandedNoNinoText
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

      s"has a text input field for the $firstNameText in Welsh" in {
        document.select(firstNameTextSelector).text() shouldBe firstNameTextWelsh
        document.select(firstNameInputSelector).attr("type") shouldBe "text"
      }

      s"has a text input field for the $lastNameText in Welsh" in {
        document.select(lastNameTextSelector).text() shouldBe lastNameTextWelsh
        document.select(lastNameInputSelector).attr("type") shouldBe "text"
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

      s"has text on the screen of $dateOfBirthText in Welsh" in {
        document.select(dateOfBirthSelector).text() shouldBe dateOfBirthTextWelsh
      }

      s"has text on the screen of $forExampleText in Welsh" in {
        document.select(forExampleSelector).text() shouldBe forExampleTextWelsh
      }

      s"has a text input field for the $dayText in Welsh" in {
        document.select(daySelector).text() shouldBe dayTextWelsh
        document.select(dayInputSelector).attr("type") shouldBe "text"
      }

      s"has a text input field for the $monthText in Welsh" in {
        document.select(monthSelector).text() shouldBe monthTextWelsh
        document.select(monthInputSelector).attr("type") shouldBe "text"
      }

      s"has a text input field for the $yearText in Welsh" in {
        document.select(yearSelector).text() shouldBe yearTextWelsh
        document.select(yearInputSelector).attr("type") shouldBe "text"
      }

      s"has a text input field for the $ninoText in Welsh" in {
        document.select(ninoSelector).text() shouldBe ninoTextWelsh
        document.select(itsOnYourSelector).text() shouldBe itsOnYourTextWelsh
        document.select(ninoInputSelector).attr("type") shouldBe "text"
      }

      s"has text on the screen of $noNinoText in Welsh" in {
        document.select(noNinoSelector).text() shouldBe noNinoTextWelsh
      }

      s"has text on the screen of $expandedNoNinoText in Welsh" in {
        document.select(noNinoExpandedSelector).text() shouldBe expandedNoNinoTextWelsh
      }

      s"has a $saveAndContinueText button in Welsh" in {
        document.select(saveAndContinueSelector).text() shouldBe saveAndContinueTextWelsh
      }
    }
  }

  "RegistrationController submit individual method for a new individual" should {
    "Return a bad request with the relevant errors when each address line is greater than 30 characters in english" which {

      val body: JsObject = Json.obj(
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

      lazy val res = postContactDetailsPage(language = English, postBody = body)

      lazy val document: Document = Jsoup.parse(res.body)

      "has a status of 400" in {
        res.status shouldBe BAD_REQUEST
      }

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

    "Return a bad request with the relevant errors when each address line is greater than 30 characters in welsh" which {

      val body: JsObject = Json.obj(
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

      lazy val res = postContactDetailsPage(language = Welsh, postBody = body)

      lazy val document: Document = Jsoup.parse(res.body)

      "has a status of 400" in {
        res.status shouldBe BAD_REQUEST
      }

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

  override lazy val extraConfig: Map[String, Any] = Map(
    "feature-switch.ivUplift.enabled" -> "false"
  )

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

  private def postContactDetailsPage(language: Language, postBody: JsObject): WSResponse = {

    stubsSetup

    await(
      ws.url(s"http://localhost:$port/business-rates-property-linking/complete-contact-details")
        .withCookies(languageCookie(language), getSessionCookie(testSessionId))
        .withHttpHeaders(HeaderNames.COOKIE -> testSessionId, "Csrf-Token" -> "nocheck")
        .withFollowRedirects(follow = false)
        .post(postBody)
    )
  }

  private def stubsSetup: StubMapping = {

    stubFor {
      get("/business-rates-authorisation/authenticate")
        .willReturn {
          aResponse.withStatus(OK).withBody(Json.toJson(testAccounts).toString())
        }
    }

    val authResponseBody =
      """{ "affinityGroup": "Individual", "credentialRole": "User", "optionalName": {"name": "Test First Name", "lastName": "Test Last Name"}, "email": "test@test.com", "groupIdentifier": "1", "externalId": "3", "confidenceLevel": 200}"""

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

}
