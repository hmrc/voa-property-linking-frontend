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

class ShowRemoveAgentFromIpOrganisationISpec extends ISpecBase with HtmlComponentHelpers {

  val testSessionId = s"stubbed-${UUID.randomUUID}"

  lazy val mockRepository: ManageAgentSessionRepository = app.injector.instanceOf[ManageAgentSessionRepository]
  implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId(testSessionId)))

  val headingTag = "h1"
  val paragraph1id = "remove-agent-from-org-p1"
  val paragraph2id = "remove-agent-from-org-p2"
  val confirmAndRemoveButtonId = "submit-button"
  val cancelLinkId = "cancel-link"
  val backLinkId = "back-link"

  val titleText = "Are you sure you want to remove Test Agent from your account? - Valuation Office Agency - GOV.UK"
  val headingText = "Are you sure you want to remove Test Agent from your account?"
  val paragraph1Text = "They will no longer be able to add properties to your account and act on them for you."
  val paragraph2Text = "You will no longer be able to assign properties to them or have them act for you."
  val confirmAndRemoveButtonText = "Confirm and remove"
  val cancelLinkText = "Cancel"
  val backLinkText = "Back"

  val titleTextWelsh = "Ydych chi’n siŵr eich bod am dynnu Test Agent o’ch cyfrif? - Valuation Office Agency - GOV.UK"
  val headingTextWelsh = "Ydych chi’n siŵr eich bod am dynnu Test Agent o’ch cyfrif?"
  val paragraph1TextWelsh =
    "Ni fyddant bellach yn gallu ychwanegu eiddo at eich cyfrif a gweithredu arnynt ar eich rhan."
  val paragraph2TextWelsh = "Ni fyddwch bellach yn gallu aseinio eiddo iddynt na’u cael i weithredu ar eich rhan."
  val confirmAndRemoveButtonTextWelsh = "Cadarnhau a dileu"
  val cancelLinkTextWelsh = "Canslo"
  val backLinkTextWelsh = "Yn ôl"

  val backLinkHref = "/business-rates-property-linking/my-organisation/manage-agent"
  val cancelLinkHref = "/business-rates-property-linking/my-organisation/manage-agent/property-links?agentCode=1"

  "showRemoveAgentFromIpOrganisation displays the correct content in English" which {
    lazy val document: Document = getRemoveAgentQuestionPage(English)

    s"has a title of $titleText" in {
      document.title() shouldBe titleText
    }

    s"has a heading of $headingText" in {
      document.getElementsByTag(headingTag).text shouldBe headingText
    }

    s"has a first paragraph of $paragraph1Text" in {
      document.getElementById(paragraph1id).text shouldBe paragraph1Text
    }

    s"has a second paragraph of $paragraph2Text" in {
      document.getElementById(paragraph2id).text shouldBe paragraph2Text
    }

    s"has a $confirmAndRemoveButtonText button" in {
      document.getElementById(confirmAndRemoveButtonId).text shouldBe confirmAndRemoveButtonText
    }

    s"has a $cancelLinkText link" in {
      document.getElementById(cancelLinkId).text shouldBe cancelLinkText
      document.getElementById(cancelLinkId).attr("href") shouldBe cancelLinkHref
    }

    s"has a $backLinkText link" in {
      document.getElementById(backLinkId).text shouldBe backLinkText
      document.getElementById(backLinkId).attr("href") shouldBe backLinkHref
    }
  }

  "showRemoveAgentFromIpOrganisation displays the correct content in Welsh" which {
    lazy val document: Document = getRemoveAgentQuestionPage(Welsh)

    s"has a title of $titleText in Welsh" in {
      document.title() shouldBe titleTextWelsh
    }

    s"has a heading of $headingText in Welsh" in {
      document.getElementsByTag(headingTag).text shouldBe headingTextWelsh
    }

    s"has a first paragraph of $paragraph1Text in Welsh" in {
      document.getElementById(paragraph1id).text shouldBe paragraph1TextWelsh
    }

    s"has a second paragraph of $paragraph2Text in Welsh" in {
      document.getElementById(paragraph2id).text shouldBe paragraph2TextWelsh
    }

    s"has a $confirmAndRemoveButtonText button in Welsh" in {
      document.getElementById(confirmAndRemoveButtonId).text shouldBe confirmAndRemoveButtonTextWelsh
    }

    s"has a $cancelLinkText link in Welsh" in {
      document.getElementById(cancelLinkId).text shouldBe cancelLinkTextWelsh
      document.getElementById(cancelLinkId).attr("href") shouldBe cancelLinkHref
    }

    s"has a $backLinkText link in Welsh" in {
      document.getElementById(backLinkId).text shouldBe backLinkTextWelsh
      document.getElementById(backLinkId).attr("href") shouldBe backLinkHref
    }
  }

  private def getRemoveAgentQuestionPage(language: Language) = {
    await(
      mockRepository.saveOrUpdate(
        AgentSummary(
          listYears = Some(List("2017", "2023")),
          name = "Test Agent",
          organisationId = 1L,
          representativeCode = 1L,
          appointedDate = LocalDate.now(),
          propertyCount = 10
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
          s"http://localhost:$port/business-rates-property-linking/my-organisation/manage-agent/remove/from-organisation")
        .withCookies(languageCookie(language), getSessionCookie(testSessionId))
        .withFollowRedirects(follow = false)
        .withHttpHeaders(HeaderNames.COOKIE -> "sessionId")
        .get()
    )

    res.status shouldBe OK
    Jsoup.parse(res.body)
  }

}
