package controllers.propertyLinking

import base.{HtmlComponentHelpers, ISpecBase}
import binders.propertylinks.ClaimPropertyReturnToPage
import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import models.{ClientDetails, LinkingSession, Owner, PropertyOwnership, PropertyRelationship}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.http.HeaderNames
import play.api.http.Status.OK
import play.api.libs.json.Json
import play.api.libs.ws.WSResponse
import play.api.test.Helpers._
import repositories.PropertyLinkingSessionRepository
import utils.ListYearsHelpers

import java.time.LocalDate

class ClaimPropertyOwnershipISpec extends ISpecBase with HtmlComponentHelpers with ListYearsHelpers {

  lazy val mockPropertyLinkingSessionRepository: PropertyLinkingSessionRepository = app.injector.instanceOf[PropertyLinkingSessionRepository]
  val localCouncilReferenceValue = "2050466366770"
  val propertyAddress = "Test Address, Test Lane, T35 T3R"
  val dayOfOwnership: Int = 1
  val monthOfOwnership: Int = 1
  val yearOfOwnership: Int = 2020

  val titleText = "When you became the owner or occupier of the property - Valuation Office Agency - GOV.UK"
  val titleTextAgent = "When your client became the owner or occupier of the property - Valuation Office Agency - GOV.UK"
  val backLinkText = "Back"
  val localCouncilText = s"Local council reference: $localCouncilReferenceValue"
  val propertyDetailsText = s"Property: $propertyAddress"
  val captionText = "Add a property"
  val headingText = "When you became the owner or occupier of the property"
  val headingTextAgent = "When your client became the owner or occupier of the property"
  val iHaveOwnedText = "I have owned or occupied the property on more than one occasion"
  val iHaveOwnedTextAgent = "My client has owned or occupied the property on more than one occasion"
  val youCanAddText = "You can add the property for each period you were connected to it, but you need to do it one period at a time. However, if you want to talk to the Valuation Office Agency about the valuation, you will need to select the right valuation period."
  val youCanAddTextAgent = "You can add the property for each period your client was connected to it, but you need to do it one period at a time. However, if you want to talk to the Valuation Office Agency about the valuation, you will need to select the right valuation period."
  val onWhatDateText = "On what date did you become the owner or occupier?"
  val onWhatDateTextAgent = "On what date did your client become the owner or occupier?"
  val forExampleText = "For example, 1 4 2017"
  val dayText = "Day"
  val monthText = "Month"
  val yearText = "Year"
  val continueText = "Continue"
  val errorText = "Error:"

  val titleTextWelsh = "Pryd y daethoch yn berchennog neu’n feddiannydd yr eiddo - Valuation Office Agency - GOV.UK"
  val titleTextAgentWelsh = "Pryd ddaeth eich cleient yn berchennog neu’n feddiannydd yr eiddo - Valuation Office Agency - GOV.UK"
  val backLinkTextWelsh = "Yn ôl"
  val localCouncilTextWelsh = s"Cyfeirnod yr awdurdod lleol: $localCouncilReferenceValue"
  val propertyDetailsTextWelsh = s"Eiddo: $propertyAddress"
  val captionTextWelsh = "Ychwanegu eiddo"
  val headingTextWelsh = "Pryd y daethoch yn berchennog neu’n feddiannydd yr eiddo"
  val headingTextAgentWelsh = "Pryd ddaeth eich cleient yn berchennog neu’n feddiannydd yr eiddo"
  val iHaveOwnedTextWelsh = "Rwyf wedi bod yn berchen ar yr eiddo neu wedi ei feddiannu ar fwy nag un achlysur"
  val iHaveOwnedTextAgentWelsh = "Mae fy nghleient wedi bod yn berchen ar yr eiddo neu wedi meddiannu’r eiddo ar fwy nag un achlysur"
  val youCanAddTextWelsh = "Gallwch ychwanegu’r eiddo ar gyfer pob cyfnod amser roeddech yn gysylltiedig ag ef, ond mae’n rhaid i chi wneud hyn un cyfnod amser ar y tro. Serch hyn, os ydych eisiau siarad gydag Asiantaeth y Swyddfa Brisio am eich prisiad, bydd rhaid i chi ddewis y cyfnod prisio cywir."
  val youCanAddTextAgentWelsh = "Gallwch ychwanegu’r eiddo ar gyfer pob cyfnod amser roedd eich cleient yn gysylltiedig ag ef, ond mae’n rhaid i chi wneud hyn un cyfnod amser ar y tro. Serch hyn, os ydych eisiau siarad gydag Asiantaeth y Swyddfa Brisio am eich prisiad, bydd rhaid i chi ddewis y cyfnod prisio cywir."
  val onWhatDateTextWelsh = "Ar ba ddyddiad ddaethoch chi’n berchennog neu’r meddiannydd?"
  val onWhatDateTextAgentWelsh = "Ar ba ddyddiad y daeth eich cleient yn berchennog neu’n feddiannydd?"
  val forExampleTextWelsh = "Er enghraifft, 1 4 2017"
  val dayTextWelsh = "Diwrnod"
  val monthTextWelsh = "Mis"
  val yearTextWelsh = "Blwyddyn"
  val continueTextWelsh = "Parhau"
  val errorTextWelsh = "Gwall:"

