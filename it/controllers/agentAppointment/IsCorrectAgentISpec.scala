package controllers.agentAppointment

import base.{HtmlComponentHelpers, ISpecBase}
import com.github.tomakehurst.wiremock.client.WireMock.{aResponse, get, post, stubFor}
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import models.propertyrepresentation.{AgentSelected, SearchedAgent, SelectedAgent}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.http.HeaderNames
import play.api.http.Status._
import play.api.libs.json.Json
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import repositories.AppointAgentSessionRepository
import uk.gov.hmrc.http.{HeaderCarrier, SessionId}

import java.util.UUID

class IsCorrectAgentISpec extends ISpecBase with HtmlComponentHelpers {

  val testSessionId = s"stubbed-${UUID.randomUUID}"
  val agentName = "Test Organisation"
  val agentAddress = "Test Street, Test Road, TE5 7ER"

  val titleText = "Is this your agent? - Valuation Office Agency - GOV.UK"
  val backLinkText = "Back"
  val headerText = "Is this your agent?"
  val captionText = "Appoint an agent"
  val yesText = "Yes"
  val noText = "No, enter a new code"
  val continueText = "Continue"
  val noSelectionErrorText = "Select yes if this is your agent"
  val errorText = "Error:"

  val titleTextWelsh = "Ai’ch asiant chi yw hwn? - Valuation Office Agency - GOV.UK"
  val backLinkTextWelsh = "Yn ôl"
  val headerTextWelsh = "Ai’ch asiant chi yw hwn?"
  val captionTextWelsh = "Penodi asiant"
  val yesTextWelsh = "Iawn"
  val noTextWelsh = "Na, nodwch god newydd"
  val continueTextWelsh = "Yn eich blaen"
  val noSelectionErrorTextWelsh = "Dewiswch ie os mai hwn yw eich asiant"
  val errorTextWelsh = "Gwall:"

  val backLinkSelector = "#back-link"
  val headerSelector = "h1"
  val captionSelector = "#caption"
  val agentDetailsSelector = "#agent-details"
  val yesSelector = "#main-content > div > div > form > div > div > div:nth-child(1) > label"
  val yesRadioSelector = "#isThisYourAgent"
  val noSelector = "#main-content > div > div > form > div > div > div:nth-child(2) > label"
  val noRadioSelector = "#isThisYourAgent-2"
  val continueSelector = "#continue-button"
  val errorAtSummarySelector = "#main-content > div > div > div.govuk-error-summary > div > div > ul > li > a"
  val errorAtLabelSelector = "#isThisYourAgent-error"

  val backLinkHref = "/business-rates-property-linking/my-organisation/appoint-new-agent/agent-code?backLinkUrl=%2Fbusiness-rates-dashboard%2Fhome"
  val noSelectionErrorHref = "#isThisYourAgent"

  "AddAgentController isCorrectAgent get method" should {
    "Show the isYourAgent page with the correct English page content" when {

      lazy val document = getIsCorrectAgentPage(English)

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

      s"has inset text of the agents details" in {
        document.select(agentDetailsSelector).text shouldBe agentName + " " + agentAddress
      }

      s"has a radio button for $yesText" in {
        document.select(yesRadioSelector).attr("type") shouldBe "radio"
        document.select(yesSelector).text() shouldBe yesText
      }

      s"has a radio button for $noText" in {
        document.select(noRadioSelector).attr("type") shouldBe "radio"
        document.select(noSelector).text() shouldBe noText
      }

      s"has a $continueText button" in {
        document.select(continueSelector).text shouldBe continueText
      }
    }

    "Show the isYourAgent page with the correct Welsh page content" when {

      lazy val document = getIsCorrectAgentPage(Welsh)

      s"has a title of $titleText in welsh" in {
        document.title() shouldBe titleTextWelsh
      }

      s"has a $backLinkText link which takes you to the agent details page in welsh" in {
        document.select(backLinkSelector).text() shouldBe backLinkTextWelsh
        document.select(backLinkSelector).attr("href") shouldBe backLinkHref
      }

      s"has a header of '$headerText' with a caption above of '$captionText' in welsh" in {
        document.select(headerSelector).text shouldBe headerTextWelsh
        document.select(captionSelector).text shouldBe captionTextWelsh
      }

      s"has inset text of the agents details" in {
        document.select(agentDetailsSelector).text shouldBe agentName + " " + agentAddress
      }

      s"has a radio button for $yesText in welsh" in {
        document.select(yesRadioSelector).attr("type") shouldBe "radio"
        document.select(yesSelector).text() shouldBe yesTextWelsh
      }

      s"has a radio button for $noText in welsh" in {
        document.select(noRadioSelector).attr("type") shouldBe "radio"
        document.select(noSelector).text() shouldBe noTextWelsh
      }

      s"has a $continueText button in welsh" in {
        document.select(continueSelector).text shouldBe continueTextWelsh
      }
    }
  }

