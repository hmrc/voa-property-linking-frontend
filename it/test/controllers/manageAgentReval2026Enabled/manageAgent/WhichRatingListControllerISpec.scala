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

package controllers.manageAgentReval2026Enabled.manageAgent

import base.{HtmlComponentHelpers, ISpecBase}
import com.github.tomakehurst.wiremock.client.WireMock.{aResponse, get, post, stubFor}
import models.propertyrepresentation.AgentSummary
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.http.HeaderNames
import play.api.http.Status.OK
import play.api.libs.json.{JsObject, Json}
import play.api.test.Helpers._
import repositories.ManageAgentSessionRepository
import uk.gov.hmrc.http.{HeaderCarrier, SessionId}

import java.time.LocalDate
import java.util.UUID

class WhichRatingListControllerISpec extends ISpecBase with HtmlComponentHelpers {
  override lazy val extraConfig: Map[String, String] =
    Map("featureFlags.agentJourney2026Enabled" -> "true")

  lazy val mockRepository: ManageAgentSessionRepository = app.injector.instanceOf[ManageAgentSessionRepository]
  val testSessionId = s"stubbed-${UUID.randomUUID}"
  implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId(testSessionId)))

  //English content
  val titleText = "Select which rating list Test Agent can act on for you - Valuation Office Agency - GOV.UK"
  val errorTitleText = "Error: Select which rating list Test Agent can act on for you - Valuation Office Agency - GOV.UK"
  val backLinkText = "Back"
  val captionText = "Manage agent"
  val headerText = "Select which rating list Test Agent can act on for you"
  val currentlyThisTextSingleListYear = "Currently this agent can act for you on the 2023 rating list"
  val currentlyThisTextTwoListYears = "Currently this agent can act for you on the 2023 and 2017 rating lists"
  val currentlyThisTextThreeListYears = "Currently this agent can act for you on the 2026, 2023, and 2017 rating lists"
  val p1Text = "The rating list you choose for this agent will apply to all properties that you assign to them and they add to your account."
  val p2Text = "The agent will only be able to act for you on valuations on the rating list you choose."
  val selectAllThatApplyText = "Select all that apply."
  val checkBox2026Text = "2026"
  val checkBox2026HintText = "For valuations from 1 April 2026."
  val checkBox2023Text = "2023"
  val checkBox2023HintText = "For valuations between 1 April 2023 and 31 March 2026."
  val checkBox2017Text = "2017"
  val checkBox2017HintText = "For valuations between 1 April 2017 and 31 March 2023."
  val continueText = "Continue"
  val errorText = "Select which rating list you want this agent to act on for you"
  val thereIsAProblemText = "There is a problem"
  val aboveRadioErrorText = "Error: Select which rating list you want this agent to act on for you"

  //Welsh Content
  val titleTextWelsh = "Dewiswch pa restr ardrethu y gall Test Agent weithredu arni ar eich rhan - Valuation Office Agency - GOV.UK"
  val errorTitleTextWelsh = "Gwall: Dewiswch pa restr ardrethu y gall Test Agent weithredu arni ar eich rhan - Valuation Office Agency - GOV.UK"
  val backLinkTextWelsh = "Yn ôl"
  val captionTextWelsh = "Rheoli asiant"
  val headerTextWelsh = "Dewiswch pa restr ardrethu y gall Test Agent weithredu arni ar eich rhan"
  val currentlyThisTextSingleListYearWelsh = "Ar hyn o bryd gall yr asiant hwn weithredu ar eich rhan ar restr ardrethu 2023"
  val currentlyThisTextTwoListYearsWelsh = "Ar hyn o bryd gall yr asiant hwn weithredu ar eich rhan ar restrau ardrethu 2023 a 2017"
  val currentlyThisTextThreeListYearsWelsh = "Ar hyn o bryd gall yr asiant hwn weithredu ar eich rhan ar restrau ardrethu 2026, 2023 a 2017"
  val p1TextWelsh = "Bydd y rhestr ardrethu a ddewiswch ar gyfer yr asiant hwn yn berthnasol i’r holl eiddo rydych chi’n ei aseinio iddynt ac maen nhw’n ychwanegu at eich cyfrif."
  val p2TextWelsh = "Dim ond ar brisiadau ar y rhestr ardrethu a ddewiswch y bydd yr asiant yn gallu gweithredu ar eich rhan."
  val selectAllThatApplyTextWelsh = "Dewiswch bob un sy’n berthnasol."
  val checkBox2026TextWelsh = "2026"
  val checkBox2026HintTextWelsh = "Ar gyfer prisiadau o 1 Ebrill 2026."
  val checkBox2023TextWelsh = "2023"
  val checkBox2023HintTextWelsh = "Ar gyfer prisiadau rhwng 1 Ebrill 2023 a 31 Mawrth 2026."
  val checkBox2017TextWelsh = "2017"
  val checkBox2017HintTextWelsh = "Ar gyfer prisiadau rhwng 1 Ebrill 2017 a 31 Mawrth 2023."
  val continueTextWelsh = "Parhau"
  val errorTextWelsh = "Dewiswch pa restr ardrethu rydych chi am i’r asiant hwn weithredu arni ar eich rhan"
  val thereIsAProblemTextWelsh = "Mae yna broblem"
  val aboveRadioErrorTextWelsh = "Gwall: Dewiswch pa restr ardrethu rydych chi am i’r asiant hwn weithredu arni ar eich rhan"

  //Selectors
  val backLinkTextSelector = "#back-link"
  val captionTextSelector = "#main-content > div > div > span"
  val headerTextSelector = "#main-content > div > div > h1"
  val currentlyThisTextSelector = "#main-content > div > div > div"
  val currentlyThisTextSelectorWithError = "#main-content > div > div > div.govuk-inset-text"
  val p1TextSelector = "#main-content > div > div > p:nth-child(4)"
  val p2TextSelector = "#main-content > div > div > p:nth-child(5)"
  val selectAllThatApplyTextSelector = "#main-content > div > div > form > div > fieldset > legend"
  val checkBox2026Selector = "#listYears"
  val checkBox2026TextSelector = "#main-content > div > div > form > div > fieldset > div > div:nth-child(1) > label"
  val checkBox2026HintTextSelector = "#listYears-item-hint"
  val checkBox2023Selector = "#listYears-2"
  val checkBox2023TextSelector = "#main-content > div > div > form > div > fieldset > div > div:nth-child(2) > label"
  val checkBox2023HintTextSelector = "#listYears-2-item-hint"
  val checkBox2017Selector = "#listYears-3"
  val checkBox2017TextSelector = "#main-content > div > div > form > div > fieldset > div > div:nth-child(3) > label"
  val checkBox2017HintTextSelector = "#listYears-3-item-hint"
  val continueTextSelector = "#continue"
  val errorTextSelector = "#main-content > div > div > div.govuk-error-summary > div > div > ul > li > a"
  val thereIsAProblemTextSelector = "#main-content > div > div > div.govuk-error-summary > div > h2"
  val aboveRadioErrorTextSelector = "#listYears-error"

  val backLinkHref = "/business-rates-property-linking/my-organisation/manage-agent"

  "WhichRatingListController show method" should {
    "Display the 'which rating list' screen with the correct content for agent assigned to one listYear - English" which {

      lazy val document = getWhichRatingListPage(English, List("2023"))

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

      s"has inset text on the screen of '$currentlyThisTextSingleListYear'" in {
        document.select(currentlyThisTextSelector).text() shouldBe currentlyThisTextSingleListYear
      }

      s"has text on the screen of '$p1Text'" in {
        document.select(p1TextSelector).text() shouldBe p1Text
      }

      s"has text on the screen of '$p2Text'" in {
        document.select(p2TextSelector).text() shouldBe p2Text
      }

      s"has text on the screen of '$selectAllThatApplyText'" in {
        document.select(selectAllThatApplyTextSelector).text() shouldBe selectAllThatApplyText
      }

      s"has an un-checked '$checkBox2026Text' radio button, with hint text of '$checkBox2026HintText'" in {
        document.select(checkBox2026TextSelector).hasAttr("checked") shouldBe false
        document.select(checkBox2026TextSelector).text() shouldBe checkBox2026Text
        document.select(checkBox2026HintTextSelector).text() shouldBe checkBox2026HintText
      }

      s"has an un-checked '$checkBox2023Text' radio button, with hint text of '$checkBox2023HintText'" in {
        document.select(checkBox2023TextSelector).hasAttr("checked") shouldBe false
        document.select(checkBox2023TextSelector).text() shouldBe checkBox2023Text
        document.select(checkBox2023HintTextSelector).text() shouldBe checkBox2023HintText
      }

      s"has an un-checked '$checkBox2017Text' radio button, with hint text of '$checkBox2017HintText'" in {
        document.select(checkBox2017TextSelector).hasAttr("checked") shouldBe false
        document.select(checkBox2017TextSelector).text() shouldBe checkBox2017Text
        document.select(checkBox2017HintTextSelector).text() shouldBe checkBox2017HintText
      }

      s"has a '$continueText' button on the screen, which submits the users choice" in {
        document.select(continueTextSelector).text() shouldBe continueText
      }
    }

    "Display the 'which rating list' screen with the correct content for agent assigned to one list year - English" which {

      lazy val document = getWhichRatingListPage(Welsh, List("2023"))

      s"has a title of $titleText" in {
        document.title() shouldBe titleTextWelsh
      }

      "has a back link which takes you to the agent details page" in {
        document.select(backLinkTextSelector).text() shouldBe backLinkTextWelsh
        document.select(backLinkTextSelector).attr("href") shouldBe backLinkHref
      }

      s"has a header of '$headerText' with a caption above of '$captionText'" in {
        document.select(headerTextSelector).text shouldBe headerTextWelsh
        document.select(captionTextSelector).text shouldBe captionTextWelsh
      }

      s"has inset text on the screen of '$currentlyThisTextSingleListYear'" in {
        document.select(currentlyThisTextSelector).text() shouldBe currentlyThisTextSingleListYearWelsh
      }

      s"has text on the screen of '$p1Text'" in {
        document.select(p1TextSelector).text() shouldBe p1TextWelsh
      }

      s"has text on the screen of '$p2Text'" in {
        document.select(p2TextSelector).text() shouldBe p2TextWelsh
      }

      s"has text on the screen of '$selectAllThatApplyText'" in {
        document.select(selectAllThatApplyTextSelector).text() shouldBe selectAllThatApplyTextWelsh
      }

      s"has an un-checked '$checkBox2026Text' radio button, with hint text of '$checkBox2026HintText'" in {
        document.select(checkBox2026TextSelector).hasAttr("checked") shouldBe false
        document.select(checkBox2026TextSelector).text() shouldBe checkBox2026TextWelsh
        document.select(checkBox2026HintTextSelector).text() shouldBe checkBox2026HintTextWelsh
      }

      s"has an un-checked '$checkBox2023Text' radio button, with hint text of '$checkBox2023HintText'" in {
        document.select(checkBox2023TextSelector).hasAttr("checked") shouldBe false
        document.select(checkBox2023TextSelector).text() shouldBe checkBox2023TextWelsh
        document.select(checkBox2023HintTextSelector).text() shouldBe checkBox2023HintTextWelsh
      }

      s"has an un-checked '$checkBox2017Text' radio button, with hint text of '$checkBox2017HintText'" in {
        document.select(checkBox2017TextSelector).hasAttr("checked") shouldBe false
        document.select(checkBox2017TextSelector).text() shouldBe checkBox2017TextWelsh
        document.select(checkBox2017HintTextSelector).text() shouldBe checkBox2017HintTextWelsh
      }

      s"has a '$continueText' button on the screen, which submits the users choice" in {
        document.select(continueTextSelector).text() shouldBe continueTextWelsh
      }
    }

    "Display the 'which rating list' screen with the correct content for agent assigned to two list years - English" which {

      lazy val document = getWhichRatingListPage(English, List("2023", "2017"))

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

      s"has inset text on the screen of '$currentlyThisTextSingleListYear'" in {
        document.select(currentlyThisTextSelector).text() shouldBe currentlyThisTextTwoListYears
      }

      s"has text on the screen of '$p1Text'" in {
        document.select(p1TextSelector).text() shouldBe p1Text
      }

      s"has text on the screen of '$p2Text'" in {
        document.select(p2TextSelector).text() shouldBe p2Text
      }

      s"has text on the screen of '$selectAllThatApplyText'" in {
        document.select(selectAllThatApplyTextSelector).text() shouldBe selectAllThatApplyText
      }

      s"has an un-checked '$checkBox2026Text' radio button, with hint text of '$checkBox2026HintText'" in {
        document.select(checkBox2026TextSelector).hasAttr("checked") shouldBe false
        document.select(checkBox2026TextSelector).text() shouldBe checkBox2026Text
        document.select(checkBox2026HintTextSelector).text() shouldBe checkBox2026HintText
      }

      s"has an un-checked '$checkBox2023Text' radio button, with hint text of '$checkBox2023HintText'" in {
        document.select(checkBox2023TextSelector).hasAttr("checked") shouldBe false
        document.select(checkBox2023TextSelector).text() shouldBe checkBox2023Text
        document.select(checkBox2023HintTextSelector).text() shouldBe checkBox2023HintText
      }

      s"has an un-checked '$checkBox2017Text' radio button, with hint text of '$checkBox2017HintText'" in {
        document.select(checkBox2017TextSelector).hasAttr("checked") shouldBe false
        document.select(checkBox2017TextSelector).text() shouldBe checkBox2017Text
        document.select(checkBox2017HintTextSelector).text() shouldBe checkBox2017HintText
      }

      s"has a '$continueText' button on the screen, which submits the users choice" in {
        document.select(continueTextSelector).text() shouldBe continueText
      }
    }

    "Display the 'which rating list' screen with the correct content for agent assigned to two list years - Welsh" which {

      lazy val document = getWhichRatingListPage(Welsh, List("2023", "2017"))

      s"has a title of $titleText" in {
        document.title() shouldBe titleTextWelsh
      }

      "has a back link which takes you to the agent details page" in {
        document.select(backLinkTextSelector).text() shouldBe backLinkTextWelsh
        document.select(backLinkTextSelector).attr("href") shouldBe backLinkHref
      }

      s"has a header of '$headerText' with a caption above of '$captionText'" in {
        document.select(headerTextSelector).text shouldBe headerTextWelsh
        document.select(captionTextSelector).text shouldBe captionTextWelsh
      }

      s"has inset text on the screen of '$currentlyThisTextSingleListYear'" in {
        document.select(currentlyThisTextSelector).text() shouldBe currentlyThisTextTwoListYearsWelsh
      }

      s"has text on the screen of '$p1Text'" in {
        document.select(p1TextSelector).text() shouldBe p1TextWelsh
      }

      s"has text on the screen of '$p2Text'" in {
        document.select(p2TextSelector).text() shouldBe p2TextWelsh
      }

      s"has text on the screen of '$selectAllThatApplyText'" in {
        document.select(selectAllThatApplyTextSelector).text() shouldBe selectAllThatApplyTextWelsh
      }

      s"has an un-checked '$checkBox2026Text' radio button, with hint text of '$checkBox2026HintText'" in {
        document.select(checkBox2026TextSelector).hasAttr("checked") shouldBe false
        document.select(checkBox2026TextSelector).text() shouldBe checkBox2026TextWelsh
        document.select(checkBox2026HintTextSelector).text() shouldBe checkBox2026HintTextWelsh
      }

      s"has an un-checked '$checkBox2023Text' radio button, with hint text of '$checkBox2023HintText'" in {
        document.select(checkBox2023TextSelector).hasAttr("checked") shouldBe false
        document.select(checkBox2023TextSelector).text() shouldBe checkBox2023TextWelsh
        document.select(checkBox2023HintTextSelector).text() shouldBe checkBox2023HintTextWelsh
      }

      s"has an un-checked '$checkBox2017Text' radio button, with hint text of '$checkBox2017HintText'" in {
        document.select(checkBox2017TextSelector).hasAttr("checked") shouldBe false
        document.select(checkBox2017TextSelector).text() shouldBe checkBox2017TextWelsh
        document.select(checkBox2017HintTextSelector).text() shouldBe checkBox2017HintTextWelsh
      }

      s"has a '$continueText' button on the screen, which submits the users choice" in {
        document.select(continueTextSelector).text() shouldBe continueTextWelsh
      }
    }

    "Display the 'which rating list' screen with the correct content for agent assigned to all list years - English" which {

      lazy val document = getWhichRatingListPage(English, List("2026", "2023", "2017"))

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

      s"has inset text on the screen of '$currentlyThisTextSingleListYear'" in {
        document.select(currentlyThisTextSelector).text() shouldBe currentlyThisTextThreeListYears
      }

      s"has text on the screen of '$p1Text'" in {
        document.select(p1TextSelector).text() shouldBe p1Text
      }

      s"has text on the screen of '$p2Text'" in {
        document.select(p2TextSelector).text() shouldBe p2Text
      }

      s"has text on the screen of '$selectAllThatApplyText'" in {
        document.select(selectAllThatApplyTextSelector).text() shouldBe selectAllThatApplyText
      }

      s"has an un-checked '$checkBox2026Text' radio button, with hint text of '$checkBox2026HintText'" in {
        document.select(checkBox2026TextSelector).hasAttr("checked") shouldBe false
        document.select(checkBox2026TextSelector).text() shouldBe checkBox2026Text
        document.select(checkBox2026HintTextSelector).text() shouldBe checkBox2026HintText
      }

      s"has an un-checked '$checkBox2023Text' radio button, with hint text of '$checkBox2023HintText'" in {
        document.select(checkBox2023TextSelector).hasAttr("checked") shouldBe false
        document.select(checkBox2023TextSelector).text() shouldBe checkBox2023Text
        document.select(checkBox2023HintTextSelector).text() shouldBe checkBox2023HintText
      }

      s"has an un-checked '$checkBox2017Text' radio button, with hint text of '$checkBox2017HintText'" in {
        document.select(checkBox2017TextSelector).hasAttr("checked") shouldBe false
        document.select(checkBox2017TextSelector).text() shouldBe checkBox2017Text
        document.select(checkBox2017HintTextSelector).text() shouldBe checkBox2017HintText
      }

      s"has a '$continueText' button on the screen, which submits the users choice" in {
        document.select(continueTextSelector).text() shouldBe continueText
      }
    }

    "Display the 'which rating list' screen with the correct content for agent assigned to all list years - Welsh" which {

      lazy val document = getWhichRatingListPage(Welsh, List("2026", "2023", "2017"))

      s"has a title of $titleText" in {
        document.title() shouldBe titleTextWelsh
      }

      "has a back link which takes you to the agent details page" in {
        document.select(backLinkTextSelector).text() shouldBe backLinkTextWelsh
        document.select(backLinkTextSelector).attr("href") shouldBe backLinkHref
      }

      s"has a header of '$headerText' with a caption above of '$captionText'" in {
        document.select(headerTextSelector).text shouldBe headerTextWelsh
        document.select(captionTextSelector).text shouldBe captionTextWelsh
      }

      s"has inset text on the screen of '$currentlyThisTextSingleListYear'" in {
        document.select(currentlyThisTextSelector).text() shouldBe currentlyThisTextThreeListYearsWelsh
      }

      s"has text on the screen of '$p1Text'" in {
        document.select(p1TextSelector).text() shouldBe p1TextWelsh
      }

      s"has text on the screen of '$p2Text'" in {
        document.select(p2TextSelector).text() shouldBe p2TextWelsh
      }

      s"has text on the screen of '$selectAllThatApplyText'" in {
        document.select(selectAllThatApplyTextSelector).text() shouldBe selectAllThatApplyTextWelsh
      }

      s"has an un-checked '$checkBox2026Text' radio button, with hint text of '$checkBox2026HintText'" in {
        document.select(checkBox2026TextSelector).hasAttr("checked") shouldBe false
        document.select(checkBox2026TextSelector).text() shouldBe checkBox2026TextWelsh
        document.select(checkBox2026HintTextSelector).text() shouldBe checkBox2026HintTextWelsh
      }

      s"has an un-checked '$checkBox2023Text' radio button, with hint text of '$checkBox2023HintText'" in {
        document.select(checkBox2023TextSelector).hasAttr("checked") shouldBe false
        document.select(checkBox2023TextSelector).text() shouldBe checkBox2023TextWelsh
        document.select(checkBox2023HintTextSelector).text() shouldBe checkBox2023HintTextWelsh
      }

      s"has an un-checked '$checkBox2017Text' radio button, with hint text of '$checkBox2017HintText'" in {
        document.select(checkBox2017TextSelector).hasAttr("checked") shouldBe false
        document.select(checkBox2017TextSelector).text() shouldBe checkBox2017TextWelsh
        document.select(checkBox2017HintTextSelector).text() shouldBe checkBox2017HintTextWelsh
      }

      s"has a '$continueText' button on the screen, which submits the users choice" in {
        document.select(continueTextSelector).text() shouldBe continueTextWelsh
      }
    }

    "Display the 'which rating list' screen with the correct content for agent assigned to all list years with cached answers - English" which {

      lazy val document = getWhichRatingListPage(English, listYears = List("2026", "2023", "2017"), withCachedListYears = Some(List("2026", "2023", "2017")))

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

      s"has inset text on the screen of '$currentlyThisTextSingleListYear'" in {
        document.select(currentlyThisTextSelector).text() shouldBe currentlyThisTextThreeListYears
      }

      s"has text on the screen of '$p1Text'" in {
        document.select(p1TextSelector).text() shouldBe p1Text
      }

      s"has text on the screen of '$p2Text'" in {
        document.select(p2TextSelector).text() shouldBe p2Text
      }

      s"has text on the screen of '$selectAllThatApplyText'" in {
        document.select(selectAllThatApplyTextSelector).text() shouldBe selectAllThatApplyText
      }

      s"has an checked '$checkBox2026Text' radio button, with hint text of '$checkBox2026HintText'" in {
        document.select(checkBox2026Selector).hasAttr("checked") shouldBe true
        document.select(checkBox2026TextSelector).text() shouldBe checkBox2026Text
        document.select(checkBox2026HintTextSelector).text() shouldBe checkBox2026HintText
      }

      s"has an checked '$checkBox2023Text' radio button, with hint text of '$checkBox2023HintText'" in {
        document.select(checkBox2023Selector).hasAttr("checked") shouldBe true
        document.select(checkBox2023TextSelector).text() shouldBe checkBox2023Text
        document.select(checkBox2023HintTextSelector).text() shouldBe checkBox2023HintText
      }

      s"has an checked '$checkBox2017Text' radio button, with hint text of '$checkBox2017HintText'" in {
        document.select(checkBox2017Selector).hasAttr("checked") shouldBe true
        document.select(checkBox2017TextSelector).text() shouldBe checkBox2017Text
        document.select(checkBox2017HintTextSelector).text() shouldBe checkBox2017HintText
      }

      s"has a '$continueText' button on the screen, which submits the users choice" in {
        document.select(continueTextSelector).text() shouldBe continueText
      }
    }

    "Display the 'which rating list' screen with the correct content for agent assigned to all list years with cached answers - Welsh" which {

      lazy val document = getWhichRatingListPage(Welsh, listYears = List("2026", "2023", "2017"), withCachedListYears = Some(List("2026", "2023", "2017")))

      s"has a title of $titleText" in {
        document.title() shouldBe titleTextWelsh
      }

      "has a back link which takes you to the agent details page" in {
        document.select(backLinkTextSelector).text() shouldBe backLinkTextWelsh
        document.select(backLinkTextSelector).attr("href") shouldBe backLinkHref
      }

      s"has a header of '$headerText' with a caption above of '$captionText'" in {
        document.select(headerTextSelector).text shouldBe headerTextWelsh
        document.select(captionTextSelector).text shouldBe captionTextWelsh
      }

      s"has inset text on the screen of '$currentlyThisTextSingleListYear'" in {
        document.select(currentlyThisTextSelector).text() shouldBe currentlyThisTextThreeListYearsWelsh
      }

      s"has text on the screen of '$p1Text'" in {
        document.select(p1TextSelector).text() shouldBe p1TextWelsh
      }

      s"has text on the screen of '$p2Text'" in {
        document.select(p2TextSelector).text() shouldBe p2TextWelsh
      }

      s"has text on the screen of '$selectAllThatApplyText'" in {
        document.select(selectAllThatApplyTextSelector).text() shouldBe selectAllThatApplyTextWelsh
      }

      s"has an checked '$checkBox2026Text' radio button, with hint text of '$checkBox2026HintText'" in {
        document.select(checkBox2026Selector).hasAttr("checked") shouldBe true
        document.select(checkBox2026TextSelector).text() shouldBe checkBox2026TextWelsh
        document.select(checkBox2026HintTextSelector).text() shouldBe checkBox2026HintTextWelsh
      }

      s"has an checked '$checkBox2023Text' radio button, with hint text of '$checkBox2023HintText'" in {
        document.select(checkBox2023Selector).hasAttr("checked") shouldBe true
        document.select(checkBox2023TextSelector).text() shouldBe checkBox2023TextWelsh
        document.select(checkBox2023HintTextSelector).text() shouldBe checkBox2023HintTextWelsh
      }

      s"has an checked '$checkBox2017Text' radio button, with hint text of '$checkBox2017HintText'" in {
        document.select(checkBox2017Selector).hasAttr("checked") shouldBe true
        document.select(checkBox2017TextSelector).text() shouldBe checkBox2017TextWelsh
        document.select(checkBox2017HintTextSelector).text() shouldBe checkBox2017HintTextWelsh
      }

      s"has a '$continueText' button on the screen, which submits the users choice" in {
        document.select(continueTextSelector).text() shouldBe continueTextWelsh
      }
    }
  }

  "WhichRatingListController post method" should {

    "Display the 'which rating list' screen with the correct content when no check box was selected - English" which {

      lazy val result = postWhichRatingListPage(English, List("2026", "2023", "2017"))
      lazy val document = Jsoup.parse(result.body)

      s"has a status of $BAD_REQUEST" in {
        result.status shouldBe BAD_REQUEST
      }

      s"has a title of $titleText" in {
        document.title() shouldBe errorTitleText
      }

      s"has an error summary that contains the correct error message '$errorText'" in {
        document.select(thereIsAProblemTextSelector).text() shouldBe thereIsAProblemText
        document.select(errorTextSelector).text() shouldBe errorText
      }

      "has a back link which takes you to the agent details page" in {
        document.select(backLinkTextSelector).text() shouldBe backLinkText
        document.select(backLinkTextSelector).attr("href") shouldBe backLinkHref
      }

      s"has a header of '$headerText' with a caption above of '$captionText'" in {
        document.select(headerTextSelector).text shouldBe headerText
        document.select(captionTextSelector).text shouldBe captionText
      }

      s"has inset text on the screen of '$currentlyThisTextSingleListYear'" in {
        document.select(currentlyThisTextSelectorWithError).text() shouldBe currentlyThisTextThreeListYears
      }

      s"has text on the screen of '$p1Text'" in {
        document.select("#main-content > div > div > p:nth-child(5)").text() shouldBe p1Text
      }

      s"has text on the screen of '$p2Text'" in {
        document.select("#main-content > div > div > p:nth-child(6)").text() shouldBe p2Text
      }

      s"has text on the screen of '$selectAllThatApplyText'" in {
        document.select(selectAllThatApplyTextSelector).text() shouldBe selectAllThatApplyText
      }

      s"has an error message above the radio button of $aboveRadioErrorText" in {
        document.select(aboveRadioErrorTextSelector).text() shouldBe aboveRadioErrorText
      }

      s"has an un-checked '$checkBox2026Text' radio button, with hint text of '$checkBox2026HintText'" in {
        document.select(checkBox2026TextSelector).hasAttr("checked") shouldBe false
        document.select(checkBox2026TextSelector).text() shouldBe checkBox2026Text
        document.select(checkBox2026HintTextSelector).text() shouldBe checkBox2026HintText
      }

      s"has an un-checked '$checkBox2023Text' radio button, with hint text of '$checkBox2023HintText'" in {
        document.select(checkBox2023TextSelector).hasAttr("checked") shouldBe false
        document.select(checkBox2023TextSelector).text() shouldBe checkBox2023Text
        document.select(checkBox2023HintTextSelector).text() shouldBe checkBox2023HintText
      }

      s"has an un-checked '$checkBox2017Text' radio button, with hint text of '$checkBox2017HintText'" in {
        document.select(checkBox2017TextSelector).hasAttr("checked") shouldBe false
        document.select(checkBox2017TextSelector).text() shouldBe checkBox2017Text
        document.select(checkBox2017HintTextSelector).text() shouldBe checkBox2017HintText
      }

      s"has a '$continueText' button on the screen, which submits the users choice" in {
        document.select(continueTextSelector).text() shouldBe continueText
      }
    }

    "Display the 'which rating list' screen with the correct content when no check box was selected - Welsh" which {

      lazy val result = postWhichRatingListPage(Welsh, List("2026", "2023", "2017"))
      lazy val document = Jsoup.parse(result.body)

      s"has a status of $BAD_REQUEST" in {
        result.status shouldBe BAD_REQUEST
      }

      s"has a title of $errorTitleText" in {
        document.title() shouldBe errorTitleTextWelsh
      }

      s"has an error summary that contains the correct error message '$errorText'" in {
        document.select(thereIsAProblemTextSelector).text() shouldBe thereIsAProblemTextWelsh
        document.select(errorTextSelector).text() shouldBe errorTextWelsh
      }

      "has a back link which takes you to the agent details page" in {
        document.select(backLinkTextSelector).text() shouldBe backLinkTextWelsh
        document.select(backLinkTextSelector).attr("href") shouldBe backLinkHref
      }

      s"has a header of '$headerText' with a caption above of '$captionText'" in {
        document.select(headerTextSelector).text shouldBe headerTextWelsh
        document.select(captionTextSelector).text shouldBe captionTextWelsh
      }

      s"has inset text on the screen of '$currentlyThisTextSingleListYear'" in {
        document.select(currentlyThisTextSelectorWithError).text() shouldBe currentlyThisTextThreeListYearsWelsh
      }

      s"has text on the screen of '$p1Text'" in {
        document.select("#main-content > div > div > p:nth-child(5)").text() shouldBe p1TextWelsh
      }

      s"has text on the screen of '$p2Text'" in {
        document.select("#main-content > div > div > p:nth-child(6)").text() shouldBe p2TextWelsh
      }

      s"has text on the screen of '$selectAllThatApplyText'" in {
        document.select(selectAllThatApplyTextSelector).text() shouldBe selectAllThatApplyTextWelsh
      }

      s"has an error message above the radio button of $aboveRadioErrorText" in {
        document.select(aboveRadioErrorTextSelector).text() shouldBe aboveRadioErrorTextWelsh
      }

      s"has an un-checked '$checkBox2026Text' radio button, with hint text of '$checkBox2026HintText'" in {
        document.select(checkBox2026TextSelector).hasAttr("checked") shouldBe false
        document.select(checkBox2026TextSelector).text() shouldBe checkBox2026TextWelsh
        document.select(checkBox2026HintTextSelector).text() shouldBe checkBox2026HintTextWelsh
      }

      s"has an un-checked '$checkBox2023Text' radio button, with hint text of '$checkBox2023HintText'" in {
        document.select(checkBox2023TextSelector).hasAttr("checked") shouldBe false
        document.select(checkBox2023TextSelector).text() shouldBe checkBox2023TextWelsh
        document.select(checkBox2023HintTextSelector).text() shouldBe checkBox2023HintTextWelsh
      }

      s"has an un-checked '$checkBox2017Text' radio button, with hint text of '$checkBox2017HintText'" in {
        document.select(checkBox2017TextSelector).hasAttr("checked") shouldBe false
        document.select(checkBox2017TextSelector).text() shouldBe checkBox2017TextWelsh
        document.select(checkBox2017HintTextSelector).text() shouldBe checkBox2017HintTextWelsh
      }

      s"has a '$continueText' button on the screen, which submits the users choice" in {
        document.select(continueTextSelector).text() shouldBe continueTextWelsh
      }
    }

    "Redirect to the correct url on successful submission with all checkboxes selected" should {
      val requestBody = Json.obj("listYearOne" -> "2026", "listYearTwo" -> "2023", "listYearThree" -> "2017")

      lazy val result = postWhichRatingListPage(Welsh, List("2026", "2023", "2017"), body = requestBody)

      s"should have the correct $SEE_OTHER status" in {
        result.status shouldBe SEE_OTHER
      }
      //TODO - update when following controller has been updated
      s"should have the redirect location" in {
        result.headers("Location")
          .head shouldBe "/business-rates-property-linking/my-organisation/appoint/ratings-list/are-you-sure?chosenListYear=2023"
      }
    }

    "Redirect to the correct url on successful submission with two checkboxes selected" should {
      val requestBody = Json.obj("listYearOne" -> "2026", "listYearTwo" -> "2023")

      lazy val result = postWhichRatingListPage(Welsh, List("2026", "2023", "2017"), body = requestBody)

      s"should have the correct $SEE_OTHER status" in {
        result.status shouldBe SEE_OTHER
      }
      //TODO - update when following controller has been updated
      s"should have the redirect location" in {
        result.headers("Location")
          .head shouldBe "/business-rates-property-linking/my-organisation/appoint/ratings-list/are-you-sure?chosenListYear=2023"
      }
    }

    "Redirect to the correct url on successful submission with one checkbox selected" should {
      val requestBody = Json.obj("listYearOne" -> "2026")

      lazy val result = postWhichRatingListPage(Welsh, List("2026", "2023", "2017"), body = requestBody)

      s"should have the correct $SEE_OTHER status" in {
        result.status shouldBe SEE_OTHER
      }

      //TODO - update when following controller has been updated
      s"should have the redirect location" in {
        result.headers("Location")
          .head shouldBe "/business-rates-property-linking/my-organisation/appoint/ratings-list/are-you-sure?chosenListYear=2023"
      }
    }
  }

  private def getWhichRatingListPage(language: Language, listYears: List[String], withCachedListYears: Option[List[String]] = None): Document = {

    await(
      mockRepository.saveOrUpdate(
        AgentSummary(
          listYears = Some(listYears),
          name = "Test Agent",
          organisationId = 100L,
          representativeCode = 100L,
          appointedDate = LocalDate.now(),
          propertyCount = 1,
          proposedListYears = withCachedListYears
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
      ws.url(s"http://localhost:$port/business-rates-property-linking/my-organisation/appoint/ratings-list/confirm-reval")
        .withCookies(languageCookie(language), getSessionCookie(testSessionId))
        .withFollowRedirects(follow = false)
        .get()
    )

    res.status shouldBe OK
    Jsoup.parse(res.body)
  }

  private def postWhichRatingListPage(language: Language, listYears: List[String], body: JsObject = JsObject.empty) = {
    await(
      mockRepository.saveOrUpdate(
        AgentSummary(
          listYears = Some(listYears),
          name = "Test Agent",
          organisationId = 100L,
          representativeCode = 100L,
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

    await(
      ws.url(s"http://localhost:$port/business-rates-property-linking/my-organisation/appoint/ratings-list/confirm-reval")
        .withCookies(languageCookie(language), getSessionCookie(testSessionId))
        .withFollowRedirects(follow = false)
        .withHttpHeaders(HeaderNames.COOKIE -> "sessionId", "Csrf-Token" -> "nocheck")
        .post(body = body)
    )
  }
}
