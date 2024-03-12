package controllers.manageAgent

import base.{HtmlComponentHelpers, ISpecBase}
import com.github.tomakehurst.wiremock.client.WireMock.{aResponse, get, post, stubFor}
import models.propertyrepresentation.AgentSummary
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.http.Status.OK
import play.api.libs.json.Json
import play.api.test.Helpers._
import repositories.ManageAgentSessionRepository
import uk.gov.hmrc.http.{HeaderCarrier, SessionId}

import java.time.LocalDate
import java.util.UUID

class RatingListConfirmedISpec extends ISpecBase with HtmlComponentHelpers {

  val testSessionId = s"stubbed-${UUID.randomUUID}"

  lazy val mockRepository: ManageAgentSessionRepository = app.injector.instanceOf[ManageAgentSessionRepository]
  implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId(testSessionId)))

  val titleText = "The rating lists that Test Agent can act for you on has been changed - Valuation Office Agency - GOV.UK"
  val headerText = "The rating lists that Test Agent can act for you on has been changed"
  def thisAgentTextSingle(listYear: String) = s"This agent can act for you on your property valuations on the $listYear rating list, for properties that you assign to them or they add to your account"
  val thisAgentTextMultiple = "This agent can act for you on your property valuations on the 2023 and 2017 rating lists, for properties that you assign to them or they add to your account"
  val whatHappensText = "What happens next"
  val youCanText = "You can change the rating lists this agent can act for you on at any time."
  val goToYourText = "Go to your account home"

  val titleTextWelsh = "Welsh The rating lists that Test Agent can act for you on has been changed - Valuation Office Agency - GOV.UK"
  val headerTextWelsh = "Welsh The rating lists that Test Agent can act for you on has been changed"
  def thisAgentTextSingleWelsh(listYear: String) = s"Welsh This agent can act for you on your property valuations on the $listYear rating list, for properties that you assign to them or they add to your account"
  val thisAgentTextMultipleWelsh = "Welsh This agent can act for you on your property valuations on the 2023 and 2017 rating lists, for properties that you assign to them or they add to your account"
  val whatHappensTextWelsh = "Welsh What happens next"
  val youCanTextWelsh = "Welsh You can change the rating lists this agent can act for you on at any time."
  val goToYourTextWelsh = "Welsh Go to your account home"

  val headerSelector = "h1.govuk-panel__title"
  val thisAgentSelector = "#main-content > div > div > p:nth-child(2)"
  val whatHappensSelector = "h2.govuk-heading-m"
  val youCanSelector = "#main-content > div > div > p:nth-child(4)"
  val goToYourSelector = "#homeLink"

  val goHomeHref = "/business-rates-dashboard/home"

  "RatingListConfirmedController" should {
    "Show an English confirmed screen with the correct text when the user has chosen both list years and the language is set to English" which {

      lazy val document = getRatingsListConfirmedPage(English, List("2023", "2017"))
      
      s"has a title of $titleText" in {
        document.title() shouldBe s"$titleText"
      }

      s"has a panel containing a header of '$headerText'" in {
        document.select(headerSelector).text shouldBe headerText
      }

      s"has text on the screen of '$thisAgentTextMultiple'" in {
        document.select(thisAgentSelector).text() shouldBe thisAgentTextMultiple
      }

      s"has a small heading on the screen of '$whatHappensText'" in {
        document.select(whatHappensSelector).text() shouldBe whatHappensText
      }

      s"has text on the screen of '$youCanText'" in {
        document.select(youCanSelector).text() shouldBe youCanText
      }

      s"has a link on the screen of '$goToYourText'" in {
        document.select(goToYourSelector).text() shouldBe goToYourText
        document.select(goToYourSelector).attr("href") shouldBe goHomeHref
      }
    }

    "Show an English confirmed screen with the correct text when the user has chosen one list year and the language is set to English" which {

      lazy val document = getRatingsListConfirmedPage(English, List("2017"))

      s"has a title of $titleText" in {
        document.title() shouldBe s"$titleText"
      }

      s"has a panel containing a header of '$headerText'" in {
        document.select(headerSelector).text shouldBe headerText
      }

      s"has text on the screen of '${thisAgentTextSingle("2017")}'" in {
        document.select(thisAgentSelector).text() shouldBe thisAgentTextSingle("2017")
      }

      s"has a small heading on the screen of '$whatHappensText'" in {
        document.select(whatHappensSelector).text() shouldBe whatHappensText
      }

      s"has text on the screen of '$youCanText'" in {
        document.select(youCanSelector).text() shouldBe youCanText
      }

      s"has a link on the screen of '$goToYourText'" in {
        document.select(goToYourSelector).text() shouldBe goToYourText
        document.select(goToYourSelector).attr("href") shouldBe goHomeHref
      }
    }

    "Show a Welsh confirmed screen with the correct text when the user has chosen both list years and the language is set to Welsh" which {

      lazy val document = getRatingsListConfirmedPage(Welsh, List("2023", "2017"))

      s"has a title of $titleText in welsh" in {
        document.title() shouldBe s"$titleTextWelsh"
      }

      s"has a panel containing a header of '$headerText' in welsh" in {
        document.select(headerSelector).text shouldBe headerTextWelsh
      }

      s"has text on the screen of '$thisAgentTextMultiple' in welsh" in {
        document.select(thisAgentSelector).text() shouldBe thisAgentTextMultipleWelsh
      }

      s"has a small heading on the screen of '$whatHappensText' in welsh" in {
        document.select(whatHappensSelector).text() shouldBe whatHappensTextWelsh
      }

      s"has text on the screen of '$youCanText' in welsh" in {
        document.select(youCanSelector).text() shouldBe youCanTextWelsh
      }

      s"has a link on the screen of '$goToYourText' in welsh" in {
        document.select(goToYourSelector).text() shouldBe goToYourTextWelsh
        document.select(goToYourSelector).attr("href") shouldBe goHomeHref
      }
    }

    "Show a Welsh confirmed screen with the correct text when the user has chosen one list year and the language is set to Welsh" which {

      lazy val document = getRatingsListConfirmedPage(Welsh, List("2023"))

      s"has a title of $titleText in welsh" in {
        document.title() shouldBe s"$titleTextWelsh"
      }

      s"has a panel containing a header of '$headerText' in welsh" in {
        document.select(headerSelector).text shouldBe headerTextWelsh
      }

      s"has text on the screen of '${thisAgentTextSingleWelsh("2023")}' in welsh" in {
        document.select(thisAgentSelector).text() shouldBe thisAgentTextSingleWelsh("2023")
      }

      s"has a small heading on the screen of '$whatHappensText' in welsh" in {
        document.select(whatHappensSelector).text() shouldBe whatHappensTextWelsh
      }

      s"has text on the screen of '$youCanText' in welsh" in {
        document.select(youCanSelector).text() shouldBe youCanTextWelsh
      }

      s"has a link on the screen of '$goToYourText' in welsh" in {
        document.select(goToYourSelector).text() shouldBe goToYourTextWelsh
        document.select(goToYourSelector).attr("href") shouldBe goHomeHref
      }
    }

  }
  private def getRatingsListConfirmedPage(language: Language, listYears: List[String]): Document = {

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
      ws.url(s"http://localhost:$port/business-rates-property-linking/my-organisation/appoint/ratings-list/confirmed")
        .withCookies(languageCookie(language), getSessionCookie(testSessionId))
        .withFollowRedirects(follow = false)
        .get()
    )

    res.status shouldBe OK
    Jsoup.parse(res.body)
  }

}
