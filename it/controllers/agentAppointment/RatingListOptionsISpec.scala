package controllers.agentAppointment

import base.{HtmlComponentHelpers, ISpecBase}
import com.github.tomakehurst.wiremock.client.WireMock.{aResponse, get, post, stubFor}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.http.Status._
import play.api.libs.json.Json
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import java.util.UUID

class RatingListOptionsISpec extends ISpecBase with HtmlComponentHelpers {

  val testSessionId = s"stubbed-${UUID.randomUUID}"


  val titleText = "Choose a rating list Test Agent can act on for you - Valuation Office Agency - GOV.UK"
  val errorTitleText = "Error: Choose a rating list Test Agent can act on for you - Valuation Office Agency - GOV.UK"
  val backLinkText = "Back"
  val captionText = "Appoint an agent"
  val headerText = "Choose a rating list Test Agent can act on for you"
  val thisAgentText = "This agent can act for you on:"
  val the2023RatingListText = "the 2023 rating list"
  val the2017RatingListText = "the 2017 rating list"
  val bothListsText = "both rating lists"
  val moreAboutText = "More about rating lists"
  val theVoaCalculatesText =
    "The Valuation Office Agency (VOA) calculates the rateable values for business properties in England and Wales. It holds the information on rating lists."
  val theVoaUpdatesText = "The VOA updates rateable values and publishes a new rating list every few years."
  val the2023ListText =
    "The 2023 rating list is the current rating list and has the current valuation for your property. It may also have previous valuations for your property that have an effective date after 1 April 2023."
  val the2017ListText =
    "The 2017 rating list has previous valuations for your property that have an effective date between 1 April 2017 and 31 March 2023."
  val choosingAListText =
    "Choosing a rating list is different to assigning properties to the agent. You will assign properties after choosing a rating list."
  val doYouWantText = "Do you want this agent to act for you on both rating lists?"
  val yesText = "Yes"
  val thisAgentCanText =
    "This agent can act for you on both the 2023 and 2017 rating lists. Choose this option if you’re not sure."
  val noText = "No"
  val youWantToText = "You want to choose either the 2023 or 2017 rating list."
  val continueText = "Continue"
  val errorText = "Select an option"
  val thereIsAProblemText = "There is a problem"
  val aboveRadioErrorText = "Error: Select an option"

  val errorTitleTextWelsh = "Gwall: Welsh Choose a rating list Test Agent can act on for you - Valuation Office Agency - GOV.UK"
  val titleTextWelsh = "Welsh Choose a rating list Test Agent can act on for you - Valuation Office Agency - GOV.UK"
  val backLinkTextWelsh = "Yn ôl"
  val captionTextWelsh = "Appoint an agent"
  val headerTextWelsh = "Welsh Choose a rating list Test Agent can act on for you"
  val thisAgentTextWelsh = "Welsh This agent can act for you on:"
  val the2023RatingListTextWelsh = "Welsh the 2023 rating list"
  val the2017RatingListTextWelsh = "Welsh the 2017 rating list"
  val bothListsTextWelsh = "Welsh both rating lists"
  val moreAboutTextWelsh = "Welsh More about rating lists"
  val theVoaCalculatesTextWelsh =
    "Welsh The Valuation Office Agency (VOA) calculates the rateable values for business properties in England and Wales. It holds the information on rating lists."
  val theVoaUpdatesTextWelsh = "Welsh The VOA updates rateable values and publishes a new rating list every few years."
  val the2023ListTextWelsh =
    "Welsh The 2023 rating list is the current rating list and has the current valuation for your property. It may also have previous valuations for your property that have an effective date after 1 April 2023."
  val the2017ListTextWelsh =
    "Welsh The 2017 rating list has previous valuations for your property that have an effective date between 1 April 2017 and 31 March 2023."
  val choosingAListTextWelsh =
    "Choosing a rating list is different to assigning properties to the agent. You will assign properties after choosing a rating list."
  val doYouWantTextWelsh = "Welsh Do you want this agent to act for you on both rating lists?"
  val yesTextWelsh = "Welsh Yes"
  val thisAgentCanTextWelsh =
    "Welsh This agent can act for you on both the 2023 and 2017 rating lists. Choose this option if you’re not sure."
  val noTextWelsh = "Welsh No"
  val youWantToTextWelsh = "Welsh You want to choose either the 2023 or 2017 rating list."
  val continueTextWelsh = "Parhau"
  val errorTextWelsh = "Dewiswch opsiwn"
  val thereIsAProblemTextWelsh = "Mae yna broblem"
  val aboveRadioErrorTextWelsh = "Gwall: Dewis opsiwn"

