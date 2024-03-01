package controllers.agent

import base.{HtmlComponentHelpers, ISpecBase}
import com.github.tomakehurst.wiremock.client.WireMock.{aResponse, get, post, stubFor}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.http.Status.OK
import play.api.libs.json.Json
import play.api.test.Helpers._
import uk.gov.hmrc.http.{HeaderCarrier, SessionId}

import java.util.UUID

class YourAgentsISpec extends ISpecBase with HtmlComponentHelpers {

  val testSessionId = s"stubbed-${UUID.randomUUID}"
  val agentName = "Test Agent"

  implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId(testSessionId)))

  val titleText = "Your agents - Valuation Office Agency - GOV.UK"
  val backLinkText = "Back"
  val headingText = "Your agents"
  val appointAgentLinkText = "Appoint an agent"
  val helpWithAppointingLinkText = "Help with appointing and managing agents"
  val agentTableHeadingText = "Agent"
  val ratingListTableHeadingText = "Rating lists they can act on for you"
  val assignedToTableHeadingText = "Assigned to"
  val bothListYearsText = "2023 and 2017 rating lists"
  val listYear2017Text = "2017 rating list"
  val listYear2023Text = "2023 rating list"
  val ofText = "of"

  val titleTextWelsh = "Eich asiantiaid - Valuation Office Agency - GOV.UK"
  val backLinkTextWelsh = "Yn ôl"
  val headingTextWelsh = "Eich asiantiaid"
  val appointAgentLinkTextWelsh = "Penodi asiant"
  val helpWithAppointingLinkTextWelsh = "Help gyda phenodi a rheoli asiantiaid"
  val agentTableHeadingTextWelsh = "Asiant"
  val ratingListTableHeadingTextWelsh = "Rhestrau ardrethu y gallant weithredu arnynt ar eich rhan"
  val assignedToTableHeadingTextWelsh = "Neilltuwyd i"
  val bothListYearsTextWelsh = "Rhestrau ardrethu 2023 a 2017"
  val listYear2017TextWelsh = "Rhestrau ardrethu 2017"
  val listYear2023TextWelsh = "Rhestrau ardrethu 2023"
  val ofTextWelsh = "o"

  val backLinkSelector = "#back-link"
  val headingSelector = "h1.govuk-heading-l"
  val appointAgentLinkSelector = "#add-agent-link"
  val helpWithAppointingLinkSelector = "#help-with-agent-link"
  val agentTableHeadingSelector = ".govuk-table__header:nth-child(1)"
  val ratingListTableHeadingSelector = ".govuk-table__header:nth-child(2)"
  val assignedToTableHeadingSelector = ".govuk-table__header:nth-child(3)"
  val agentNameSelector: Int => String = (row: Int) => s".govuk-table__row:nth-child($row) > .govuk-table__cell:nth-child(1) > a"
  val ratingListSelector: Int => String = (row: Int) => s".govuk-table__row:nth-child($row) > .govuk-table__cell:nth-child(2)"
  val assignedPropertiesSelector: Int => String = (row: Int) => s".govuk-table__row:nth-child($row) > .govuk-table__cell:nth-child(3)"

  val backLinkHref = s"/business-rates-dashboard/home"
  val agentHref: Int => String = (agentCode: Int) => s"/business-rates-property-linking/my-organisation/manage-agent/property-links?agentCode=$agentCode"
  val appointAgentHref = "/business-rates-property-linking/my-organisation/appoint-new-agent"
  val helpWithAppointingHref = "https://www.gov.uk/guidance/appoint-an-agent"

  "ManageAgentController showAgents method" should {
    "display the 'Your agents' page with the correct text in English" which {
      lazy val document = getYourAgentsPage(English)

      s"has a title of $titleText" in {
        document.title() shouldBe titleText
      }

      "has a back link that takes you to home page" in {
        document.select(backLinkSelector).text() shouldBe backLinkText
        document.select(backLinkSelector).attr("href") shouldBe backLinkHref
      }

      s"has a heading of $headingText" in {
        document.select(headingSelector).text shouldBe headingText
      }

      s"has an '$appointAgentLinkText' link that takes you to the 'Appoint an agent to your account' page" in {
        document.select(appointAgentLinkSelector).text() shouldBe appointAgentLinkText
        document.select(appointAgentLinkSelector).attr("href") shouldBe appointAgentHref
      }

      s"has a '$helpWithAppointingLinkText' link that takes you to the Gov.uk Guidance page" in {
        document.select(helpWithAppointingLinkSelector).text() shouldBe helpWithAppointingLinkText
        document.select(helpWithAppointingLinkSelector).attr("href") shouldBe helpWithAppointingHref
      }

      s"has a table with the headings '$agentTableHeadingText', '$ratingListTableHeadingText' and '$assignedToTableHeadingText'" in {
        document.select(agentTableHeadingSelector).text() shouldBe agentTableHeadingText
        document.select(ratingListTableHeadingSelector).text() shouldBe ratingListTableHeadingText
        document.select(assignedToTableHeadingSelector).text() shouldBe assignedToTableHeadingText
      }

      s"has the correct table information for row one ($agentName, $bothListYearsText, 10 $ofText 10 properties)" in {
        document.select(agentNameSelector(1)).text() shouldBe agentName
        document.select(agentNameSelector(1)).attr("href") shouldBe agentHref(1001)
        document.select(ratingListSelector(1)).text shouldBe bothListYearsText
        document.select(assignedPropertiesSelector(1)).text shouldBe s"10 $ofText 10"
      }

      s"has the correct table information for row two (${agentName}2, $listYear2023Text, 5 $ofText 10 properties)" in {
        document.select(agentNameSelector(2)).text() shouldBe s"${agentName}2"
        document.select(agentNameSelector(2)).attr("href") shouldBe agentHref(1002)
        document.select(ratingListSelector(2)).text shouldBe listYear2023Text
        document.select(assignedPropertiesSelector(2)).text shouldBe s"5 $ofText 10"
      }

      s"has the correct table information for row three (${agentName}3, $listYear2017Text, 5 $ofText 10 properties)" in {
        document.select(agentNameSelector(3)).text() shouldBe s"${agentName}3"
        document.select(agentNameSelector(3)).attr("href") shouldBe agentHref(1003)
        document.select(ratingListSelector(3)).text shouldBe listYear2017Text
        document.select(assignedPropertiesSelector(3)).text shouldBe s"5 $ofText 10"
      }
    }

    "display the 'Your agents' page with the correct text in Welsh" which {
      lazy val document = getYourAgentsPage(Welsh)

      s"has a title of $titleText in Welsh" in {
        document.title() shouldBe titleTextWelsh
      }

      "has a back link that takes you to home page in Welsh" in {
        document.select(backLinkSelector).text() shouldBe backLinkTextWelsh
        document.select(backLinkSelector).attr("href") shouldBe backLinkHref
      }

      s"has a heading of $headingText in Welsh" in {
        document.select(headingSelector).text shouldBe headingTextWelsh
      }

      s"has an '$appointAgentLinkText' link that takes you to the 'Appoint an agent to your account' page in Welsh" in {
        document.select(appointAgentLinkSelector).text() shouldBe appointAgentLinkTextWelsh
        document.select(appointAgentLinkSelector).attr("href") shouldBe appointAgentHref
      }

      s"has a '$helpWithAppointingLinkText' link that takes you to the Gov.uk Guidance page in Welsh" in {
        document.select(helpWithAppointingLinkSelector).text() shouldBe helpWithAppointingLinkTextWelsh
        document.select(helpWithAppointingLinkSelector).attr("href") shouldBe helpWithAppointingHref
      }

      s"has a table with the headings '$agentTableHeadingText', '$ratingListTableHeadingText' and '$assignedToTableHeadingText' in Welsh" in {
        document.select(agentTableHeadingSelector).text() shouldBe agentTableHeadingTextWelsh
        document.select(ratingListTableHeadingSelector).text() shouldBe ratingListTableHeadingTextWelsh
        document.select(assignedToTableHeadingSelector).text() shouldBe assignedToTableHeadingTextWelsh
      }

      s"has the correct table information for row one ($agentName, $bothListYearsText, 10 $ofText 10 properties) in Welsh" in {
        document.select(agentNameSelector(1)).text() shouldBe agentName
        document.select(agentNameSelector(1)).attr("href") shouldBe agentHref(1001)
        document.select(ratingListSelector(1)).text shouldBe bothListYearsTextWelsh
        document.select(assignedPropertiesSelector(1)).text shouldBe s"10 $ofTextWelsh 10"
      }

      s"has the correct table information for row two (${agentName}2, $listYear2023Text, 5 $ofText 10 properties) in Welsh" in {
        document.select(agentNameSelector(2)).text() shouldBe s"${agentName}2"
        document.select(agentNameSelector(2)).attr("href") shouldBe agentHref(1002)
        document.select(ratingListSelector(2)).text shouldBe listYear2023TextWelsh
        document.select(assignedPropertiesSelector(2)).text shouldBe s"5 $ofTextWelsh 10"
      }

      s"has the correct table information for row three (${agentName}3, $listYear2017Text, 5 $ofText 10 properties) in Welsh" in {
        document.select(agentNameSelector(3)).text() shouldBe s"${agentName}3"
        document.select(agentNameSelector(3)).attr("href") shouldBe agentHref(1003)
        document.select(ratingListSelector(3)).text shouldBe listYear2017TextWelsh
        document.select(assignedPropertiesSelector(3)).text shouldBe s"5 $ofTextWelsh 10"
      }
    }
  }

  private def getYourAgentsPage(language: Language): Document = {
    stubFor {
      get("/property-linking/owner/agents")
        .willReturn {
          aResponse.withStatus(OK).withBody(Json.toJson(testAgentList).toString())
        }
    }

    stubFor {
      get("/property-linking/owner/property-links/count")
        .willReturn {
          aResponse.withStatus(OK).withBody(Json.toJson(10).toString())
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
