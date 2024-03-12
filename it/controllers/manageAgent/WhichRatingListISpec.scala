package controllers.manageAgent

import base.{HtmlComponentHelpers, ISpecBase}
import com.github.tomakehurst.wiremock.client.WireMock.{aResponse, get, post, stubFor}
import models.propertyrepresentation.AgentSummary
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.http.HeaderNames
import play.api.http.Status.OK
import play.api.libs.json.Json
import play.api.test.Helpers._
import repositories.ManageAgentSessionRepository
import uk.gov.hmrc.http.{HeaderCarrier, SessionId}

import java.time.LocalDate
import java.util.UUID

class WhichRatingListISpec extends ISpecBase with HtmlComponentHelpers {
  lazy val mockRepository: ManageAgentSessionRepository = app.injector.instanceOf[ManageAgentSessionRepository]
  val testSessionId = s"stubbed-${UUID.randomUUID}"
  implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId(testSessionId)))
  val titleText = "Choose the 2023 or 2017 rating list - Valuation Office Agency - GOV.UK"
  val errorTitleText = "Error: Choose the 2023 or 2017 rating list - Valuation Office Agency - GOV.UK"
  val backLinkText = "Back"
  val captionText = "Manage agent"
  val headerText = "Choose the 2023 or 2017 rating list"
  val currentlyThisTextMultiple = "Currently this agent can act for you on the 2023 and 2017 rating lists"
  val theRatingListText =
    "The rating list you choose for this agent will apply to all properties that you assign to them and they add to your account."
  val theAgentText = "The agent will only be able to act for you on valuations on the rating list you choose."
  val whichRatingText = "Which rating list do you want this agent to act on for you?"
  val the2023ListText = "2023 rating list"
  val theAgent2023Text =
    "The agent can only act for you on your current valuation for your property and any previous valuations that have an effective date after 1 April 2023."
  val the2017ListText = "2017 rating list"
  val theAgent2017Text =
    "The agent can only act for you on previous valuations for your property that have an effective date between 1 April 2017 to 31 March 2023."
  val continueText = "Continue"
  val errorText = "Select which rating list you want this agent to act on for you"
  val thereIsAProblemText = "There is a problem"
  val aboveRadioErrorText = "Error: Select which rating list you want this agent to act on for you"
  val titleTextWelsh = "Dewiswch restr ardrethu 2023 neu 2017 - Valuation Office Agency - GOV.UK"
  val errorTitleTextWelsh = "Gwall: Dewiswch restr ardrethu 2023 neu 2017 - Valuation Office Agency - GOV.UK"
  val backLinkTextWelsh = "Yn ôl"
  val captionTextWelsh = "Rheoli asiant"
  val headerTextWelsh = "Dewiswch restr ardrethu 2023 neu 2017"
  val currentlyThisTextMultipleWelsh = "Welsh Currently this agent can act for you on the 2023 and 2017 rating lists"
  val theRatingListTextWelsh =
    "Bydd y rhestr ardrethu a ddewiswch ar gyfer yr asiant hwn yn berthnasol i’r holl eiddo rydych chi’n ei aseinio iddynt ac i’r rheiny maen nhw’n ychwanegu at eich cyfrif."
  val theAgentTextWelsh =
    "Dim ond ar brisiadau ar y rhestr ardrethu a ddewiswch y bydd yr asiant yn gallu gweithredu ar eich rhan."
  val whichRatingTextWelsh = "Pa restr ardrethu yr hoffech i’r asiant hwn ei gweithredu ar eich rhan?"
  val the2023ListTextWelsh = "rhestr ardrethu 2023"
  val theAgent2023TextWelsh =
    "Dim ond ar eich prisiad cyfredol ar gyfer eich eiddo ac unrhyw brisiadau blaenorol sydd â dyddiad dod i rym ar ôl 1 Ebrill 2023 y gall yr asiant weithredu ar eich rhan."
  val the2017ListTextWelsh = "rhestr ardrethu 2017"
  val theAgent2017TextWelsh =
    "Dim ond ar brisiadau blaenorol ar gyfer eich eiddo sydd â dyddiad dod i rym rhwng 1 Ebrill 2017 a 31 Mawrth 2023 y gall yr asiant weithredu ar eich rhan."
  val continueTextWelsh = "Parhau"
  val errorTextWelsh = "Dewis ar ba restr ardrethu chi am i’r asiant hwn weithredu ar eich rhan"
  val thereIsAProblemTextWelsh = "Mae yna broblem"
  val aboveRadioErrorTextWelsh = "Gwall: Dewis ar ba restr ardrethu chi am i’r asiant hwn weithredu ar eich rhan"
  val backLinkSelector = "#back-link"
  val captionSelector = "span.govuk-caption-l"
  val headerSelector = "h1.govuk-heading-l"
  val currentlyThisSelector = "#main-content > div > div > div:nth-child(3).govuk-inset-text"
  val theRatingListSelector = "#main-content > div > div > p:nth-child(4)"
  val theAgentSelector = "#main-content > div > div > p:nth-child(5)"
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
  val backLinkHref = "/business-rates-property-linking/my-organisation/appoint/ratings-list/choose"

  def currentlyThisTextSingle(listYear: String) = s"Currently this agent can act for you on the $listYear rating list"

  def currentlyThisTextSingleWelsh(listYear: String) =
    s"Welsh Currently this agent can act for you on the $listYear rating list"

  "WhichRatingListController show method" should {
    "Show an English which rating list screen with the correct text for already being on both lists when the language is set to English" which {

      lazy val document = getWhichRatingListPage(English, List("2023", "2017"))

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

      s"has inset text on the screen of '$currentlyThisTextMultiple'" in {
        document.select(currentlyThisSelector).text() shouldBe currentlyThisTextMultiple
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

    "Show an English choose rating list screen with the correct text for already being only on the 2017 list when the language is set to English" which {

      lazy val document = getWhichRatingListPage(English, List("2017"))

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

      s"has inset text on the screen of '${currentlyThisTextSingle("2017")}'" in {
        document.select(currentlyThisSelector).text() shouldBe currentlyThisTextSingle("2017")
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
        document.select(the2017RadioButtonSelector).hasAttr("checked") shouldBe true
        document.select(the2017ListSelector).text() shouldBe the2017ListText
        document.select(theAgent2017Selector).text() shouldBe theAgent2017Text
      }

      s"has a '$continueText' button on the screen, which submits the users choice" in {
        document.select(continueSelector).text() shouldBe continueText
      }
    }

    "Show an English choose rating list screen with the correct text for already being only on the 2023 list when the language is set to English" which {

      lazy val document = getWhichRatingListPage(English, List("2023"))

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

      s"has inset text on the screen of '${currentlyThisTextSingle("2023")}'" in {
        document.select(currentlyThisSelector).text() shouldBe currentlyThisTextSingle("2023")
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
        document.select(the2023RadioButtonSelector).hasAttr("checked") shouldBe true
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

    "Show a Welsh choose rating list screen with the correct text for already being on both lists when the language is set to Welsh" which {

      lazy val document = getWhichRatingListPage(Welsh, List("2023", "2017"))

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

      s"has inset text on the screen of '$currentlyThisTextMultiple' in welsh" in {
        document.select(currentlyThisSelector).text() shouldBe currentlyThisTextMultipleWelsh
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

    "Show a Welsh choose rating list screen with the correct text for already being only on the 2017 list when the language is set to Welsh" which {

      lazy val document = getWhichRatingListPage(Welsh, List("2017"))

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

      s"has inset text on the screen of '${currentlyThisTextSingle("2017")}' in welsh" in {
        document.select(currentlyThisSelector).text() shouldBe currentlyThisTextSingleWelsh("2017")
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
        document.select(the2017RadioButtonSelector).hasAttr("checked") shouldBe true
        document.select(the2017ListSelector).text() shouldBe the2017ListTextWelsh
        document.select(theAgent2017Selector).text() shouldBe theAgent2017TextWelsh
      }

      s"has a '$continueText' button on the screen in welsh, which submits the users choice" in {
        document.select(continueSelector).text() shouldBe continueTextWelsh
      }
    }

    "Show a Welsh choose rating list screen with the correct text for already being only on the 2023 list when the language is set to Welsh" which {

      lazy val document = getWhichRatingListPage(Welsh, List("2023"))

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

      s"has inset text on the screen of '${currentlyThisTextSingle("2023")}' in welsh" in {
        document.select(currentlyThisSelector).text() shouldBe currentlyThisTextSingleWelsh("2023")
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
        document.select(the2023RadioButtonSelector).hasAttr("checked") shouldBe true
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

    "Show a NOT_FOUND page when the agent has no listYears data" in {

      await(
        mockRepository.saveOrUpdate(
          AgentSummary(
            listYears = None,
            name = "Test Agent",
            organisationId = 100L,
            representativeCode = 100L,
            appointedDate = LocalDate.now(),
            propertyCount = 1
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

      val res = await(
        ws.url(s"http://localhost:$port/business-rates-property-linking/my-organisation/appoint/ratings-list/confirm")
          .withCookies(languageCookie(English), getSessionCookie(testSessionId))
          .withFollowRedirects(follow = false)
          .get()
      )

      res.status shouldBe NOT_FOUND
    }

  }

  "WhichRatingListController post method" should {
    "Redirect to the are you sure single page if the user has chosen the 2017 list year" in {
      await(
        mockRepository.saveOrUpdate(
          AgentSummary(
            listYears = Some(List("2017")),
            name = "Test Agent",
            organisationId = 100L,
            representativeCode = 100L,
            appointedDate = LocalDate.now(),
            propertyCount = 1
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

      val requestBody = Json.obj(
        "multipleListYears" -> "false"
      )

      val res = await(
        ws.url(s"http://localhost:$port/business-rates-property-linking/my-organisation/appoint/ratings-list/confirm")
          .withCookies(languageCookie(English), getSessionCookie(testSessionId))
          .withFollowRedirects(follow = false)
          .withHttpHeaders(HeaderNames.COOKIE -> "sessionId", "Csrf-Token" -> "nocheck")
          .post(requestBody)
      )

      res.status shouldBe SEE_OTHER
      res
        .headers("Location")
        .head shouldBe "/business-rates-property-linking/my-organisation/appoint/ratings-list/are-you-sure?chosenListYear=2017"

    }

    "Redirect to the are you sure single page if the user has chosen the 2023 list year" in {
      await(
        mockRepository.saveOrUpdate(
          AgentSummary(
            listYears = Some(List("2017")),
            name = "Test Agent",
            organisationId = 100L,
            representativeCode = 100L,
            appointedDate = LocalDate.now(),
            propertyCount = 1
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

      val requestBody = Json.obj(
        "multipleListYears" -> "true"
      )

      val res = await(
        ws.url(s"http://localhost:$port/business-rates-property-linking/my-organisation/appoint/ratings-list/confirm")
          .withCookies(languageCookie(English), getSessionCookie(testSessionId))
          .withFollowRedirects(follow = false)
          .withHttpHeaders(HeaderNames.COOKIE -> "sessionId", "Csrf-Token" -> "nocheck")
          .post(requestBody)
      )

      res.status shouldBe SEE_OTHER
      res
        .headers("Location")
        .head shouldBe "/business-rates-property-linking/my-organisation/appoint/ratings-list/are-you-sure?chosenListYear=2023"

    }

    "Return a bad request and show an error on the which list year page if the user has chosen no list years in English" when {

      lazy val document = postWhichRatingListPage(English, listYears = List("2017"))

      s"has a title of $errorTitleText" in {
        document.title() shouldBe errorTitleText
      }

      "has a back link which takes you to the agent details page" in {
        document.select(backLinkSelector).text() shouldBe backLinkText
        document.select(backLinkSelector).attr("href") shouldBe backLinkHref
      }

      s"has an error summary that contains the correct error message '$errorText'" in {
        document.select(errorSummaryTitleSelector).text() shouldBe thereIsAProblemText
        document.select(errorSummaryErrorSelector).text() shouldBe errorText
      }

      s"has a header of '$headerText' with a caption above of '$captionText'" in {
        document.select(headerSelector).text shouldBe headerText
        document.select(captionSelector).text shouldBe captionText
      }

      s"has a medium heading on the screen of '$whichRatingText'" in {
        document.select(whichRatingSelector).text() shouldBe whichRatingText
      }

      s"has an error message above the radio button of $aboveRadioErrorText" in {
        document.select(aboveRadiosErrorSelector).text() shouldBe aboveRadioErrorText
      }

      s"has an un-checked '$the2023ListText' radio button, with hint text of '$theAgent2023Text'" in {
        document.select(the2023RadioButtonSelector).hasAttr("checked") shouldBe false
        document.select(the2023ListSelector).text() shouldBe the2023ListText
        document.select(theAgent2023Selector).text() shouldBe theAgent2023Text
      }

      s"has a checked '$the2017ListText' radio button, with hint text of '$theAgent2017Text'" in {
        document.select(the2017RadioButtonSelector).hasAttr("checked") shouldBe true
        document.select(the2017ListSelector).text() shouldBe the2017ListText
        document.select(theAgent2017Selector).text() shouldBe theAgent2017Text
      }

      s"has a '$continueText' button on the screen, which submits the users choice" in {
        document.select(continueSelector).text() shouldBe continueText
      }
    }

    "Return a bad request and show an error on the choose list page if the user has chosen no list years in Welsh" when {

      lazy val document = postWhichRatingListPage(Welsh, listYears = List("2023"))

      s"has a title of $errorTitleText in welsh" in {
        document.title() shouldBe errorTitleTextWelsh
      }

      "has a back link which takes you to the agent details page" in {
        document.select(backLinkSelector).text() shouldBe backLinkTextWelsh
        document.select(backLinkSelector).attr("href") shouldBe backLinkHref
      }

      s"has an error summary that contains the correct error message '$errorText' in welsh" in {
        document.select(errorSummaryTitleSelector).text() shouldBe thereIsAProblemTextWelsh
        document.select(errorSummaryErrorSelector).text() shouldBe errorTextWelsh
      }

      s"has a header of '$headerText' with a caption above of '$captionText' in welsh" in {
        document.select(headerSelector).text shouldBe headerTextWelsh
        document.select(captionSelector).text shouldBe captionTextWelsh
      }

      s"has a medium heading on the screen of '$whichRatingText' in welsh" in {
        document.select(whichRatingSelector).text() shouldBe whichRatingTextWelsh
      }

      s"has an error message above the radio button of $aboveRadioErrorText in welsh" in {
        document.select(aboveRadiosErrorSelector).text() shouldBe aboveRadioErrorTextWelsh
      }

      s"has a checked '$the2023ListText' radio button, with hint text of '$theAgent2023Text' in welsh" in {
        document.select(the2023RadioButtonSelector).hasAttr("checked") shouldBe true
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

    "Show a NOT_FOUND page when the agent has no listYears data" in {

      await(
        mockRepository.saveOrUpdate(
          AgentSummary(
            listYears = None,
            name = "Test Agent",
            organisationId = 100L,
            representativeCode = 100L,
            appointedDate = LocalDate.now(),
            propertyCount = 1
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

      val res = await(
        ws.url(s"http://localhost:$port/business-rates-property-linking/my-organisation/appoint/ratings-list/confirm")
          .withCookies(languageCookie(English), getSessionCookie(testSessionId))
          .withFollowRedirects(follow = false)
          .withHttpHeaders(HeaderNames.COOKIE -> "sessionId", "Csrf-Token" -> "nocheck")
          .post(body = "")
      )

      res.status shouldBe NOT_FOUND
    }

  }

  private def getWhichRatingListPage(language: Language, listYears: List[String]): Document = {

    await(
      mockRepository.saveOrUpdate(
        AgentSummary(
          listYears = Some(listYears),
          name = "Test Agent",
          organisationId = 100L,
          representativeCode = 100L,
          appointedDate = LocalDate.now(),
          propertyCount = 1
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

    val res = await(
      ws.url(s"http://localhost:$port/business-rates-property-linking/my-organisation/appoint/ratings-list/confirm")
        .withCookies(languageCookie(language), getSessionCookie(testSessionId))
        .withFollowRedirects(follow = false)
        .get()
    )

    res.status shouldBe OK
    Jsoup.parse(res.body)
  }

  private def postWhichRatingListPage(language: Language, listYears: List[String]): Document = {

    await(
      mockRepository.saveOrUpdate(
        AgentSummary(
          listYears = Some(listYears),
          name = "Test Agent",
          organisationId = 100L,
          representativeCode = 100L,
          appointedDate = LocalDate.now(),
          propertyCount = 1
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

    val res = await(
      ws.url(s"http://localhost:$port/business-rates-property-linking/my-organisation/appoint/ratings-list/confirm")
        .withCookies(languageCookie(language), getSessionCookie(testSessionId))
        .withFollowRedirects(follow = false)
        .withHttpHeaders(HeaderNames.COOKIE -> "sessionId", "Csrf-Token" -> "nocheck")
        .post(body = "")
    )

    res.status shouldBe BAD_REQUEST
    Jsoup.parse(res.body)
  }

}
