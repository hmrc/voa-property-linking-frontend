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
import models.propertyrepresentation.AgentSummary
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.http.HeaderNames
import play.api.http.Status.OK
import play.api.libs.json.Json
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import repositories.ManageAgentSessionRepository
import uk.gov.hmrc.http.{HeaderCarrier, SessionId}

import java.time.LocalDate
import java.util.UUID

class ConfirmRemoveAgentFromOrgISpec extends ISpecBase with HtmlComponentHelpers {

  val testSessionId = s"stubbed-${UUID.randomUUID}"

  lazy val mockRepository: ManageAgentSessionRepository = app.injector.instanceOf[ManageAgentSessionRepository]
  implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId(testSessionId)))

  val agentCode = 1L
  val agentName = "Test Name"

  val headingSelector = "h1"
  val theAgentSelector = "#remove-agent-confirmation-p1"
  val whatHappensSelector = "#main-content > div > div > h2"
  val ifYouWantSelector = "#remove-agent-confirmation-p2"
  val agentCodeSelector = "#remove-agent-confirmation-p2 > strong"
  val goToLinkSelector = "#main-content > div > div > p:nth-child(5) > a"

  val titleText = s"$agentName has been removed from your account - Valuation Office Agency - GOV.UK"
  val headingText = s"$agentName has been removed from your account"
  val theAgentText = "The agent can no longer act for you."
  val whatHappensText = "What happens next"
  val ifYouWantText =
    s"If you want the agent to act for you again, you can reappoint them to your account using agent code ${agentCode.toString}."
  val goToLinkText = "Go to your account home"

  val titleTextWelsh = s"Mae $agentName wedi’i dynnu o’ch cyfrif - Valuation Office Agency - GOV.UK"
  val headingTextWelsh = s"Mae $agentName wedi’i dynnu o’ch cyfrif"
  val theAgentTextWelsh = "Ni all yr asiant weithredu ar eich rhan mwyach."
  val whatHappensTextWelsh = "Beth sy’n digwydd nesaf"
  val ifYouWantTextWelsh =
    s"Os ydych am i’r asiant weithredu ar eich rhan eto, gallwch ei ailbenodi i’ch cyfrif gan ddefnyddio cod asiant ${agentCode.toString}."
  val goToLinkTextWelsh = "Ewch i hafan eich cyfrif"

  val goToLinkHref = "/business-rates-dashboard/home"

  "confirmRemoveAgentFromOrganisation displays the correct content in English" which {
    lazy val document: Document = getConfirmRemoveAgentFromOrgPage(English)

    s"has a title of $titleText" in {
      document.title() shouldBe titleText
    }

    s"has a heading of $headingText" in {
      document.getElementsByTag(headingSelector).text shouldBe headingText
    }

    s"has text on the screen of $theAgentText" in {
      document.select(theAgentSelector).text shouldBe theAgentText
    }

    s"has text on the screen of $whatHappensText" in {
      document.select(whatHappensSelector).text shouldBe whatHappensText
    }

    s"has text on the screen of $ifYouWantText" in {
      document.select(ifYouWantSelector).text shouldBe ifYouWantText
    }

    s"has bold text on the screen of ${agentCode.toString}" in {
      document.select(agentCodeSelector).text shouldBe agentCode.toString
    }

    s"has a $goToLinkText link" in {
      document.select(goToLinkSelector).text shouldBe goToLinkText
      document.select(goToLinkSelector).attr("href") shouldBe goToLinkHref
    }
  }

  "confirmRemoveAgentFromOrganisation displays the correct content in Welsh" which {
    lazy val document: Document = getConfirmRemoveAgentFromOrgPage(Welsh)

    s"has a title of $titleText in welsh" in {
      document.title() shouldBe titleTextWelsh
    }

    s"has a heading of $headingText in welsh" in {
      document.getElementsByTag(headingSelector).text shouldBe headingTextWelsh
    }

    s"has text on the screen of $theAgentText in welsh" in {
      document.select(theAgentSelector).text shouldBe theAgentTextWelsh
    }

    s"has text on the screen of $whatHappensText in welsh" in {
      document.select(whatHappensSelector).text shouldBe whatHappensTextWelsh
    }

    s"has text on the screen of $ifYouWantText in welsh" in {
      document.select(ifYouWantSelector).text shouldBe ifYouWantTextWelsh
    }

    s"has bold text on the screen of ${agentCode.toString}" in {
      document.select(agentCodeSelector).text shouldBe agentCode.toString
    }

    s"has a $goToLinkText link in welsh" in {
      document.select(goToLinkSelector).text shouldBe goToLinkTextWelsh
      document.select(goToLinkSelector).attr("href") shouldBe goToLinkHref
    }
  }

  private def getConfirmRemoveAgentFromOrgPage(language: Language): Document = {
    await(
      mockRepository.saveOrUpdate(
        AgentSummary(
          listYears = Some(List("2017", "2023")),
          name = agentName,
          organisationId = 1L,
          representativeCode = agentCode,
          appointedDate = LocalDate.now(),
          propertyCount = 1
        )
      )
    )

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
        s"http://localhost:$port/business-rates-property-linking/my-organisation/manage-agent/remove/from-organisation/confirm"
      ).withCookies(languageCookie(language), getSessionCookie(testSessionId))
        .withFollowRedirects(follow = false)
        .withHttpHeaders(HeaderNames.COOKIE -> "sessionId")
        .get()
    )

    res.status shouldBe OK
    Jsoup.parse(res.body)
  }

}
