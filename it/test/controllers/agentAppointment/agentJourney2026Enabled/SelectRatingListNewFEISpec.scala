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

package controllers.agentAppointment.agentJourney2026Enabled

import base.{HtmlComponentHelpers, ISpecBase}
import com.github.tomakehurst.wiremock.client.WireMock.{aResponse, get, post, stubFor}
import models.propertyrepresentation.{AgentSelected, AppointNewAgentSession, ManagingProperty, SearchedAgent, SelectedAgent}
import models.searchApi.OwnerAuthResult
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.http.HeaderNames
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import play.api.libs.json.{JsObject, Json}
import play.api.libs.ws.WSResponse
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import repositories.AppointAgentSessionRepository
import uk.gov.hmrc.http.{HeaderCarrier, SessionId}

import java.util.UUID

class SelectRatingListNewFEISpec extends ISpecBase with HtmlComponentHelpers {

  override lazy val extraConfig: Map[String, String] = Map("feature-switch.agentJourney2026Enabled" -> "true")

  val testSessionId = s"stubbed-${UUID.randomUUID}"

//  TODO: Should the title be sanitised?
  val titleText = "Select which rating list Some Org can act on for you - Valuation Office Agency - GOV.UK"
  val backLinkText = "Back"
  val captionText = "Appoint an agent"
  val headerText = "Select which rating list Some Org can act on for you"
  val choosingText =
    "Choosing a rating list is different to assigning properties to the agent. You will assign properties after choosing a rating list."
  val theRatingListText =
    "The rating list you choose for this agent will apply to all properties that you assign to them and they add to your account."
  val theAgentText = "The agent will only be able to act for you on valuations on the rating list you choose."
  val selectAllText = "Select all that apply."
  val listYear2026Text = "2026"
  val listYear2023Text = "2023"
  val listYear2017Text = "2017"
  val valuationsFromText = "For valuations from 1 April 2026."
  val valuationsBetween2023Text = "For valuations between 1 April 2023 and 31 March 2026."
  val valuationsBetween2017Text = "For valuations between 1 April 2017 and 31 March 2023."
  val continueText = "Continue"
  val errorText = "Error: "
  val thereIsAProblemText = "There is a problem"
  val selectionErrorText = "Select which rating list you want this agent to act on for you"

  val titleTextWelsh =
    "Dewiswch pa restr ardrethu y gall Some Org weithredu arni ar eich rhan - Valuation Office Agency - GOV.UK"
  val backLinkTextWelsh = "Yn ôl"
  val captionTextWelsh = "Penodi Asiant"
  val headerTextWelsh = "Dewiswch pa restr ardrethu y gall Some Org weithredu arni ar eich rhan"
  val choosingTextWelsh =
    "Mae dewis rhestr ardrethu yn wahanol i neilltuo eiddo i asiant. Byddwch yn neilltuo eiddo ar ôl i chi ddewis rhestr ardrethu."
  val theRatingListTextWelsh =
    "Bydd y rhestr ardrethu a ddewiswch ar gyfer yr asiant hwn yn berthnasol i’r holl eiddo rydych chi’n ei aseinio iddynt ac maen nhw’n ychwanegu at eich cyfrif."
  val theAgentTextWelsh =
    "Dim ond ar brisiadau ar y rhestr ardrethu a ddewiswch y bydd yr asiant yn gallu gweithredu ar eich rhan."
  val selectAllTextWelsh = "Dewiswch bob un sy’n berthnasol."
  val listYear2026TextWelsh = "2026"
  val listYear2023TextWelsh = "2023"
  val listYear2017TextWelsh = "2017"
  val valuationsFromTextWelsh = "Ar gyfer prisiadau o 1 Ebrill 2026."
  val valuationsBetween2023TextWelsh = "Ar gyfer prisiadau rhwng 1 Ebrill 2023 a 31 Mawrth 2026."
  val valuationsBetween2017TextWelsh = "Ar gyfer prisiadau rhwng 1 Ebrill 2017 a 31 Mawrth 2023."
  val continueTextWelsh = "Parhau"
  val errorTextWelsh = "Gwall: "
  val thereIsAProblemTextWelsh = "Mae yna broblem"
  val selectionErrorTextWelsh = "Dewiswch pa restr ardrethu rydych chi am i’r asiant hwn weithredu arni ar eich rhan"

