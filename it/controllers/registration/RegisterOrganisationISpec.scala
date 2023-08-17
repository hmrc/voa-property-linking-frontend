package controllers.registration

import base.{HtmlComponentHelpers, ISpecBase}
import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.http.Status.OK
import play.api.libs.json.Json
import play.api.test.Helpers._
import utils.ListYearsHelpers

class RegisterOrganisationISpec extends ISpecBase with HtmlComponentHelpers with ListYearsHelpers {

  val titleText = "Complete your contact details - Valuation Office Agency - GOV.UK"
  val headingText = "Complete your contact details"
  val weUseYourText = "We use your contact details to send you correspondence related to the service and your account."
  val registeringAsAgentText = "I’m registering as an agent"
  val registeringAsAgentExpandedText = "You’ll need to provide information about your own business here, not your client’s business."
  val firstNameText = "First name"
  val lastNameText = "Last name"
  val businessNameText = "Business name"
  val enterAddressManuallyText = "Enter address manually"
  val findAddressByPostcodeText = "Find address by postcode"
  val findAddressButtonText = "Find address"
  val addressText = "Address"
  val postcodeText = "Postcode"
  val moreThanOneAddressText = "There’s more than one address for my business"
  val moreThanOneAddressExpandedText = "You should enter the address that you want any business correspondence to be sent to."
  val phoneNumText = "Business telephone number"
  val emailAddressText = "Business email address"
  val confirmEmailAddressText = "Confirm business email address"
  val doYouWantToRegisterAsAgentText = "Do you want to register as an agent?"
  val yesText = "Yes"
  val noText = "No"
  val representAnotherBusinessText = "If you want to represent another business (either as a professional surveyor, an accountant, friend or relative), we will give you a unique agent code. You will need to give this code to that business so they can appoint you to speak for them."
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

  val titleTextWelsh = "Cwblhewch eich manylion cyswllt - Valuation Office Agency - GOV.UK"
  val headingTextWelsh = "Cwblhewch eich manylion cyswllt"
  val weUseYourTextWelsh = "Rydym yn defnyddio eich manylion cyswllt i anfon gohebiaeth atoch sy’n ymwneud â’r gwasanaeth a’ch cyfrif."
  val registeringAsAgentTextWelsh = "Rwy’n cofrestru fel asiant"
  val registeringAsAgentExpandedTextWelsh = "Bydd angen i chi ddarparu gwybodaeth am eich busnes eich hun yma, nid busnes eich cleient."
  val firstNameTextWelsh = "Enw cyntaf"
  val lastNameTextWelsh = "Cyfenw"
  val businessNameTextWelsh = "Enw busnes"
  val enterAddressManuallyTextWelsh = "Nodwchy cyfeiriad â llaw"
  val findAddressByPostcodeTextWelsh = "Canfod cyfeiriad yn ôl cod post"
  val findAddressButtonTextWelsh = "Dod o hyd i gyfeiriad"
  val addressTextWelsh = "Cyfeiriad"
  val postcodeTextWelsh = "Cod post"
  val moreThanOneAddressTextWelsh = "Mae mwy nag un cyfeiriad ar gyfer fy musnes"
  val moreThanOneAddressExpandedTextWelsh = "Dylech nodi’r cyfeiriad rydych yn dymuno i unrhyw ohebiaeth busnes gael ei danfon ato"
  val phoneNumTextWelsh = "Rhif ffôn busnes"
  val emailAddressTextWelsh = "Cyfeiriad e-bost busnes"
  val confirmEmailAddressTextWelsh = "Cadarnhau cyfeiriad e-bost busnes"
  val doYouWantToRegisterAsAgentTextWelsh = "Ydych chi eisiau cofrestru fel asiant?"
  val yesTextWelsh = "Ie"
  val noTextWelsh = "Na"
  val representAnotherBusinessTextWelsh = "Os ydych chi eisiau cynrychioli busnes arall (naill ai fel syrfëwr proffesiynol, cyfrifydd, ffrind neu berthynas), byddwn yn rhoi cod asiant unigryw i chi. Bydd angen i chi roi’r côd hwn i’r busnes hwnnw er mwyn iddynt allu eich penodi i siarad ar eu rhan."
  val dateOfBirthTextWelsh = "Dyddiad geni"
  val forExampleTextWelsh = "Er enghraifft, 28 4 2017"
  val dayTextWelsh = "Diwrnod"
  val monthTextWelsh = "Mis"
  val yearTextWelsh = "Blwyddyn"
  val ninoTextWelsh = "Rhif Yswiriant Gwladol"
  val itsOnYourTextWelsh = "Mae ar eich cerdyn Yswiriant Gwladol, llythyr budd-dal, slip cyflog neu P60. Er enghraifft, QQ123456C."
  val noNinoTextWelsh = "Does gen i ddim rhif Yswiriant Gwladol"
  val expandedNoNinoTextWelsh = "Os nad oes gennych y manylion hyn bydd angen i chi gysylltu ag Asiantaeth y Swyddfa Brisio (VOA)."
  val saveAndContinueTextWelsh = "Arbed a pharhau"

