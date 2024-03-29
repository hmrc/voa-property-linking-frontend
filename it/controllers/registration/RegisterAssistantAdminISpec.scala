package controllers.registration

import base.{HtmlComponentHelpers, ISpecBase}
import com.github.tomakehurst.wiremock.client.WireMock.{aResponse, _}
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import models.{Address, GroupAccount}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.http.Status.OK
import play.api.libs.json.Json
import play.api.test.Helpers._
import utils.ListYearsHelpers

class RegisterAssistantAdminISpec extends ISpecBase with HtmlComponentHelpers with ListYearsHelpers {

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
  val dateOfBirthText = "Date of birth"
  val forExampleText = "For example, 28 4 2017"
  val dayText = "Day"
  val monthText = "Month"
  val yearText = "Year"
  val ninoText = "National Insurance number"
  val itsOnYourText = "It’s on your National Insurance card, benefit letter, payslip or P60. For example, QQ123456C."
  val confirmText = "Confirm"

  val titleTextWelsh = "Cwblhewch eich manylion cyswllt - Valuation Office Agency - GOV.UK"
  val headingTextWelsh = "Cwblhewch eich manylion cyswllt"
  val weUseYourTextWelsh = "Rydym yn defnyddio’ch manylion cyswllt i anfon gohebiaeth atoch sy’n ymwneud â’ch cyfrif a’r gwasanaeth."
  val youHaveTextWelsh = "Rydych wedi cael eich ychwanegu fel defnyddiwr i’ch sefydliad, cadarnhewch eich manylion isod"
  val yourOrgTextWelsh = "Manylion eich sefydliad"
  val orgNameTextWelsh = "Enw sefydliad"
  val postalAddressTextWelsh = "Cyfeiriad post"
  val phoneNumTextWelsh = "Rhif ffôn"
  val emailTextWelsh = "Cyfeiriad gohebiaeth e-bost"
  val yourDetailsTextWelsh = "Eich manylion"
  val firstNameTextWelsh = "Enw cyntaf"
  val lastNameTextWelsh = "Cyfenw"
  val dateOfBirthTextWelsh = "Dyddiad geni"
  val forExampleTextWelsh = "Er enghraifft, 28 4 2017"
  val dayTextWelsh = "Diwrnod"
  val monthTextWelsh = "Mis"
  val yearTextWelsh = "Blwyddyn"
  val ninoTextWelsh = "Rhif Yswiriant Gwladol"
  val itsOnYourTextWelsh = "Mae ar eich cerdyn Yswiriant Gwladol, llythyr budd-dal, slip cyflog neu P60. Er enghraifft, QQ123456C."
  val confirmTextWelsh = "Cadarnhau"

  val headingSelector = "#main-content > div > div > h1"
  val weUseYourSelector = "#main-content > div > div > p.govuk-body"
  val youHaveSelector = "#main-content > div > div > p.govuk-hint"
  val yourOrgSelector = "#main-content > div > div > h2"
  val orgNameSelector = "#main-content > div > div > form > table > tbody > tr:nth-child(1) > th"
  val orgNameValueSelector = "#main-content > div > div > form > table > tbody > tr:nth-child(1) > td"
  val postalAddressSelector = "#main-content > div > div > form > table > tbody > tr:nth-child(2) > th"
  val postalAddressValueSelector = "#main-content > div > div > form > table > tbody > tr:nth-child(2) > td"
  val phoneNumSelector = "#main-content > div > div > form > table > tbody > tr:nth-child(3) > th"
  val phoneNumValueSelector = "#main-content > div > div > form > table > tbody > tr:nth-child(3) > td"
  val emailSelector = "#main-content > div > div > form > table > tbody > tr:nth-child(4) > th"
  val emailValueSelector = "#main-content > div > div > form > table > tbody > tr:nth-child(4) > td"
  val yourDetailsSelector = "#main-content > div > div > form > h2"
  val firstNameTextSelector = "#main-content > div > div > form > div:nth-child(4) > label"
  val firstNameInputSelector = "input[id='firstName']"
  val lastNameTextSelector = "#main-content > div > div > form > div:nth-child(5) > label"
  val lastNameInputSelector = "input[id='lastName']"
  val dateOfBirthSelector = "#dob > div > fieldset > legend > h1"
  val forExampleSelector = "#dob_dates-hint"
  val daySelector = "#dob_dates > div:nth-child(1) > div > label"
  val dayInputSelector = "input[id='dob-day']"
  val monthSelector = "#dob_dates > div:nth-child(2) > div > label"
  val monthInputSelector = "input[id='dob-month']"
  val yearSelector = "#dob_dates > div:nth-child(3) > div > label"
  val yearInputSelector = "input[id='dob-year']"
  val ninoSelector = "#main-content > div > div > form > div:nth-child(7) > label"
  val ninoInputSelector = "input[id='nino']"
  val itsOnYourSelector = "#nino-hint"
  val confirmSelector = "button.govuk-button"

  "RegistrationController show method for a new assistant to the organisation" should {
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

      s"has a $confirmText button" in {
        document.select(confirmSelector).text() shouldBe confirmText
      }
    }

    "Show a Welsh registration contact details screen with the correct text" which {

      lazy val document: Document = getSuccessPage(Welsh)

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

      s"has text on the screen of $dateOfBirthText in welsh" in {
        document.select(dateOfBirthSelector).text() shouldBe dateOfBirthTextWelsh
      }

      s"has text on the screen of $forExampleText in welsh" in {
        document.select(forExampleSelector).text() shouldBe forExampleTextWelsh
      }

      s"has a text input field for the $dayText in welsh" in {
        document.select(daySelector).text() shouldBe dayTextWelsh
        document.select(dayInputSelector).attr("type") shouldBe "text"
      }

      s"has a text input field for the $monthText in welsh" in {
        document.select(monthSelector).text() shouldBe monthTextWelsh
        document.select(monthInputSelector).attr("type") shouldBe "text"
      }

      s"has a text input field for the $yearText in welsh" in {
        document.select(yearSelector).text() shouldBe yearTextWelsh
        document.select(yearInputSelector).attr("type") shouldBe "text"
      }

      s"has a text input field for the $ninoText in welsh" in {
        document.select(ninoSelector).text() shouldBe ninoTextWelsh
        document.select(itsOnYourSelector).text() shouldBe itsOnYourTextWelsh
        document.select(ninoInputSelector).attr("type") shouldBe "text"
      }

      s"has a $confirmText button in welsh" in {
        document.select(confirmSelector).text() shouldBe confirmTextWelsh
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

    val testGroup = GroupAccount(id = 1L, groupId = "1L", companyName = "Test name", addressId = 1L, email = "test@email.com", phone = "1234567890", isAgent = false, agentCode = Some(1L))

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

    val testAddress = Address(addressUnitId = Some(1L), line1 = "Test 1", line2 = "Test 2", line3 = "Test 3", line4 = "Test 4", postcode = "TF4 9ER")

    stubFor(
      get("/property-linking/address/1")
        .willReturn {
          aResponse.withStatus(OK).withBody(Json.toJson(testAddress).toString())
        }
    )

  }

}
