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
import com.github.tomakehurst.wiremock.client.WireMock.{aResponse, get, post, stubFor}
import models.propertyrepresentation.{AgentSelected, ManagingProperty, SearchedAgent, SelectedAgent}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import play.api.libs.json.Json
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import repositories.AppointAgentSessionRepository
import uk.gov.hmrc.http.{HeaderCarrier, SessionId}
import play.api.http.HeaderNames

import java.util.UUID

class SelectRatingListISpec extends ISpecBase with HtmlComponentHelpers {

  val testSessionId = s"stubbed-${UUID.randomUUID}"

  val titleText = "Choose the 2023 or 2017 rating list - Valuation Office Agency - GOV.UK"
  val backLinkText = "Back"
  val captionText = "Appoint an agent"
  val headerText = "Choose the 2023 or 2017 rating list"
  val theRatingListText =
    "The rating list you choose for this agent will apply to all properties that you assign to them and they add to your account."
  val theAgentText = "The agent will only be able to act for you on valuations on the rating list you choose."
  val whichRatingText = "Which rating list do you want this agent to act on for you?"
  val the2023ListText = "2023 rating list"
  val theAgent2023Text =
    "The agent can only act for you on your current valuation for your property and any previous valuations that have an effective date after 1 April 2023."
  val the2017ListText = "2017 rating list"
  val theAgent2017Text =
    "The agent can only act for you on previous valuations for your property that have an effective date between 1 April 2017 to 31 March 2023."
  val continueText = "Continue"
  val thereIsAProblemText = "There is a problem"
  val selectionErrorText = "Select which rating list you want this agent to act on for you"

  val titleTextWelsh = "Dewiswch restr ardrethu 2023 neu 2017 - Valuation Office Agency - GOV.UK"
  val backLinkTextWelsh = "Yn ôl"
  val captionTextWelsh = "Penodi Asiant"
  val headerTextWelsh = "Dewiswch restr ardrethu 2023 neu 2017"
  val theRatingListTextWelsh =
    "Bydd y rhestr ardrethu a ddewiswch ar gyfer yr asiant hwn yn berthnasol i’r holl eiddo rydych chi’n ei aseinio iddynt ac i’r rheiny maen nhw’n ychwanegu at eich cyfrif."
  val theAgentTextWelsh =
    "Dim ond ar brisiadau ar y rhestr ardrethu a ddewiswch y bydd yr asiant yn gallu gweithredu ar eich rhan."
  val whichRatingTextWelsh = "Pa restr ardrethu yr hoffech i’r asiant hwn ei gweithredu ar eich rhan?"
  val the2023ListTextWelsh = "rhestr ardrethu 2023"
  val theAgent2023TextWelsh =
    "Dim ond ar eich prisiad cyfredol ar gyfer eich eiddo ac unrhyw brisiadau blaenorol sydd â dyddiad dod i rym ar ôl 1 Ebrill 2023 y gall yr asiant weithredu ar eich rhan."
  val the2017ListTextWelsh = "rhestr ardrethu 2017"
  val theAgent2017TextWelsh =
    "Dim ond ar brisiadau blaenorol ar gyfer eich eiddo sydd â dyddiad dod i rym rhwng 1 Ebrill 2017 a 31 Mawrth 2023 y gall yr asiant weithredu ar eich rhan."
  val continueTextWelsh = "Parhau"
  val thereIsAProblemTextWelsh = "Mae yna broblem"
  val selectionErrorTextWelsh = "Dewis ar ba restr ardrethu chi am i’r asiant hwn weithredu ar eich rhan"

  val backLinkSelector = "#back-link"
  val captionSelector = "span.govuk-caption-l"
  val headerSelector = "h1.govuk-heading-l"
  val theRatingListSelector = "#main-content > div > div > p:nth-child(3)"
  val theAgentSelector = "#main-content > div > div > p:nth-child(4)"
  val whichRatingSelector = "#main-content > div > div > form > div > fieldset > legend"
  val the2023RadioButtonSelector = "#multipleListYears"
  val the2023ListSelector = "#main-content > div > div > form > div > fieldset > div > div:nth-child(1) > label"
  val theAgent2023Selector = "#multipleListYears-item-hint"
  val the2017RadioButtonSelector = "#multipleListYears-2"
  val the2017ListSelector = "#main-content > div > div > form > div > fieldset > div > div:nth-child(2) > label"
  val theAgent2017Selector = "#multipleListYears-2-item-hint"
  val continueSelector = "#continue"
  val errorSummaryTitleSelector = "#main-content > div > div > div.govuk-error-summary > div > h2"
  val errorSummaryErrorSelector = "#main-content > div > div > div.govuk-error-summary > div > div"
  val aboveRadiosErrorSelector = "#multipleListYears-error"
  val inputErrorSummarySelector = "#main-content > div > div > div > div > div"

