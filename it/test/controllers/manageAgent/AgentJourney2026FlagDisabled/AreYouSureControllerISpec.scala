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

package controllers.manageAgent.AgentJourney2026FlagDisabled

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
    Map("feature-switch.agentJourney2026Enabled" -> "false")

  def titleText(listYear: String) =
    s"Are you sure you want Test Agent to act for you on the $listYear rating list? - Valuation Office Agency - GOV.UK"

  val backLinkText = "Back"
  val captionText = "Manage agent"

  def headerText(listYear: String) = s"Are you sure you want Test Agent to act for you on the $listYear rating list?"

  def thisAgentText(listYear: String) = s"This agent will only be able to act for you on the $listYear rating list."

  def theyWillText(otherListYear: String) =
    otherListYear match {
      case "2017" =>
        s"They will not be able to see valuations on the 2023 rating list, or act on them for you."
      case "2023" =>
        s"They will not be able to see valuations on the 2017 rating list, or act on them for you."
    }

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

  def theyWillTextWelsh(otherListYear: String) =
    otherListYear match {
      case "2017" =>
        s"Ni fyddant yn gallu gweld prisiadau o restr ardrethu 2023, na gweithredu arnynt ar eich rhan."
      case "2023" =>
        s"Ni fyddant yn gallu gweld prisiadau o restr ardrethu 2017, na gweithredu arnynt ar eich rhan."
    }

  val restrictingTextWelsh =
    "Rhybudd Bydd cyfyngu asiant i un rhestr ardrethu tra bo achosion Gwirio a Herio ar y gweill ganddo ar restr ardrethu arall yn golygu na fydd modd iddo weithredu arnynt ar eich rhan mwyach."
  val confirmTextWelsh = "Cadarnhau"
  val cancelTextWelsh = "Canslo"

  val backLinkSelector = "#back-link"
  val captionSelector = "span.govuk-caption-l"
  val headerSelector = "h1.govuk-heading-l"
  val thisAgentSelector = "#main-content > div > div > p:nth-child(3)"
  val theyWillSelector = "#main-content > div > div > p:nth-child(4)"
  val restrictingSelector = "#main-content > div > div > div.govuk-warning-text > strong"
  val confirmSelector = "#submit-button"
  val cancelSelector = "#cancel-link"

  val cancelHref = "/business-rates-property-linking/my-organisation/manage-agent/property-links?agentCode=100"
  val backLinkHref = "/business-rates-property-linking/my-organisation/appoint/ratings-list/confirm"

  "AreYouSureController show method (AgentJourney2026 disabled)" should {
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

      s"has text on the screen of '${theyWillText(otherListYear = "2017")}'" in {
        document.select(theyWillSelector).text() shouldBe theyWillText(otherListYear = "2017")
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

      s"has text on the screen of '${theyWillText(otherListYear = "2023")}'" in {
        document.select(theyWillSelector).text() shouldBe theyWillText(otherListYear = "2023")
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

      s"has text on the screen of '${theyWillText(otherListYear = "2017")}' in welsh" in {
        document.select(theyWillSelector).text() shouldBe theyWillTextWelsh(otherListYear = "2017")
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

      s"has text on the screen of '${theyWillText(otherListYear = "2023")}' in welsh" in {
        document.select(theyWillSelector).text() shouldBe theyWillTextWelsh(otherListYear = "2023")
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

    "Redirect to the confirmation page and REVOKE 2017 and not APPOINT 2023 when currentYears is both and they chose 2023" in {
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

    "Redirect to the confirmation page and APPOINT 2023 and REVOKE 2017 when currentYears is 2023 and they chose 2017" in {
      setCurrentListYears(List("2023"))

      stubsSetup

      val res = submitNewListYear(chosenListYear = "2017")

      res.status shouldBe SEE_OTHER
      res
        .headers("Location")
        .head shouldBe "/business-rates-property-linking/my-organisation/appoint/ratings-list/confirmed"

      verifyAppointedListYears(amount = 1, chosenListYear = "2017")
      verifyAppointedListYears(amount = 0, chosenListYear = "2023")
      verifyRevokedListYears(amount = 0, chosenListYear = "2017")
      verifyRevokedListYears(amount = 1, chosenListYear = "2023")

    }

    "Redirect to the confirmation page and REVOKE 2023 and not APPOINT 2017 when currentYears is both and they chose 2017" in {
      setCurrentListYears(List("2017", "2023"))

      stubsSetup

      val res = submitNewListYear(chosenListYear = "2017")

      res.status shouldBe SEE_OTHER
      res
        .headers("Location")
        .head shouldBe "/business-rates-property-linking/my-organisation/appoint/ratings-list/confirmed"

      verifyAppointedListYears(amount = 0, chosenListYear = "2017")
      verifyAppointedListYears(amount = 0, chosenListYear = "2023")
      verifyRevokedListYears(amount = 0, chosenListYear = "2017")
      verifyRevokedListYears(amount = 1, chosenListYear = "2023")

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
