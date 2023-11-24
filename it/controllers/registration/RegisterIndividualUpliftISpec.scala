package controllers.registration

import base.{HtmlComponentHelpers, ISpecBase}
import com.github.tomakehurst.wiremock.client.WireMock.{aResponse, _}
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.http.HeaderNames
import play.api.http.Status.OK
import play.api.libs.json.Json
import play.api.test.Helpers._
import utils.ListYearsHelpers


class RegisterIndividualUpliftISpec extends ISpecBase with HtmlComponentHelpers with ListYearsHelpers {

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

  val titleTextWelsh = "Cwblhewch eich manylion cyswllt - Valuation Office Agency - GOV.UK"
  val headingTextWelsh = "Cwblhewch eich manylion cyswllt"
  val weUseYourTextWelsh = "Rydym yn defnyddio’ch manylion cyswllt i anfon gohebiaeth atoch sy’n ymwneud â’ch cyfrif a’r gwasanaeth."
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

    val authResponseBody = """{ "affinityGroup": "Individual", "credentialRole": "User", "optionalName": {"name": "Test First Name", "lastName": "Test Last Name"}, "email": "test@test.com", "groupIdentifier": "1", "externalId": "3", "confidenceLevel": 200}"""

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
