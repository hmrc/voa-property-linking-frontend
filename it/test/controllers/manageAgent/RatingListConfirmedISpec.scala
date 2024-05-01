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

  val titleText =
    "The rating lists that Test Agent can act for you on has been changed - Valuation Office Agency - GOV.UK"
  val headerText = "The rating lists that Test Agent can act for you on has been changed"
  def thisAgentTextSingle(listYear: String) =
    s"This agent can act for you on your property valuations on the $listYear rating list, for properties that you assign to them or they add to your account"
  val thisAgentTextMultiple =
    "This agent can act for you on your property valuations on the 2023 and 2017 rating lists, for properties that you assign to them or they add to your account"
  val whatHappensText = "What happens next"
  val youCanText = "You can change the rating lists this agent can act for you on at any time."
  val goToYourText = "Go to your account home"

  val titleTextWelsh =
    "Mae’r rhestrau ardrethu y gall Test Agent weithredu arnynt ar eich rhan wedi newid - Valuation Office Agency - GOV.UK"
  val headerTextWelsh = "Mae’r rhestrau ardrethu y gall Test Agent weithredu arnynt ar eich rhan wedi newid"
  def thisAgentTextSingleWelsh(listYear: String) =
    s"Gall yr asiant hwn weithredu ar eich rhan ar brisiadau eich eiddo o restr ardrethu $listYear, ar gyfer eiddo rydych yn eu neilltuo iddo, ac ar gyfer eiddo y mae’n eu hychwanegu at eich cyfrif"
  val thisAgentTextMultipleWelsh =
    "Gall yr asiant hwn weithredu ar eich rhan ar brisiadau eich eiddo o restrau ardrethu 2023 a 2017, ar gyfer eiddo rydych yn eu neilltuo iddo, ac ar gyfer eiddo y mae’n eu hychwanegu at eich cyfrif"
  val whatHappensTextWelsh = "Yr hyn sy’n digwydd nesaf"
  val youCanTextWelsh =
    "Gallwch newid pa restrau ardrethu y gall yr asiant hwn weithredu arnynt ar eich rhan ar unrhyw adeg."
  val goToYourTextWelsh = "Ewch i hafan eich cyfrif"

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
