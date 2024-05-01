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

class AgentCodeISpec extends ISpecBase with HtmlComponentHelpers {

  val testSessionId = s"stubbed-${UUID.randomUUID}"

  implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId(testSessionId)))

  val titleText = "What is your agent’s code? - Valuation Office Agency - GOV.UK"
  val backLinkText = "Back"
  val continueButtonText = "Continue"
  val captionText = "Appoint an agent"
  val headerText = "What is your agent’s code?"
  val hintText = "This is a number given to the agent by the Valuation Office Agency."

  val titleTextWelsh = "Beth yw cod eich asiant? - Valuation Office Agency - GOV.UK"
  val backLinkTextWelsh = "Yn ôl"
  val continueButtonTextWelsh = "Yn eich blaen"
  val captionTextWelsh = "Penodi asiant"
  val headerTextWelsh = "Beth yw cod eich asiant?"
  val hintTextWelsh = "Dyma’r rhif a roddir i’r asiant gan Asiantaeth y Swyddfa Brisio."

  val backLinkSelector = "#back-link"
  val captionSelector = ".govuk-caption-l"
  val headerSelector = "#agentCode-label"
  val hintTextSelector = "#agentCode-hint"
  val continueButtonSelector = "button.govuk-button"

  val backLinkHref = s"/business-rates-property-linking/my-organisation/appoint-new-agent/start"
  val backLinkFromCyaChangeLinkHref =
    s"/business-rates-property-linking/my-organisation/appoint-new-agent/check-your-answers"

  "AddAgentController showAgentCodePage method" should {
    "display 'Agent code' screen with the correct text, correct text input and the language" which {

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

      "displays a correct hint text" in {
        document.select(hintTextSelector).text() shouldBe hintText
      }

      "displays a correct caption above the header" in {
        document.select(captionSelector).text shouldBe captionText
      }

      "displays a correct text in continue button" in {
        document.select(continueButtonSelector).text shouldBe continueButtonText
      }
    }

    "display 'Agent code' screen with the correct back link location when coming from CYA change link" which {

      lazy val document = getPage(English, true)

      "has a back link which takes you to Manage agent properties screen" in {
        document.select(backLinkSelector).text() shouldBe backLinkText
        document.select(backLinkSelector).attr("href") shouldBe backLinkFromCyaChangeLinkHref
      }

    }

    "display 'Agent code' screen with the correct text, correct text input and the language for Welsh" which {

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

      "displays a correct hint text" in {
        document.select(hintTextSelector).text() shouldBe hintTextWelsh
      }

      "displays a correct caption above the header" in {
        document.select(captionSelector).text shouldBe captionTextWelsh
      }

      "displays a correct text in continue button" in {
        document.select(continueButtonSelector).text shouldBe continueButtonTextWelsh
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

  private def getPage(language: Language, fromCyaPage: Boolean = false): Document = {

    lazy val mockAppointAgentSessionRepository: AppointAgentSessionRepository =
      app.injector.instanceOf[AppointAgentSessionRepository]
    implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId(testSessionId)))

    val startCacheData: Start = Start(status = StartJourney, backLink = Some("some/back/link"))

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
    val url = if (fromCyaPage) "agent-code?fromCyaChange=true" else "agent-code"
    val res = await(
      ws.url(s"http://localhost:$port/business-rates-property-linking/my-organisation/appoint-new-agent/$url")
        .withCookies(languageCookie(language), getSessionCookie(testSessionId))
        .withFollowRedirects(follow = false)
        .get()
    )

    res.status shouldBe OK
    Jsoup.parse(res.body)
  }
}