  val backLinkHref = "/business-rates-property-linking/my-organisation/appoint-new-agent/ratings-list"
  val checkYourAnswersBackLink = "/business-rates-property-linking/my-organisation/appoint-new-agent/check-your-answers"

  "SelectRatingList show method" should {

    "Show a 'Choose the 2023 or 2017 rating list screen' when the language is set to English" which {

      lazy val document = getSelectRatingListPage(English)

      s"has a title of $titleText" in {
        document.title() shouldBe titleText
      }

      "has a back link which takes you to the agent details page" in {
        document.select(backLinkSelector).text() shouldBe backLinkText
        document.select(backLinkSelector).attr("href") shouldBe backLinkHref
      }

      s"has a header of '$headerText' with a caption above of '$captionText'" in {
        document.select(headerSelector).text shouldBe headerText
        document.select(captionSelector).text shouldBe captionText
      }

      s"has text on the screen of '$theRatingListText'" in {
        document.select(theRatingListSelector).text() shouldBe theRatingListText
      }

      s"has text on the screen of '$theAgentText'" in {
        document.select(theAgentSelector).text() shouldBe theAgentText
      }

      s"has a medium heading on the screen of '$whichRatingText'" in {
        document.select(whichRatingSelector).text() shouldBe whichRatingText
      }

      s"has an un-checked '$the2023ListText' radio button, with hint text of '$theAgent2023Text'" in {
        document.select(the2023RadioButtonSelector).hasAttr("checked") shouldBe false
        document.select(the2023ListSelector).text() shouldBe the2023ListText
        document.select(theAgent2023Selector).text() shouldBe theAgent2023Text
      }

      s"has an un-checked '$the2017ListText' radio button, with hint text of '$theAgent2017Text'" in {
        document.select(the2017RadioButtonSelector).hasAttr("checked") shouldBe false
        document.select(the2017ListSelector).text() shouldBe the2017ListText
        document.select(theAgent2017Selector).text() shouldBe theAgent2017Text
      }

      s"has a '$continueText' button on the screen, which submits the users choice" in {
        document.select(continueSelector).text() shouldBe continueText
      }
    }

    "Show a 'Choose the 2023 or 2017 rating list screen' when the language is set to Welsh" which {

      lazy val document = getSelectRatingListPage(Welsh)

      s"has a title of $titleText in welsh" in {
        document.title() shouldBe titleTextWelsh
      }

      "has a back link which takes you to the agent details page" in {
        document.select(backLinkSelector).text() shouldBe backLinkTextWelsh
        document.select(backLinkSelector).attr("href") shouldBe backLinkHref
      }

      s"has a header of '$headerText' in welsh with a caption above of '$captionText' in welsh" in {
        document.select(headerSelector).text shouldBe headerTextWelsh
        document.select(captionSelector).text shouldBe captionTextWelsh
      }

      s"has text on the screen of '$theRatingListText' in welsh" in {
        document.select(theRatingListSelector).text() shouldBe theRatingListTextWelsh
      }

      s"has text on the screen of '$theAgentText' in welsh" in {
        document.select(theAgentSelector).text() shouldBe theAgentTextWelsh
      }

      s"has a medium heading on the screen of '$whichRatingText' in welsh" in {
        document.select(whichRatingSelector).text() shouldBe whichRatingTextWelsh
      }

      s"has an un-checked '$the2023ListText' radio button, with hint text of '$theAgent2023Text' in welsh" in {
        document.select(the2023RadioButtonSelector).hasAttr("checked") shouldBe false
        document.select(the2023ListSelector).text() shouldBe the2023ListTextWelsh
        document.select(theAgent2023Selector).text() shouldBe theAgent2023TextWelsh
      }

      s"has an un-checked '$the2017ListText' radio button, with hint text of '$theAgent2017Text' in welsh" in {
        document.select(the2017RadioButtonSelector).hasAttr("checked") shouldBe false
        document.select(the2017ListSelector).text() shouldBe the2017ListTextWelsh
        document.select(theAgent2017Selector).text() shouldBe theAgent2017TextWelsh
      }

      s"has a '$continueText' button on the screen in welsh, which submits the users choice" in {
        document.select(continueSelector).text() shouldBe continueTextWelsh
      }
    }

    "Show a 'Choose the 2023 or 2017 rating list screen' when coming back from check your answers" which {

      lazy val document = getSelectRatingListPage(English, "?fromCyaChange=true")

      s"has a title of $titleText" in {
        document.title() shouldBe titleText
      }

      "has a back link which takes you to the agent details page" in {
        document.select(backLinkSelector).text() shouldBe backLinkText
        document.select(backLinkSelector).attr("href") shouldBe backLinkHref
      }

      s"has a header of '$headerText' with a caption above of '$captionText'" in {
        document.select(headerSelector).text shouldBe headerText
        document.select(captionSelector).text shouldBe captionText
      }

      s"has text on the screen of '$theRatingListText'" in {
        document.select(theRatingListSelector).text() shouldBe theRatingListText
      }

      s"has text on the screen of '$theAgentText'" in {
        document.select(theAgentSelector).text() shouldBe theAgentText
      }

      s"has a medium heading on the screen of '$whichRatingText'" in {
        document.select(whichRatingSelector).text() shouldBe whichRatingText
      }

      s"has an un-checked '$the2023ListText' radio button, with hint text of '$theAgent2023Text'" in {
        document.select(the2023RadioButtonSelector).hasAttr("checked") shouldBe false
        document.select(the2023ListSelector).text() shouldBe the2023ListText
        document.select(theAgent2023Selector).text() shouldBe theAgent2023Text
      }

      s"has an un-checked '$the2017ListText' radio button, with hint text of '$theAgent2017Text'" in {
        document.select(the2017RadioButtonSelector).hasAttr("checked") shouldBe false
        document.select(the2017ListSelector).text() shouldBe the2017ListText
        document.select(theAgent2017Selector).text() shouldBe theAgent2017Text
      }

      s"has a '$continueText' button on the screen, which submits the users choice" in {
        document.select(continueSelector).text() shouldBe continueText
      }
    }

  }

