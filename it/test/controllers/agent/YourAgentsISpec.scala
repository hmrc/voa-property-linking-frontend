/*
 * Copyright 2024 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
  val twoListYearsText = "2023 and 2017 rating lists"
  val threeListYearsText = "2026, 2023, and 2017 rating lists"
  val listYear2023Text = "2023 rating list"
  val ofText = "of"
  val noAgentsText = "You have no agents."

  val titleTextWelsh = "Eich asiantiaid - Valuation Office Agency - GOV.UK"
  val backLinkTextWelsh = "Yn ôl"
  val headingTextWelsh = "Eich asiantiaid"
  val appointAgentLinkTextWelsh = "Penodi asiant"
  val helpWithAppointingLinkTextWelsh = "Help gyda phenodi a rheoli asiantau"
  val agentTableHeadingTextWelsh = "Asiant"
  val ratingListTableHeadingTextWelsh = "Rhestrau ardrethu y gall yr asiant hwn weithredu arnynt ar eich rhan"
  val assignedToTableHeadingTextWelsh = "Neilltuwyd i"
  val twoListYearsTextWelsh = "Rhestrau ardrethu 2023 a 2017"
  val threeListYearsTextWelsh = "Rhestrau ardrethu 2026, 2023, a 2017"
  val listYear2023TextWelsh = "Rhestr ardrethu 2023"
  val ofTextWelsh = "o"
  val noAgentsTextWelsh = "Does gennych chi ddim asiantiaid."

  val backLinkSelector = "#back-link"
  val headingSelector = "h1.govuk-heading-l"
  val appointAgentLinkSelector = "#add-agent-link"
  val helpWithAppointingLinkSelector = "#help-with-agent-link"
  val agentTableHeadingSelector = ".govuk-table__header:nth-child(1)"
  val ratingListTableHeadingSelector = ".govuk-table__header:nth-child(2)"
  val assignedToTableHeadingSelector = ".govuk-table__header:nth-child(3)"
  val agentNameSelector: Int => String = (row: Int) =>
    s".govuk-table__row:nth-child($row) > .govuk-table__cell:nth-child(1) > a"
  val ratingListSelector: Int => String = (row: Int) =>
    s".govuk-table__row:nth-child($row) > .govuk-table__cell:nth-child(2)"
  val assignedPropertiesSelector: Int => String = (row: Int) =>
    s".govuk-table__row:nth-child($row) > .govuk-table__cell:nth-child(3)"
  val noAgentsSelector = "#no-agents"

  val backLinkHref = s"/business-rates-dashboard/home"
  val agentHref: Int => String = (agentCode: Int) =>
    s"/business-rates-property-linking/my-organisation/manage-agent/property-links?agentCode=$agentCode"
  val appointAgentHref = "/business-rates-property-linking/my-organisation/appoint-new-agent"
  val helpWithAppointingHref = "https://www.gov.uk/guidance/appoint-an-agent"

  "ManageAgentController showAgents method" should {
    "display the 'Your agents' page with the correct text in English when the user has multiple agents appointed with all three list year variations" which {
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

      s"has a '$helpWithAppointingLinkText' link that takes you to the Gov.uk Guidance page" in {
        document.select(helpWithAppointingLinkSelector).text() shouldBe helpWithAppointingLinkText
        document.select(helpWithAppointingLinkSelector).attr("href") shouldBe helpWithAppointingHref
      }

      s"has a table with the headings '$agentTableHeadingText', '$ratingListTableHeadingText' and '$assignedToTableHeadingText'" in {
        document.select(agentTableHeadingSelector).text() shouldBe agentTableHeadingText
        document.select(ratingListTableHeadingSelector).text() shouldBe ratingListTableHeadingText
        document.select(assignedToTableHeadingSelector).text() shouldBe assignedToTableHeadingText
      }

      s"has the correct table information for row one ($agentName, $twoListYearsText, 10 $ofText 10 properties)" in {
        document.select(agentNameSelector(1)).text() shouldBe agentName
        document.select(agentNameSelector(1)).attr("href") shouldBe agentHref(1001)
        document.select(ratingListSelector(1)).text shouldBe twoListYearsText
        document.select(assignedPropertiesSelector(1)).text shouldBe s"10 $ofText 10"
      }

      s"has the correct table information for row two (${agentName}2, $listYear2023Text, 5 $ofText 10 properties)" in {
        document.select(agentNameSelector(2)).text() shouldBe s"${agentName}2"
        document.select(agentNameSelector(2)).attr("href") shouldBe agentHref(1002)
        document.select(ratingListSelector(2)).text shouldBe listYear2023Text
        document.select(assignedPropertiesSelector(2)).text shouldBe s"5 $ofText 10"
      }

      s"has the correct table information for row three (${agentName}3, $threeListYearsText, 5 $ofText 10 properties)" in {
        document.select(agentNameSelector(3)).text() shouldBe s"${agentName}3"
        document.select(agentNameSelector(3)).attr("href") shouldBe agentHref(1003)
        document.select(ratingListSelector(3)).text shouldBe threeListYearsText
        document.select(assignedPropertiesSelector(3)).text shouldBe s"5 $ofText 10"
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

      s"has a '$helpWithAppointingLinkText' link that takes you to the Gov.uk Guidance page" in {
        document.select(helpWithAppointingLinkSelector).text() shouldBe helpWithAppointingLinkText
        document.select(helpWithAppointingLinkSelector).attr("href") shouldBe helpWithAppointingHref
      }

      s"doesn't have a table with the headings '$agentTableHeadingText', '$ratingListTableHeadingText' and '$assignedToTableHeadingText'" in {
        document.select(agentTableHeadingSelector).size() shouldBe 0
        document.select(ratingListTableHeadingSelector).size() shouldBe 0
        document.select(assignedToTableHeadingSelector).size() shouldBe 0
      }

      s"has the text $noAgentsText" in {
        document.select(noAgentsSelector).text() shouldBe noAgentsText
      }
    }

    "display the 'Your agents' page with the correct text in Welsh when the user has multiple agents appointed with all three list year variations" which {
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

      s"has a '$helpWithAppointingLinkText' link that takes you to the Gov.uk Guidance page in Welsh" in {
        document.select(helpWithAppointingLinkSelector).text() shouldBe helpWithAppointingLinkTextWelsh
        document.select(helpWithAppointingLinkSelector).attr("href") shouldBe helpWithAppointingHref
      }

      s"has a table with the headings '$agentTableHeadingText', '$ratingListTableHeadingText' and '$assignedToTableHeadingText' in Welsh" in {
        document.select(agentTableHeadingSelector).text() shouldBe agentTableHeadingTextWelsh
        document.select(ratingListTableHeadingSelector).text() shouldBe ratingListTableHeadingTextWelsh
        document.select(assignedToTableHeadingSelector).text() shouldBe assignedToTableHeadingTextWelsh
      }

      s"has the correct table information for row one ($agentName, $twoListYearsText, 10 $ofText 10 properties) in Welsh" in {
        document.select(agentNameSelector(1)).text() shouldBe agentName
        document.select(agentNameSelector(1)).attr("href") shouldBe agentHref(1001)
        document.select(ratingListSelector(1)).text shouldBe twoListYearsTextWelsh
        document.select(assignedPropertiesSelector(1)).text shouldBe s"10 $ofTextWelsh 10"
      }

      s"has the correct table information for row two (${agentName}2, $listYear2023Text, 5 $ofText 10 properties) in Welsh" in {
        document.select(agentNameSelector(2)).text() shouldBe s"${agentName}2"
        document.select(agentNameSelector(2)).attr("href") shouldBe agentHref(1002)
        document.select(ratingListSelector(2)).text shouldBe listYear2023TextWelsh
        document.select(assignedPropertiesSelector(2)).text shouldBe s"5 $ofTextWelsh 10"
      }

      s"has the correct table information for row three (${agentName}3, $threeListYearsText, 5 $ofText 10 properties) in Welsh" in {
        document.select(agentNameSelector(3)).text() shouldBe s"${agentName}3"
        document.select(agentNameSelector(3)).attr("href") shouldBe agentHref(1003)
        document.select(ratingListSelector(3)).text shouldBe threeListYearsTextWelsh
        document.select(assignedPropertiesSelector(3)).text shouldBe s"5 $ofTextWelsh 10"
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

      s"has a '$helpWithAppointingLinkText' link that takes you to the Gov.uk Guidance page in Welsh" in {
        document.select(helpWithAppointingLinkSelector).text() shouldBe helpWithAppointingLinkTextWelsh
        document.select(helpWithAppointingLinkSelector).attr("href") shouldBe helpWithAppointingHref
      }

      s"doesn't have a table with the headings '$agentTableHeadingText', '$ratingListTableHeadingText' and '$assignedToTableHeadingText' in Welsh" in {
        document.select(agentTableHeadingSelector).size() shouldBe 0
        document.select(ratingListTableHeadingSelector).size() shouldBe 0
        document.select(assignedToTableHeadingSelector).size() shouldBe 0
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
