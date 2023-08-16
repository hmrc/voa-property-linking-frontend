package controllers.agentAppointment

import base.{HtmlComponentHelpers, ISpecBase}
import com.github.tomakehurst.wiremock.client.WireMock.{aResponse, get, post, stubFor}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.http.Status.OK
import play.api.libs.json.Json
import play.api.test.Helpers.{await, defaultAwaitTimeout}

import java.util.UUID

class SelectRatingListISpec  extends ISpecBase with HtmlComponentHelpers {

    val testSessionId = s"stubbed-${UUID.randomUUID}"

    val titleText = "Choose the 2023 or 2017 rating list - Valuation Office Agency - GOV.UK"
    val errorTitleText = "Error: Choose the 2023 or 2017 rating list - Valuation Office Agency - GOV.UK"
    val backLinkText = "Back"
    val captionText = "Appoint an agent"
    val headerText = "Choose the 2023 or 2017 rating list"
    val theRatingListText = "The rating list you choose for this agent will apply to all properties that you assign to them and they add to your account."
    val theAgentText = "The agent will only be able to act for you on valuations on the rating list you choose."
    val whichRatingText = "Which rating list do you want this agent to act on for you?"
    val the2023ListText = "2023 rating list"
    val theAgent2023Text = "The agent can only act for you on your current valuation for your property and any previous valuations that have an effective date after 1 April 2023."
    val the2017ListText = "2017 rating list"
    val theAgent2017Text = "The agent can only act for you on previous valuations for your property that have an effective date between 1 April 2017 to 31 March 2023."
    val continueText = "Continue"
    val errorText = "Select which rating list you want this agent to act on for you"
    val thereIsAProblemText = "There is a problem"
    val aboveRadioErrorText = "Error: Select which rating list you want this agent to act on for you"

    val titleTextWelsh = "Welsh Choose the 2023 or 2017 rating list - Valuation Office Agency - GOV.UK"
    val errorTitleTextWelsh = "Gwall: Welsh Choose the 2023 or 2017 rating list - Valuation Office Agency - GOV.UK"
    val backLinkTextWelsh = "Yn ôl"
    val captionTextWelsh = "Penodi Asiant"
    val headerTextWelsh = "Welsh Choose the 2023 or 2017 rating list"
    val theRatingListTextWelsh = "Welsh The rating list you choose for this agent will apply to all properties that you assign to them and they add to your account."
    val theAgentTextWelsh = "Welsh The agent will only be able to act for you on valuations on the rating list you choose."
    val whichRatingTextWelsh = "Pa restr ardrethu yr hoffech i’r asiant hwn ei gweithredu ar eich rhan?"
    val the2023ListTextWelsh = "rhestr ardrethu 2023"
    val theAgent2023TextWelsh = "Welsh The agent can only act for you on your current valuation for your property and any previous valuations that have an effective date after 1 April 2023."
    val the2017ListTextWelsh = "rhestr ardrethu 2017"
    val theAgent2017TextWelsh = "Welsh The agent can only act for you on previous valuations for your property that have an effective date between 1 April 2017 to 31 March 2023."
    val continueTextWelsh = "Parhau"
    val errorTextWelsh = "Welsh Select which rating list you want this agent to act on for you"
    val thereIsAProblemTextWelsh = "Mae yna broblem"
    val aboveRadioErrorTextWelsh = "Gwall: Welsh Select which rating list you want this agent to act on for you"

    val backLinkSelector = "#back-link"
    val captionSelector = "span.govuk-caption-l"
    val headerSelector = "h1.govuk-heading-l"
    val theRatingListSelector = "#main-content > div > div > p:nth-child(3)"
    val theAgentSelector = "#main-content > div > div > p:nth-child(4)"
    val whichRatingSelector = "#main-content > div > div > form > div > fieldset > legend"
    val the2023RadioButtonSelector = "#multipleListYears"
    val the2023ListSelector = "#main-content > div > div > form > div > fieldset > div > div:nth-child(1) > label"
    val theAgent2023Selector = "#multipleListYears-item-hint"
    val the2017RadioButtonSelector = "#multipleListYears-2"
    val the2017ListSelector = "#main-content > div > div > form > div > fieldset > div > div:nth-child(2) > label"
    val theAgent2017Selector = "#multipleListYears-2-item-hint"
    val continueSelector = "#continue"
    val errorSummaryTitleSelector = "#main-content > div > div > div.govuk-error-summary > div > h2"
    val errorSummaryErrorSelector = "#main-content > div > div > div.govuk-error-summary > div > div"
    val aboveRadiosErrorSelector = "#multipleListYears-error"