  "SelectRatingList Controller submit method" should {
    "redirect too check your answers when answer is selected and no properties" in {
      submitSelectRatingListCommonStubbing()

      stubFor {
        get(
          "/property-linking/my-organisation/agents/1001/available-property-links?sortField=ADDRESS&sortOrder=ASC&startPoint=1&pageSize=15&requestTotalRowCount=false"
        ).willReturn {
          aResponse.withStatus(OK).withBody(Json.toJson(testOwnerAuthResultNoProperties).toString())
        }
      }

      val requestBody = Json.obj("multipleListYears" -> "2017")

      val res = await(
        ws.url(
          s"http://localhost:$port/business-rates-property-linking/my-organisation/appoint-new-agent/ratings-list-select"
        ).withCookies(languageCookie(English), getSessionCookie(testSessionId))
          .withFollowRedirects(follow = false)
          .withHttpHeaders(HeaderNames.COOKIE -> "sessionId", "Csrf-Token" -> "nocheck")
          .post(body = requestBody)
      )

      res.status shouldBe SEE_OTHER
      res
        .headers("Location")
        .head shouldBe "/business-rates-property-linking/my-organisation/appoint-new-agent/check-your-answers"

    }

    "redirect too single property after answer is selected" in {
      submitSelectRatingListCommonStubbing()

      stubFor {
        get(
          "/property-linking/my-organisation/agents/1001/available-property-links?sortField=ADDRESS&sortOrder=ASC&startPoint=1&pageSize=15&requestTotalRowCount=false"
        ).willReturn {
          aResponse.withStatus(OK).withBody(Json.toJson(testOwnerAuthResult1).toString())
        }
      }

      val requestBody = Json.obj("multipleListYears" -> "2017")

      val res = await(
        ws.url(
          s"http://localhost:$port/business-rates-property-linking/my-organisation/appoint-new-agent/ratings-list-select"
        ).withCookies(languageCookie(English), getSessionCookie(testSessionId))
          .withFollowRedirects(follow = false)
          .withHttpHeaders(HeaderNames.COOKIE -> "sessionId", "Csrf-Token" -> "nocheck")
          .post(body = requestBody)
      )

      res.status shouldBe SEE_OTHER
      res
        .headers("Location")
        .head shouldBe "/business-rates-property-linking/my-organisation/appoint-new-agent/one-property"

    }

    "redirect too multiple property after answer is selected" in {
      submitSelectRatingListCommonStubbing()

      stubFor {
        get(
          "/property-linking/my-organisation/agents/1001/available-property-links?sortField=ADDRESS&sortOrder=ASC&startPoint=1&pageSize=15&requestTotalRowCount=false"
        ).willReturn {
          aResponse.withStatus(OK).withBody(Json.toJson(testOwnerAuthResultMultipleProperty).toString())
        }
      }

      val requestBody = Json.obj("multipleListYears" -> "2017")

      val res = await(
        ws.url(
          s"http://localhost:$port/business-rates-property-linking/my-organisation/appoint-new-agent/ratings-list-select"
        ).withCookies(languageCookie(English), getSessionCookie(testSessionId))
          .withFollowRedirects(follow = false)
          .withHttpHeaders(HeaderNames.COOKIE -> "sessionId", "Csrf-Token" -> "nocheck")
          .post(body = requestBody)
      )

      res.status shouldBe SEE_OTHER
      res
        .headers("Location")
        .head shouldBe "/business-rates-property-linking/my-organisation/appoint-new-agent/multiple-properties"
    }

    "receive a bad request when answer is not selected in english" in {
      submitSelectRatingListCommonStubbing()

      stubFor {
        get(
          "/property-linking/my-organisation/agents/1001/available-property-links?sortField=ADDRESS&sortOrder=ASC&startPoint=1&pageSize=15&requestTotalRowCount=false"
        ).willReturn {
          aResponse.withStatus(OK).withBody(Json.toJson(testOwnerAuthResultMultipleProperty).toString())
        }
      }

      val requestBody = Json.obj()

      val res = await(
        ws.url(
          s"http://localhost:$port/business-rates-property-linking/my-organisation/appoint-new-agent/ratings-list-select"
        ).withCookies(languageCookie(English), getSessionCookie(testSessionId))
          .withFollowRedirects(follow = false)
          .withHttpHeaders(HeaderNames.COOKIE -> "sessionId", "Csrf-Token" -> "nocheck")
          .post(body = requestBody)
      )

      res.status shouldBe BAD_REQUEST
      val body = Jsoup.parse(res.body)
      body.select(inputErrorSummarySelector).text() shouldBe selectionErrorText

    }

    "receive a bad request when answer is not selected in welsh" in {
      submitSelectRatingListCommonStubbing()

      stubFor {
        get(
          "/property-linking/my-organisation/agents/1001/available-property-links?sortField=ADDRESS&sortOrder=ASC&startPoint=1&pageSize=15&requestTotalRowCount=false"
        ).willReturn {
          aResponse.withStatus(OK).withBody(Json.toJson(testOwnerAuthResultMultipleProperty).toString())
        }
      }

      val requestBody = Json.obj()

      val res = await(
        ws.url(
          s"http://localhost:$port/business-rates-property-linking/my-organisation/appoint-new-agent/ratings-list-select"
        ).withCookies(languageCookie(Welsh), getSessionCookie(testSessionId))
          .withFollowRedirects(follow = false)
          .withHttpHeaders(HeaderNames.COOKIE -> "sessionId", "Csrf-Token" -> "nocheck")
          .post(body = requestBody)
      )

      res.status shouldBe BAD_REQUEST
      val body = Jsoup.parse(res.body)
      body.select(inputErrorSummarySelector).text() shouldBe selectionErrorTextWelsh

    }
  }