  val backLinkSelector = "#back-link"
  val localCouncilSelector = "#local-authority-reference"
  val propertyDetailsSelector = "#address"
  val captionSelector = "span.govuk-caption-l"
  val headingSelector = "#main-content > div > div > h1"
  val iHaveOwnedSelector = "#main-content > div > div > details:nth-child(4) > summary > span"
  val youCanAddSelector = "#main-content > div > div > details:nth-child(4) > div"
  val onWhatDateSelector = "#interestedStartDate > div > fieldset > legend > h1"
  val forExampleSelector = "#interestedStartDate_dates-hint"
  val daySelector = "#interestedStartDate_dates > div:nth-child(1) > div > label"
  val dayValueSelector = "#interestedStartDate-day"
  val monthSelector = "#interestedStartDate_dates > div:nth-child(2) > div > label"
  val monthValueSelector = "#interestedStartDate-month"
  val yearSelector = "#interestedStartDate_dates > div:nth-child(3) > div > label"
  val yearValueSelector = "#interestedStartDate-year"
  val continueSelector = "#continue"

  val backLinkHref = "/business-rates-property-linking/my-organisation/claim/property-links"
  val backLinkFromCyaHref = "/business-rates-property-linking/my-organisation/claim/property-links/summary"

  "ClaimPropertyOwnershipController showOwnership method" should {
    "Show an English when you became the owner screen for a user with the correct text when the language is set to English" which {

      lazy val document: Document = getClaimOwnershipPage(language = English, userIsAgent = false, fromCya = false)

      s"has a title of $titleText" in {
        document.title() shouldBe titleText
      }

      "has a back link which takes you to the claim property links page" in {
        document.select(backLinkSelector).text() shouldBe backLinkText
        document.select(backLinkSelector).attr("href") shouldBe backLinkHref
      }

      s"has text on the screen of the $localCouncilText" in {
        document.select(localCouncilSelector).text() shouldBe localCouncilText
      }

      s"has text on the screen of the $propertyDetailsText" in {
        document.select(propertyDetailsSelector).text() shouldBe propertyDetailsText
      }

      s"has a header of $headingText with a caption above of '$captionText'" in {
        document.select(headingSelector).text shouldBe headingText
        document.select(captionSelector).text shouldBe captionText
      }

      s"has summary text on the screen of $iHaveOwnedText" in {
        document.select(iHaveOwnedSelector).text() shouldBe iHaveOwnedText
      }

      s"has inset text on the screen of $youCanAddText" in {
        document.select(youCanAddSelector).text() shouldBe youCanAddText
      }

      s"has a subheading on the screen of $onWhatDateText" in {
        document.select(onWhatDateSelector).text() shouldBe onWhatDateText
      }

      s"has text on the screen of $forExampleText" in {
        document.select(forExampleSelector).text() shouldBe forExampleText
      }

      s"has an empty input field for the $dayText" in {
        document.select(daySelector).text() shouldBe dayText
        document.select(dayValueSelector).attr("value") shouldBe ""
      }

      s"has an empty input field for the $monthText" in {
        document.select(monthSelector).text() shouldBe monthText
        document.select(monthValueSelector).attr("value") shouldBe ""
      }

      s"has an empty input field for the $yearText" in {
        document.select(yearSelector).text() shouldBe yearText
        document.select(yearValueSelector).attr("value") shouldBe ""
      }

      s"has a $continueText button on the screen" in {
        document.select(continueSelector).text() shouldBe continueText
      }

    }

    "Show an English when you became the owner screen for an agent with the correct text when the language is set to English" which {

      lazy val document: Document = getClaimOwnershipPage(language = English, userIsAgent = true, fromCya = false)

      s"has a title of $titleTextAgent" in {
        document.title() shouldBe titleTextAgent
      }

      "has a back link which takes you to the claim property links page" in {
        document.select(backLinkSelector).text() shouldBe backLinkText
        document.select(backLinkSelector).attr("href") shouldBe backLinkHref
      }

      s"has text on the screen of the $localCouncilText" in {
        document.select(localCouncilSelector).text() shouldBe localCouncilText
      }

      s"has text on the screen of the $propertyDetailsText" in {
        document.select(propertyDetailsSelector).text() shouldBe propertyDetailsText
      }

      s"has a header of $headingTextAgent with a caption above of '$captionText'" in {
        document.select(headingSelector).text shouldBe headingTextAgent
        document.select(captionSelector).text shouldBe captionText
      }

      s"has summary text on the screen of $iHaveOwnedTextAgent" in {
        document.select(iHaveOwnedSelector).text() shouldBe iHaveOwnedTextAgent
      }

      s"has inset text on the screen of $youCanAddTextAgent" in {
        document.select(youCanAddSelector).text() shouldBe youCanAddTextAgent
      }

      s"has a subheading on the screen of $onWhatDateTextAgent" in {
        document.select(onWhatDateSelector).text() shouldBe onWhatDateTextAgent
      }

      s"has text on the screen of $forExampleText" in {
        document.select(forExampleSelector).text() shouldBe forExampleText
      }

      s"has an empty input field for the $dayText" in {
        document.select(daySelector).text() shouldBe dayText
        document.select(dayValueSelector).attr("value") shouldBe ""
      }

      s"has an empty input field for the $monthText" in {
        document.select(monthSelector).text() shouldBe monthText
        document.select(monthValueSelector).attr("value") shouldBe ""
      }

      s"has an empty input field for the $yearText" in {
        document.select(yearSelector).text() shouldBe yearText
        document.select(yearValueSelector).attr("value") shouldBe ""
      }

      s"has a $continueText button on the screen" in {
        document.select(continueSelector).text() shouldBe continueText
      }

    }

    "Show a Welsh when you became the owner screen for a user with the correct text when the language is set to Welsh" which {

      lazy val document: Document = getClaimOwnershipPage(language = Welsh, userIsAgent = false, fromCya = false)

      s"has a title of $titleText in welsh" in {
        document.title() shouldBe titleTextWelsh
      }

      "has a back link which takes you to the claim property links page in welsh" in {
        document.select(backLinkSelector).text() shouldBe backLinkTextWelsh
        document.select(backLinkSelector).attr("href") shouldBe backLinkHref
      }

      s"has text on the screen of the $localCouncilText in welsh" in {
        document.select(localCouncilSelector).text() shouldBe localCouncilTextWelsh
      }

      s"has text on the screen of the $propertyDetailsText in welsh" in {
        document.select(propertyDetailsSelector).text() shouldBe propertyDetailsTextWelsh
      }

      s"has a header of $headingText with a caption above of '$captionText' in welsh" in {
        document.select(headingSelector).text shouldBe headingTextWelsh
        document.select(captionSelector).text shouldBe captionTextWelsh
      }

      s"has summary text on the screen of $iHaveOwnedText in welsh" in {
        document.select(iHaveOwnedSelector).text() shouldBe iHaveOwnedTextWelsh
      }

      s"has inset text on the screen of $youCanAddText in welsh" in {
        document.select(youCanAddSelector).text() shouldBe youCanAddTextWelsh
      }

      s"has a subheading on the screen of $onWhatDateText in welsh" in {
        document.select(onWhatDateSelector).text() shouldBe onWhatDateTextWelsh
      }

      s"has text on the screen of $forExampleText in welsh" in {
        document.select(forExampleSelector).text() shouldBe forExampleTextWelsh
      }

      s"has an empty input field for the $dayText in welsh" in {
        document.select(daySelector).text() shouldBe dayTextWelsh
        document.select(dayValueSelector).attr("value") shouldBe ""
      }

      s"has an empty input field for the $monthText in welsh" in {
        document.select(monthSelector).text() shouldBe monthTextWelsh
        document.select(monthValueSelector).attr("value") shouldBe ""
      }

      s"has an empty input field for the $yearText in welsh" in {
        document.select(yearSelector).text() shouldBe yearTextWelsh
        document.select(yearValueSelector).attr("value") shouldBe ""
      }

      s"has a $continueText button on the screen in welsh" in {
        document.select(continueSelector).text() shouldBe continueTextWelsh
      }

    }

    "Show a Welsh when you became the owner screen for an agent with the correct text when the language is set to Welsh" which {

      lazy val document: Document = getClaimOwnershipPage(language = Welsh, userIsAgent = true, fromCya = false)

      s"has a title of $titleTextAgent in welsh" in {
        document.title() shouldBe titleTextAgentWelsh
      }

      "has a back link which takes you to the claim property links page in welsh" in {
        document.select(backLinkSelector).text() shouldBe backLinkTextWelsh
        document.select(backLinkSelector).attr("href") shouldBe backLinkHref
      }

      s"has text on the screen of the $localCouncilText in welsh" in {
        document.select(localCouncilSelector).text() shouldBe localCouncilTextWelsh
      }

      s"has text on the screen of the $propertyDetailsText in welsh" in {
        document.select(propertyDetailsSelector).text() shouldBe propertyDetailsTextWelsh
      }

      s"has a header of $headingTextAgent with a caption above of '$captionText' in welsh" in {
        document.select(headingSelector).text shouldBe headingTextAgentWelsh
        document.select(captionSelector).text shouldBe captionTextWelsh
      }

      s"has summary text on the screen of $iHaveOwnedTextAgent in welsh" in {
        document.select(iHaveOwnedSelector).text() shouldBe iHaveOwnedTextAgentWelsh
      }

      s"has inset text on the screen of $youCanAddTextAgent in welsh" in {
        document.select(youCanAddSelector).text() shouldBe youCanAddTextAgentWelsh
      }

      s"has a subheading on the screen of $onWhatDateTextAgent in welsh" in {
        document.select(onWhatDateSelector).text() shouldBe onWhatDateTextAgentWelsh
      }

      s"has text on the screen of $forExampleText in welsh" in {
        document.select(forExampleSelector).text() shouldBe forExampleTextWelsh
      }

      s"has an empty input field for the $dayText in welsh" in {
        document.select(daySelector).text() shouldBe dayTextWelsh
        document.select(dayValueSelector).attr("value") shouldBe ""
      }

      s"has an empty input field for the $monthText in welsh" in {
        document.select(monthSelector).text() shouldBe monthTextWelsh
        document.select(monthValueSelector).attr("value") shouldBe ""
      }

      s"has an empty input field for the $yearText in welsh" in {
        document.select(yearSelector).text() shouldBe yearTextWelsh
        document.select(yearValueSelector).attr("value") shouldBe ""
      }

      s"has a $continueText button on the screen in welsh" in {
        document.select(continueSelector).text() shouldBe continueTextWelsh
      }

    }

    "Show a when you became the owner screen for a user with values populated if the user has come from cya" which {

      lazy val document: Document = getClaimOwnershipPage(language = English, userIsAgent = false, fromCya = true)

      s"has a title of $titleText" in {
        document.title() shouldBe titleText
      }

      "has a back link which takes you to the summary page" in {
        document.select(backLinkSelector).text() shouldBe backLinkText
        document.select(backLinkSelector).attr("href") shouldBe backLinkFromCyaHref
      }

      s"has text on the screen of the $localCouncilText" in {
        document.select(localCouncilSelector).text() shouldBe localCouncilText
      }

      s"has text on the screen of the $propertyDetailsText" in {
        document.select(propertyDetailsSelector).text() shouldBe propertyDetailsText
      }

      s"has a header of $headingText with a caption above of '$captionText'" in {
        document.select(headingSelector).text shouldBe headingText
        document.select(captionSelector).text shouldBe captionText
      }

      s"has summary text on the screen of $iHaveOwnedText" in {
        document.select(iHaveOwnedSelector).text() shouldBe iHaveOwnedText
      }

      s"has inset text on the screen of $youCanAddText" in {
        document.select(youCanAddSelector).text() shouldBe youCanAddText
      }

      s"has a subheading on the screen of $onWhatDateText" in {
        document.select(onWhatDateSelector).text() shouldBe onWhatDateText
      }

      s"has text on the screen of $forExampleText" in {
        document.select(forExampleSelector).text() shouldBe forExampleText
      }

      s"has a populated input field for the $dayText with value $dayOfOwnership" in {
        document.select(daySelector).text() shouldBe dayText
        document.select(dayValueSelector).attr("value") shouldBe s"$dayOfOwnership"
      }

      s"has a populated input field for the $monthText with value $monthOfOwnership" in {
        document.select(monthSelector).text() shouldBe monthText
        document.select(monthValueSelector).attr("value") shouldBe s"$monthOfOwnership"
      }

      s"has a populated input field for the $yearText with value $yearOfOwnership" in {
        document.select(yearSelector).text() shouldBe yearText
        document.select(yearValueSelector).attr("value") shouldBe s"$yearOfOwnership"
      }

      s"has a $continueText button on the screen" in {
        document.select(continueSelector).text() shouldBe continueText
      }

    }

  }