    val backLinkHref = "/business-rates-property-linking/my-organisation/appoint-new-agent/ratings-list"

    "SelectRatingList show method" should {
      "Show a 'Choose the 2023 or 2017 rating list screen' when the language is set to English" which {

        lazy val document = getSelectRatingListPage(English)

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


        s"has text on the screen of '$theRatingListText'" in {
          document.select(theRatingListSelector).text() shouldBe theRatingListText
        }

        s"has text on the screen of '$theAgentText'" in {
          document.select(theAgentSelector).text() shouldBe theAgentText
        }

        s"has a medium heading on the screen of '$whichRatingText'" in {
          document.select(whichRatingSelector).text() shouldBe whichRatingText
        }

        s"has an un-checked '$the2023ListText' radio button, with hint text of '$theAgent2023Text'" in {
          document.select(the2023RadioButtonSelector).hasAttr("checked") shouldBe false
          document.select(the2023ListSelector).text() shouldBe the2023ListText
          document.select(theAgent2023Selector).text() shouldBe theAgent2023Text
        }

        s"has an un-checked '$the2017ListText' radio button, with hint text of '$theAgent2017Text'" in {
          document.select(the2017RadioButtonSelector).hasAttr("checked") shouldBe false
          document.select(the2017ListSelector).text() shouldBe the2017ListText
          document.select(theAgent2017Selector).text() shouldBe theAgent2017Text
        }

        s"has a '$continueText' button on the screen, which submits the users choice" in {
          document.select(continueSelector).text() shouldBe continueText
        }
      }

      "Show a 'Choose the 2023 or 2017 rating list screen' when the language is set to Welsh" which {

        lazy val document = getSelectRatingListPage(Welsh)

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

        s"has text on the screen of '$theRatingListText' in welsh" in {
          document.select(theRatingListSelector).text() shouldBe theRatingListTextWelsh
        }

        s"has text on the screen of '$theAgentText' in welsh" in {
          document.select(theAgentSelector).text() shouldBe theAgentTextWelsh
        }

        s"has a medium heading on the screen of '$whichRatingText' in welsh" in {
          document.select(whichRatingSelector).text() shouldBe whichRatingTextWelsh
        }

        s"has an un-checked '$the2023ListText' radio button, with hint text of '$theAgent2023Text' in welsh" in {
          document.select(the2023RadioButtonSelector).hasAttr("checked") shouldBe false
          document.select(the2023ListSelector).text() shouldBe the2023ListTextWelsh
          document.select(theAgent2023Selector).text() shouldBe theAgent2023TextWelsh
        }

        s"has an un-checked '$the2017ListText' radio button, with hint text of '$theAgent2017Text' in welsh" in {
          document.select(the2017RadioButtonSelector).hasAttr("checked") shouldBe false
          document.select(the2017ListSelector).text() shouldBe the2017ListTextWelsh
          document.select(theAgent2017Selector).text() shouldBe theAgent2017TextWelsh
        }

        s"has a '$continueText' button on the screen in welsh, which submits the users choice" in {
          document.select(continueSelector).text() shouldBe continueTextWelsh
        }
      }
    }
    private def getSelectRatingListPage(language: Language): Document = {

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
        ws.url(s"http://localhost:$port/business-rates-property-linking/my-organisation/appoint-new-agent/ratings-list-select")
          .withCookies(languageCookie(language), getSessionCookie(testSessionId))
          .withFollowRedirects(follow = false)
          .get()
      )

      res.status shouldBe OK
      Jsoup.parse(res.body)
    }

  }

