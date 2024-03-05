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

class YourAgentsFlagOffISpec extends ISpecBase with HtmlComponentHelpers {

  override lazy val extraConfig: Map[String, Any] = Map(
    "feature-switch.agentListYears.enabled" -> "false"
  )

  val testSessionId = s"stubbed-${UUID.randomUUID}"
  val agentName = "Test Agent"

  implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId(testSessionId)))

  val titleText = "Your agents - Valuation Office Agency - GOV.UK"
  val backLinkText = "Back"
  val headingText = "Your agents"
  val appointAgentLinkText = "Appoint an agent"
  val helpWithAppointingLinkText = "Help with appointing and managing agents"
  val agentTableHeadingText = "Agent"
  val assignedToTableHeadingText = "Assigned to"
  val actionTableHeadingText = "Action"
  val ofPropertiesText: (Int, Int) => String = (x: Int, y: Int) => s"$x of $y properties"
  val viewText = "View"
  val noAgentsText = "You have no agents."

  val titleTextWelsh = "Eich asiantiaid - Valuation Office Agency - GOV.UK"
  val backLinkTextWelsh = "Yn ôl"
  val headingTextWelsh = "Eich asiantiaid"
  val appointAgentLinkTextWelsh = "Penodi asiant"
  val helpWithAppointingLinkTextWelsh = "Help gyda phenodi a rheoli asiantiaid"
  val agentTableHeadingTextWelsh = "Asiant"
  val assignedToTableHeadingTextWelsh = "Neilltuwyd i"
  val actionTableHeadingTextWelsh = "Gweithred"
  val ofPropertiesTextWelsh: (Int, Int) => String = (x: Int, y: Int) => s"$x o $y eiddo"
  val viewTextWelsh = "Gweld"
  val noAgentsTextWelsh = "Does gennych chi ddim asiantiaid."

  val backLinkSelector = "#back-link"
  val headingSelector = "h1.govuk-heading-l"
  val appointAgentLinkSelector = "#add-agent-link"
  val helpWithAppointingLinkSelector = "#help-with-agent-link"
  val agentTableHeadingSelector = ".govuk-table__header:nth-child(1)"
  val assignedToTableHeadingSelector = ".govuk-table__header:nth-child(2)"
  val actionTableHeadingSelector = ".govuk-table__header:nth-child(3)"
  val agentNameSelector: Int => String = (row: Int) => s".govuk-table__row:nth-child($row) > .govuk-table__cell:nth-child(1)"
  val assignedPropertiesSelector: Int => String = (row: Int) => s".govuk-table__row:nth-child($row) > .govuk-table__cell:nth-child(2)"
  val actionSelector: Int => String = (row: Int) => s".govuk-table__row:nth-child($row) > .govuk-table__cell:nth-child(3) > a"
  val noAgentsSelector = "#no-agents"

  val backLinkHref = s"/business-rates-dashboard/home"
  val viewHref: Int => String = (agentCode: Int) => s"/business-rates-property-linking/my-organisation/manage-agent/property-links?agentCode=$agentCode"
  val appointAgentHref = "/business-rates-property-linking/my-organisation/appoint-new-agent"

  "ManageAgentController showAgents method with agentListYears flag OFF" should {
    "display the 'Your agents' page with the correct text in English when the user has agents appointed" which {
      lazy val document = getYourAgentsPage(language = English, agentsAppointed = true)

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

      s"doesn't have a '$helpWithAppointingLinkText' link" in {
        document.select(helpWithAppointingLinkSelector).size() shouldBe 0
      }

      s"has a table with the headings '$agentTableHeadingText', '$assignedToTableHeadingText' and '$actionTableHeadingText'" in {
        document.select(agentTableHeadingSelector).text() shouldBe agentTableHeadingText
        document.select(assignedToTableHeadingSelector).text() shouldBe assignedToTableHeadingText
        document.select(actionTableHeadingSelector).text shouldBe actionTableHeadingText
      }

      s"has the correct table information for row one ($agentName, ${ofPropertiesText(10, 10)}, $viewText)" in {
        document.select(agentNameSelector(1)).text() shouldBe agentName
        document.select(assignedPropertiesSelector(1)).text shouldBe ofPropertiesText(10, 10)
        document.select(actionSelector(1)).text() shouldBe viewText
        document.select(actionSelector(1)).attr("href") shouldBe viewHref(1001)
      }

      s"has the correct table information for row two (${agentName}2, ${ofPropertiesText(5, 10)}, $viewText)" in {
        document.select(agentNameSelector(2)).text() shouldBe s"${agentName}2"
        document.select(assignedPropertiesSelector(2)).text shouldBe ofPropertiesText(5, 10)
        document.select(actionSelector(2)).text() shouldBe viewText
        document.select(actionSelector(2)).attr("href") shouldBe viewHref(1002)
      }

      s"has the correct table information for row three (${agentName}3, ${ofPropertiesText(5, 10)}, $viewText)" in {
        document.select(agentNameSelector(3)).text() shouldBe s"${agentName}3"
        document.select(assignedPropertiesSelector(3)).text shouldBe ofPropertiesText(5, 10)
        document.select(actionSelector(3)).text() shouldBe viewText
        document.select(actionSelector(3)).attr("href") shouldBe viewHref(1003)
      }
    }

    "display the 'Your agents' page with the correct text in English when the user doesn't have an agent appointed" which {
      lazy val document = getYourAgentsPage(language = English, agentsAppointed = false)

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

      s"doesn't have a '$helpWithAppointingLinkText' link" in {
        document.select(helpWithAppointingLinkSelector).size() shouldBe 0
      }

      s"doesn't have a table with the headings '$agentTableHeadingText', '$assignedToTableHeadingText' and '$actionTableHeadingText'" in {
        document.select(agentTableHeadingSelector).size() shouldBe 0
        document.select(assignedToTableHeadingSelector).size() shouldBe 0
        document.select(actionTableHeadingSelector).size() shouldBe 0
      }

      s"has the text $noAgentsText" in {
        document.select(noAgentsSelector).text() shouldBe noAgentsText
      }
    }

    "display the 'Your agents' page with the correct text in Welsh when the user has agents appointed" which {
      lazy val document = getYourAgentsPage(language = Welsh, agentsAppointed = true)

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

      s"doesn't have a '$helpWithAppointingLinkText' link in Welsh" in {
        document.select(helpWithAppointingLinkSelector).size() shouldBe 0
      }

      s"has a table with the headings '$agentTableHeadingText', '$assignedToTableHeadingText' and '$actionTableHeadingText' in Welsh" in {
        document.select(agentTableHeadingSelector).text() shouldBe agentTableHeadingTextWelsh
        document.select(assignedToTableHeadingSelector).text() shouldBe assignedToTableHeadingTextWelsh
        document.select(actionTableHeadingSelector).text shouldBe actionTableHeadingTextWelsh
      }

      s"has the correct table information for row one ($agentName, ${ofPropertiesText(10, 10)}, $viewText) in Welsh" in {
        document.select(agentNameSelector(1)).text() shouldBe agentName
        document.select(assignedPropertiesSelector(1)).text shouldBe ofPropertiesTextWelsh(10, 10)
        document.select(actionSelector(1)).text() shouldBe viewTextWelsh
        document.select(actionSelector(1)).attr("href") shouldBe viewHref(1001)
      }

      s"has the correct table information for row two (${agentName}2, ${ofPropertiesText(5, 10)}, $viewText) in Welsh" in {
        document.select(agentNameSelector(2)).text() shouldBe s"${agentName}2"
        document.select(assignedPropertiesSelector(2)).text shouldBe ofPropertiesTextWelsh(5, 10)
        document.select(actionSelector(2)).text() shouldBe viewTextWelsh
        document.select(actionSelector(2)).attr("href") shouldBe viewHref(1002)
      }

      s"has the correct table information for row three (${agentName}3, ${ofPropertiesText(5, 10)}, $viewText) in Welsh" in {
        document.select(agentNameSelector(3)).text() shouldBe s"${agentName}3"
        document.select(assignedPropertiesSelector(3)).text shouldBe ofPropertiesTextWelsh(5, 10)
        document.select(actionSelector(3)).text() shouldBe viewTextWelsh
        document.select(actionSelector(3)).attr("href") shouldBe viewHref(1003)
      }
    }

    "display the 'Your agents' page with the correct text in Welsh when the user doesn't have an agent appointed" which {
      lazy val document = getYourAgentsPage(language = Welsh, agentsAppointed = false)

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

      s"doesn't have a '$helpWithAppointingLinkText' link" in {
        document.select(helpWithAppointingLinkSelector).size() shouldBe 0
      }

      s"doesn't have a table with the headings '$agentTableHeadingText', '$assignedToTableHeadingText' and '$actionTableHeadingText' in Welsh" in {
        document.select(agentTableHeadingSelector).size() shouldBe 0
        document.select(assignedToTableHeadingSelector).size() shouldBe 0
        document.select(actionTableHeadingSelector).size() shouldBe 0
      }

      s"has the text $noAgentsText in Welsh" in {
        document.select(noAgentsSelector).text() shouldBe noAgentsTextWelsh
      }
    }
  }

  private def getYourAgentsPage(language: Language, agentsAppointed: Boolean): Document = {
    val agentList = if (agentsAppointed) testAgentList else noAgentsList
    stubFor {
      get("/property-linking/owner/agents")
        .willReturn {
          aResponse.withStatus(OK).withBody(Json.toJson(agentList).toString())
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
