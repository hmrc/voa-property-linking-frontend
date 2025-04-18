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
import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import models.propertyrepresentation.AgentAppointmentChangesResponse
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.http.HeaderNames
import play.api.http.Status.OK
import play.api.libs.json.Json
import play.api.libs.ws.WSResponse
import play.api.test.Helpers._
import utils.ListYearsHelpers

class AreYouSureControllerISpec extends ISpecBase with HtmlComponentHelpers with ListYearsHelpers {

  override lazy val extraConfig: Map[String, String] =
    Map("feature-switch.agentJourney2026Enabled" -> "true")

  def titleText(listYear: String) =
    s"Are you sure you want Test Agent to act for you on the $listYear rating list? - Valuation Office Agency - GOV.UK"

  val backLinkText = "Back"
  val captionText = "Manage agent"

  def headerText(listYear: String) = s"Are you sure you want Test Agent to act for you on the $listYear rating list?"

  def thisAgentText(listYear: String) = s"This agent will only be able to act for you on the $listYear rating list."

  def theyWillText(listYear: String) =
    listYear match {
      case "2017" =>
        s"They will not be able to see valuations on the 2026 or 2023 rating lists, or act on them for you."
      case "2023" =>
        s"They will not be able to see valuations on the 2026 or 2017 rating lists, or act on them for you."
      case "2026" =>
        s"They will not be able to see valuations on the 2023 or 2017 rating lists, or act on them for you."
    }
  def forAllText(year: String) =
    s"For your property valuations on the $year rating list, this agent will be able to:"

  val seeDetailedText = "see detailed property information"
  val seeCheckText = "see Check and Challenge case correspondence such as messages and emails"
  val sendCheckText = "send Check and Challenge cases"
  val thisAppliesText = "This applies to properties that you assign to them or they add to your account"

  val restrictingText =
    "Warning Restricting an agent to a single rating list when they have Check and Challenge cases in progress on the other rating list means they will no longer be able to act on them for you."
  val confirmText = "Confirm"
  val cancelText = "Cancel"

  def titleTextWelsh(listYear: String) =
    s"A ydych yn siŵr eich bod am i Test Agent weithredu ar restr ardrethu $listYear ar eich rhan? - Valuation Office Agency - GOV.UK"

  val backLinkTextWelsh = "Yn ôl"
  val captionTextWelsh = "Rheoli asiant"

  def headerTextWelsh(listYear: String) =
    s"A ydych yn siŵr eich bod am i Test Agent weithredu ar restr ardrethu $listYear ar eich rhan?"

  def thisAgentTextWelsh(listYear: String) =
    s"Bydd yr asiant hwn ond yn gallu gweithredu ar eich rhan ar restr ardrethu $listYear."

  def theyWillTextWelsh(listYear: String) =
    listYear match {
      case "2017" =>
        s"Ni fyddant yn gallu gweld prisiadau ar restrau ardrethu 2026 na 2023, na gweithredu arnynt ar eich rhan."
      case "2023" =>
        s"Ni fyddant yn gallu gweld prisiadau ar restrau ardrethu 2026 na 2017, na gweithredu arnynt ar eich rhan."
      case "2026" =>
        s"Ni fyddant yn gallu gweld prisiadau ar restrau ardrethu 2023 na 2017, na gweithredu arnynt ar eich rhan."
    }

  def forAllTextWelsh(year: String) =
    s"Ar gyfer eich prisiadau eiddo ar restr ardrethu $year, bydd yr asiant hwn yn gallu:"
  val seeDetailedTextWelsh = "gweld gwybodaeth fanwl am eiddo"
  val seeCheckTextWelsh = "gweld gohebiaeth achosion Gwirio a Herio megis negeseuon ac e-byst"
  val sendCheckTextWelsh = "anfon achosion Gwirio a Herio"
  val thisAppliesTextWelsh =
    "Mae hyn yn berthnasol i eiddo rydych chi’n ei aseinio iddyn nhw neu maen nhw’n eu hychwanegu at eich cyfrif"

  val restrictingTextWelsh =
    "Rhybudd Bydd cyfyngu asiant i un rhestr ardrethu tra bo achosion Gwirio a Herio ar y gweill ganddo ar restr ardrethu arall yn golygu na fydd modd iddo weithredu arnynt ar eich rhan mwyach."
  val confirmTextWelsh = "Cadarnhau"
  val cancelTextWelsh = "Canslo"

