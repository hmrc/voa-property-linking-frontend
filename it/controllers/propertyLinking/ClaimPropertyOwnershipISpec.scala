package controllers.propertyLinking

import base.{HtmlComponentHelpers, ISpecBase}
import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import models.propertyrepresentation.AgentAppointmentChangesResponse
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.http.HeaderNames
import play.api.http.Status.OK
import play.api.libs.json.Json
import play.api.libs.ws.WSResponse
import play.api.test.Helpers._
import utils.ListYearsHelpers

class ClaimPropertyOwnershipISpec extends ISpecBase with HtmlComponentHelpers with ListYearsHelpers {

  val titleText = "When you became the owner or occupier of the property - Valuation Office Agency - GOV.UK"
  val backLinkText = "Back"
  val localCouncilText = "Local council reference: XXX"
  val propertyDetailsText = "Property: XXX"
  val captionText = "Add a property"
  val headingText = "When you became the owner or occupier of the property"
  val iHaveOwnedText = "I have owned or occupied the property on more than one occasion"
  val youCanAddText = "You can add the property for each period you were connected to it, but you need to do it one period at a time. However, if you want to talk to the Valuation Office Agency about the valuation, you will need to select the right valuation period."
  val onWhatDateText = "On what date did you become the owner or occupier?"
  val forExampleText = "For example, 1 4 2017"
  val dayText = "Day"
  val monthText = "Month"
  val yearText = "Year"
  val continueText = "Continue"

  val titleTextWelsh = "Pryd y daethoch yn berchennog neu’n feddiannydd yr eiddo - Valuation Office Agency - GOV.UK"
  val backLinkTextWelsh = "Yn ôl"
  val localCouncilTextWelsh = "Cyfeirnod yr awdurdod lleol:  XXX"
  val propertyDetailsTextWelsh = "Eiddo: XXX"
  val captionTextWelsh = "Ychwanegu eiddo"
  val headingTextWelsh = "Pryd y daethoch yn berchennog neu’n feddiannydd yr eiddo"
  val iHaveOwnedTextWelsh = "Rwyf wedi bod yn berchen ar yr eiddo neu wedi ei feddiannu ar fwy nag un achlysur"
  val youCanAddTextWelsh = "Gallwch ychwanegu’r eiddo ar gyfer pob cyfnod amser roeddech yn gysylltiedig ag ef, ond mae’n rhaid i chi wneud hyn un cyfnod amser ar y tro. Serch hyn, os ydych eisiau siarad gydag Asiantaeth y Swyddfa Brisio am eich prisiad, bydd rhaid i chi ddewis y cyfnod prisio cywir."
  val onWhatDateTextWelsh = "Ar ba ddyddiad ddaethoch chi’n berchennog neu’r meddiannydd?"
  val forExampleTextWelsh = "Er enghraifft, 1 4 2017"
  val dayTextWelsh = "Diwrnod"
  val monthTextWelsh = "Mis"
  val yearTextWelsh = "Blwyddyn"
  val continueTextWelsh = "Parhau"

  val backLinkSelector = "#back-link"
  val localCouncilSelector = "#local-authority-reference"
  val propertyDetailsSelector = "#address"
  val captionSelector = "span.govuk-caption-l"
  val headingSelector = "h1"
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

//  TODO: Is there a back link from cya? Also does this go back too far?
  val backLinkHref = "/business-rates-property-linking/my-organisation/claim/property-links"

  "ClaimPropertyOwnershipController showOwnership method" should {
    "Show an English when you became the owner screen with the correct text when the language is set to English" which {

      lazy val document: Document = getClaimOwnershipPage(language = English)

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
        document.select(dayValueSelector).text() shouldBe ""
      }

      s"has an empty input field for the $monthText" in {
        document.select(monthSelector).text() shouldBe monthText
        document.select(monthValueSelector).text() shouldBe ""
      }

      s"has an empty input field for the $yearText" in {
        document.select(yearSelector).text() shouldBe yearText
        document.select(yearValueSelector).text() shouldBe ""
      }

      s"has a $continueText button on the screen" in {
        document.select(continueSelector).text() shouldBe continueText
      }

    }

  }

  "ClaimPropertyOwnershipController submitOwnership method" should {
    "Redirect to the next page when they enter a valid date" in {
      setCurrentListYears(List("2017"))

      stubsSetup

      val res = submitOwnership

      res.status shouldBe SEE_OTHER
      res.headers("Location").head shouldBe "/business-rates-property-linking/my-organisation/appoint/ratings-list/confirmed"

      verifyAppointedListYears(amount = 1, chosenListYear = "2023")
      verifyAppointedListYears(amount = 0, chosenListYear = "2017")
      verifyRevokedListYears(amount = 1, chosenListYear = "2017")
      verifyRevokedListYears(amount = 0, chosenListYear = "2023")

    }
  }

  private def getClaimOwnershipPage(language: Language): Document = {

    stubsSetup

    val res = await(
      ws.url(s"http://localhost:$port/business-rates-property-linking/my-organisation/claim/property-links/ownership")
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

    stubFor {
      post("/auth/authorise")
        .willReturn {
          aResponse.withStatus(OK).withBody("{}")
        }
    }

    stubFor {
      post("/property-linking/my-organisation/agent/submit-appointment-changes")
        .willReturn {
          aResponse.withStatus(ACCEPTED).withBody(Json.toJson(AgentAppointmentChangesResponse("success")).toString())
        }
    }
  }

  private def submitOwnership: WSResponse = {
    await(
      ws.url(s"http://localhost:$port/business-rates-property-linking/my-organisation/claim/property-links/ownership")
        .withCookies(languageCookie(English), getSessionCookie(testSessionId))
        .withFollowRedirects(follow = false)
        .withHttpHeaders(HeaderNames.COOKIE -> "sessionId", "Csrf-Token" -> "nocheck")
        .post(body = "")
    )
  }


}