  val headingSelector = "#main-content > div > div > h1"
  val weUseYourSelector = "#contactDetailsUse"
  val registeringAsAgentSelector = "#main-content > div > div > details:nth-child(4) > summary > span"
  val registeringAsAgentExpandedSelector = "#main-content > div > div > details:nth-child(4) > div"
  val firstNameTextSelector = "#main-content > div > div > form > div:nth-child(2) > label"
  val firstNameInputSelector = "input[id='firstName']"
  val lastNameTextSelector = "#main-content > div > div > form > div:nth-child(3) > label"
  val lastNameInputSelector = "input[id='lastName']"
  val businessNameSelector = "#main-content > div > div > form > div:nth-child(4) > label"
  val businessNameInputSelector = "input[id='companyName']"
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
  val moreThanOneAddressSelector = "#main-content > div > div > form > details:nth-child(6) > summary > span"
  val moreThanOneAddressExpandedSelector = "#main-content > div > div > form > details:nth-child(6) > div"
  val phoneNumSelector = "#main-content > div > div > form > div:nth-child(7) > label"
  val phoneNumInputSelector = "input[id='phone']"
  val emailSelector = "#main-content > div > div > form > div:nth-child(8) > label"
  val emailInputSelector = "input[id='email']"
  val confirmEmailSelector = "#main-content > div > div > form > div:nth-child(9) > label"
  val confirmEmailInputSelector = "input[id='confirmedBusinessEmail']"
  val doYouWantToRegisterAsAgentSelector = "#main-content > div > div > form > div:nth-child(10) > fieldset > legend"
  val yesSelector = "#main-content > div > div > form > div:nth-child(10) > fieldset > div > div:nth-child(1) > label"
  val noSelector = "#main-content > div > div > form > div:nth-child(10) > fieldset > div > div:nth-child(2) > label"
  val yesInputSelector = "#isAgent"
  val noInputSelector = "#isAgent-2"
  val representAnotherBusinessSelector = "#main-content > div > div > form > div.govuk-inset-text"
  val dateOfBirthSelector = "#dob > div > fieldset > legend > h1"
  val forExampleSelector = "#dob_dates-hint"
  val daySelector = "#dob_dates > div:nth-child(1) > div > label"
  val dayInputSelector = "input[id='dob-day']"
  val monthSelector = "#dob_dates > div:nth-child(2) > div > label"
  val monthInputSelector = "input[id='dob-month']"
  val yearSelector = "#dob_dates > div:nth-child(3) > div > label"
  val yearInputSelector = "input[id='dob-year']"
  val ninoSelector = "#main-content > div > div > form > div:nth-child(13) > label"
  val ninoInputSelector = "input[id='nino']"
  val itsOnYourSelector = "#nino-hint"
  val noNinoSelector = "#main-content > div > div > form > details:nth-child(14) > summary > span"
  val noNinoExpandedSelector = "#main-content > div > div > form > details:nth-child(14) > div"
  val saveAndContinueSelector = "button[id='save-and-continue']"