  val backLinkSelector = "#back-link"
  val captionSelector = "span.govuk-caption-l"
  val headerSelector = "h1.govuk-heading-l"
  val thisAgentSelector = "#main-content > div > div > p:nth-child(3)"
  val theyWillSelector = "#main-content > div > div > p:nth-child(4)"
  val forAllSelector = "#main-content > div > div > p:nth-child(5)"
  val bulletPointSelector = "#main-content > div > div > ul.govuk-list--bullet > li"
  val thisAppliesSelector = "#main-content > div > div > p:nth-child(7)"
  val restrictingSelector = "#main-content > div > div > div.govuk-warning-text > strong"
  val confirmSelector = "#submit-button"
  val cancelSelector = "#cancel-link"

  val cancelHref = "/business-rates-property-linking/my-organisation/manage-agent/property-links?agentCode=100"
  val backLinkHref = "/business-rates-property-linking/my-organisation/appoint/ratings-list/confirm-reval"

  "AreYouSureController show method" should {
    "Show an English are you sure screen with the correct text when chosen 2017 and the language is set to English" which {

      lazy val document: Document = getAreYouSurePage(language = English, chosenListYear = "2017")

      s"has a title of ${titleText(listYear = "2017")}" in {
        document.title() shouldBe titleText(listYear = "2017")
      }

      "has a back link which takes you to the choose ratings list page" in {
        document.select(backLinkSelector).text() shouldBe backLinkText
        document.select(backLinkSelector).attr("href") shouldBe backLinkHref
      }

      s"has a header of '${headerText(listYear = "2017")}' with a caption above of '$captionText'" in {
        document.select(headerSelector).text shouldBe headerText(listYear = "2017")
        document.select(captionSelector).text shouldBe captionText
      }

      s"has text on the screen of '${thisAgentText(listYear = "2017")}'" in {
        document.select(thisAgentSelector).text() shouldBe thisAgentText(listYear = "2017")
      }

      s"has text on the screen of '${theyWillText(listYear = "2017")}'" in {
        document.select(theyWillSelector).text() shouldBe theyWillText(listYear = "2017")
      }

      s"has text on the screen of '${forAllText("2017")}'" in {
        document.select(forAllSelector).text() shouldBe forAllText("2017")
      }

      s"has radio buttons on the screen with values of '$seeDetailedText', '$seeCheckText' and '$sendCheckText'" in {
        document.select(bulletPointSelector).get(0).text() shouldBe seeDetailedText
        document.select(bulletPointSelector).get(1).text() shouldBe seeCheckText
        document.select(bulletPointSelector).get(2).text() shouldBe sendCheckText
      }

      s"has text on the screen of '$thisAppliesText'" in {
        document.select(thisAppliesSelector).text() shouldBe thisAppliesText
      }

      s"has a warning, with warning text on the screen of '$restrictingText'" in {
        document.select(restrictingSelector).text() shouldBe restrictingText
      }

      s"has a '$confirmText' link on the screen" in {
        document.select(confirmSelector).text() shouldBe confirmText
      }

      s"has a '$cancelText' link on the screen, which takes you to the agent details screen " in {
        document.select(cancelSelector).text() shouldBe cancelText
        document.select(cancelSelector).attr("href") shouldBe cancelHref
      }
    }

    "Show an English are you sure screen with the correct text when chosen 2023 and the language is set to English" which {

      lazy val document: Document = getAreYouSurePage(language = English, chosenListYear = "2023")

      s"has a title of ${titleText(listYear = "2023")}" in {
        document.title() shouldBe titleText(listYear = "2023")
      }

      "has a back link which takes you to the choose ratings list page" in {
        document.select(backLinkSelector).text() shouldBe backLinkText
        document.select(backLinkSelector).attr("href") shouldBe backLinkHref
      }

      s"has a header of '${headerText(listYear = "2023")}' with a caption above of '$captionText'" in {
        document.select(headerSelector).text shouldBe headerText(listYear = "2023")
        document.select(captionSelector).text shouldBe captionText
      }

      s"has text on the screen of '${thisAgentText(listYear = "2023")}'" in {
        document.select(thisAgentSelector).text() shouldBe thisAgentText(listYear = "2023")
      }

      s"has text on the screen of '${theyWillText(listYear = "2023")}'" in {
        document.select(theyWillSelector).text() shouldBe theyWillText(listYear = "2023")
      }

      s"has text on the screen of '${forAllText("2023")}'" in {
        document.select(forAllSelector).text() shouldBe forAllText("2023")
      }

      s"has radio buttons on the screen with values of '$seeDetailedText', '$seeCheckText' and '$sendCheckText'" in {
        document.select(bulletPointSelector).get(0).text() shouldBe seeDetailedText
        document.select(bulletPointSelector).get(1).text() shouldBe seeCheckText
        document.select(bulletPointSelector).get(2).text() shouldBe sendCheckText
      }

      s"has text on the screen of '$thisAppliesText'" in {
        document.select(thisAppliesSelector).text() shouldBe thisAppliesText
      }

      s"has a warning, with warning text on the screen of '$restrictingText'" in {
        document.select(restrictingSelector).text() shouldBe restrictingText
      }

      s"has a '$confirmText' link on the screen" in {
        document.select(confirmSelector).text() shouldBe confirmText
      }

      s"has a '$cancelText' link on the screen, which takes you to the agent details screen " in {
        document.select(cancelSelector).text() shouldBe cancelText
        document.select(cancelSelector).attr("href") shouldBe cancelHref
      }
    }

    "Show an English are you sure screen with the correct text when chosen 2026 and the language is set to English" which {

      lazy val document: Document = getAreYouSurePage(language = English, chosenListYear = "2026")

      s"has a title of ${titleText(listYear = "2026")}" in {
        document.title() shouldBe titleText(listYear = "2026")
      }

      "has a back link which takes you to the choose ratings list page" in {
        document.select(backLinkSelector).text() shouldBe backLinkText
        document.select(backLinkSelector).attr("href") shouldBe backLinkHref
      }

      s"has a header of '${headerText(listYear = "2026")}' with a caption above of '$captionText'" in {
        document.select(headerSelector).text shouldBe headerText(listYear = "2026")
        document.select(captionSelector).text shouldBe captionText
      }

      s"has text on the screen of '${thisAgentText(listYear = "2026")}'" in {
        document.select(thisAgentSelector).text() shouldBe thisAgentText(listYear = "2026")
      }

      s"has text on the screen of '${theyWillText(listYear = "2026")}'" in {
        document.select(theyWillSelector).text() shouldBe theyWillText(listYear = "2026")

      }

      s"has text on the screen of '${forAllText("2026")}'" in {
        document.select(forAllSelector).text() shouldBe forAllText("2026")
      }

      s"has radio buttons on the screen with values of '$seeDetailedText', '$seeCheckText' and '$sendCheckText'" in {
        document.select(bulletPointSelector).get(0).text() shouldBe seeDetailedText
        document.select(bulletPointSelector).get(1).text() shouldBe seeCheckText
        document.select(bulletPointSelector).get(2).text() shouldBe sendCheckText
      }

      s"has text on the screen of '$thisAppliesText'" in {
        document.select(thisAppliesSelector).text() shouldBe thisAppliesText
      }

      s"has a warning, with warning text on the screen of '$restrictingText'" in {
        document.select(restrictingSelector).text() shouldBe restrictingText
      }

      s"has a '$confirmText' link on the screen" in {
        document.select(confirmSelector).text() shouldBe confirmText
      }

      s"has a '$cancelText' link on the screen, which takes you to the agent details screen " in {
        document.select(cancelSelector).text() shouldBe cancelText
        document.select(cancelSelector).attr("href") shouldBe cancelHref
      }
    }

    "Show a Welsh are you sure screen with the correct text when chosen 2017 and the language is set to Welsh" which {

      lazy val document: Document = getAreYouSurePage(language = Welsh, chosenListYear = "2017")

      s"has a title of ${titleText(listYear = "2017")} in welsh" in {
        document.title() shouldBe titleTextWelsh(listYear = "2017")
      }

      "has a back link which takes you to the choose ratings list page" in {
        document.select(backLinkSelector).text() shouldBe backLinkTextWelsh
        document.select(backLinkSelector).attr("href") shouldBe backLinkHref
      }

      s"has a header of '${headerText(listYear = "2017")}' with a caption above of '$captionText' in welsh" in {
        document.select(headerSelector).text shouldBe headerTextWelsh(listYear = "2017")
        document.select(captionSelector).text shouldBe captionTextWelsh
      }

      s"has text on the screen of '${thisAgentText(listYear = "2017")}' in welsh" in {
        document.select(thisAgentSelector).text() shouldBe thisAgentTextWelsh(listYear = "2017")
      }

      s"has text on the screen of '${theyWillText(listYear = "2017")}' in welsh" in {
        document.select(theyWillSelector).text() shouldBe theyWillTextWelsh(listYear = "2017")
      }

      s"has text on the screen of '${forAllText("2017")}' in Welsh" in {
        document.select(forAllSelector).text() shouldBe forAllTextWelsh("2017")
      }

      s"has radio buttons on the screen with values of '$seeDetailedText', '$seeCheckText' and '$sendCheckText' in Welsh" in {
        document.select(bulletPointSelector).get(0).text() shouldBe seeDetailedTextWelsh
        document.select(bulletPointSelector).get(1).text() shouldBe seeCheckTextWelsh
        document.select(bulletPointSelector).get(2).text() shouldBe sendCheckTextWelsh
      }

      s"has text on the screen of '$thisAppliesText' in Welsh" in {
        document.select(thisAppliesSelector).text() shouldBe thisAppliesTextWelsh
      }

      s"has a warning, with warning text on the screen of '$restrictingText' in welsh" in {
        document.select(restrictingSelector).text() shouldBe restrictingTextWelsh
      }

      s"has a '$confirmText' link on the screen in welsh" in {
        document.select(confirmSelector).text() shouldBe confirmTextWelsh
      }

      s"has a '$cancelText' link on the screen in welsh, which takes you to the agent details screen " in {
        document.select(cancelSelector).text() shouldBe cancelTextWelsh
        document.select(cancelSelector).attr("href") shouldBe cancelHref
      }
    }

    "Show a Welsh are you sure screen with the correct text when chosen 2023 and the language is set to Welsh" which {

      lazy val document: Document = getAreYouSurePage(language = Welsh, chosenListYear = "2023")

      s"has a title of ${titleText(listYear = "2023")} in welsh" in {
        document.title() shouldBe titleTextWelsh(listYear = "2023")
      }

      "has a back link which takes you to the choose ratings list page" in {
        document.select(backLinkSelector).text() shouldBe backLinkTextWelsh
        document.select(backLinkSelector).attr("href") shouldBe backLinkHref
      }

      s"has a header of '${headerText(listYear = "2023")}' with a caption above of '$captionText' in welsh" in {
        document.select(headerSelector).text shouldBe headerTextWelsh(listYear = "2023")
        document.select(captionSelector).text shouldBe captionTextWelsh
      }

      s"has text on the screen of '${thisAgentText(listYear = "2023")}' in welsh" in {
        document.select(thisAgentSelector).text() shouldBe thisAgentTextWelsh(listYear = "2023")
      }

      s"has text on the screen of '${theyWillText(listYear = "2023")}' in welsh" in {
        document.select(theyWillSelector).text() shouldBe theyWillTextWelsh(listYear = "2023")
      }

      s"has text on the screen of '${forAllText("2023")}' in Welsh" in {
        document.select(forAllSelector).text() shouldBe forAllTextWelsh("2023")
      }

      s"has radio buttons on the screen with values of '$seeDetailedText', '$seeCheckText' and '$sendCheckText' in Welsh" in {
        document.select(bulletPointSelector).get(0).text() shouldBe seeDetailedTextWelsh
        document.select(bulletPointSelector).get(1).text() shouldBe seeCheckTextWelsh
        document.select(bulletPointSelector).get(2).text() shouldBe sendCheckTextWelsh
      }

      s"has text on the screen of '$thisAppliesText' in Welsh" in {
        document.select(thisAppliesSelector).text() shouldBe thisAppliesTextWelsh
      }

      s"has a warning, with warning text on the screen of '$restrictingText' in welsh" in {
        document.select(restrictingSelector).text() shouldBe restrictingTextWelsh
      }

      s"has a '$confirmText' link on the screen in welsh" in {
        document.select(confirmSelector).text() shouldBe confirmTextWelsh
      }

      s"has a '$cancelText' link on the screen in welsh, which takes you to the agent details screen " in {
        document.select(cancelSelector).text() shouldBe cancelTextWelsh
        document.select(cancelSelector).attr("href") shouldBe cancelHref
      }
    }

    "Show a Welsh are you sure screen with the correct text when chosen 2026 and the language is set to Welsh" which {

      lazy val document: Document = getAreYouSurePage(language = Welsh, chosenListYear = "2026")

      s"has a title of ${titleText(listYear = "2026")} in welsh" in {
        document.title() shouldBe titleTextWelsh(listYear = "2026")
      }

      "has a back link which takes you to the choose ratings list page" in {
        document.select(backLinkSelector).text() shouldBe backLinkTextWelsh
        document.select(backLinkSelector).attr("href") shouldBe backLinkHref
      }

      s"has a header of '${headerText(listYear = "2026")}' with a caption above of '$captionText' in welsh" in {
        document.select(headerSelector).text shouldBe headerTextWelsh(listYear = "2026")
        document.select(captionSelector).text shouldBe captionTextWelsh
      }

      s"has text on the screen of '${thisAgentText(listYear = "2026")}' in welsh" in {
        document.select(thisAgentSelector).text() shouldBe thisAgentTextWelsh(listYear = "2026")
      }

      s"has text on the screen of '${theyWillText(listYear = "2026")}' in welsh" in {
        document.select(theyWillSelector).text() shouldBe theyWillTextWelsh(listYear = "2026")
      }

      s"has text on the screen of '${forAllText("2026")}' in Welsh" in {
        document.select(forAllSelector).text() shouldBe forAllTextWelsh("2026")
      }

      s"has radio buttons on the screen with values of '$seeDetailedText', '$seeCheckText' and '$sendCheckText' in Welsh" in {
        document.select(bulletPointSelector).get(0).text() shouldBe seeDetailedTextWelsh
        document.select(bulletPointSelector).get(1).text() shouldBe seeCheckTextWelsh
        document.select(bulletPointSelector).get(2).text() shouldBe sendCheckTextWelsh
      }

      s"has text on the screen of '$thisAppliesText' in Welsh" in {
        document.select(thisAppliesSelector).text() shouldBe thisAppliesTextWelsh
      }

      s"has a warning, with warning text on the screen of '$restrictingText' in welsh" in {
        document.select(restrictingSelector).text() shouldBe restrictingTextWelsh
      }

      s"has a '$confirmText' link on the screen in welsh" in {
        document.select(confirmSelector).text() shouldBe confirmTextWelsh
      }

      s"has a '$cancelText' link on the screen in welsh, which takes you to the agent details screen " in {
        document.select(cancelSelector).text() shouldBe cancelTextWelsh
        document.select(cancelSelector).attr("href") shouldBe cancelHref
      }
    }

    "Show the not_found page when you send in an invalid list year on the url" in {

      setCurrentListYears(List("2017"))

      stubsSetup

      val res = await(
        ws.url(
          s"http://localhost:$port/business-rates-property-linking/my-organisation/appoint/ratings-list/are-you-sure?chosenListYear=2000"
        ).withCookies(languageCookie(English), getSessionCookie(testSessionId))
          .withFollowRedirects(follow = false)
          .get()
      )

      res.status shouldBe NOT_FOUND
    }

  }