  val backLinkTextSelector = "#back-link"
  val captionTextSelector = "span.govuk-caption-l"
  val headerTextSelector = "h1.govuk-heading-l"
  val choosingTextSelector = "div.govuk-inset-text"
  val theRatingListTextSelector = "#theRatingList"
  val theAgentTextSelector = "#theAgentWill"
  val selectAllTextSelector = "legend.govuk-fieldset__legend"
  val listYear2026TextSelector = "#main-content > div > div > form > div > fieldset > div > div:nth-child(1) > label"
  val listYear2026CheckboxSelector = "#listYears"
  val listYear2023TextSelector = "#main-content > div > div > form > div > fieldset > div > div:nth-child(2) > label"
  val listYear2023CheckboxSelector = "#listYears-2"
  val listYear2017TextSelector = "#main-content > div > div > form > div > fieldset > div > div:nth-child(3) > label"
  val listYear2017CheckboxSelector = "#listYears-3"
  val valuationsFromTextSelector = "#listYears-item-hint"
  val valuationsBetween2023TextSelector = "#listYears-2-item-hint"
  val valuationsBetween2017TextSelector = "#listYears-3-item-hint"
  val continueTextSelector = "#continue"
  val thereIsAProblemTextSelector = "#main-content > div > div > div.govuk-error-summary > div > h2"
  val selectionErrorAtTopTextSelector = "#main-content > div > div > div.govuk-error-summary > div > div > ul > li > a"
  val selectionErrorAtCheckboxTextSelector = "#listYears-error"

  val backLinkHref = "/business-rates-property-linking/my-organisation/appoint-new-agent/is-correct-agent"
  val checkYourAnswersBackLinkHref =
    "/business-rates-property-linking/my-organisation/appoint-new-agent/check-your-answers"
  val errorHref = "#listYears"

