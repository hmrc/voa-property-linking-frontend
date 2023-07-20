package controllers.propertyLinking

import base.{HtmlComponentHelpers, ISpecBase}
import com.github.tomakehurst.wiremock.client.WireMock.{aResponse, get, post, stubFor}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.http.Status.OK
import play.api.libs.json.Json
import play.api.test.Helpers._
import uk.gov.hmrc.http.{HeaderCarrier, SessionId}

import java.util.UUID

class MyAgentISpec extends ISpecBase with HtmlComponentHelpers {


  val testSessionId = s"stubbed-${UUID.randomUUID}"

  implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId(testSessionId)))

  val titleText = "Your agents - Valuation Office Agency - GOV.UK"
  val backLinkText = "Back"
  val headingText = "Your agents"
  val appointLinkText = "Appoint an agent"
  val helpWithAppointingLink = "Help with appointing and managing agents"
  val agentNameHeader = "Agent"
  val ratingListHeader = "Rating lists they can act on for you"
  val assignedPropertyHeader = "Assigned to"
  val ratingListValueMultiple = "2023 and 2017 rating lists"

  def agentNameValue(name: String) = s"$name"

  def ratingListValueSingle(listYear: String) = s"$listYear rating list"

  def ratingListValueSingleWelsh(listYear: String) = s"$listYear rating list"


  val titleTextWelsh = "Eich asiantiaid - Valuation Office Agency - GOV.UK"
  val backLinkTextWelsh = "Yn ôl"
  val headingTextWelsh = "Eich asiantiaid"
  val appointLinkTextWelsh = "Penodi asiant"
  val helpWithAppointingLinkWelsh = "Help with appointing and managing agents"
  val agentNameHeaderWelsh = "Asiant"
  val ratingListHeaderWelsh = "Rating lists they can act on for you"
  val assignedPropertyHeaderWelsh = "Neilltuwyd i"
  val ratingListValueMultipleWelsh = "2023 and 2017 rating lists"

  val backLinkSelector = "#back-link"
  val headingSelector = "h1.govuk-heading-l"
  val appointLinkSelector = "#add-agent-link"
  val helpWithAppointingLinkSelector = "#help-with-agent-link"
  val agentNameHeaderSelector = ".govuk-table__header:nth-child(1)"
  val ratingListHeaderSelector = ".govuk-table__header:nth-child(2)"
  val assignedPropertyHeaderSelector = ".govuk-table__header:nth-child(3)"
  val agentNameValueSelector = ".govuk-table__row:nth-child(1) > .govuk-table__cell:nth-child(1)"
  val ratingListValueSelector = ".govuk-table__row:nth-child(1) > .govuk-table__cell:nth-child(2)" // 2023 and 2017
  val ratingListValueSelectorFor2023value= ".govuk-table__row:nth-child(2) > .govuk-table__cell:nth-child(2)" // 2023 only
  val ratingListValueSelectorFor2017Value = ".govuk-table__row:nth-child(3) > .govuk-table__cell:nth-child(2)"  // 2017 only
  val agentNameLinkSelector = "TODO"

  val backLinkHref = s"/business-rates-dashboard/home"
  val agentNameValueHref = "/business-rates-property-linking/my-organisation/manage-agent/property-links?agentCode=100"
  val appointLinkHref = "/business-rates-property-linking/my-organisation/appoint-new-agent"
  val helpHref = "https://www.gov.uk/guidance/appoint-an-agent"

  "ManageAgentController showAgents method" should {
    "display 'Your agents' screen with the correct text and the language is set to English" which {

      lazy val document = getYourAgentsPage(English, List("2023", "2017"),2)

      s"has a title of $titleText" in {
        document.title() shouldBe titleText
      }

      "has a back link which takes you to 'Your business rates valuation account'- dashboard home page" in {
        document.select(backLinkSelector).text() shouldBe backLinkText
        document.select(backLinkSelector).attr("href") shouldBe backLinkHref
      }

      s"has a header of $headingText" in {
        document.select(headingSelector).text shouldBe headingText
      }

      "has a 'Appoint an agent' link which takes you to the .govuk Guidance page" in {
        document.select(appointLinkSelector).text() shouldBe appointLinkText
        document.select(appointLinkSelector).attr("href") shouldBe appointLinkHref
      }

      "has a 'Help' link which takes you to the 'Appoint an agent to your account' page" in {
        document.select(appointLinkSelector).text() shouldBe appointLinkText
        document.select(appointLinkSelector).attr("href") shouldBe appointLinkHref
      }

      "has a table with correct headings" in {
        document.select(agentNameHeaderSelector).text() shouldBe agentNameHeader
        document.select(ratingListHeaderSelector).text() shouldBe ratingListHeader
        document.select(assignedPropertyHeaderSelector).text() shouldBe assignedPropertyHeader
      }

      "'Agent' cell value match agent name and link takes you 'Manage this agent' view " in {
        document.select(agentNameValueSelector).text() shouldBe agentNameValue(name = "Test Agent")
      }

      "'Rating list' cell value displays correct message " in {
        document.select(ratingListValueSelector).text shouldBe ratingListValueMultiple
      }
    }
    "display 'Your agents' screen with the correct text and the language is set to English and rating list is 2023" which {
      "'Rating list' cell value displays correct message " in {

        lazy val document = getYourAgentsPage(English, List("2023"),2)

        document.select(ratingListValueSelectorFor2023value).text() shouldBe ratingListValueSingle(listYear = "2023")
      }
    }

    "display 'Your agents' screen with the correct text and the language is set to English and rating list is 2017" which {
      "'Rating list' cell value displays correct message " in {

        lazy val document = getYourAgentsPage(English, List("2017"), 2)

        document.select(ratingListValueSelectorFor2017Value).text() shouldBe ratingListValueSingle(listYear = "2017")
      }
    }
    "display 'Your agents' screen with the correct text and the language is set to Welsh" which {

      lazy val document = getYourAgentsPage(Welsh, List("2023", "2017"), 2)

      s"has a title of $titleText" in {
        document.title() shouldBe titleTextWelsh
      }

      "has a back link which takes you to 'Your business rates valuation account'- dashboard home page" in {
        document.select(backLinkSelector).text() shouldBe backLinkTextWelsh
        document.select(backLinkSelector).attr("href") shouldBe backLinkHref
      }

      s"has a header of $headingText" in {
        document.select(headingSelector).text shouldBe headingTextWelsh
      }

      "has a 'Appoint an agent' link which takes you to the .govuk Guidance page" in {
        document.select(appointLinkSelector).text() shouldBe appointLinkTextWelsh
        document.select(appointLinkSelector).attr("href") shouldBe appointLinkHref
      }

      "has a 'Help' link which takes you to the 'Appoint an agent to your account' page" in {
        document.select(appointLinkSelector).text() shouldBe appointLinkTextWelsh
        document.select(appointLinkSelector).attr("href") shouldBe appointLinkHref
      }

      "has a table with correct headings" in {
        document.select(agentNameHeaderSelector).text() shouldBe agentNameHeaderWelsh
        document.select(ratingListHeaderSelector).text() shouldBe ratingListHeaderWelsh
        document.select(assignedPropertyHeaderSelector).text() shouldBe assignedPropertyHeaderWelsh
      }


      "'Rating list' cell value displays correct message " in {
        document.select(ratingListValueSelector).text() shouldBe ratingListValueMultipleWelsh
      }
    }
    "display 'Your agents' screen with the correct text and the language is set to Welsh and rating list is 2023" which {
      "'Rating list' cell value displays correct message " in {

        lazy val document = getYourAgentsPage(Welsh, List("2023"), 2)

        document.select(ratingListValueSelectorFor2023value).text() shouldBe ratingListValueSingleWelsh(listYear = "2023")
      }
    }

    "display 'Your agents' screen with the correct text and the language is set to Welsh and rating list is 2017" which {
      "'Rating list' cell value displays correct message " in {

        lazy val document = getYourAgentsPage(Welsh, List("2017"), 2)

        document.select(ratingListValueSelectorFor2017Value).text() shouldBe ratingListValueSingleWelsh(listYear = "2017")
      }
    }
  }

  private def getYourAgentsPage(language: Language, listYears: List[String], resultCount: Int): Document = {



    stubFor {
      get("/property-linking/owner/agents")
        .willReturn {
          aResponse.withStatus(OK).withBody(Json.toJson(testAgentList).toString())
        }
    }

    stubFor {
      get("/property-linking/owner/property-links/count")
        .willReturn {
          aResponse.withStatus(OK).withBody(Json.toJson(testResultCount).toString())
        }
    }

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
      ws.url(s"http://localhost:$port/business-rates-property-linking/my-organisation/agents")
        .withCookies(languageCookie(language), getSessionCookie(testSessionId))
        .withFollowRedirects(follow = false)
        .get()
    )

    res.status shouldBe OK
    Jsoup.parse(res.body)

  }
}