  "RegistrationController show method for a new organisation" should {
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

      s"has text on the screen of $registeringAsAgentText" in {
        document.select(registeringAsAgentSelector).text() shouldBe registeringAsAgentText
        document.select(registeringAsAgentExpandedSelector).text() shouldBe registeringAsAgentExpandedText
      }

      s"has a text input field for the $firstNameText" in {
        document.select(firstNameTextSelector).text() shouldBe firstNameText
        document.select(firstNameInputSelector).attr("type") shouldBe "text"
      }

      s"has a text input field for the $lastNameText" in {
        document.select(lastNameTextSelector).text() shouldBe lastNameText
        document.select(lastNameInputSelector).attr("type") shouldBe "text"
      }

      s"has a text input field for the $businessNameText" in {
        document.select(businessNameSelector).text() shouldBe businessNameText
        document.select(businessNameInputSelector).attr("type") shouldBe "text"
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

      s"has text on the screen of $moreThanOneAddressText" in {
        document.select(moreThanOneAddressSelector).text() shouldBe moreThanOneAddressText
        document.select(moreThanOneAddressExpandedSelector).text() shouldBe moreThanOneAddressExpandedText
      }

      s"has a text input field for the $phoneNumText" in {
        document.select(phoneNumSelector).text() shouldBe phoneNumText
        document.select(phoneNumInputSelector).attr("type") shouldBe "text"
      }

      s"has a text input field for the $emailAddressText" in {
        document.select(emailSelector).text() shouldBe emailAddressText
        document.select(emailInputSelector).attr("type") shouldBe "text"
      }

      s"has a text input field for the $confirmEmailAddressText" in {
        document.select(confirmEmailSelector).text() shouldBe confirmEmailAddressText
        document.select(confirmEmailInputSelector).attr("type") shouldBe "text"
      }

      s"has radio input fields for the $doYouWantToRegisterAsAgentText" in {
        document.select(doYouWantToRegisterAsAgentSelector).text() shouldBe doYouWantToRegisterAsAgentText
        document.select(yesSelector).text() shouldBe yesText
        document.select(yesInputSelector).attr("type") shouldBe "radio"
        document.select(noSelector).text() shouldBe noText
        document.select(noInputSelector).attr("type") shouldBe "radio"
      }

      s"has text on the screen of $representAnotherBusinessText" in {
        document.select(representAnotherBusinessSelector).text() shouldBe representAnotherBusinessText
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

      s"has text on the screen of $registeringAsAgentText in Welsh" in {
        document.select(registeringAsAgentSelector).text() shouldBe registeringAsAgentTextWelsh
        document.select(registeringAsAgentExpandedSelector).text() shouldBe registeringAsAgentExpandedTextWelsh
      }

      s"has a text input field for the $firstNameText in Welsh" in {
        document.select(firstNameTextSelector).text() shouldBe firstNameTextWelsh
        document.select(firstNameInputSelector).attr("type") shouldBe "text"
      }

      s"has a text input field for the $lastNameText in Welsh" in {
        document.select(lastNameTextSelector).text() shouldBe lastNameTextWelsh
        document.select(lastNameInputSelector).attr("type") shouldBe "text"
      }

      s"has a text input field for the $businessNameText in Welsh" in {
        document.select(businessNameSelector).text() shouldBe businessNameTextWelsh
        document.select(businessNameInputSelector).attr("type") shouldBe "text"
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

      s"has text on the screen of $moreThanOneAddressText in Welsh" in {
        document.select(moreThanOneAddressSelector).text() shouldBe moreThanOneAddressTextWelsh
        document.select(moreThanOneAddressExpandedSelector).text() shouldBe moreThanOneAddressExpandedTextWelsh
      }

      s"has a text input field for the $phoneNumText in Welsh" in {
        document.select(phoneNumSelector).text() shouldBe phoneNumTextWelsh
        document.select(phoneNumInputSelector).attr("type") shouldBe "text"
      }

      s"has a text input field for the $emailAddressText in Welsh" in {
        document.select(emailSelector).text() shouldBe emailAddressTextWelsh
        document.select(emailInputSelector).attr("type") shouldBe "text"
      }

      s"has a text input field for the $confirmEmailAddressText in Welsh" in {
        document.select(confirmEmailSelector).text() shouldBe confirmEmailAddressTextWelsh
        document.select(confirmEmailInputSelector).attr("type") shouldBe "text"
      }

      s"has radio input fields for the $doYouWantToRegisterAsAgentText in Welsh" in {
        document.select(doYouWantToRegisterAsAgentSelector).text() shouldBe doYouWantToRegisterAsAgentTextWelsh
        document.select(yesSelector).text() shouldBe yesTextWelsh
        document.select(yesInputSelector).attr("type") shouldBe "radio"
        document.select(noSelector).text() shouldBe noTextWelsh
        document.select(noInputSelector).attr("type") shouldBe "radio"
      }

      s"has text on the screen of $representAnotherBusinessText in Welsh" in {
        document.select(representAnotherBusinessSelector).text() shouldBe representAnotherBusinessTextWelsh
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

    val authResponseBody = """{ "affinityGroup": "Organisation", "credentialRole": "User", "optionalName": {"name": "Test First Name", "lastName": "Test Last Name"}, "email": "test@test.com", "groupIdentifier": "1", "externalId": "3", "confidenceLevel": 200}"""

    stubFor {
      post("/auth/authorise")
        .willReturn {
          aResponse.withStatus(OK).withBody(authResponseBody)
        }
    }

    stubFor(
      get(s"/property-linking/groups?groupId=3")
        .willReturn(notFound)
    )

  }

}
