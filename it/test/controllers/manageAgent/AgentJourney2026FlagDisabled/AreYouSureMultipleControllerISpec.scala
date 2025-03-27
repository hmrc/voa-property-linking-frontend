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

class AreYouSureMultipleControllerISpec extends ISpecBase with HtmlComponentHelpers with ListYearsHelpers {

  override lazy val extraConfig: Map[String, String] =
    Map("featureFlags.agentJourney2026Enabled" -> "false")

  val titleText =
    "Are you sure you want Test Agent to act for you on the 2023 and 2017 rating lists? - Valuation Office Agency - GOV.UK"
  val backLinkText = "Back"
  val captionText = "Manage agent"
  val headerText = "Are you sure you want Test Agent to act for you on the 2023 and 2017 rating lists?"
  val forAllText = "For all your property valuations on the 2023 and 2017 rating lists, this agent will be able to:"
  val seeDetailedText = "see detailed property information"
  val seeCheckText = "see Check and Challenge case correspondence such as messages and emails"
  val sendCheckText = "send Check and Challenge cases"
  val thisAppliesText = "This applies to properties that you assign to them or they add to your account"
  val confirmText = "Confirm"
  val cancelText = "Cancel"

  val titleTextWelsh =
    "Ydych chi’n siŵr eich bod am i Test Agent weithredu ar eich rhan ar restrau ardrethu 2023 a 2017? - Valuation Office Agency - GOV.UK"
  val backLinkTextWelsh = "Yn ôl"
  val captionTextWelsh = "Rheoli asiant"
  val headerTextWelsh =
    "Ydych chi’n siŵr eich bod am i Test Agent weithredu ar eich rhan ar restrau ardrethu 2023 a 2017?"
  val forAllTextWelsh =
    "Ar gyfer eich holl brisiadau eiddo ar restrau ardrethu 2023 a 2017, bydd yr asiant hwn yn gallu:"
  val seeDetailedTextWelsh = "gweld gwybodaeth fanwl am eiddo"
  val seeCheckTextWelsh = "gweld gohebiaeth ynghylch achosion Gwirio a Herio, megis negeseuon ac e-byst"
  val sendCheckTextWelsh = "anfon achosion Gwirio a Herio"
  val thisAppliesTextWelsh =
    "Mae hyn yn berthnasol i eiddo rydych yn eu neilltuo iddo, ac i eiddo y mae’n eu hychwanegu at eich cyfrif"
  val confirmTextWelsh = "Cadarnhau"
  val cancelTextWelsh = "Canslo"

  val backLinkSelector = "#back-link"
  val captionSelector = "span.govuk-caption-l"
  val headerSelector = "h1.govuk-heading-l"
  val forAllSelector = "#main-content > div > div > p:nth-child(3)"
  val bulletPointSelector = "#main-content > div > div > ul.govuk-list--bullet > li"
  val thisAppliesSelector = "#main-content > div > div > p:nth-child(5)"
  val confirmSelector = "#submit-button"
  val cancelSelector = "#cancel-link"

  val cancelHref = "/business-rates-property-linking/my-organisation/manage-agent/property-links?agentCode=100"
  val backLinkHref = "/business-rates-property-linking/my-organisation/appoint/ratings-list/choose"