  "ClaimPropertyOwnershipController submitOwnership method" should {
    "Redirect to the occupancy page when they enter a valid date" in {

      val res = submitOwnership(day = "01", month = "01", year = "2020", userIsAgent = true, fromCya = false)

      res.status shouldBe SEE_OTHER
      res.headers("Location").head shouldBe "/business-rates-property-linking/my-organisation/claim/property-links/occupancy"

    }

    "Redirect to the summary page when they enter a valid date and came from cya" in {

      val res = submitOwnership(day = "01", month = "01", year = "2020", userIsAgent = true, fromCya = true)

      res.status shouldBe SEE_OTHER
      res.headers("Location").head shouldBe "/business-rates-property-linking/my-organisation/claim/property-links/summary"

    }

    "Return a bad request with errors for a user when no data is entered for the day/month/year" which {

      lazy val res = submitOwnership(day = "", month = "", year = "", userIsAgent = false, fromCya = true)

      lazy val document = Jsoup.parse(res.body)

      "has a status of 400" in {
        res.status shouldBe BAD_REQUEST
      }

      s"has a title of ${errorText + titleText}" in {
        document.title() shouldBe errorText + titleText
      }

    }

    "Return a bad request with errors for a user when the date is in the future" which {

      lazy val res = submitOwnership(day = "01", month = "01", year = "9999", userIsAgent = false, fromCya = true)

      lazy val document = Jsoup.parse(res.body)

      "has a status of 400" in {
        res.status shouldBe BAD_REQUEST
      }

      s"has a title of ${errorText + titleText}" in {
        document.title() shouldBe errorText + titleText
      }

    }

    //TODO: Copy the above two over for a client and for welsh

  }

