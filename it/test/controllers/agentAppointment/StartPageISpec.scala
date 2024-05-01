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

package controllers.agentAppointment

import base.{HtmlComponentHelpers, ISpecBase}
import com.github.tomakehurst.wiremock.client.WireMock._
import models.propertyrepresentation._
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.http.Status.OK
import play.api.libs.json.Json
import play.api.test.Helpers._
import repositories.AppointAgentSessionRepository
import uk.gov.hmrc.http.{HeaderCarrier, SessionId}

import java.util.UUID

class StartPageISpec extends ISpecBase with HtmlComponentHelpers {

  val testSessionId = s"stubbed-${UUID.randomUUID}"

  implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId(testSessionId)))

  val titleText = "Appoint an agent to your account - Valuation Office Agency - GOV.UK"
  val backLinkText = "Back"
  val startButtonText = "Start now"
  val headerText = "Appoint an agent to your account"
  val p1Text = "When you appoint an agent to your account they can act for you. This means they can:"
  val bullet1Text = "see detailed property information"
  val bullet2Text = "see Check and Challenge case correspondence such as messages and emails"
  val bullet3Text = "send Check and Challenge cases"
  val bullet4Text = "add your properties to your account"
  val p2Text = "They can act for you on the properties you assign to them and the properties they add to your account."
  val bulletHeaderText = "You can:"
  val bullet5Text = "appoint more than one agent to your account"
  val bullet6Text = "assign more than one agent to your property"
  val bullet7Text = "choose the rating lists an agent can act on for you"
  val helpLinkText = "Help with appointing and managing agents"

  val titleTextWelsh = "Penodi asiant i’ch cyfrif - Valuation Office Agency - GOV.UK"
  val backLinkTextWelsh = "Yn ôl"
  val startButtonTextWelsh = "Dechrau nawr"
  val headerTextWelsh = "Penodi asiant i’ch cyfrif"
  val p1TextWelsh =
    "Pan fyddwch yn penodi asiant i’ch cyfrif, bydd yr asiant hwn yn gallu gweithredu ar eich rhan. Mae hyn yn golygu ei fod yn gallu:"
  val bullet1TextWelsh = "gweld gwybodaeth manwl am eiddo"
  val bullet2TextWelsh = "gweld gohebiaeth ynghylch achosion Gwirio a Herio, megis negeseuon ac e-byst"
  val bullet3TextWelsh = "anfon achosion Gwirio a Herio"
  val bullet4TextWelsh = "ychwanegu eich eiddo i’ch cyfrif"
  val p2TextWelsh =
    "Gall yr asiant weithredu ar eiddo rydych yn eu neilltuo iddo, ac ar eiddo y mae’n eu hychwanegu at eich cyfrif."
  val bulletHeaderTextWelsh = "Gallwch wneud y canlynol:"
  val bullet5TextWelsh = "penodi mwy nag un asiant i’ch cyfrif"
  val bullet6TextWelsh = "neilltuo’ch eiddo i fwy nag un asiant"
  val bullet7TextWelsh = "dewis pa restr ardrethu y gall asiant ei gweithredu ar eich rhan"
  val helpLinkTextWelsh = "Help gyda phenodi a rheoli asiantau"

  val backLinkSelector = "#back-link"
  val headerSelector = "#main-content > div > div > h1"
  val p1TextSelector = "#they-can-intro"
  val bulletListTheyCanSelector = "#they-can-list > li"
  val p2TextSelector = "#they-can-info"
  val bulletHeaderTextSelector = "#you-can-intro"
  val bulletListYouCanSelector = "#you-can-list > li"
  val helpLinkSelector = "#help-link"
  val startButtonSelector = "#start-now-button"

  val startButtonHref = "/business-rates-property-linking/my-organisation/appoint-new-agent/agent-code"
  val backLinkHref = "/business-rates-dashboard/home"
  val helpLinkHref = "https://www.gov.uk/guidance/appoint-an-agent"

  "showStartPage method" should {
    "display 'Start page' screen with the correct content for English" which {

      lazy val document = getPage(English)

      s"has a title of $titleText" in {
        document.title() shouldBe titleText
      }

      "has a back link which takes you to Manage agent properties screen" in {
        document.select(backLinkSelector).text() shouldBe backLinkText
        document.select(backLinkSelector).attr("href") shouldBe backLinkHref
      }

      "displays a correct header" in {
        document.select(headerSelector).text() shouldBe headerText
      }

      "displays a correct page content text" in {
        document.select(p1TextSelector).text() shouldBe p1Text
        document.select(bulletListTheyCanSelector).get(0).text() shouldBe bullet1Text
        document.select(bulletListTheyCanSelector).get(1).text() shouldBe bullet2Text
        document.select(bulletListTheyCanSelector).get(2).text() shouldBe bullet3Text
        document.select(bulletListTheyCanSelector).get(3).text() shouldBe bullet4Text
        document.select(p2TextSelector).text() shouldBe p2Text
        document.select(bulletHeaderTextSelector).text() shouldBe bulletHeaderText
        document.select(bulletListYouCanSelector).get(0).text() shouldBe bullet5Text
        document.select(bulletListYouCanSelector).get(1).text() shouldBe bullet6Text
        document.select(bulletListYouCanSelector).get(2).text() shouldBe bullet7Text
        document.select(helpLinkSelector).text() shouldBe helpLinkText
        document.select(helpLinkSelector).attr("href") shouldBe helpLinkHref
      }

      "displays a correct text in start button" in {
        document.select(startButtonSelector).text shouldBe startButtonText
        document.select(startButtonSelector).attr("href") shouldBe startButtonHref
      }
    }

    "display 'Start page' screen with the correct content for Welsh" which {

      lazy val document = getPage(Welsh)

      s"has a title of $titleTextWelsh" in {
        document.title() shouldBe titleTextWelsh
      }

      "has a back link which takes you to Manage agent properties screen" in {
        document.select(backLinkSelector).text() shouldBe backLinkTextWelsh
        document.select(backLinkSelector).attr("href") shouldBe backLinkHref
      }

      "displays a correct header" in {
        document.select(headerSelector).text() shouldBe headerTextWelsh
      }

      "displays a correct page content text" in {
        document.select(p1TextSelector).text() shouldBe p1TextWelsh
        document.select(bulletListTheyCanSelector).get(0).text() shouldBe bullet1TextWelsh
        document.select(bulletListTheyCanSelector).get(1).text() shouldBe bullet2TextWelsh
        document.select(bulletListTheyCanSelector).get(2).text() shouldBe bullet3TextWelsh
        document.select(bulletListTheyCanSelector).get(3).text() shouldBe bullet4TextWelsh
        document.select(p2TextSelector).text() shouldBe p2TextWelsh
        document.select(bulletHeaderTextSelector).text() shouldBe bulletHeaderTextWelsh
        document.select(bulletListYouCanSelector).get(0).text() shouldBe bullet5TextWelsh
        document.select(bulletListYouCanSelector).get(1).text() shouldBe bullet6TextWelsh
        document.select(bulletListYouCanSelector).get(2).text() shouldBe bullet7TextWelsh
        document.select(helpLinkSelector).text() shouldBe helpLinkTextWelsh
        document.select(helpLinkSelector).attr("href") shouldBe helpLinkHref
      }

      "displays a correct text in start button" in {
        document.select(startButtonSelector).text shouldBe startButtonTextWelsh
        document.select(startButtonSelector).attr("href") shouldBe startButtonHref
      }
    }
  }

  class AssignAllSetup {
    val agentCode = 1001
    val agentName = "Test Agent"

    stubFor {
      post("/property-linking/my-organisation/agent/submit-appointment-changes")
        .willReturn {
          aResponse.withStatus(OK).withBody(Json.toJson(AgentAppointmentChangesResponse("some-id")).toString())
        }
    }

    stubFor {
      get(
        "/property-linking/owner/property-links?sortField=ADDRESS&sortOrder=ASC&startPoint=1&pageSize=100&requestTotalRowCount=false")
        .willReturn {
          aResponse.withStatus(OK).withBody(Json.toJson(testOwnerAuthResult).toString())
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
  }

  def getPage(language: Language): Document = {

    lazy val mockAppointAgentSessionRepository: AppointAgentSessionRepository =
      app.injector.instanceOf[AppointAgentSessionRepository]
    val startCacheData: Start = Start(status = StartJourney, backLink = Some("/business-rates-dashboard/home"))

    await(mockAppointAgentSessionRepository.saveOrUpdate(startCacheData))

    stubFor {
      get("/property-linking/owner/agents")
        .willReturn {
          aResponse.withStatus(OK).withBody(Json.toJson(testAgentListFor2017).toString())
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
      ws.url(s"http://localhost:$port/business-rates-property-linking/my-organisation/appoint-new-agent/start")
        .withCookies(languageCookie(language), getSessionCookie(testSessionId))
        .withFollowRedirects(follow = false)
        .get()
    )

    res.status shouldBe OK
    Jsoup.parse(res.body)
  }
}