  "SelectRatingListNew show method" should {

    "Show the correct page when the language is set to English" which {

      lazy val document = getPage(English)

      s"has a title of $titleText" in {
        document.title() shouldBe titleText
      }

      "has a back link which takes you to the agent details page" in {
        document.select(backLinkTextSelector).text() shouldBe backLinkText
        document.select(backLinkTextSelector).attr("href") shouldBe backLinkHref
      }

      s"has a header of '$headerText' with a caption above of '$captionText'" in {
        document.select(headerTextSelector).text shouldBe headerText
        document.select(captionTextSelector).text shouldBe captionText
      }

      s"has inset-text on the screen of '$choosingText'" in {
        document.select(choosingTextSelector).text() shouldBe choosingText
      }

      s"has text on the screen of '$theRatingListText'" in {
        document.select(theRatingListTextSelector).text() shouldBe theRatingListText
      }

      s"has text on the screen of '$theAgentText'" in {
        document.select(theAgentTextSelector).text() shouldBe theAgentText
      }

      s"has text on the screen of '$selectAllText'" in {
        document.select(selectAllTextSelector).text() shouldBe selectAllText
      }

      s"has an unchecked checkbox on the screen for '$listYear2026Text' with hint text '$valuationsFromText'" in {
        document.select(listYear2026TextSelector).text() shouldBe listYear2026Text
        document.select(listYear2026CheckboxSelector).attr("type") shouldBe "checkbox"
        document.select(listYear2026CheckboxSelector).hasAttr("checked") shouldBe false
        document.select(valuationsFromTextSelector).text() shouldBe valuationsFromText
      }

      s"has an unchecked checkbox on the screen for '$listYear2023Text' with hint text '$valuationsBetween2023Text'" in {
        document.select(listYear2023TextSelector).text() shouldBe listYear2023Text
        document.select(listYear2023CheckboxSelector).attr("type") shouldBe "checkbox"
        document.select(listYear2023CheckboxSelector).hasAttr("checked") shouldBe false
        document.select(valuationsBetween2023TextSelector).text() shouldBe valuationsBetween2023Text
      }

      s"has an unchecked checkbox on the screen for '$listYear2017Text' with hint text '$valuationsBetween2017Text'" in {
        document.select(listYear2017TextSelector).text() shouldBe listYear2017Text
        document.select(listYear2017CheckboxSelector).attr("type") shouldBe "checkbox"
        document.select(listYear2017CheckboxSelector).hasAttr("checked") shouldBe false
        document.select(valuationsBetween2017TextSelector).text() shouldBe valuationsBetween2017Text
      }

      s"has a '$continueText' button on the screen, which submits the users choice" in {
        document.select(continueTextSelector).text() shouldBe continueText
      }
    }

    "Show the correct page when the language is set to Welsh" which {

      lazy val document = getPage(Welsh)

      s"has a title of $titleText in welsh" in {
        document.title() shouldBe titleTextWelsh
      }

      "has a back link which takes you to the agent details page in welsh" in {
        document.select(backLinkTextSelector).text() shouldBe backLinkTextWelsh
        document.select(backLinkTextSelector).attr("href") shouldBe backLinkHref
      }

      s"has a header of '$headerText' with a caption above of '$captionText' in welsh" in {
        document.select(headerTextSelector).text shouldBe headerTextWelsh
        document.select(captionTextSelector).text shouldBe captionTextWelsh
      }

      s"has inset-text on the screen of '$choosingText' in welsh" in {
        document.select(choosingTextSelector).text() shouldBe choosingTextWelsh
      }

      s"has text on the screen of '$theRatingListText' in welsh" in {
        document.select(theRatingListTextSelector).text() shouldBe theRatingListTextWelsh
      }

      s"has text on the screen of '$theAgentText' in welsh" in {
        document.select(theAgentTextSelector).text() shouldBe theAgentTextWelsh
      }

      s"has text on the screen of '$selectAllText' in welsh" in {
        document.select(selectAllTextSelector).text() shouldBe selectAllTextWelsh
      }

      s"has a checkbox on the screen for '$listYear2026Text' with hint text '$valuationsFromText' in welsh" in {
        document.select(listYear2026TextSelector).text() shouldBe listYear2026TextWelsh
        document.select(listYear2026CheckboxSelector).attr("type") shouldBe "checkbox"
        document.select(valuationsFromTextSelector).text() shouldBe valuationsFromTextWelsh
      }

      s"has a checkbox on the screen for '$listYear2023Text' with hint text '$valuationsBetween2023Text' in welsh" in {
        document.select(listYear2023TextSelector).text() shouldBe listYear2023TextWelsh
        document.select(listYear2023CheckboxSelector).attr("type") shouldBe "checkbox"
        document.select(valuationsBetween2023TextSelector).text() shouldBe valuationsBetween2023TextWelsh
      }

      s"has a checkbox on the screen for '$listYear2017Text' with hint text '$valuationsBetween2017Text' in welsh" in {
        document.select(listYear2017TextSelector).text() shouldBe listYear2017TextWelsh
        document.select(listYear2017CheckboxSelector).attr("type") shouldBe "checkbox"
        document.select(valuationsBetween2017TextSelector).text() shouldBe valuationsBetween2017TextWelsh
      }

      s"has a '$continueText' button on the screen in welsh" in {
        document.select(continueTextSelector).text() shouldBe continueTextWelsh
      }
    }

    "Show the correct page when the back link is from cya and has pre-populated data" which {

      lazy val document = getPage(English, "?fromCyaChange=true", listYears = Seq("2023", "2017"))

      s"has a title of $titleText" in {
        document.title() shouldBe titleText
      }

      "has a back link which takes you to the agent details page" in {
        document.select(backLinkTextSelector).text() shouldBe backLinkText
        document.select(backLinkTextSelector).attr("href") shouldBe checkYourAnswersBackLinkHref
      }

      s"has a header of '$headerText' with a caption above of '$captionText'" in {
        document.select(headerTextSelector).text shouldBe headerText
        document.select(captionTextSelector).text shouldBe captionText
      }

      s"has inset-text on the screen of '$choosingText'" in {
        document.select(choosingTextSelector).text() shouldBe choosingText
      }

      s"has text on the screen of '$theRatingListText'" in {
        document.select(theRatingListTextSelector).text() shouldBe theRatingListText
      }

      s"has text on the screen of '$theAgentText'" in {
        document.select(theAgentTextSelector).text() shouldBe theAgentText
      }

      s"has text on the screen of '$selectAllText'" in {
        document.select(selectAllTextSelector).text() shouldBe selectAllText
      }

      s"has an unchecked checkbox on the screen for '$listYear2026Text' with hint text '$valuationsFromText'" in {
        document.select(listYear2026TextSelector).text() shouldBe listYear2026Text
        document.select(listYear2026CheckboxSelector).attr("type") shouldBe "checkbox"
        document.select(listYear2026CheckboxSelector).hasAttr("checked") shouldBe false
        document.select(valuationsFromTextSelector).text() shouldBe valuationsFromText
      }

      s"has a checked checkbox on the screen for '$listYear2023Text' with hint text '$valuationsBetween2023Text'" in {
        document.select(listYear2023TextSelector).text() shouldBe listYear2023Text
        document.select(listYear2023CheckboxSelector).attr("type") shouldBe "checkbox"
        document.select(listYear2023CheckboxSelector).hasAttr("checked") shouldBe true
        document.select(valuationsBetween2023TextSelector).text() shouldBe valuationsBetween2023Text
      }

      s"has a checked checkbox on the screen for '$listYear2017Text' with hint text '$valuationsBetween2017Text'" in {
        document.select(listYear2017TextSelector).text() shouldBe listYear2017Text
        document.select(listYear2017CheckboxSelector).attr("type") shouldBe "checkbox"
        document.select(listYear2017CheckboxSelector).hasAttr("checked") shouldBe true
        document.select(valuationsBetween2017TextSelector).text() shouldBe valuationsBetween2017Text
      }

      s"has a '$continueText' button on the screen, which submits the users choice" in {
        document.select(continueTextSelector).text() shouldBe continueText
      }
    }
  }