  "AreYouSureController post method" should {
    "Redirect to the confirmation page and REVOKE 2017 and APPOINT 2026 when currentYears is 2017 and they chose 2026" in {
      setCurrentListYears(List("2017"))

      stubsSetup

      val res = submitNewListYear(chosenListYear = "2026")

      res.status shouldBe SEE_OTHER
      res
        .headers("Location")
        .head shouldBe "/business-rates-property-linking/my-organisation/appoint/ratings-list/confirmed"

      verifyAppointedListYears(amount = 1, chosenListYear = "2026")
      verifyAppointedListYears(amount = 0, chosenListYear = "2017")
      verifyRevokedListYears(amount = 1, chosenListYear = "2017")
      verifyRevokedListYears(amount = 0, chosenListYear = "2026")

    }

    "Redirect to the confirmation page and REVOKE 2017 and not APPOINT 2026 when currentYears is 2017+2023 and they chose 2026" in {
      setCurrentListYears(List("2017", "2026"))

      stubsSetup

      val res = submitNewListYear(chosenListYear = "2026")

      res.status shouldBe SEE_OTHER
      res
        .headers("Location")
        .head shouldBe "/business-rates-property-linking/my-organisation/appoint/ratings-list/confirmed"

      verifyAppointedListYears(amount = 0, chosenListYear = "2026")
      verifyAppointedListYears(amount = 0, chosenListYear = "2017")
      verifyRevokedListYears(amount = 1, chosenListYear = "2017")
      verifyRevokedListYears(amount = 0, chosenListYear = "2026")

    }

    "Redirect to the confirmation page and REVOKE 2017 and not APPOINT 2026 when currentYears is 2017+2026 and they chose 2026" in {
      setCurrentListYears(List("2017", "2026"))

      stubsSetup

      val res = submitNewListYear(chosenListYear = "2026")

      res.status shouldBe SEE_OTHER
      res
        .headers("Location")
        .head shouldBe "/business-rates-property-linking/my-organisation/appoint/ratings-list/confirmed"

      verifyAppointedListYears(amount = 0, chosenListYear = "2026")
      verifyAppointedListYears(amount = 0, chosenListYear = "2017")
      verifyRevokedListYears(amount = 1, chosenListYear = "2017")
      verifyRevokedListYears(amount = 0, chosenListYear = "2026")

    }

    "Redirect to the confirmation page and REVOKE 2017+2023 and not APPOINT 2026 when currentYears is 2017+2023+2026 and they chose 2026" in {
      setCurrentListYears(List("2017", "2023", "2026"))

      stubsSetup

      val res = submitNewListYear(chosenListYear = "2026")

      res.status shouldBe SEE_OTHER
      res
        .headers("Location")
        .head shouldBe "/business-rates-property-linking/my-organisation/appoint/ratings-list/confirmed"

      verifyAppointedListYears(amount = 0, chosenListYear = "2026")
      verifyAppointedListYears(amount = 0, chosenListYear = "2023")
      verifyAppointedListYears(amount = 0, chosenListYear = "2017")
      verifyRevokedListYearsMultiple(amount = 1, chosenListYears = List("2017", "2023"))
      verifyRevokedListYears(amount = 0, chosenListYear = "2026")

    }

    "Redirect to the confirmation page and do not APPOINT/REVOKE anything when currentYears is 2026 and they chose 2026" in {
      setCurrentListYears(List("2026"))

      stubsSetup

      val res = submitNewListYear(chosenListYear = "2026")

      res.status shouldBe SEE_OTHER
      res
        .headers("Location")
        .head shouldBe "/business-rates-property-linking/my-organisation/appoint/ratings-list/confirmed"

      verifyAppointedListYears(amount = 0, chosenListYear = "2017")
      verifyAppointedListYears(amount = 0, chosenListYear = "2023")
      verifyAppointedListYears(amount = 0, chosenListYear = "2026")
      verifyRevokedListYears(amount = 0, chosenListYear = "2017")
      verifyRevokedListYears(amount = 0, chosenListYear = "2023")
      verifyRevokedListYears(amount = 0, chosenListYear = "2026")
    }

    "Redirect to the confirmation page and REVOKE 2017 and APPOINT 2023 when currentYears is 2017 and they chose 2023" in {
      setCurrentListYears(List("2017"))

      stubsSetup

      val res = submitNewListYear(chosenListYear = "2023")

      res.status shouldBe SEE_OTHER
      res
        .headers("Location")
        .head shouldBe "/business-rates-property-linking/my-organisation/appoint/ratings-list/confirmed"

      verifyAppointedListYears(amount = 1, chosenListYear = "2023")
      verifyAppointedListYears(amount = 0, chosenListYear = "2017")
      verifyRevokedListYears(amount = 1, chosenListYear = "2017")
      verifyRevokedListYears(amount = 0, chosenListYear = "2023")

    }

    "Redirect to the confirmation page and REVOKE 2017 and not APPOINT 2023 when currentYears is 2017+2023 and they chose 2023" in {
      setCurrentListYears(List("2017", "2023"))

      stubsSetup

      val res = submitNewListYear(chosenListYear = "2023")

      res.status shouldBe SEE_OTHER
      res
        .headers("Location")
        .head shouldBe "/business-rates-property-linking/my-organisation/appoint/ratings-list/confirmed"

      verifyAppointedListYears(amount = 0, chosenListYear = "2023")
      verifyAppointedListYears(amount = 0, chosenListYear = "2017")
      verifyRevokedListYears(amount = 1, chosenListYear = "2017")
      verifyRevokedListYears(amount = 0, chosenListYear = "2023")

    }

    "Redirect to the confirmation page and REVOKE 2026 and not APPOINT 2023 when currentYears is 2023+2026 and they chose 2023" in {
      setCurrentListYears(List("2023", "2026"))

      stubsSetup

      val res = submitNewListYear(chosenListYear = "2023")

      res.status shouldBe SEE_OTHER
      res
        .headers("Location")
        .head shouldBe "/business-rates-property-linking/my-organisation/appoint/ratings-list/confirmed"

      verifyAppointedListYears(amount = 0, chosenListYear = "2026")
      verifyAppointedListYears(amount = 0, chosenListYear = "2023")
      verifyRevokedListYears(amount = 0, chosenListYear = "2023")
      verifyRevokedListYears(amount = 1, chosenListYear = "2026")

    }

    "Redirect to the confirmation page and REVOKE 2017+2026 and not APPOINT 2023 when currentYears is 2017+2023+2026 and they chose 2023" in {
      setCurrentListYears(List("2017", "2023", "2026"))

      stubsSetup

      val res = submitNewListYear(chosenListYear = "2023")

      res.status shouldBe SEE_OTHER
      res
        .headers("Location")
        .head shouldBe "/business-rates-property-linking/my-organisation/appoint/ratings-list/confirmed"

      verifyAppointedListYears(amount = 0, chosenListYear = "2026")
      verifyAppointedListYears(amount = 0, chosenListYear = "2023")
      verifyAppointedListYears(amount = 0, chosenListYear = "2017")
      verifyRevokedListYearsMultiple(amount = 1, chosenListYears = List("2017", "2026"))
      verifyRevokedListYears(amount = 0, chosenListYear = "2023")

    }

    "Redirect to the confirmation page and do not APPOINT/REVOKE anything when currentYears is 2023 and they chose 2023" in {
      setCurrentListYears(List("2023"))

      stubsSetup

      val res = submitNewListYear(chosenListYear = "2023")

      res.status shouldBe SEE_OTHER
      res
        .headers("Location")
        .head shouldBe "/business-rates-property-linking/my-organisation/appoint/ratings-list/confirmed"

      verifyAppointedListYears(amount = 0, chosenListYear = "2017")
      verifyAppointedListYears(amount = 0, chosenListYear = "2023")
      verifyRevokedListYears(amount = 0, chosenListYear = "2017")
      verifyRevokedListYears(amount = 0, chosenListYear = "2023")
    }

    "Redirect to the confirmation page and REVOKE 2023 and APPOINT 2017 when currentYears is 2023 and they chose 2017" in {
      setCurrentListYears(List("2023"))

      stubsSetup

      val res = submitNewListYear(chosenListYear = "2017")

      res.status shouldBe SEE_OTHER
      res
        .headers("Location")
        .head shouldBe "/business-rates-property-linking/my-organisation/appoint/ratings-list/confirmed"

      verifyAppointedListYears(amount = 0, chosenListYear = "2023")
      verifyAppointedListYears(amount = 1, chosenListYear = "2017")
      verifyRevokedListYears(amount = 0, chosenListYear = "2017")
      verifyRevokedListYears(amount = 1, chosenListYear = "2023")

    }

    "Redirect to the confirmation page and REVOKE 2023 and not APPOINT 2017 when currentYears is 2017+2023 and they chose 2017" in {
      setCurrentListYears(List("2017", "2023"))

      stubsSetup

      val res = submitNewListYear(chosenListYear = "2017")

      res.status shouldBe SEE_OTHER
      res
        .headers("Location")
        .head shouldBe "/business-rates-property-linking/my-organisation/appoint/ratings-list/confirmed"

      verifyAppointedListYears(amount = 0, chosenListYear = "2023")
      verifyAppointedListYears(amount = 0, chosenListYear = "2017")
      verifyRevokedListYears(amount = 0, chosenListYear = "2017")
      verifyRevokedListYears(amount = 1, chosenListYear = "2023")

    }

    "Redirect to the confirmation page and REVOKE 2026 and not APPOINT 2017 when currentYears is 2017+2026 and they chose 2017" in {
      setCurrentListYears(List("2017", "2026"))

      stubsSetup

      val res = submitNewListYear(chosenListYear = "2017")

      res.status shouldBe SEE_OTHER
      res
        .headers("Location")
        .head shouldBe "/business-rates-property-linking/my-organisation/appoint/ratings-list/confirmed"

      verifyAppointedListYears(amount = 0, chosenListYear = "2026")
      verifyAppointedListYears(amount = 0, chosenListYear = "2017")
      verifyRevokedListYears(amount = 0, chosenListYear = "2017")
      verifyRevokedListYears(amount = 1, chosenListYear = "2026")

    }

    "Redirect to the confirmation page and REVOKE 2023+2026 and not APPOINT 2017 when currentYears is 2017+2023+2026 and they chose 2017" in {
      setCurrentListYears(List("2017", "2023", "2026"))

      stubsSetup

      val res = submitNewListYear(chosenListYear = "2017")

      res.status shouldBe SEE_OTHER
      res
        .headers("Location")
        .head shouldBe "/business-rates-property-linking/my-organisation/appoint/ratings-list/confirmed"

      verifyAppointedListYears(amount = 0, chosenListYear = "2026")
      verifyAppointedListYears(amount = 0, chosenListYear = "2023")
      verifyAppointedListYears(amount = 0, chosenListYear = "2017")
      verifyRevokedListYearsMultiple(amount = 1, chosenListYears = List("2023", "2026"))
      verifyRevokedListYears(amount = 0, chosenListYear = "2017")

    }

    "Redirect to the confirmation page and do not APPOINT/REVOKE anything when currentYears is 2017 and they chose 2017" in {
      setCurrentListYears(List("2017"))

      stubsSetup

      val res = submitNewListYear(chosenListYear = "2017")

      res.status shouldBe SEE_OTHER
      res
        .headers("Location")
        .head shouldBe "/business-rates-property-linking/my-organisation/appoint/ratings-list/confirmed"

      verifyAppointedListYears(amount = 0, chosenListYear = "2017")
      verifyAppointedListYears(amount = 0, chosenListYear = "2023")
      verifyRevokedListYears(amount = 0, chosenListYear = "2017")
      verifyRevokedListYears(amount = 0, chosenListYear = "2023")
    }

  }