  private def getClaimOwnershipPage(language: Language, userIsAgent: Boolean, fromCya: Boolean): Document = {

    stubsSetup(userIsAgent, fromCya)

    val res = await(
      ws.url(s"http://localhost:$port/business-rates-property-linking/my-organisation/claim/property-links/ownership")
        .withCookies(languageCookie(language), getSessionCookie(testSessionId))
        .withFollowRedirects(follow = false)
        .get()
    )

    res.status shouldBe OK
    Jsoup.parse(res.body)
  }

  private def stubsSetup(userIsAgent: Boolean, fromCya: Boolean): StubMapping = {

    await(
      mockPropertyLinkingSessionRepository.saveOrUpdate(
        LinkingSession(
          address = propertyAddress,
          uarn = 1L,
          submissionId = "PL-123456",
          personId = 1L,
          earliestStartDate = LocalDate.of(2017, 4, 1),
          propertyRelationship = Some(PropertyRelationship(capacity = Owner, uarn = 1)),
          propertyOwnership = if (fromCya) Some(PropertyOwnership(LocalDate.of(yearOfOwnership, monthOfOwnership, dayOfOwnership))) else None,
          propertyOccupancy = None,
          hasRatesBill = None,
          clientDetails = if (userIsAgent) Some(ClientDetails(123, "Client Name")) else None,
          localAuthorityReference = localCouncilReferenceValue,
          rtp = ClaimPropertyReturnToPage.FMBR,
          fromCya = Some(fromCya),
          isSubmitted = None
        )))

    stubFor {
      get("/business-rates-authorisation/authenticate")
        .willReturn {
          aResponse.withStatus(OK).withBody(Json.toJson(testAccounts).toString())
        }
    }

    stubFor {
      post("/auth/authorise")
        .willReturn {
          aResponse.withStatus(OK).withBody("{}")
        }
    }
  }

  private def submitOwnership(day: String, month: String, year: String, userIsAgent: Boolean, fromCya: Boolean): WSResponse = {

    stubsSetup(userIsAgent, fromCya)

    val requestBody = Json.obj(
      "interestedStartDate.day" -> day,
      "interestedStartDate.month" -> month,
      "interestedStartDate.year" -> year
    )

    await(
      ws.url(s"http://localhost:$port/business-rates-property-linking/my-organisation/claim/property-links/ownership")
        .withCookies(languageCookie(English), getSessionCookie(testSessionId))
        .withFollowRedirects(follow = false)
        .withHttpHeaders(HeaderNames.COOKIE -> "sessionId", "Csrf-Token" -> "nocheck")
        .post(body = requestBody)
    )
  }


}