  "SelectRatingList Controller submit method" should {
    "redirect to the check your answers page when one list year chosen and no properties" in {
      val requestBody = Json.obj("listYearOne" -> "2017")
      val res = successPostPage(English, requestBody, testOwnerAuthResultNoProperties)

      res.status shouldBe SEE_OTHER
      res
        .headers("Location")
        .head shouldBe "/business-rates-property-linking/my-organisation/appoint-new-agent/check-your-answers"

    }

    "redirect to the single property page when two list years chosen and one property" in {
      val requestBody = Json.obj("listYearOne" -> "2017", "listYearTwo" -> "2023")
      val res = successPostPage(English, requestBody, testOwnerAuthResult1)

      res.status shouldBe SEE_OTHER
      res
        .headers("Location")
        .head shouldBe "/business-rates-property-linking/my-organisation/appoint-new-agent/one-property"

    }

    "redirect to the multiple property page when three list years chosen and multiple property" in {
      val requestBody = Json.obj("listYearOne" -> "2017", "listYearTwo" -> "2023", "listYearThree" -> "2026")
      val res = successPostPage(English, requestBody, testOwnerAuthResultMultipleProperty)

      res.status shouldBe SEE_OTHER
      res
        .headers("Location")
        .head shouldBe "/business-rates-property-linking/my-organisation/appoint-new-agent/multiple-properties"
    }

    "redirect to the check your answers page when ManagingProperty cached data and fromCyaPage" in {
      val requestBody = Json.obj("listYearOne" -> "2017")
      val res = successPostPage(
        English,
        requestBody,
        testOwnerAuthResultNoProperties,
        cacheDataType = "ManagingProperty",
        checkYourAnswers = "?fromCyaChange=true"
      )

      res.status shouldBe SEE_OTHER
      res
        .headers("Location")
        .head shouldBe "/business-rates-property-linking/my-organisation/appoint-new-agent/check-your-answers"

    }

    "redirect to the next page when ManagingProperty cached data and not fromCyaPage" in {
      val requestBody = Json.obj("listYearOne" -> "2017")
      val res =
        successPostPage(English, requestBody, testOwnerAuthResultMultipleProperty, cacheDataType = "ManagingProperty")

      res.status shouldBe SEE_OTHER
      res
        .headers("Location")
        .head shouldBe "/business-rates-property-linking/my-organisation/appoint-new-agent/multiple-properties"

    }

    "receive a bad request when answer is not selected in english" which {

      lazy val document = errorPostPage(English, Json.obj(), testOwnerAuthResultMultipleProperty)

      s"has a title of ${errorText + titleText}" in {
        document.title() shouldBe errorText + titleText
      }

      "has a back link which takes you to the agent details page" in {
        document.select(backLinkTextSelector).text() shouldBe backLinkText
        document.select(backLinkTextSelector).attr("href") shouldBe backLinkHref
      }

      s"has a header of '$headerText' with a caption above of '$captionText'" in {
        document.select(headerTextSelector).text shouldBe headerText
        document.select(captionTextSelector).text shouldBe captionText
      }

      s"has inset-text on the screen of '$choosingText'" in {
        document.select(choosingTextSelector).text() shouldBe choosingText
      }

      s"has text on the screen of '$theRatingListText'" in {
        document.select(theRatingListTextSelector).text() shouldBe theRatingListText
      }

      s"has text on the screen of '$theAgentText'" in {
        document.select(theAgentTextSelector).text() shouldBe theAgentText
      }

      s"has text on the screen of '$selectAllText'" in {
        document.select(selectAllTextSelector).text() shouldBe selectAllText
      }

      s"has an unchecked checkbox on the screen for '$listYear2026Text' with hint text '$valuationsFromText'" in {
        document.select(listYear2026TextSelector).text() shouldBe listYear2026Text
        document.select(listYear2026CheckboxSelector).attr("type") shouldBe "checkbox"
        document.select(listYear2026CheckboxSelector).hasAttr("checked") shouldBe false
        document.select(valuationsFromTextSelector).text() shouldBe valuationsFromText
      }

      s"has an unchecked checkbox on the screen for '$listYear2023Text' with hint text '$valuationsBetween2023Text'" in {
        document.select(listYear2023TextSelector).text() shouldBe listYear2023Text
        document.select(listYear2023CheckboxSelector).attr("type") shouldBe "checkbox"
        document.select(listYear2023CheckboxSelector).hasAttr("checked") shouldBe false
        document.select(valuationsBetween2023TextSelector).text() shouldBe valuationsBetween2023Text
      }

      s"has an unchecked checkbox on the screen for '$listYear2017Text' with hint text '$valuationsBetween2017Text'" in {
        document.select(listYear2017TextSelector).text() shouldBe listYear2017Text
        document.select(listYear2017CheckboxSelector).attr("type") shouldBe "checkbox"
        document.select(listYear2017CheckboxSelector).hasAttr("checked") shouldBe false
        document.select(valuationsBetween2017TextSelector).text() shouldBe valuationsBetween2017Text
      }

      s"has a '$continueText' button on the screen, which submits the users choice" in {
        document.select(continueTextSelector).text() shouldBe continueText
      }
    }

    "receive a bad request when answer is not selected in welsh" which {

      lazy val document = errorPostPage(Welsh, Json.obj(), testOwnerAuthResultMultipleProperty)

      s"has a title of ${errorText + titleText} in welsh" in {
        document.title() shouldBe errorTextWelsh + titleTextWelsh
      }

      "has a back link which takes you to the agent details page in welsh" in {
        document.select(backLinkTextSelector).text() shouldBe backLinkTextWelsh
        document.select(backLinkTextSelector).attr("href") shouldBe backLinkHref
      }

      s"has a header of '$headerText' with a caption above of '$captionText' in welsh" in {
        document.select(headerTextSelector).text shouldBe headerTextWelsh
        document.select(captionTextSelector).text shouldBe captionTextWelsh
      }

      s"has inset-text on the screen of '$choosingText' in welsh" in {
        document.select(choosingTextSelector).text() shouldBe choosingTextWelsh
      }

      s"has text on the screen of '$theRatingListText' in welsh" in {
        document.select(theRatingListTextSelector).text() shouldBe theRatingListTextWelsh
      }

      s"has text on the screen of '$theAgentText' in welsh" in {
        document.select(theAgentTextSelector).text() shouldBe theAgentTextWelsh
      }

      s"has text on the screen of '$selectAllText in welsh" in {
        document.select(selectAllTextSelector).text() shouldBe selectAllTextWelsh
      }

      s"has an unchecked checkbox on the screen for '$listYear2026Text' with hint text '$valuationsFromText' in welsh" in {
        document.select(listYear2026TextSelector).text() shouldBe listYear2026TextWelsh
        document.select(listYear2026CheckboxSelector).attr("type") shouldBe "checkbox"
        document.select(listYear2026CheckboxSelector).hasAttr("checked") shouldBe false
        document.select(valuationsFromTextSelector).text() shouldBe valuationsFromTextWelsh
      }

      s"has an unchecked checkbox on the screen for '$listYear2023Text' with hint text '$valuationsBetween2023Text' in welsh" in {
        document.select(listYear2023TextSelector).text() shouldBe listYear2023TextWelsh
        document.select(listYear2023CheckboxSelector).attr("type") shouldBe "checkbox"
        document.select(listYear2023CheckboxSelector).hasAttr("checked") shouldBe false
        document.select(valuationsBetween2023TextSelector).text() shouldBe valuationsBetween2023TextWelsh
      }

      s"has an unchecked checkbox on the screen for '$listYear2017Text' with hint text '$valuationsBetween2017Text' in welsh" in {
        document.select(listYear2017TextSelector).text() shouldBe listYear2017TextWelsh
        document.select(listYear2017CheckboxSelector).attr("type") shouldBe "checkbox"
        document.select(listYear2017CheckboxSelector).hasAttr("checked") shouldBe false
        document.select(valuationsBetween2017TextSelector).text() shouldBe valuationsBetween2017TextWelsh
      }

      s"has a '$continueText' button on the screen, which submits the users choice in welsh" in {
        document.select(continueTextSelector).text() shouldBe continueTextWelsh
      }
    }
  }