  val backLinkSelector = "#back-link"
  val captionSelector = "span.govuk-caption-l"
  val headerSelector = "h1.govuk-heading-l"
  val thisAgentSelector = "#main-content > div > div > p"
  val bulletPointSelector = "#main-content > div > div > ul.govuk-list--bullet > li"
  val moreAboutSelector =
    "#main-content > div > div > details:nth-child(5) > summary > span.govuk-details__summary-text"
  val ratingListDetailedSummarySelector = "#main-content > div > div > details:nth-child(5) > div > p"
  val choosingAListSelector = "#main-content > div > div > div:nth-child(6).govuk-inset-text"
  val doYouWantSelector = "#main-content > div > div > form > div > fieldset > legend"
  val yesRadioButtonSelector = "#multipleListYears"
  val yesSelector = "#main-content > div > div > form > div > fieldset > div > div:nth-child(1) > label"
  val thisAgentCanSelector = "#multipleListYears-item-hint"
  val noRadioButtonSelector = "#multipleListYears-2"
  val noSelector = "#main-content > div > div > form > div > fieldset > div > div:nth-child(2) > label"
  val youWantToSelector = "#multipleListYears-2-item-hint"
  val continueSelector = "#continue"
  val errorSummaryTitleSelector = "#main-content > div > div > div.govuk-error-summary > div > h2"
  val errorSummaryErrorSelector = "#main-content > div > div > div.govuk-error-summary > div > div"
  val aboveRadiosErrorSelector = "#multipleListYears-error"

  val backLinkHref = "/business-rates-property-linking/my-organisation/appoint-new-agent/is-correct-agent"