  private def getSelectRatingListPage(language: Language, checkYourAnswers: String = ""): Document = {

    implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId(testSessionId)))

    lazy val mockAppointAgentSessionRepository: AppointAgentSessionRepository =
      app.injector.instanceOf[AppointAgentSessionRepository]

    val searchedAgentData: SearchedAgent = SearchedAgent.apply(1001, "Some Org", "street", AgentSelected, None)

    val selectedAgentData = SelectedAgent.apply(searchedAgentData, true, Some(true), None)

    await(mockAppointAgentSessionRepository.saveOrUpdate(selectedAgentData))

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
        s"http://localhost:$port/business-rates-property-linking/my-organisation/appoint-new-agent/ratings-list-select$checkYourAnswers"
      ).withCookies(languageCookie(language), getSessionCookie(testSessionId))
        .withFollowRedirects(follow = false)
        .get()
    )

    res.status shouldBe OK
    Jsoup.parse(res.body)
  }

  private def submitSelectRatingListCommonStubbing() = {
    implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId(testSessionId)))

    lazy val mockAppointAgentSessionRepository: AppointAgentSessionRepository =
      app.injector.instanceOf[AppointAgentSessionRepository]

    val searchedAgentData: SearchedAgent = SearchedAgent.apply(1001, "Some Org", "street", AgentSelected, None)

    val selectedAgentData = SelectedAgent.apply(searchedAgentData, true, Some(true), Some("2017"))

    val managedAgentData = ManagingProperty.apply(selectedAgentData, "None", false, 1, 1)
    await(mockAppointAgentSessionRepository.saveOrUpdate(managedAgentData))

    stubFor {
      post("/auth/authorise")
        .willReturn {
          aResponse.withStatus(OK).withBody("{}")
        }
    }

    stubFor {
      get("/business-rates-authorisation/authenticate")
        .willReturn {
          aResponse.withStatus(OK).withBody(Json.toJson(testAccounts).toString())
        }
    }

    stubFor {
      get("/property-linking/owner/property-links/count")
        .willReturn {
          aResponse.withStatus(OK).withBody(Json.toJson(0).toString())
        }
    }
  }

}