  private def getPage(
        language: Language,
        checkYourAnswers: String = "",
        listYears: Seq[String] = Seq.empty
  ): Document = {

    commonStubs(listYears)

    val res = await(
      ws.url(
        s"http://localhost:$port/business-rates-property-linking/my-organisation/appoint-new-agent/ratings-list-new$checkYourAnswers"
      ).withCookies(languageCookie(language), getSessionCookie(testSessionId))
        .withFollowRedirects(follow = false)
        .get()
    )

    res.status shouldBe OK
    Jsoup.parse(res.body)
  }
  private def successPostPage(
        language: Language,
        requestBody: JsObject,
        properties: OwnerAuthResult,
        cacheDataType: String = "selectedAgent",
        checkYourAnswers: String = ""
  ): WSResponse = {

    commonStubs(cacheDataType = cacheDataType)

    stubFor {
      get(
        "/property-linking/my-organisation/agents/1001/available-property-links?sortField=ADDRESS&sortOrder=ASC&startPoint=1&pageSize=15&requestTotalRowCount=false"
      ).willReturn {
        aResponse.withStatus(OK).withBody(Json.toJson(properties).toString())
      }
    }

    await(
      ws.url(
        s"http://localhost:$port/business-rates-property-linking/my-organisation/appoint-new-agent/ratings-list-new$checkYourAnswers"
      ).withCookies(languageCookie(language), getSessionCookie(testSessionId))
        .withFollowRedirects(follow = false)
        .withHttpHeaders(HeaderNames.COOKIE -> "sessionId", "Csrf-Token" -> "nocheck")
        .post(body = requestBody)
    )
  }