  "AddAgentController agentSelected post method" should {

    "Return a BAD_REQUEST and show the error on the isYourAgent page with the correct English page content if the user chooses no radio button" when {

      lazy val document = postIsCorrectAgentWithErrorPage(English)

      s"has a title of ${errorText + " " + titleText}" in {
        document.title() shouldBe errorText + " " + titleText
      }

      s"has an error in the error summary of $noSelectionErrorText" in {
        document.select(errorAtSummarySelector).text shouldBe noSelectionErrorText
        document.select(errorAtSummarySelector).attr("href") shouldBe noSelectionErrorHref
      }

      s"has an error above the label of $noSelectionErrorText" in {
        document.select(errorAtLabelSelector).text shouldBe errorText + " " + noSelectionErrorText
      }
    }

    "Return a BAD_REQUEST and show the error on the isYourAgent page with the correct Welsh page content if the user chooses no radio button" when {

      lazy val document = postIsCorrectAgentWithErrorPage(Welsh)

      s"has a title of ${errorText + " " + titleText} in welsh" in {
        document.title() shouldBe errorTextWelsh + " " + titleTextWelsh
      }

      s"has an error in the error summary of $noSelectionErrorText in welsh" in {
        document.select(errorAtSummarySelector).text shouldBe noSelectionErrorTextWelsh
        document.select(errorAtSummarySelector).attr("href") shouldBe noSelectionErrorHref
      }

      s"has an error above the label of $noSelectionErrorText in welsh" in {
        document.select(errorAtLabelSelector).text shouldBe errorTextWelsh + " " + noSelectionErrorTextWelsh
      }
    }

    s"Redirect to the Choose a ratings list page when the user chooses $yesText" when {
      lazy val res = validPostIsCorrectAgentPage(true)

      "has the correct status and redirect location" in {
        res.status shouldBe SEE_OTHER
        res.header("Location") shouldBe Some("/business-rates-property-linking/my-organisation/appoint-new-agent/ratings-list")
      }
    }

    s"Redirect to the Agent Code page when the user chooses $noText" when {
      lazy val res = validPostIsCorrectAgentPage(false)

      "has the correct status and redirect location" in {
        res.status shouldBe SEE_OTHER
        res.header("Location") shouldBe Some("/business-rates-property-linking/my-organisation/appoint-new-agent/agent-code")
      }
    }

  }

  private def getIsCorrectAgentPage(language: Language): Document = {

    commonSetup()

    val res = await(
      ws.url(s"http://localhost:$port/business-rates-property-linking/my-organisation/appoint-new-agent/is-correct-agent")
        .withCookies(languageCookie(language), getSessionCookie(testSessionId))
        .withFollowRedirects(follow = false)
        .get()
    )

    res.status shouldBe OK
    Jsoup.parse(res.body)

  }
  private def postIsCorrectAgentWithErrorPage(language: Language): Document = {

    commonSetup()

    val res = await(
      ws.url(s"http://localhost:$port/business-rates-property-linking/my-organisation/appoint-new-agent/is-correct-agent?backLinkUrl=%2Fbusiness-rates-property-linking%2Fmy-organisation%2Fappoint-new-agent%2Fagent-code?backLinkUrl=%2Fbusiness-rates-dashboard%2Fhome")
        .withCookies(languageCookie(language), getSessionCookie(testSessionId))
        .withFollowRedirects(follow = false)
        .withHttpHeaders(HeaderNames.COOKIE -> "sessionId", "Csrf-Token" -> "nocheck")
        .post(body = "")
    )

    res.status shouldBe BAD_REQUEST
    Jsoup.parse(res.body)

  }
  private def validPostIsCorrectAgentPage(answer: Boolean) = {

    commonSetup()

    stubFor {
      get("/property-linking/my-organisation/agents/1001/available-property-links?sortField=ADDRESS&sortOrder=ASC&startPoint=1&pageSize=15&requestTotalRowCount=false")
        .willReturn {
          aResponse.withStatus(OK).withBody(Json.toJson(testOwnerAuthResult1).toString())
        }
    }

    val requestBody = Json.obj(
      "isThisYourAgent" -> answer
    )

    await(
      ws.url(s"http://localhost:$port/business-rates-property-linking/my-organisation/appoint-new-agent/is-correct-agent?backLinkUrl=%2Fbusiness-rates-property-linking%2Fmy-organisation%2Fappoint-new-agent%2Fagent-code?backLinkUrl=%2Fbusiness-rates-dashboard%2Fhome")
        .withCookies(languageCookie(English), getSessionCookie(testSessionId))
        .withFollowRedirects(follow = false)
        .withHttpHeaders(HeaderNames.COOKIE -> "sessionId", "Csrf-Token" -> "nocheck")
        .post(body = requestBody)
    )
  }

  def commonSetup(): StubMapping = {

    implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId(testSessionId)))

    lazy val mockAppointAgentSessionRepository: AppointAgentSessionRepository = app.injector.instanceOf[AppointAgentSessionRepository]

    val searchedAgentData: SearchedAgent = SearchedAgent.apply(1001, agentName, agentAddress, AgentSelected, None)

    val selectedAgentData = SelectedAgent.apply(searchedAgentData, isTheCorrectAgent = true, None, None)

    await(mockAppointAgentSessionRepository.saveOrUpdate(selectedAgentData))
    
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

}