  "AreYouSureController show method with AgentJourney2026 flag disabled" should {
    "Show an English are you sure multiple screen with the correct text when the language is set to English" which {

      lazy val document: Document = getAreYouSureMultiplePage(English)

      s"has a title of $titleText" in {
        document.title() shouldBe titleText
      }

      "has a back link which takes you to the which ratings list page" in {
        document.select(backLinkSelector).text() shouldBe backLinkText
        document.select(backLinkSelector).attr("href") shouldBe backLinkHref
      }

      s"has a header of '$headerText' with a caption above of '$captionText'" in {
        document.select(headerSelector).text shouldBe headerText
        document.select(captionSelector).text shouldBe captionText
      }

      s"has text on the screen of '$forAllText'" in {
        document.select(forAllSelector).text() shouldBe forAllText
      }

      s"has radio buttons on the screen with values of '$seeDetailedText', '$seeCheckText' and '$sendCheckText'" in {
        document.select(bulletPointSelector).get(0).text() shouldBe seeDetailedText
        document.select(bulletPointSelector).get(1).text() shouldBe seeCheckText
        document.select(bulletPointSelector).get(2).text() shouldBe sendCheckText
      }

      s"has text on the screen of '$thisAppliesText'" in {
        document.select(thisAppliesSelector).text() shouldBe thisAppliesText
      }

      s"has a '$confirmText' link on the screen" in {
        document.select(confirmSelector).text() shouldBe confirmText
      }

      s"has a '$cancelText' link on the screen, which takes you to the agent details screen" in {
        document.select(cancelSelector).text() shouldBe cancelText
        document.select(cancelSelector).attr("href") shouldBe cancelHref
      }
    }

    "Show a Welsh are you sure multiple screen with the correct text when the language is set to Welsh" which {

      lazy val document: Document = getAreYouSureMultiplePage(Welsh)

      s"has a title of $titleText in welsh" in {
        document.title() shouldBe titleTextWelsh
      }

      "has a back link which takes you to the which ratings list page" in {
        document.select(backLinkSelector).text() shouldBe backLinkTextWelsh
        document.select(backLinkSelector).attr("href") shouldBe backLinkHref
      }

      s"has a header of '$headerText' with a caption above of '$captionText' in welsh" in {
        document.select(headerSelector).text shouldBe headerTextWelsh
        document.select(captionSelector).text shouldBe captionTextWelsh
      }

      s"has text on the screen of '$forAllText' in welsh" in {
        document.select(forAllSelector).text() shouldBe forAllTextWelsh
      }

      s"has radio buttons on the screen with values of '$seeDetailedText', '$seeCheckText' and '$sendCheckText' in welsh" in {
        document.select(bulletPointSelector).get(0).text() shouldBe seeDetailedTextWelsh
        document.select(bulletPointSelector).get(1).text() shouldBe seeCheckTextWelsh
        document.select(bulletPointSelector).get(2).text() shouldBe sendCheckTextWelsh
      }

      s"has text on the screen of '$thisAppliesText' in welsh" in {
        document.select(thisAppliesSelector).text() shouldBe thisAppliesTextWelsh
      }

      s"has a '$confirmText' link on the screen in welsh" in {
        document.select(confirmSelector).text() shouldBe confirmTextWelsh
      }

      s"has a '$cancelText' link on the screen in welsh, which takes you to the agent details screen" in {
        document.select(cancelSelector).text() shouldBe cancelTextWelsh
        document.select(cancelSelector).attr("href") shouldBe cancelHref
      }
    }

  }

  "AreYouSureMultipleController post method with AgentJourney2026 flag disabled" should {
    "Redirect to the confirmation page and APPOINT 2023 when current is 2017" in {
      setCurrentListYears(List("2017"), List("2017", "2023"))

      stubsSetup

      val res = submitNewListYear

      res.status shouldBe SEE_OTHER
      res
        .headers("Location")
        .head shouldBe "/business-rates-property-linking/my-organisation/appoint/ratings-list/confirmed"

      verifyAppointedListYears(amount = 1, chosenListYear = "2023")
      verifyAppointedListYears(amount = 0, chosenListYear = "2017")
      verifyRevokedListYears(amount = 0, chosenListYear = "2017")
      verifyRevokedListYears(amount = 0, chosenListYear = "2023")

    }

    "Redirect to the confirmation page and APPOINT 2017 when current is 2023" in {
      setCurrentListYears(List("2023"), List("2017", "2023"))

      stubsSetup

      val res = submitNewListYear

      res.status shouldBe SEE_OTHER
      res
        .headers("Location")
        .head shouldBe "/business-rates-property-linking/my-organisation/appoint/ratings-list/confirmed"

      verifyAppointedListYears(amount = 1, chosenListYear = "2017")
      verifyAppointedListYears(amount = 0, chosenListYear = "2023")
      verifyRevokedListYears(amount = 0, chosenListYear = "2017")
      verifyRevokedListYears(amount = 0, chosenListYear = "2023")

    }

    "Redirect to the confirmation page and do not APPOINT 2023+2017 when current is 2023+2017" in {
      setCurrentListYears(List("2017", "2023"), List("2017", "2023"))

      stubsSetup

      val res = submitNewListYear

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

  private def getAreYouSureMultiplePage(language: Language): Document = {
   setCurrentListYears(List("2017"), List("2017", "2023"))

    stubsSetup

    val res = await(
      ws.url(
        s"http://localhost:$port/business-rates-property-linking/my-organisation/appoint/ratings-list/are-you-sure-multiple"
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

  private def submitNewListYear: WSResponse =
    await(
      ws.url(
        s"http://localhost:$port/business-rates-property-linking/my-organisation/appoint/ratings-list/are-you-sure-multiple"
      ).withCookies(languageCookie(English), getSessionCookie(testSessionId))
        .withFollowRedirects(follow = false)
        .withHttpHeaders(HeaderNames.COOKIE -> "sessionId", "Csrf-Token" -> "nocheck")
        .post(body = "")
    )

}