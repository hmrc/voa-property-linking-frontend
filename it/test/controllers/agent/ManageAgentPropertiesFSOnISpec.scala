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
import models.propertyrepresentation.AgentList
import models.searchApi.OwnerAuthResult
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.http.Status.OK
import play.api.libs.json.Json
import play.api.test.Helpers._
import repositories.ManageAgentSessionRepository
import uk.gov.hmrc.http.{HeaderCarrier, SessionId}

import java.util.UUID

class ManageAgentPropertiesFSOnISpec extends ISpecBase with HtmlComponentHelpers {

  override lazy val extraConfig: Map[String, Any] = Map(
    "feature-switch.agentListYears.enabled" -> "true"
  )

  val testSessionId = s"stubbed-${UUID.randomUUID}"
  val testAddress = "TEST ADDRESS"
  val testAddress2 = "ADDRESSTEST 2"

  lazy val mockRepository: ManageAgentSessionRepository = app.injector.instanceOf[ManageAgentSessionRepository]
  implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId(testSessionId)))

  val titleText = "Your agent - Valuation Office Agency - GOV.UK"
  val backLinkText = "Back"
  val captionText = "Agent"
  val headerText = "Test Agent"
  val manageButtonText = "Manage this agent"
  val ratingListSectionHeadingText = "Rating lists they can act on for you"
  val oneRatingListText = "This agent can act for you on your property valuations on the 2023 rating list, for properties that you assign them to or they add to your account."
  val twoRatingListText = "This agent can act for you on your property valuations on the 2023 and 2017 rating lists, for properties that you assign them to or they add to your account."
  val threeRatingListText = "This agent can act for you on your property valuations on the 2026, 2023, and 2017 rating lists, for properties that you assign them to or they add to your account."
  val assignedPropertiesHeadingText = "Assigned properties"
  val assignedToNoPropertiesText = "This agent is not assigned to any properties"

  val titleTextWelsh = "Eich asiant - Valuation Office Agency - GOV.UK"
  val backLinkTextWelsh = "Yn ôl"
  val captionTextWelsh = "Asiant"
  val headerTextWelsh = "Test Agent"
  val manageButtonTextWelsh = "Rheoli’r asiant hwn"
  val ratingListSectionHeadingTextWelsh = "Rhestrau ardrethu y gall yr asiant hwn weithredu arnynt ar eich rhan"
  val oneRatingListTextWelsh = "Gall yr asiant hwn weithredu ar eich rhan ar eich prisiadau eiddo ar restr ardrethu 2026, ac ar gyfer eiddo y mae’n eu hychwanegu at eich cyfrif."
  val twoRatingListTextWelsh = "Gall yr asiant hwn weithredu ar eich rhan ar eich prisiadau eiddo ar restrau ardrethu 2026 a 2023, ac ar gyfer eiddo y mae’n eu hychwanegu at eich cyfrif."
  val threeRatingListTextWelsh = "Gall yr asiant hwn weithredu ar eich rhan ar eich prisiadau eiddo ar restrau ardrethu 2026, 2023 a 2017, ac ar gyfer eiddo y mae’n eu hychwanegu at eich cyfrif."
  val assignedPropertiesHeadingTextWelsh = "Eiddo wedi’u neilltuo"
  val assignedToNoPropertiesTextWelsh = "Nid oes eiddo wedi’i neilltuo i’r asiant hwn." // full stop, english has none

  val backLinkTextSelector = "#back-link"
  val captionTextSelector = ".govuk-caption-l"
  val headerTextSelector = "h1.govuk-heading-l"
  val manageButtonTextSelector = ".govuk-button"
  val ratingListSectionHeadingTextSelector = ".govuk-heading-m:nth-of-type(2)"
  val ratingListTextSelector = "#ratingListText"
  val assignedPropertiesHeadingTextSelector = "h2.govuk-heading-m:nth-of-type(3)"
  def assignedPropertiesTextSelector(bulletNumber: Int) = s".govuk-list--bullet > li:nth-child($bulletNumber)"
  val assignedToNoPropertiesTextSelector = ".govuk-body:nth-child(7)"

  val backLinkHref = "/business-rates-property-linking/my-organisation/agents"
  val manageAgentButtonHref = "/business-rates-property-linking/my-organisation/manage-agent/1001"

  "ManageAgentController manageAgentProperties method" should {
    "display 'Manage Agent Properties' screen with the correct text and the language is set to English and Agent got multiple properties assigned and all three rating lists" which {

      lazy val document = getYourAgentsPropertiesPage(English, testOwnerAuthResultMultipleProperty, testAgentListMethod(Some(Seq("2026", "2023", "2017"))))

      s"has a title of $titleText" in {
        document.title() shouldBe titleText
      }

      "has a back link which takes you to 'Agent List' page" in {
        document.select(backLinkTextSelector).text() shouldBe backLinkText
        document.select(backLinkTextSelector).attr("href") shouldBe backLinkHref
      }

      "displays a correct caption above the header" in {
        document.select(captionTextSelector).text shouldBe captionText
      }

      "displays a correct header with agent name included" in {
        document.select(headerTextSelector).text() shouldBe headerText
      }

      "displays a manage agent button" in {
        document.select(manageButtonTextSelector).text() shouldBe manageButtonText
        document.select(manageButtonTextSelector).attr("href") shouldBe manageAgentButtonHref
      }

      "has heading above rating list info with correct text" in {
        document.select(ratingListSectionHeadingTextSelector).text() shouldBe ratingListSectionHeadingText
      }

      s"has text on the page of $threeRatingListText" in {
        document.select(ratingListTextSelector).text shouldBe threeRatingListText
      }

      "has heading above assigned properties list with correct text" in {
        document.select(assignedPropertiesHeadingTextSelector).text() shouldBe assignedPropertiesHeadingText
      }

      s"has a bullet point list for the two assigned properties" in {
        document.select(assignedPropertiesTextSelector(1)).text shouldBe testAddress
        document.select(assignedPropertiesTextSelector(2)).text shouldBe testAddress2
      }
    }
  }

  private def getYourAgentsPropertiesPage(
        language: Language,
        authDetails: OwnerAuthResult,
        list: AgentList
  ): Document = {

    stubFor {
      get(
        "/property-linking/my-organisation/agents/1001/property-links?sortField=ADDRESS&sortOrder=ASC&startPoint=1&pageSize=100&requestTotalRowCount=true"
      ).willReturn {
        aResponse.withStatus(OK).withBody(Json.toJson(authDetails).toString())
      }
    }

    stubFor {
      get("/property-linking/my-organisation/agent/1001")
        .willReturn {
          aResponse.withStatus(OK).withBody(Json.toJson(testAgentDetails).toString())
        }
    }

    stubFor {
      get("/property-linking/owner/agents")
        .willReturn {
          aResponse.withStatus(OK).withBody(Json.toJson(list).toString())
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
      ws.url(
        s"http://localhost:$port/business-rates-property-linking/my-organisation/manage-agent/property-links?agentCode=1001"
      ).withCookies(languageCookie(language), getSessionCookie(testSessionId))
        .withFollowRedirects(follow = false)
        .get()
    )

    res.status shouldBe OK
    Jsoup.parse(res.body)

  }
}