  private def getAreYouSurePage(language: Language, chosenListYear: String): Document = {

    setCurrentListYears(List(chosenListYear))

    stubsSetup

    val res = await(
      ws.url(
        s"http://localhost:$port/business-rates-property-linking/my-organisation/appoint/ratings-list/are-you-sure?chosenListYear=$chosenListYear"
      ).withCookies(languageCookie(language), getSessionCookie(testSessionId))
        .withFollowRedirects(follow = false)
        .get()
    )

    res.status shouldBe OK
    Jsoup.parse(res.body)
  }

  private def stubsSetup: StubMapping = {

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

    stubFor {
      post("/property-linking/my-organisation/agent/submit-appointment-changes")
        .willReturn {
          aResponse.withStatus(ACCEPTED).withBody(Json.toJson(AgentAppointmentChangesResponse("success")).toString())
        }
    }
  }

  private def submitNewListYear(chosenListYear: String): WSResponse =
    await(
      ws.url(
        s"http://localhost:$port/business-rates-property-linking/my-organisation/appoint/ratings-list/are-you-sure?chosenListYear=$chosenListYear"
      ).withCookies(languageCookie(English), getSessionCookie(testSessionId))
        .withFollowRedirects(follow = false)
        .withHttpHeaders(HeaderNames.COOKIE -> "sessionId", "Csrf-Token" -> "nocheck")
        .post(body = "")
    )

}