  "RatingListOptionController show method" should {
    "Show an English choose rating list screen with the correct text when the language is set to English" when {

      lazy val document = getRatingListOptionPage(English)

      s"has a title of $titleText" in {
        document.title() shouldBe titleText
      }

      "has a back link which takes you to the agent details page" in {
        document.select(backLinkSelector).text() shouldBe backLinkText
        document.select(backLinkSelector).attr("href") shouldBe backLinkHref
      }

      s"has a header of '$headerText' with a caption above of '$captionText'" in {
        document.select(headerSelector).text shouldBe headerText
        document.select(captionSelector).text shouldBe captionText
      }

      s"has text on the screen of '$thisAgentCanText'" in {
        document.select(thisAgentCanSelector).text() shouldBe thisAgentCanText
      }

      s"has radio buttons on the screen with values of '$the2023RatingListText', '$the2017RatingListText' and '$bothListsText'" in {
        document.select(bulletPointSelector).get(0).text() shouldBe the2023RatingListText
        document.select(bulletPointSelector).get(1).text() shouldBe the2017RatingListText
        document.select(bulletPointSelector).get(2).text() shouldBe bothListsText
      }

      s"has a details summary section on the screen with summary text '$moreAboutText', then summary text of" +
        s"'$theVoaCalculatesText', '$theVoaUpdatesText', '$the2023ListText' and '$the2017ListText'" in {
        document.select(moreAboutSelector).text() shouldBe moreAboutText
        document.select(ratingListDetailedSummarySelector).get(0).text() shouldBe theVoaCalculatesText
        document.select(ratingListDetailedSummarySelector).get(1).text() shouldBe theVoaUpdatesText
        document.select(ratingListDetailedSummarySelector).get(2).text() shouldBe the2023ListText
        document.select(ratingListDetailedSummarySelector).get(3).text() shouldBe the2017ListText
      }

      s"has inset text on the screen of '$choosingAListText', which has a link to the manage agent screens" in {
        document.select(choosingAListSelector).text() shouldBe choosingAListText
      }

      s"has a medium heading on the screen of '$doYouWantText'" in {
        document.select(doYouWantSelector).text() shouldBe doYouWantText
      }

      s"has a un-checked '$yesText' radio button, with hint text of '$thisAgentCanText'" in {
        document.select(yesRadioButtonSelector).hasAttr("checked") shouldBe false
        document.select(yesSelector).text() shouldBe yesText
        document.select(thisAgentCanSelector).text() shouldBe thisAgentCanText
      }

      s"has an un-checked '$noText' radio button, with hint text of '$youWantToText'" in {
        document.select(noRadioButtonSelector).hasAttr("checked") shouldBe false
        document.select(noSelector).text() shouldBe noText
        document.select(youWantToSelector).text() shouldBe youWantToText
      }

      s"has a '$continueText' button on the screen, which submits the users choice" in {
        document.select(continueSelector).text() shouldBe continueText
      }
    }

    "Show a Welsh choose rating list screen with the correct text when the language is set to Welsh" which {

      lazy val document = getRatingListOptionPage(Welsh)

      s"has a title of $titleText in welsh" in {
        document.title() shouldBe titleTextWelsh
      }

      "has a back link which takes you to the agent details page" in {
        document.select(backLinkSelector).text() shouldBe backLinkTextWelsh
        document.select(backLinkSelector).attr("href") shouldBe backLinkHref
      }

      s"has a header of '$headerText' in welsh with a caption above of '$captionText' in welsh" in {
        document.select(headerSelector).text shouldBe headerTextWelsh
        document.select(captionSelector).text shouldBe captionTextWelsh
      }

      s"has text on the screen of '$thisAgentCanText' in welsh" in {
        document.select(thisAgentCanSelector).text() shouldBe thisAgentCanTextWelsh
      }

      s"has radio buttons on the screen with values of '$the2023RatingListText', '$the2017RatingListText' and '$bothListsText' in welsh" in {
        document.select(bulletPointSelector).get(0).text() shouldBe the2023RatingListTextWelsh
        document.select(bulletPointSelector).get(1).text() shouldBe the2017RatingListTextWelsh
        document.select(bulletPointSelector).get(2).text() shouldBe bothListsTextWelsh
      }

      s"has a details summary section on the screen with summary text '$moreAboutText', then summary text of" +
        s"'$theVoaCalculatesText', '$theVoaUpdatesText', '$the2023ListText' and '$the2017ListText' in welsh" in {
        document.select(moreAboutSelector).text() shouldBe moreAboutTextWelsh
        document.select(ratingListDetailedSummarySelector).get(0).text() shouldBe theVoaCalculatesTextWelsh
        document.select(ratingListDetailedSummarySelector).get(1).text() shouldBe theVoaUpdatesTextWelsh
        document.select(ratingListDetailedSummarySelector).get(2).text() shouldBe the2023ListTextWelsh
        document.select(ratingListDetailedSummarySelector).get(3).text() shouldBe the2017ListTextWelsh
      }

      s"has inset text on the screen of '$choosingAListText' in welsh, which has a link to the manage agent screens" in {
        document.select(choosingAListSelector).text() shouldBe choosingAListTextWelsh
      }

      s"has a medium heading on the screen of '$doYouWantText' in welsh" in {
        document.select(doYouWantSelector).text() shouldBe doYouWantTextWelsh
      }

      s"has a un-checked '$yesText' radio button, with hint text of '$thisAgentCanText' in welsh" in {
        document.select(yesRadioButtonSelector).hasAttr("checked") shouldBe false
        document.select(yesSelector).text() shouldBe yesTextWelsh
        document.select(thisAgentCanSelector).text() shouldBe thisAgentCanTextWelsh
      }

      s"has an un-checked '$noText' radio button, with hint text of '$youWantToText' in welsh" in {
        document.select(noRadioButtonSelector).hasAttr("checked") shouldBe false
        document.select(noSelector).text() shouldBe noTextWelsh
        document.select(youWantToSelector).text() shouldBe youWantToTextWelsh
      }

      s"has a '$continueText' button on the screen in welsh, which submits the users choice" in {
        document.select(continueSelector).text() shouldBe continueTextWelsh
      }
    }
  }

  private def getRatingListOptionPage(language: Language): Document = {

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

    val res = await(
      ws.url(s"http://localhost:$port/business-rates-property-linking/my-organisation/appoint-new-agent/ratings-list")
        .withCookies(languageCookie(language), getSessionCookie(testSessionId))
        .withFollowRedirects(follow = false)
        .get()
    )

    res.status shouldBe OK
    Jsoup.parse(res.body)

  }

}