  private def errorPostPage(language: Language, requestBody: JsObject, properties: OwnerAuthResult): Document = {

    commonStubs()

    stubFor {
      get(
        "/property-linking/my-organisation/agents/1001/available-property-links?sortField=ADDRESS&sortOrder=ASC&startPoint=1&pageSize=15&requestTotalRowCount=false"
      ).willReturn {
        aResponse.withStatus(OK).withBody(Json.toJson(properties).toString())
      }
    }

    val res = await(
      ws.url(
        s"http://localhost:$port/business-rates-property-linking/my-organisation/appoint-new-agent/ratings-list-new"
      ).withCookies(languageCookie(language), getSessionCookie(testSessionId))
        .withFollowRedirects(follow = false)
        .withHttpHeaders(HeaderNames.COOKIE -> "sessionId", "Csrf-Token" -> "nocheck")
        .post(body = requestBody)
    )

    res.status shouldBe BAD_REQUEST
    Jsoup.parse(res.body)

  }

  private def commonStubs(listYears: Seq[String] = Seq.empty, cacheDataType: String = "selectedAgent") = {
    implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId(testSessionId)))

    lazy val mockAppointAgentSessionRepository: AppointAgentSessionRepository =
      app.injector.instanceOf[AppointAgentSessionRepository]

    val searchedAgentData: SearchedAgent = SearchedAgent.apply(1001, "Some Org", "street", AgentSelected, None)

    val selectedAgentData = SelectedAgent.apply(searchedAgentData, isTheCorrectAgent = true, listYears)

    val managingProperty = ManagingProperty.apply(selectedAgentData, "selection", singleProperty = false, 1, 1)

    if (cacheDataType.equals("selectedAgent")) {
      await(mockAppointAgentSessionRepository.saveOrUpdate(selectedAgentData))
    } else {
      await(mockAppointAgentSessionRepository.saveOrUpdate(managingProperty))
    }

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
  }
}
