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
import models.AgentAppointBulkAction
import models.propertyrepresentation._
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.http.HeaderNames
import play.api.http.Status.OK
import play.api.libs.json.Json
import play.api.test.Helpers._
import repositories.{AppointAgentPropertiesSessionRepository, AppointAgentSessionRepository}
import uk.gov.hmrc.http.{HeaderCarrier, SessionId}

import java.util.UUID

class ConfirmAgentAppointControllerFSOnISpec extends ISpecBase with HtmlComponentHelpers {

  val testSessionId = s"stubbed-${UUID.randomUUID}"
  val agentCode = 1001
  val agentName = "Test Agent"

  lazy val mockAppointAgentSessionRepository: AppointAgentSessionRepository =
    app.injector.instanceOf[AppointAgentSessionRepository]
  lazy val mockAppointAgentPropertiesSessionRepository: AppointAgentPropertiesSessionRepository =
    app.injector.instanceOf[AppointAgentPropertiesSessionRepository]
  implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId(testSessionId)))

  val titleText = s"$agentName has been appointed to your account - Valuation Office Agency - GOV.UK"
  val headingText = s"$agentName has been appointed to your account"
  val thisAgentCanText = "This agent can:"
  val addPropertiesText = "add properties to your account"
  val threeListYearsText = (listYear1: String, listYear2: String, listYear3: String) =>
    s"act for you on your property valuations on the $listYear1, $listYear2, and $listYear3 rating lists, for properties that you assign to them or they add to your account"
  val twoListYearsText = (listYear1: String, listYear2: String) =>
    s"act for you on your property valuations on the $listYear1 and $listYear2 rating lists, for properties that you assign to them or they add to your account"
  val singleListYearText = (listYear: String) =>
    s"act for you on your property valuations on the $listYear rating list, for properties that you assign to them or they add to your account"
  val whatHappensNextText = "What happens next"
  val youCanAssignText =
    "You can assign or unassign this agent from your properties or change the rating lists they can act for you on by managing your agents."
  val managingAgentsText = "managing your agents."
  val goToHomeText = "Go to your account home"

  val titleTextWelsh = s"Mae $agentName wedi’i benodi i’ch cyfrif - Valuation Office Agency - GOV.UK"
  val headingTextWelsh = s"Mae $agentName wedi’i benodi i’ch cyfrif"
  val thisAgentCanTextWelsh = "Gall yr asiant hwn:"
  val addPropertiesTextWelsh = "ychwanegu eiddo at eich cyfrif"
  val threeListYearsTextWelsh = (listYear1: String, listYear2: String, listYear3: String) =>
    s"weithredu ar eich rhan ar eich prisiadau eiddo ar restrau ardrethu $listYear1, $listYear2 a $listYear3, ar gyfer eiddo rydych yn eu neilltuo iddo, ac ar gyfer eiddo y mae’n eu hychwanegu at eich cyfrif"
  val twoListYearsTextWelsh = (listYear1: String, listYear2: String) =>
    s"weithredu ar eich rhan ar eich prisiadau eiddo ar restrau ardrethu $listYear1 a $listYear2, ar gyfer eiddo rydych yn eu neilltuo iddo, ac ar gyfer eiddo y mae’n eu hychwanegu at eich cyfrif"
  val singleListYearTextWelsh = (listYear: String) =>
    s"weithredu ar eich rhan ar eich prisiadau eiddo ar restr ardrethu $listYear, ar gyfer eiddo rydych yn eu neilltuo iddo, ac ar gyfer eiddo y mae’n eu hychwanegu at eich cyfrif"
  val whatHappensNextTextWelsh = "Beth sy’n digwydd nesaf"
  val youCanAssignTextWelsh =
    "Gallwch neilltuo’ch eiddo i’r asiant hwn neu dynnu’r asiant, neu newid y rhestrau ardrethu y gall weithredu arnynt ar eich rhan, drwy reoli eich asiantau"
  val managingAgentsTextWelsh = "reoli eich asiantau"
  val goToHomeTextWelsh = "Ewch i hafan eich cyfrif"

  val headingSelector = "h1"
  val assignedToPropertiesSelector = "#assigned-to"
  val thisAgentCanSelector = "#agent-can-text"
  val addPropertiesSelector = "#add-properties-text"
  val ratingsListSelector = "#act-on-valuations-text"
  val whatHappensNextSelector = "#what-happens-next-title"
  val youCanAssignSelector = "#main-content > div > div > p:nth-child(5)"
  val managingAgentsLinkSelector = "#showAgent"
  val goToHomeSelector = "#go-home-link"

  val managingAgentsLinkHref = "/business-rates-property-linking/my-organisation/agents"
  val goToHomeLinkHref = "/business-rates-dashboard/home"

  // These are all the scenarios where the agentListYears flag is enabled, see ConfirmAgentAppointControllerFSOffISpec for when its disabled
  "onPageLoad" should {

    "return 200 & display the correct English content when the agent has been assigned to 2026, 2023 and 2017 list years" when {

      lazy val document: Document = getDocument(English, ratingsLists = Seq("2026", "2023", "2017"))

      s"has a title of $titleText" in {
        document.title() shouldBe titleText
      }

      s"has a header of '$headingText'" in {
        document.select(headingSelector).text shouldBe headingText
      }

      s"doesn't have the text on the screen for the assigned properties" in {
        document.select(assignedToPropertiesSelector).size() shouldBe 0
      }

      s"has text on the screen of $thisAgentCanText" in {
        document.select(thisAgentCanSelector).text shouldBe thisAgentCanText
      }

      s"has a bullet point on the screen of $addPropertiesText" in {
        document.select(addPropertiesSelector).text shouldBe addPropertiesText
      }

      s"has a bullet point on the screen of ${threeListYearsText("2026", "2023", "2017")}" in {
        document.select(ratingsListSelector).text shouldBe threeListYearsText("2026", "2023", "2017")
      }

      s"has a subheading on the screen of $whatHappensNextText" in {
        document.select(whatHappensNextSelector).text shouldBe whatHappensNextText
      }

      s"has text on the screen of $youCanAssignText" in {
        document.select(youCanAssignSelector).text shouldBe youCanAssignText
      }

      s"has a $managingAgentsText link which takes you to Manage agent properties screen" in {
        document.select(managingAgentsLinkSelector).text() shouldBe managingAgentsText
        document.select(managingAgentsLinkSelector).attr("href") shouldBe managingAgentsLinkHref
      }

      s"has a $goToHomeText link which takes you to the home screen" in {
        document.select(goToHomeSelector).text() shouldBe goToHomeText
        document.select(goToHomeSelector).attr("href") shouldBe goToHomeLinkHref
      }

    }

    "return 200 & display the correct English content when the agent has been assigned to 2026 and 2023 list years" when {

      lazy val document: Document = getDocument(English, ratingsLists = Seq("2023", "2026"))

      s"has a title of $titleText" in {
        document.title() shouldBe titleText
      }

      s"has a header of '$headingText'" in {
        document.select(headingSelector).text shouldBe headingText
      }

      s"doesn't have the text on the screen for the assigned properties" in {
        document.select(assignedToPropertiesSelector).size() shouldBe 0
      }

      s"has text on the screen of $thisAgentCanText" in {
        document.select(thisAgentCanSelector).text shouldBe thisAgentCanText
      }

      s"has a bullet point on the screen of $addPropertiesText" in {
        document.select(addPropertiesSelector).text shouldBe addPropertiesText
      }

      s"has a bullet point on the screen of ${twoListYearsText("2026", "2023")}" in {
        document.select(ratingsListSelector).text shouldBe twoListYearsText("2026", "2023")
      }

      s"has a subheading on the screen of $whatHappensNextText" in {
        document.select(whatHappensNextSelector).text shouldBe whatHappensNextText
      }

      s"has text on the screen of $youCanAssignText" in {
        document.select(youCanAssignSelector).text shouldBe youCanAssignText
      }

      s"has a $managingAgentsText link which takes you to Manage agent properties screen" in {
        document.select(managingAgentsLinkSelector).text() shouldBe managingAgentsText
        document.select(managingAgentsLinkSelector).attr("href") shouldBe managingAgentsLinkHref
      }

      s"has a $goToHomeText link which takes you to the home screen" in {
        document.select(goToHomeSelector).text() shouldBe goToHomeText
        document.select(goToHomeSelector).attr("href") shouldBe goToHomeLinkHref
      }

    }

    "return 200 & display the correct English content when the agent has been assigned to 2026 and 2017 list years" when {

      lazy val document: Document = getDocument(English, ratingsLists = Seq("2026", "2017"))

      s"has a title of $titleText" in {
        document.title() shouldBe titleText
      }

      s"has a header of '$headingText'" in {
        document.select(headingSelector).text shouldBe headingText
      }

      s"doesn't have the text on the screen for the assigned properties" in {
        document.select(assignedToPropertiesSelector).size() shouldBe 0
      }

      s"has text on the screen of $thisAgentCanText" in {
        document.select(thisAgentCanSelector).text shouldBe thisAgentCanText
      }

      s"has a bullet point on the screen of $addPropertiesText" in {
        document.select(addPropertiesSelector).text shouldBe addPropertiesText
      }

      s"has a bullet point on the screen of ${twoListYearsText("2026", "2017")}" in {
        document.select(ratingsListSelector).text shouldBe twoListYearsText("2026", "2017")
      }

      s"has a subheading on the screen of $whatHappensNextText" in {
        document.select(whatHappensNextSelector).text shouldBe whatHappensNextText
      }

      s"has text on the screen of $youCanAssignText" in {
        document.select(youCanAssignSelector).text shouldBe youCanAssignText
      }

      s"has a $managingAgentsText link which takes you to Manage agent properties screen" in {
        document.select(managingAgentsLinkSelector).text() shouldBe managingAgentsText
        document.select(managingAgentsLinkSelector).attr("href") shouldBe managingAgentsLinkHref
      }

      s"has a $goToHomeText link which takes you to the home screen" in {
        document.select(goToHomeSelector).text() shouldBe goToHomeText
        document.select(goToHomeSelector).attr("href") shouldBe goToHomeLinkHref
      }

    }

    "return 200 & display the correct English content when the agent has been assigned to 2023 and 2017 list years" when {

      lazy val document: Document = getDocument(English, ratingsLists = Seq("2023", "2017"))

      s"has a title of $titleText" in {
        document.title() shouldBe titleText
      }

      s"has a header of '$headingText'" in {
        document.select(headingSelector).text shouldBe headingText
      }

      s"doesn't have the text on the screen for the assigned properties" in {
        document.select(assignedToPropertiesSelector).size() shouldBe 0
      }

      s"has text on the screen of $thisAgentCanText" in {
        document.select(thisAgentCanSelector).text shouldBe thisAgentCanText
      }

      s"has a bullet point on the screen of $addPropertiesText" in {
        document.select(addPropertiesSelector).text shouldBe addPropertiesText
      }

      s"has a bullet point on the screen of ${twoListYearsText("2023", "2017")}" in {
        document.select(ratingsListSelector).text shouldBe twoListYearsText("2023", "2017")
      }

      s"has a subheading on the screen of $whatHappensNextText" in {
        document.select(whatHappensNextSelector).text shouldBe whatHappensNextText
      }

      s"has text on the screen of $youCanAssignText" in {
        document.select(youCanAssignSelector).text shouldBe youCanAssignText
      }

      s"has a $managingAgentsText link which takes you to Manage agent properties screen" in {
        document.select(managingAgentsLinkSelector).text() shouldBe managingAgentsText
        document.select(managingAgentsLinkSelector).attr("href") shouldBe managingAgentsLinkHref
      }

      s"has a $goToHomeText link which takes you to the home screen" in {
        document.select(goToHomeSelector).text() shouldBe goToHomeText
        document.select(goToHomeSelector).attr("href") shouldBe goToHomeLinkHref
      }

    }

    "return 200 & display the correct English content when the agent has been assigned to the 2026 list year only" when {

      lazy val document: Document = getDocument(English, ratingsLists = Seq("2026"))

      s"has a title of $titleText" in {
        document.title() shouldBe titleText
      }

      s"has a header of '$headingText'" in {
        document.select(headingSelector).text shouldBe headingText
      }

      s"doesn't have the text on the screen for the assigned properties" in {
        document.select(assignedToPropertiesSelector).size() shouldBe 0
      }

      s"has text on the screen of $thisAgentCanText" in {
        document.select(thisAgentCanSelector).text shouldBe thisAgentCanText
      }

      s"has a bullet point on the screen of $addPropertiesText" in {
        document.select(addPropertiesSelector).text shouldBe addPropertiesText
      }

      s"has a bullet point on the screen of ${singleListYearText("2026")}" in {
        document.select(ratingsListSelector).text shouldBe singleListYearText("2026")
      }

      s"has a subheading on the screen of $whatHappensNextText" in {
        document.select(whatHappensNextSelector).text shouldBe whatHappensNextText
      }

      s"has text on the screen of $youCanAssignText" in {
        document.select(youCanAssignSelector).text shouldBe youCanAssignText
      }

      s"has a $managingAgentsText link which takes you to Manage agent properties screen" in {
        document.select(managingAgentsLinkSelector).text() shouldBe managingAgentsText
        document.select(managingAgentsLinkSelector).attr("href") shouldBe managingAgentsLinkHref
      }

      s"has a $goToHomeText link which takes you to the home screen" in {
        document.select(goToHomeSelector).text() shouldBe goToHomeText
        document.select(goToHomeSelector).attr("href") shouldBe goToHomeLinkHref
      }

    }

    "return 200 & display the correct English content when the agent has been assigned to the 2023 list year only" when {

      lazy val document: Document = getDocument(English, ratingsLists = Seq("2023"))

      s"has a title of $titleText" in {
        document.title() shouldBe titleText
      }

      s"has a header of '$headingText'" in {
        document.select(headingSelector).text shouldBe headingText
      }

      s"doesn't have the text on the screen for the assigned properties" in {
        document.select(assignedToPropertiesSelector).size() shouldBe 0
      }

      s"has text on the screen of $thisAgentCanText" in {
        document.select(thisAgentCanSelector).text shouldBe thisAgentCanText
      }

      s"has a bullet point on the screen of $addPropertiesText" in {
        document.select(addPropertiesSelector).text shouldBe addPropertiesText
      }

      s"has a bullet point on the screen of ${singleListYearText("2023")}" in {
        document.select(ratingsListSelector).text shouldBe singleListYearText("2023")
      }

      s"has a subheading on the screen of $whatHappensNextText" in {
        document.select(whatHappensNextSelector).text shouldBe whatHappensNextText
      }

      s"has text on the screen of $youCanAssignText" in {
        document.select(youCanAssignSelector).text shouldBe youCanAssignText
      }

      s"has a $managingAgentsText link which takes you to Manage agent properties screen" in {
        document.select(managingAgentsLinkSelector).text() shouldBe managingAgentsText
        document.select(managingAgentsLinkSelector).attr("href") shouldBe managingAgentsLinkHref
      }

      s"has a $goToHomeText link which takes you to the home screen" in {
        document.select(goToHomeSelector).text() shouldBe goToHomeText
        document.select(goToHomeSelector).attr("href") shouldBe goToHomeLinkHref
      }

    }

    "return 200 & display the correct English content when the agent has been assigned to the 2017 list year only" when {

      lazy val document: Document = getDocument(English, ratingsLists = Seq("2017"))

      s"has a title of $titleText" in {
        document.title() shouldBe titleText
      }

      s"has a header of '$headingText'" in {
        document.select(headingSelector).text shouldBe headingText
      }

      s"doesn't have the text on the screen for the assigned properties" in {
        document.select(assignedToPropertiesSelector).size() shouldBe 0
      }

      s"has text on the screen of $thisAgentCanText" in {
        document.select(thisAgentCanSelector).text shouldBe thisAgentCanText
      }

      s"has a bullet point on the screen of $addPropertiesText" in {
        document.select(addPropertiesSelector).text shouldBe addPropertiesText
      }

      s"has a bullet point on the screen of ${singleListYearText("2017")}" in {
        document.select(ratingsListSelector).text shouldBe singleListYearText("2017")
      }

      s"has a subheading on the screen of $whatHappensNextText" in {
        document.select(whatHappensNextSelector).text shouldBe whatHappensNextText
      }

      s"has text on the screen of $youCanAssignText" in {
        document.select(youCanAssignSelector).text shouldBe youCanAssignText
      }

      s"has a $managingAgentsText link which takes you to Manage agent properties screen" in {
        document.select(managingAgentsLinkSelector).text() shouldBe managingAgentsText
        document.select(managingAgentsLinkSelector).attr("href") shouldBe managingAgentsLinkHref
      }

      s"has a $goToHomeText link which takes you to the home screen" in {
        document.select(goToHomeSelector).text() shouldBe goToHomeText
        document.select(goToHomeSelector).attr("href") shouldBe goToHomeLinkHref
      }

    }

    "return 200 & display the correct Welsh content when the agent has been assigned to 2026, 2023 and 2017 list years" when {

      lazy val document: Document = getDocument(Welsh, ratingsLists = Seq("2026", "2023", "2017"))

      s"has a title of $titleText in welsh" in {
        document.title() shouldBe titleTextWelsh
      }

      s"has a header of '$headingText' in welsh" in {
        document.select(headingSelector).text shouldBe headingTextWelsh
      }

      s"doesn't have the text on the screen for the assigned properties" in {
        document.select(assignedToPropertiesSelector).size() shouldBe 0
      }

      s"has text on the screen of $thisAgentCanText in welsh" in {
        document.select(thisAgentCanSelector).text shouldBe thisAgentCanTextWelsh
      }

      s"has a bullet point on the screen of $addPropertiesText in welsh" in {
        document.select(addPropertiesSelector).text shouldBe addPropertiesTextWelsh
      }

      s"has a bullet point on the screen of ${threeListYearsText("2026", "2023", "2017")} in welsh" in {
        document.select(ratingsListSelector).text shouldBe threeListYearsTextWelsh("2026", "2023", "2017")
      }

      s"has a subheading on the screen of $whatHappensNextText in welsh" in {
        document.select(whatHappensNextSelector).text shouldBe whatHappensNextTextWelsh
      }

      s"has text on the screen of $youCanAssignText in welsh" in {
        document.select(youCanAssignSelector).text shouldBe youCanAssignTextWelsh
      }

      s"has a $managingAgentsText link which takes you to Manage agent properties screen in welsh" in {
        document.select(managingAgentsLinkSelector).text() shouldBe managingAgentsTextWelsh
        document.select(managingAgentsLinkSelector).attr("href") shouldBe managingAgentsLinkHref
      }

      s"has a $goToHomeText link which takes you to the home screen in welsh" in {
        document.select(goToHomeSelector).text() shouldBe goToHomeTextWelsh
        document.select(goToHomeSelector).attr("href") shouldBe goToHomeLinkHref
      }

    }

    "return 200 & display the correct Welsh content when the agent has been assigned to 2026 and 2023 list years" when {

      lazy val document: Document = getDocument(Welsh, ratingsLists = Seq("2023", "2026"))

      s"has a title of $titleText in welsh" in {
        document.title() shouldBe titleTextWelsh
      }

      s"has a header of '$headingText' in welsh" in {
        document.select(headingSelector).text shouldBe headingTextWelsh
      }

      s"doesn't have the text on the screen for the assigned properties" in {
        document.select(assignedToPropertiesSelector).size() shouldBe 0
      }

      s"has text on the screen of $thisAgentCanText in welsh" in {
        document.select(thisAgentCanSelector).text shouldBe thisAgentCanTextWelsh
      }

      s"has a bullet point on the screen of $addPropertiesText in welsh" in {
        document.select(addPropertiesSelector).text shouldBe addPropertiesTextWelsh
      }

      s"has a bullet point on the screen of ${twoListYearsText("2026", "2023")} in welsh" in {
        document.select(ratingsListSelector).text shouldBe twoListYearsTextWelsh("2026", "2023")
      }

      s"has a subheading on the screen of $whatHappensNextText in welsh" in {
        document.select(whatHappensNextSelector).text shouldBe whatHappensNextTextWelsh
      }

      s"has text on the screen of $youCanAssignText in welsh" in {
        document.select(youCanAssignSelector).text shouldBe youCanAssignTextWelsh
      }

      s"has a $managingAgentsText link which takes you to Manage agent properties screen in welsh" in {
        document.select(managingAgentsLinkSelector).text() shouldBe managingAgentsTextWelsh
        document.select(managingAgentsLinkSelector).attr("href") shouldBe managingAgentsLinkHref
      }

      s"has a $goToHomeText link which takes you to the home screen in welsh" in {
        document.select(goToHomeSelector).text() shouldBe goToHomeTextWelsh
        document.select(goToHomeSelector).attr("href") shouldBe goToHomeLinkHref
      }

    }

    "return 200 & display the correct Welsh content when the agent has been assigned to 2026 and 2017 list years" when {

      lazy val document: Document = getDocument(Welsh, ratingsLists = Seq("2026", "2017"))

      s"has a title of $titleText in welsh" in {
        document.title() shouldBe titleTextWelsh
      }

      s"has a header of '$headingText' in welsh" in {
        document.select(headingSelector).text shouldBe headingTextWelsh
      }

      s"doesn't have the text on the screen for the assigned properties" in {
        document.select(assignedToPropertiesSelector).size() shouldBe 0
      }

      s"has text on the screen of $thisAgentCanText in welsh" in {
        document.select(thisAgentCanSelector).text shouldBe thisAgentCanTextWelsh
      }

      s"has a bullet point on the screen of $addPropertiesText in welsh" in {
        document.select(addPropertiesSelector).text shouldBe addPropertiesTextWelsh
      }

      s"has a bullet point on the screen of ${twoListYearsText("2026", "2017")} in welsh" in {
        document.select(ratingsListSelector).text shouldBe twoListYearsTextWelsh("2026", "2017")
      }

      s"has a subheading on the screen of $whatHappensNextText in welsh" in {
        document.select(whatHappensNextSelector).text shouldBe whatHappensNextTextWelsh
      }

      s"has text on the screen of $youCanAssignText in welsh" in {
        document.select(youCanAssignSelector).text shouldBe youCanAssignTextWelsh
      }

      s"has a $managingAgentsText link which takes you to Manage agent properties screen in welsh" in {
        document.select(managingAgentsLinkSelector).text() shouldBe managingAgentsTextWelsh
        document.select(managingAgentsLinkSelector).attr("href") shouldBe managingAgentsLinkHref
      }

      s"has a $goToHomeText link which takes you to the home screen in welsh" in {
        document.select(goToHomeSelector).text() shouldBe goToHomeTextWelsh
        document.select(goToHomeSelector).attr("href") shouldBe goToHomeLinkHref
      }

    }

    "return 200 & display the correct Welsh content when the agent has been assigned to 2023 and 2017 list years" when {

      lazy val document: Document = getDocument(Welsh, ratingsLists = Seq("2023", "2017"))

      s"has a title of $titleText in welsh" in {
        document.title() shouldBe titleTextWelsh
      }

      s"has a header of '$headingText' in welsh" in {
        document.select(headingSelector).text shouldBe headingTextWelsh
      }

      s"doesn't have the text on the screen for the assigned properties" in {
        document.select(assignedToPropertiesSelector).size() shouldBe 0
      }

      s"has text on the screen of $thisAgentCanText in welsh" in {
        document.select(thisAgentCanSelector).text shouldBe thisAgentCanTextWelsh
      }

      s"has a bullet point on the screen of $addPropertiesText in welsh" in {
        document.select(addPropertiesSelector).text shouldBe addPropertiesTextWelsh
      }

      s"has a bullet point on the screen of ${twoListYearsText("2023", "2017")} in welsh" in {
        document.select(ratingsListSelector).text shouldBe twoListYearsTextWelsh("2023", "2017")
      }

      s"has a subheading on the screen of $whatHappensNextText in welsh" in {
        document.select(whatHappensNextSelector).text shouldBe whatHappensNextTextWelsh
      }

      s"has text on the screen of $youCanAssignText in welsh" in {
        document.select(youCanAssignSelector).text shouldBe youCanAssignTextWelsh
      }

      s"has a $managingAgentsText link which takes you to Manage agent properties screen in welsh" in {
        document.select(managingAgentsLinkSelector).text() shouldBe managingAgentsTextWelsh
        document.select(managingAgentsLinkSelector).attr("href") shouldBe managingAgentsLinkHref
      }

      s"has a $goToHomeText link which takes you to the home screen in welsh" in {
        document.select(goToHomeSelector).text() shouldBe goToHomeTextWelsh
        document.select(goToHomeSelector).attr("href") shouldBe goToHomeLinkHref
      }

    }

    "return 200 & display the correct Welsh content when the agent has been assigned to the 2026 list year only" when {

      lazy val document: Document = getDocument(Welsh, ratingsLists = Seq("2026"))

      s"has a title of $titleText  in welsh" in {
        document.title() shouldBe titleTextWelsh
      }

      s"has a header of '$headingText' in welsh" in {
        document.select(headingSelector).text shouldBe headingTextWelsh
      }

      s"doesn't have the text on the screen for the assigned properties" in {
        document.select(assignedToPropertiesSelector).size() shouldBe 0
      }

      s"has text on the screen of $thisAgentCanText in welsh" in {
        document.select(thisAgentCanSelector).text shouldBe thisAgentCanTextWelsh
      }

      s"has a bullet point on the screen of $addPropertiesText in welsh" in {
        document.select(addPropertiesSelector).text shouldBe addPropertiesTextWelsh
      }

      s"has a bullet point on the screen of ${singleListYearText("2026")} in welsh" in {
        document.select(ratingsListSelector).text shouldBe singleListYearTextWelsh("2026")
      }

      s"has a subheading on the screen of $whatHappensNextText in welsh" in {
        document.select(whatHappensNextSelector).text shouldBe whatHappensNextTextWelsh
      }

      s"has text on the screen of $youCanAssignText in welsh" in {
        document.select(youCanAssignSelector).text shouldBe youCanAssignTextWelsh
      }

      s"has a $managingAgentsText link which takes you to Manage agent properties screen in welsh" in {
        document.select(managingAgentsLinkSelector).text() shouldBe managingAgentsTextWelsh
        document.select(managingAgentsLinkSelector).attr("href") shouldBe managingAgentsLinkHref
      }

      s"has a $goToHomeText link which takes you to the home screen in welsh" in {
        document.select(goToHomeSelector).text() shouldBe goToHomeTextWelsh
        document.select(goToHomeSelector).attr("href") shouldBe goToHomeLinkHref
      }

    }

    "return 200 & display the correct Welsh content when the agent has been assigned to the 2023 list year only" when {

      lazy val document: Document = getDocument(Welsh, ratingsLists = Seq("2023"))

      s"has a title of $titleText  in welsh" in {
        document.title() shouldBe titleTextWelsh
      }

      s"has a header of '$headingText' in welsh" in {
        document.select(headingSelector).text shouldBe headingTextWelsh
      }

      s"doesn't have the text on the screen for the assigned properties" in {
        document.select(assignedToPropertiesSelector).size() shouldBe 0
      }

      s"has text on the screen of $thisAgentCanText in welsh" in {
        document.select(thisAgentCanSelector).text shouldBe thisAgentCanTextWelsh
      }

      s"has a bullet point on the screen of $addPropertiesText in welsh" in {
        document.select(addPropertiesSelector).text shouldBe addPropertiesTextWelsh
      }

      s"has a bullet point on the screen of ${singleListYearText("2023")} in welsh" in {
        document.select(ratingsListSelector).text shouldBe singleListYearTextWelsh("2023")
      }

      s"has a subheading on the screen of $whatHappensNextText in welsh" in {
        document.select(whatHappensNextSelector).text shouldBe whatHappensNextTextWelsh
      }

      s"has text on the screen of $youCanAssignText in welsh" in {
        document.select(youCanAssignSelector).text shouldBe youCanAssignTextWelsh
      }

      s"has a $managingAgentsText link which takes you to Manage agent properties screen in welsh" in {
        document.select(managingAgentsLinkSelector).text() shouldBe managingAgentsTextWelsh
        document.select(managingAgentsLinkSelector).attr("href") shouldBe managingAgentsLinkHref
      }

      s"has a $goToHomeText link which takes you to the home screen in welsh" in {
        document.select(goToHomeSelector).text() shouldBe goToHomeTextWelsh
        document.select(goToHomeSelector).attr("href") shouldBe goToHomeLinkHref
      }

    }

    "return 200 & display the correct Welsh content when the agent has been assigned to the 2017 list year only" when {

      lazy val document: Document = getDocument(Welsh, ratingsLists = Seq("2017"))

      s"has a title of $titleText in welsh" in {
        document.title() shouldBe titleTextWelsh
      }

      s"has a header of '$headingText' in welsh" in {
        document.select(headingSelector).text shouldBe headingTextWelsh
      }

      s"doesn't have the text on the screen for the assigned properties" in {
        document.select(assignedToPropertiesSelector).size() shouldBe 0
      }

      s"has text on the screen of $thisAgentCanText in welsh" in {
        document.select(thisAgentCanSelector).text shouldBe thisAgentCanTextWelsh
      }

      s"has a bullet point on the screen of $addPropertiesText in welsh" in {
        document.select(addPropertiesSelector).text shouldBe addPropertiesTextWelsh
      }

      s"has a bullet point on the screen of ${singleListYearText("2017")} in welsh" in {
        document.select(ratingsListSelector).text shouldBe singleListYearTextWelsh("2017")
      }

      s"has a subheading on the screen of $whatHappensNextText in welsh" in {
        document.select(whatHappensNextSelector).text shouldBe whatHappensNextTextWelsh
      }

      s"has text on the screen of $youCanAssignText in welsh" in {
        document.select(youCanAssignSelector).text shouldBe youCanAssignTextWelsh
      }

      s"has a $managingAgentsText link which takes you to Manage agent properties screen in welsh" in {
        document.select(managingAgentsLinkSelector).text() shouldBe managingAgentsTextWelsh
        document.select(managingAgentsLinkSelector).attr("href") shouldBe managingAgentsLinkHref
      }

      s"has a $goToHomeText link which takes you to the home screen in welsh" in {
        document.select(goToHomeSelector).text() shouldBe goToHomeTextWelsh
        document.select(goToHomeSelector).attr("href") shouldBe goToHomeLinkHref
      }

    }

  }

  def getDocument(language: Language, ratingsLists: Seq[String]): Document = {

    val (bothRatingsListChoice, specificRatingsListChoice): (Option[Boolean], Option[String]) = ratingsLists match {
      case Seq("2023", "2017") => (Some(true), None)
      case Seq("2023")         => (Some(false), Some("2023"))
      case Seq("2017")         => (Some(false), Some("2017"))
      case _                   => (None, None)
    }

    val managingPropertyData: ManagingProperty = ManagingProperty(
      agentCode = agentCode,
      agentOrganisationName = agentName,
      isCorrectAgent = true,
      managingPropertyChoice = ChooseFromList.name,
      agentAddress = "An Address",
      backLink = None,
      totalPropertySelectionSize = 2,
      propertySelectedSize = 2,
      singleProperty = true,
      bothRatingLists = bothRatingsListChoice,
      specificRatingList = specificRatingsListChoice,
      ratingLists = ratingsLists
    )

    val propertiesSessionData: AppointAgentToSomePropertiesSession = AppointAgentToSomePropertiesSession(
      agentAppointAction = Some(
        AgentAppointBulkAction(
          agentCode = agentCode,
          name = agentName,
          propertyLinkIds = List("123", "321"),
          backLinkUrl = "some-back-link"
        )
      ),
      filters = FilterAppointProperties(None, None)
    )

    await(mockAppointAgentSessionRepository.saveOrUpdate(managingPropertyData))
    await(mockAppointAgentPropertiesSessionRepository.saveOrUpdate(propertiesSessionData))

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
      ws.url(s"http://localhost:$port/business-rates-property-linking/my-organisation/confirm-appoint-agent")
        .withCookies(languageCookie(language), getSessionCookie(testSessionId))
        .withFollowRedirects(follow = false)
        .withHttpHeaders(HeaderNames.COOKIE -> "sessionId", "Csrf-Token" -> "nocheck")
        .get()
    )
    res.status shouldBe OK
    Jsoup.parse(res.body)
  }
}
