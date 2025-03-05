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
import play.api.http.HeaderNames
import play.api.http.Status.OK
import play.api.libs.json.Json
import play.api.test.Helpers._
import repositories.ManageAgentSessionRepository
import uk.gov.hmrc.http.{HeaderCarrier, SessionId}

import java.time.LocalDate
import java.util.UUID

class ChooseRatingListISpec extends ISpecBase with HtmlComponentHelpers {

  val testSessionId = s"stubbed-${UUID.randomUUID}"

  lazy val mockRepository: ManageAgentSessionRepository = app.injector.instanceOf[ManageAgentSessionRepository]
  implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId(testSessionId)))

  val titleText = "Choose a rating list Test Agent can act on for you - Valuation Office Agency - GOV.UK"
  val errorTitleText = "Error: Choose a rating list Test Agent can act on for you - Valuation Office Agency - GOV.UK"
  val backLinkText = "Back"
  val captionText = "Manage agent"
  val headerText = "Choose a rating list Test Agent can act on for you"
  val currentlyThisTextMultiple = "Currently this agent can act for you on the 2023 and 2017 rating lists"
  def currentlyThisTextSingle(listYear: String) = s"Currently this agent can act for you on the $listYear rating list"
  val thisAgentText = "This agent can act for you on:"
  val the2023RatingListText = "the 2023 rating list"
  val the2017RatingListText = "the 2017 rating list"
  val bothListsText = "both rating lists"
  val moreAboutText = "More about rating lists"
  val theVoaCalculatesText =
    "The Valuation Office Agency (VOA) calculates the rateable values for business properties in England and Wales. It holds the information on rating lists."
  val theVoaUpdatesText = "The VOA updates rateable values and publishes a new rating list every few years."
  val the2023ListText =
    "The 2023 rating list is the current rating list and has the current valuation for your property. It may also have previous valuations for your property that have an effective date after 1 April 2023."
  val the2017ListText =
    "The 2017 rating list has previous valuations for your property that have an effective date between 1 April 2017 and 31 March 2023."
  val choosingAListText =
    "Choosing a rating list is different to assigning properties to the agent. You can assign properties to them by managing your agent."
  val managingAgentText = "managing your agent"
  val doYouWantText = "Do you want this agent to act for you on both rating lists?"
  val yesText = "Yes"
  val thisAgentCanText =
    "This agent can act for you on both the 2023 and 2017 rating lists. Choose this option if you’re not sure."
  val noText = "No"
  val youWantToText = "You want to choose either the 2023 or 2017 rating list."
  val continueText = "Continue"
  val errorText = "Select an option"
  val thereIsAProblemText = "There is a problem"
  val aboveRadioErrorText = "Error: Select an option"

  val errorTitleTextWelsh =
    "Gwall: Dewis rhestr ardrethu y gall Test Agent ei gweithredu ar eich rhan - Valuation Office Agency - GOV.UK"
  val titleTextWelsh =
    "Dewis rhestr ardrethu y gall Test Agent ei gweithredu ar eich rhan - Valuation Office Agency - GOV.UK"
  val backLinkTextWelsh = "Yn ôl"
  val captionTextWelsh = "Rheoli asiant"
  val headerTextWelsh = "Dewis rhestr ardrethu y gall Test Agent ei gweithredu ar eich rhan"
  val currentlyThisTextMultipleWelsh =
    "Ar hyn o bryd, gall yr asiant hwn weithredu ar restrau ardrethu 2023 a 2017 ar eich rhan"
  def currentlyThisTextSingleWelsh(listYear: String) =
    s"Ar hyn o bryd, gall yr asiant hwn weithredu ar restr ardrethu $listYear ar eich rhan"
  val thisAgentTextWelsh = "Gall yr asiant hwn weithredu ar y canlynol ar eich rhan:"
  val the2023RatingListTextWelsh = "rhestr ardrethu 2023"
  val the2017RatingListTextWelsh = "rhestr ardrethu 2017"
  val bothListsTextWelsh = "y ddwy restr ardrethu"
  val moreAboutTextWelsh = "Rhagor o wybodaeth am restrau ardrethu"
  val theVoaCalculatesTextWelsh =
    "Mae Asiantaeth y Swyddfa Brisio (VOA) yn cyfrifo’r gwerth ardrethol ar gyfer eiddo busnes yng Nghymru a Lloegr. Mae’n cadw’r wybodaeth ar restrau ardrethu."
  val theVoaUpdatesTextWelsh =
    "Mae’r VOA yn diweddaru gwerthoedd ardrethol ac yn cyhoeddi rhestr ardrethu newydd bob ychydig flynyddoedd."
  val the2023ListTextWelsh =
    "Rhestr ardrethu 2023 yw’r rhestr ardrethu presennol, ac mae ganddi brisiad cyfredol eich eiddo. Os oes gan eich eiddo brisiadau blaenorol a ddaeth i rym ar ôl 1 Ebrill 2023, mae’n bosibl bod y prisiadau hyn ar restr ardrethu 2023 hefyd."
  val the2017ListTextWelsh =
    "Mae prisiadau blaenorol eich eiddo ar restr ardrethu 2017 – sef y prisiadau sydd â dyddiad dod i rym rhwng 1 Ebrill 2017 a 31 Mawrth 2023."
  val choosingAListTextWelsh =
    "Mae dewis rhestr ardrethu yn wahanol i neilltuo eiddo i’r asiant. Gallwch neilltuo eiddo iddo drwy reoli’ch asiant."
  val managingAgentTextWelsh = "reoli’ch asiant"
  val doYouWantTextWelsh = "A hoffech i’r asiant hwn weithredu ar y ddwy restr ardrethu ar eich rhan?"
  val yesTextWelsh = "Iawn"
  val thisAgentCanTextWelsh =
    "Gall yr asiant hwn weithredu ar restr ardrethu 2023 a rhestr ardrethu 2017 ar eich rhan. Dewiswch yr opsiwn hwn os nad ydych yn siŵr."
  val noTextWelsh = "Na"
  val youWantToTextWelsh = "Rydych am ddewis naill ai restr ardrethu 2023 neu restr ardrethu 2017."
  val continueTextWelsh = "Parhau"
  val errorTextWelsh = "Dewiswch opsiwn"
  val thereIsAProblemTextWelsh = "Mae yna broblem"
  val aboveRadioErrorTextWelsh = "Gwall: Dewis opsiwn"

  val backLinkSelector = "#back-link"
  val captionSelector = "span.govuk-caption-l"
  val headerSelector = "h1.govuk-heading-l"
  val currentlyThisSelector = "#main-content > div > div > div:nth-child(3).govuk-inset-text"
  val thisAgentSelector = "#main-content > div > div > p"
  val bulletPointSelector = "#main-content > div > div > ul.govuk-list--bullet > li"
  val moreAboutSelector =
    "#main-content > div > div > details:nth-child(6) > summary > span.govuk-details__summary-text"
  val ratingListDetailedSummarySelector = "#main-content > div > div > details:nth-child(6) > div > p"
  val choosingAListSelector = "#main-content > div > div > div:nth-child(7).govuk-inset-text"
  val managingAgentSelector = "#main-content > div > div > div:nth-child(7).govuk-inset-text > a"
  val doYouWantSelector = "#main-content > div > div > form > div > fieldset > legend"
  val yesRadioButtonSelector = "#multipleListYears"
  val yesSelector = "#main-content > div > div > form > div > fieldset > div > div:nth-child(1) > label"
  val thisAgentCanSelector = "#multipleListYears-item-hint"
  val noRadioButtonSelector = "#multipleListYears-2"
  val noSelector = "#main-content > div > div > form > div > fieldset > div > div:nth-child(2) > label"
  val youWantToSelector = "#multipleListYears-2-item-hint"
  val continueSelector = "#continue"
  val errorSummaryTitleSelector = "#main-content > div > div > div.govuk-error-summary > div > h2"
  val errorSummaryErrorSelector = "#main-content > div > div > div.govuk-error-summary > div > div"
  val aboveRadiosErrorSelector = "#multipleListYears-error"

  val managingAgentHref = "/business-rates-property-linking/my-organisation/manage-agent"
  val backLinkHref = "/business-rates-property-linking/my-organisation/manage-agent"

  "ChooseRatingListController show method" should {
    "Show an English choose rating list screen with the correct text for already being on both lists when the language is set to English" when {

      lazy val document = getChooseRatingListPage(English, List("2023", "2017"))

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

      s"has inset text on the screen of '$currentlyThisTextMultiple'" in {
        document.select(currentlyThisSelector).text() shouldBe currentlyThisTextMultiple
      }

      s"has text on the screen of '$thisAgentCanText'" in {
        document.select(thisAgentCanSelector).text() shouldBe thisAgentCanText
      }

      s"has radio buttons on the screen with values of '$the2023RatingListText', '$the2017RatingListText' and '$bothListsText'" in {
        document.select(bulletPointSelector).get(0).text() shouldBe the2023RatingListText
        document.select(bulletPointSelector).get(1).text() shouldBe the2017RatingListText
        document.select(bulletPointSelector).get(2).text() shouldBe bothListsText
      }

      s"has a details summary section on the screen with summary text '$moreAboutText', then summary text of" +
        s"'$theVoaCalculatesText', '$theVoaUpdatesText', '$the2023ListText' and '$the2017ListText'" in {
          document.select(moreAboutSelector).text() shouldBe moreAboutText
          document.select(ratingListDetailedSummarySelector).get(0).text() shouldBe theVoaCalculatesText
          document.select(ratingListDetailedSummarySelector).get(1).text() shouldBe theVoaUpdatesText
          document.select(ratingListDetailedSummarySelector).get(2).text() shouldBe the2023ListText
          document.select(ratingListDetailedSummarySelector).get(3).text() shouldBe the2017ListText
        }

      s"has inset text on the screen of '$choosingAListText', which has a link to the manage agent screens" in {
        document.select(choosingAListSelector).text() shouldBe choosingAListText
        document.select(managingAgentSelector).text() shouldBe managingAgentText
        document.select(managingAgentSelector).attr("href") shouldBe managingAgentHref
      }

      s"has a medium heading on the screen of '$doYouWantText'" in {
        document.select(doYouWantSelector).text() shouldBe doYouWantText
      }

      s"has a checked '$yesText' radio button, with hint text of '$thisAgentCanText'" in {
        document.select(yesRadioButtonSelector).hasAttr("checked") shouldBe true
        document.select(yesSelector).text() shouldBe yesText
        document.select(thisAgentCanSelector).text() shouldBe thisAgentCanText
      }

      s"has an un-checked '$noText' radio button, with hint text of '$youWantToText'" in {
        document.select(noRadioButtonSelector).hasAttr("checked") shouldBe false
        document.select(noSelector).text() shouldBe noText
        document.select(youWantToSelector).text() shouldBe youWantToText
      }

      s"has a '$continueText' button on the screen, which submits the users choice" in {
        document.select(continueSelector).text() shouldBe continueText
      }
    }

    "Show an English choose rating list screen with the correct text for already being only on the 2017 list when the language is set to English" which {

      lazy val document = getChooseRatingListPage(English, List("2017"))

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

      s"has inset text on the screen of '${currentlyThisTextSingle("2017")}'" in {
        document.select(currentlyThisSelector).text() shouldBe currentlyThisTextSingle("2017")
      }

      s"has text on the screen of '$thisAgentCanText'" in {
        document.select(thisAgentCanSelector).text() shouldBe thisAgentCanText
      }

      s"has radio buttons on the screen with values of '$the2023RatingListText', '$the2017RatingListText' and '$bothListsText'" in {
        document.select(bulletPointSelector).get(0).text() shouldBe the2023RatingListText
        document.select(bulletPointSelector).get(1).text() shouldBe the2017RatingListText
        document.select(bulletPointSelector).get(2).text() shouldBe bothListsText
      }

      s"has a details summary section on the screen with summary text '$moreAboutText', then summary text of" +
        s"'$theVoaCalculatesText', '$theVoaUpdatesText', '$the2023ListText' and '$the2017ListText'" in {
          document.select(moreAboutSelector).text() shouldBe moreAboutText
          document.select(ratingListDetailedSummarySelector).get(0).text() shouldBe theVoaCalculatesText
          document.select(ratingListDetailedSummarySelector).get(1).text() shouldBe theVoaUpdatesText
          document.select(ratingListDetailedSummarySelector).get(2).text() shouldBe the2023ListText
          document.select(ratingListDetailedSummarySelector).get(3).text() shouldBe the2017ListText
        }

      s"has inset text on the screen of '$choosingAListText', which has a link to the manage agent screens" in {
        document.select(choosingAListSelector).text() shouldBe choosingAListText
        document.select(managingAgentSelector).text() shouldBe managingAgentText
        document.select(managingAgentSelector).attr("href") shouldBe managingAgentHref
      }

      s"has a medium heading on the screen of '$doYouWantText'" in {
        document.select(doYouWantSelector).text() shouldBe doYouWantText
      }

      s"has a checked '$yesText' radio button, with hint text of '$thisAgentCanText'" in {
        document.select(yesRadioButtonSelector).hasAttr("checked") shouldBe false
        document.select(yesSelector).text() shouldBe yesText
        document.select(thisAgentCanSelector).text() shouldBe thisAgentCanText
      }

      s"has an un-checked '$noText' radio button, with hint text of '$youWantToText'" in {
        document.select(noRadioButtonSelector).hasAttr("checked") shouldBe true
        document.select(noSelector).text() shouldBe noText
        document.select(youWantToSelector).text() shouldBe youWantToText
      }

      s"has a '$continueText' button on the screen, which submits the users choice" in {
        document.select(continueSelector).text() shouldBe continueText
      }
    }

    "Show an English choose rating list screen with the correct text for already being only on the 2023 list when the language is set to English" which {

      lazy val document = getChooseRatingListPage(English, List("2023"))

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

      s"has inset text on the screen of '${currentlyThisTextSingle("2023")}'" in {
        document.select(currentlyThisSelector).text() shouldBe currentlyThisTextSingle("2023")
      }

      s"has text on the screen of '$thisAgentCanText'" in {
        document.select(thisAgentCanSelector).text() shouldBe thisAgentCanText
      }

      s"has radio buttons on the screen with values of '$the2023RatingListText', '$the2017RatingListText' and '$bothListsText'" in {
        document.select(bulletPointSelector).get(0).text() shouldBe the2023RatingListText
        document.select(bulletPointSelector).get(1).text() shouldBe the2017RatingListText
        document.select(bulletPointSelector).get(2).text() shouldBe bothListsText
      }

      s"has a details summary section on the screen with summary text '$moreAboutText', then summary text of" +
        s"'$theVoaCalculatesText', '$theVoaUpdatesText', '$the2023ListText' and '$the2017ListText'" in {
          document.select(moreAboutSelector).text() shouldBe moreAboutText
          document.select(ratingListDetailedSummarySelector).get(0).text() shouldBe theVoaCalculatesText
          document.select(ratingListDetailedSummarySelector).get(1).text() shouldBe theVoaUpdatesText
          document.select(ratingListDetailedSummarySelector).get(2).text() shouldBe the2023ListText
          document.select(ratingListDetailedSummarySelector).get(3).text() shouldBe the2017ListText
        }

      s"has inset text on the screen of '$choosingAListText', which has a link to the manage agent screens" in {
        document.select(choosingAListSelector).text() shouldBe choosingAListText
        document.select(managingAgentSelector).text() shouldBe managingAgentText
        document.select(managingAgentSelector).attr("href") shouldBe managingAgentHref
      }

      s"has a medium heading on the screen of '$doYouWantText'" in {
        document.select(doYouWantSelector).text() shouldBe doYouWantText
      }

      s"has a checked '$yesText' radio button, with hint text of '$thisAgentCanText'" in {
        document.select(yesRadioButtonSelector).hasAttr("checked") shouldBe false
        document.select(yesSelector).text() shouldBe yesText
        document.select(thisAgentCanSelector).text() shouldBe thisAgentCanText
      }

      s"has an un-checked '$noText' radio button, with hint text of '$youWantToText'" in {
        document.select(noRadioButtonSelector).hasAttr("checked") shouldBe true
        document.select(noSelector).text() shouldBe noText
        document.select(youWantToSelector).text() shouldBe youWantToText
      }

      s"has a '$continueText' button on the screen, which submits the users choice" in {
        document.select(continueSelector).text() shouldBe continueText
      }
    }

    "Show a Welsh choose rating list screen with the correct text for already being on both lists when the language is set to Welsh" which {

      lazy val document = getChooseRatingListPage(Welsh, List("2023", "2017"))

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

      s"has inset text on the screen of '$currentlyThisTextMultiple' in welsh" in {
        document.select(currentlyThisSelector).text() shouldBe currentlyThisTextMultipleWelsh
      }

      s"has text on the screen of '$thisAgentCanText' in welsh" in {
        document.select(thisAgentCanSelector).text() shouldBe thisAgentCanTextWelsh
      }

      s"has radio buttons on the screen with values of '$the2023RatingListText', '$the2017RatingListText' and '$bothListsText' in welsh" in {
        document.select(bulletPointSelector).get(0).text() shouldBe the2023RatingListTextWelsh
        document.select(bulletPointSelector).get(1).text() shouldBe the2017RatingListTextWelsh
        document.select(bulletPointSelector).get(2).text() shouldBe bothListsTextWelsh
      }

      s"has a details summary section on the screen with summary text '$moreAboutText', then summary text of" +
        s"'$theVoaCalculatesText', '$theVoaUpdatesText', '$the2023ListText' and '$the2017ListText' in welsh" in {
          document.select(moreAboutSelector).text() shouldBe moreAboutTextWelsh
          document.select(ratingListDetailedSummarySelector).get(0).text() shouldBe theVoaCalculatesTextWelsh
          document.select(ratingListDetailedSummarySelector).get(1).text() shouldBe theVoaUpdatesTextWelsh
          document.select(ratingListDetailedSummarySelector).get(2).text() shouldBe the2023ListTextWelsh
          document.select(ratingListDetailedSummarySelector).get(3).text() shouldBe the2017ListTextWelsh
        }

      s"has inset text on the screen of '$choosingAListText' in welsh, which has a link to the manage agent screens" in {
        document.select(choosingAListSelector).text() shouldBe choosingAListTextWelsh
        document.select(managingAgentSelector).text() shouldBe managingAgentTextWelsh
        document.select(managingAgentSelector).attr("href") shouldBe managingAgentHref
      }

      s"has a medium heading on the screen of '$doYouWantText' in welsh" in {
        document.select(doYouWantSelector).text() shouldBe doYouWantTextWelsh
      }

      s"has a checked '$yesText' radio button, with hint text of '$thisAgentCanText' in welsh" in {
        document.select(yesRadioButtonSelector).hasAttr("checked") shouldBe true
        document.select(yesSelector).text() shouldBe yesTextWelsh
        document.select(thisAgentCanSelector).text() shouldBe thisAgentCanTextWelsh
      }

      s"has an un-checked '$noText' radio button, with hint text of '$youWantToText' in welsh" in {
        document.select(noRadioButtonSelector).hasAttr("checked") shouldBe false
        document.select(noSelector).text() shouldBe noTextWelsh
        document.select(youWantToSelector).text() shouldBe youWantToTextWelsh
      }

      s"has a '$continueText' button on the screen in welsh, which submits the users choice" in {
        document.select(continueSelector).text() shouldBe continueTextWelsh
      }
    }

    "Show a Welsh choose rating list screen with the correct text for already being only on the 2017 list when the language is set to Welsh" which {

      lazy val document = getChooseRatingListPage(Welsh, List("2017"))

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

      s"has inset text on the screen of '${currentlyThisTextSingle("2017")}' in welsh" in {
        document.select(currentlyThisSelector).text() shouldBe currentlyThisTextSingleWelsh("2017")
      }

      s"has text on the screen of '$thisAgentCanText' in welsh" in {
        document.select(thisAgentCanSelector).text() shouldBe thisAgentCanTextWelsh
      }

      s"has radio buttons on the screen with values of '$the2023RatingListText', '$the2017RatingListText' and '$bothListsText' in welsh" in {
        document.select(bulletPointSelector).get(0).text() shouldBe the2023RatingListTextWelsh
        document.select(bulletPointSelector).get(1).text() shouldBe the2017RatingListTextWelsh
        document.select(bulletPointSelector).get(2).text() shouldBe bothListsTextWelsh
      }

      s"has a details summary section on the screen with summary text '$moreAboutText', then summary text of" +
        s"'$theVoaCalculatesText', '$theVoaUpdatesText', '$the2023ListText' and '$the2017ListText' in welsh" in {
          document.select(moreAboutSelector).text() shouldBe moreAboutTextWelsh
          document.select(ratingListDetailedSummarySelector).get(0).text() shouldBe theVoaCalculatesTextWelsh
          document.select(ratingListDetailedSummarySelector).get(1).text() shouldBe theVoaUpdatesTextWelsh
          document.select(ratingListDetailedSummarySelector).get(2).text() shouldBe the2023ListTextWelsh
          document.select(ratingListDetailedSummarySelector).get(3).text() shouldBe the2017ListTextWelsh
        }

      s"has inset text on the screen of '$choosingAListText', which has a link to the manage agent screens in welsh" in {
        document.select(choosingAListSelector).text() shouldBe choosingAListTextWelsh
        document.select(managingAgentSelector).text() shouldBe managingAgentTextWelsh
        document.select(managingAgentSelector).attr("href") shouldBe managingAgentHref
      }

      s"has a medium heading on the screen of '$doYouWantText' in welsh" in {
        document.select(doYouWantSelector).text() shouldBe doYouWantTextWelsh
      }

      s"has a checked '$yesText' radio button, with hint text of '$thisAgentCanText' in welsh" in {
        document.select(yesRadioButtonSelector).hasAttr("checked") shouldBe false
        document.select(yesSelector).text() shouldBe yesTextWelsh
        document.select(thisAgentCanSelector).text() shouldBe thisAgentCanTextWelsh
      }

      s"has an un-checked '$noText' radio button, with hint text of '$youWantToText' in welsh" in {
        document.select(noRadioButtonSelector).hasAttr("checked") shouldBe true
        document.select(noSelector).text() shouldBe noTextWelsh
        document.select(youWantToSelector).text() shouldBe youWantToTextWelsh
      }

      s"has a '$continueText' button on the screen in welsh, which submits the users choice" in {
        document.select(continueSelector).text() shouldBe continueTextWelsh
      }
    }

    "Show a Welsh choose rating list screen with the correct text for already being only on the 2023 list when the language is set to Welsh" which {

      lazy val document = getChooseRatingListPage(Welsh, List("2023"))

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

      s"has inset text on the screen of '${currentlyThisTextSingle("2023")}' in welsh" in {
        document.select(currentlyThisSelector).text() shouldBe currentlyThisTextSingleWelsh("2023")
      }

      s"has text on the screen of '$thisAgentCanText' in welsh" in {
        document.select(thisAgentCanSelector).text() shouldBe thisAgentCanTextWelsh
      }

      s"has radio buttons on the screen with values of '$the2023RatingListText', '$the2017RatingListText' and '$bothListsText' in welsh" in {
        document.select(bulletPointSelector).get(0).text() shouldBe the2023RatingListTextWelsh
        document.select(bulletPointSelector).get(1).text() shouldBe the2017RatingListTextWelsh
        document.select(bulletPointSelector).get(2).text() shouldBe bothListsTextWelsh
      }

      s"has a details summary section on the screen with summary text '$moreAboutText', then summary text of" +
        s"'$theVoaCalculatesText', '$theVoaUpdatesText', '$the2023ListText' and '$the2017ListText' in welsh" in {
          document.select(moreAboutSelector).text() shouldBe moreAboutTextWelsh
          document.select(ratingListDetailedSummarySelector).get(0).text() shouldBe theVoaCalculatesTextWelsh
          document.select(ratingListDetailedSummarySelector).get(1).text() shouldBe theVoaUpdatesTextWelsh
          document.select(ratingListDetailedSummarySelector).get(2).text() shouldBe the2023ListTextWelsh
          document.select(ratingListDetailedSummarySelector).get(3).text() shouldBe the2017ListTextWelsh
        }

      s"has inset text on the screen of '$choosingAListText' in welsh, which has a link to the manage agent screens" in {
        document.select(choosingAListSelector).text() shouldBe choosingAListTextWelsh
        document.select(managingAgentSelector).text() shouldBe managingAgentTextWelsh
        document.select(managingAgentSelector).attr("href") shouldBe managingAgentHref
      }

      s"has a medium heading on the screen of '$doYouWantText' in welsh" in {
        document.select(doYouWantSelector).text() shouldBe doYouWantTextWelsh
      }

      s"has a checked '$yesText' radio button, with hint text of '$thisAgentCanText' in welsh" in {
        document.select(yesRadioButtonSelector).hasAttr("checked") shouldBe false
        document.select(yesSelector).text() shouldBe yesTextWelsh
        document.select(thisAgentCanSelector).text() shouldBe thisAgentCanTextWelsh
      }

      s"has an un-checked '$noText' radio button, with hint text of '$youWantToText' in welsh" in {
        document.select(noRadioButtonSelector).hasAttr("checked") shouldBe true
        document.select(noSelector).text() shouldBe noTextWelsh
        document.select(youWantToSelector).text() shouldBe youWantToTextWelsh
      }

      s"has a '$continueText' button on the screen in welsh, which submits the users choice" in {
        document.select(continueSelector).text() shouldBe continueTextWelsh
      }
    }

  }

  "ChooseRatingListController post method" should {
    "Redirect to the are you sure multiple page if the user has chosen multiple list years" in {
      await(
        mockRepository.saveOrUpdate(
          AgentSummary(
            listYears = Some(List("2017")),
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

      val requestBody = Json.obj(
        "multipleListYears" -> "true"
      )

      val res = await(
        ws.url(s"http://localhost:$port/business-rates-property-linking/my-organisation/appoint/ratings-list/choose")
          .withCookies(languageCookie(English), getSessionCookie(testSessionId))
          .withFollowRedirects(follow = false)
          .withHttpHeaders(HeaderNames.COOKIE -> "sessionId", "Csrf-Token" -> "nocheck")
          .post(requestBody)
      )

      res.status shouldBe SEE_OTHER
      res
        .headers("Location")
        .head shouldBe "/business-rates-property-linking/my-organisation/appoint/ratings-list/are-you-sure-multiple"

    }

    "Redirect to the are you sure multiple page if the user has chosen one list year" in {
      await(
        mockRepository.saveOrUpdate(
          AgentSummary(
            listYears = Some(List("2017")),
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

      val requestBody = Json.obj(
        "multipleListYears" -> "false"
      )

      val res = await(
        ws.url(s"http://localhost:$port/business-rates-property-linking/my-organisation/appoint/ratings-list/choose")
          .withCookies(languageCookie(English), getSessionCookie(testSessionId))
          .withFollowRedirects(follow = false)
          .withHttpHeaders(HeaderNames.COOKIE -> "sessionId", "Csrf-Token" -> "nocheck")
          .post(requestBody)
      )

      res.status shouldBe SEE_OTHER
      res
        .headers("Location")
        .head shouldBe "/business-rates-property-linking/my-organisation/appoint/ratings-list/confirm"

    }

    "Return a bad request and show an error on the choose list page if the user has chosen no list years in English" when {

      lazy val document = postChooseRatingListPage(English)

      s"has a title of $errorTitleText" in {
        document.title() shouldBe errorTitleText
      }

      "has a back link which takes you to the agent details page" in {
        document.select(backLinkSelector).text() shouldBe backLinkText
        document.select(backLinkSelector).attr("href") shouldBe backLinkHref
      }

      s"has an error summary that contains the correct error message '$errorText'" in {
        document.select(errorSummaryTitleSelector).text() shouldBe thereIsAProblemText
        document.select(errorSummaryErrorSelector).text() shouldBe errorText
      }

      s"has a header of '$headerText' with a caption above of '$captionText'" in {
        document.select(headerSelector).text shouldBe headerText
        document.select(captionSelector).text shouldBe captionText
      }

      s"has a medium heading on the screen of '$doYouWantText'" in {
        document.select(doYouWantSelector).text() shouldBe doYouWantText
      }

      s"has an error message above the radio button of $aboveRadioErrorText" in {
        document.select(aboveRadiosErrorSelector).text() shouldBe aboveRadioErrorText
      }

      s"has a checked '$yesText' radio button, with hint text of '$thisAgentCanText'" in {
        document.select(yesRadioButtonSelector).hasAttr("checked") shouldBe true
        document.select(yesSelector).text() shouldBe yesText
        document.select(thisAgentCanSelector).text() shouldBe thisAgentCanText
      }

      s"has an un-checked '$noText' radio button, with hint text of '$youWantToText'" in {
        document.select(noRadioButtonSelector).hasAttr("checked") shouldBe false
        document.select(noSelector).text() shouldBe noText
        document.select(youWantToSelector).text() shouldBe youWantToText
      }

      s"has a '$continueText' button on the screen, which submits the users choice" in {
        document.select(continueSelector).text() shouldBe continueText
      }
    }

    "Return a bad request and show an error on the choose list page if the user has chosen no list years in Welsh" when {

      lazy val document = postChooseRatingListPage(Welsh)

      s"has a title of $errorTitleText in welsh" in {
        document.title() shouldBe errorTitleTextWelsh
      }

      "has a back link which takes you to the agent details page" in {
        document.select(backLinkSelector).text() shouldBe backLinkTextWelsh
        document.select(backLinkSelector).attr("href") shouldBe backLinkHref
      }

      s"has an error summary that contains the correct error message '$errorText' in welsh" in {
        document.select(errorSummaryTitleSelector).text() shouldBe thereIsAProblemTextWelsh
        document.select(errorSummaryErrorSelector).text() shouldBe errorTextWelsh
      }

      s"has a header of '$headerText' with a caption above of '$captionText' in welsh" in {
        document.select(headerSelector).text shouldBe headerTextWelsh
        document.select(captionSelector).text shouldBe captionTextWelsh
      }

      s"has a medium heading on the screen of '$doYouWantText' in welsh" in {
        document.select(doYouWantSelector).text() shouldBe doYouWantTextWelsh
      }

      s"has an error message above the radio button of $aboveRadioErrorText in welsh" in {
        document.select(aboveRadiosErrorSelector).text() shouldBe aboveRadioErrorTextWelsh
      }

      s"has a checked '$yesText' radio button, with hint text of '$thisAgentCanText' in welsh" in {
        document.select(yesRadioButtonSelector).hasAttr("checked") shouldBe true
        document.select(yesSelector).text() shouldBe yesTextWelsh
        document.select(thisAgentCanSelector).text() shouldBe thisAgentCanTextWelsh
      }

      s"has an un-checked '$noText' radio button, with hint text of '$youWantToText' in welsh" in {
        document.select(noRadioButtonSelector).hasAttr("checked") shouldBe false
        document.select(noSelector).text() shouldBe noTextWelsh
        document.select(youWantToSelector).text() shouldBe youWantToTextWelsh
      }

      s"has a '$continueText' button on the screen in welsh, which submits the users choice" in {
        document.select(continueSelector).text() shouldBe continueTextWelsh
      }
    }

  }
  private def getChooseRatingListPage(language: Language, listYears: List[String]): Document = {

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

    val res = await(
      ws.url(s"http://localhost:$port/business-rates-property-linking/my-organisation/appoint/ratings-list/choose")
        .withCookies(languageCookie(language), getSessionCookie(testSessionId))
        .withFollowRedirects(follow = false)
        .get()
    )

    res.status shouldBe OK
    Jsoup.parse(res.body)

  }
  private def postChooseRatingListPage(language: Language): Document = {

    await(
      mockRepository.saveOrUpdate(
        AgentSummary(
          listYears = Some(List("2017", "2023")),
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

    val res = await(
      ws.url(s"http://localhost:$port/business-rates-property-linking/my-organisation/appoint/ratings-list/choose")
        .withCookies(languageCookie(language), getSessionCookie(testSessionId))
        .withFollowRedirects(follow = false)
        .withHttpHeaders(HeaderNames.COOKIE -> "sessionId", "Csrf-Token" -> "nocheck")
        .post("")
    )

    res.status shouldBe BAD_REQUEST
    Jsoup.parse(res.body)

  }

}
