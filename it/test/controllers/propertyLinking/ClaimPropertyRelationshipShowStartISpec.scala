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

package controllers.propertyLinking

import base.{HtmlComponentHelpers, ISpecBase}
import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.http.Status.OK
import play.api.libs.json.Json
import play.api.test.Helpers._
import utils.ListYearsHelpers

class ClaimPropertyRelationshipShowStartISpec extends ISpecBase with HtmlComponentHelpers with ListYearsHelpers {

  val titleText = "Add a property to your account"
  val clientTitleText = "Add a property to your client’s account"
  val backLinkText = "Back"
  val addressText = "Address"
  val localAuthText = "Local authority reference"
  val youllNeedText =
    "You’ll need to prove your connection to the property with evidence such as a business rates bill or lease. The date of the evidence must overlap with the period you owned or occupied the property. See how we will use the information you provide."
  val clientYoullNeedText =
    "You’ll need to prove your client’s connection to the property with evidence such as a business rates bill or lease. The date of the evidence must overlap with the period your client owned or occupied the property. See how we will use the information you provide."
  val howWeText = "how we will use the information you provide"
  val weAimText =
    "We aim to give you a decision on your request to add a property as quickly as possible, but it may take up to 15 working days."
  val whenWeText = "When we approve your request you can:"
  val changePropertyText = "change property details"
  val challengeText = "challenge the rateable value if you think it is too high"
  val startNowText = "Start now"

  val titleTextWelsh = "Ychwanegu eiddo i’ch cyfrif"
  val clientTitleTextWelsh = "Ychwanegu eiddo i gyfrif eich cleient"
  val backLinkTextWelsh = "Yn ôl"
  val addressTextWelsh = "Cyfeiriad"
  val localAuthTextWelsh = "Cyfeirnod yr awdurdod lleol"
  val youllNeedTextWelsh =
    "Bydd angen i chi brofi’ch cysylltiad â’r eiddo drwy ddarparu tystiolaeth megis bil ardrethi busnes neu brydles. Rhaid i ddyddiad y dystiolaeth orgyffwrdd â’r cyfnod yr oeddech yn berchen neu’n meddiannu’r eiddo. Ewch i sut rydym yn defnyddio’r wybodaeth rydych yn ei darparu."
  val clientYoullNeedTextWelsh =
    "Bydd angen i chi brofi cysylltiad eich cleient â’r eiddo drwy ddarparu tystiolaeth megis bil ardrethi busnes neu brydles. Rhaid i ddyddiad y dystiolaeth orgyffwrdd â’r cyfnod roedd eich cleient yn berchen neu’n meddiannu’r eiddo. Ewch i sut rydym yn defnyddio’r wybodaeth rydych yn ei darparu."
  val howWeTextWelsh = "sut rydym yn defnyddio’r wybodaeth rydych yn ei darparu"
  val weAimTextWelsh =
    "Ein nod yw rhoi penderfyniad i chi ar eich cais i ychwanegu eiddo cyn gynted â phosibl, ond gall gymryd hyd at 15 diwrnod gwaith."
  val whenWeTextWelsh = "Pan fyddwn yn cymeradwyo eich cais, gallwch wneud y canlynol:"
  val changePropertyTextWelsh = "newid manylion eich eiddo"
  val challengeTextWelsh = "herio’r gwerth ardrethol os ydych chi’n credu ei fod yn rhy uchel"
  val startNowTextWelsh = "Dechrau nawr"

  val backLinkSelector = "#back-link"
  val headerSelector = "#main-content > div > div > h1"
  val addressTextSelector = "#address-key-id"
  val addressValueSelector = "#address-value-id"
  val localAuthTextSelector = "#localAuthorityReference-key-id"
  val localAuthValueSelector = "#localAuthorityReference-value-id"
  val youllNeedTextSelector = "#main-content > div > div > p:nth-child(3)"
  val howWeTextSelector = "#main-content > div > div > p:nth-child(3) > a"
  val weAimTextSelector = "#main-content > div > div > p:nth-child(4)"
  val whenWeTextSelector = "#main-content > div > div > p:nth-child(5)"
  val bulletListSelector = "#main-content > div > div > ul > li"
  val startNowTextSelector = "#main-content > div > div > a"

  val backLinkHref = "http://localhost:9300/business-rates-find/valuations/2198480000?valuationId=10007980"
  val howWeWillHref = "https://www.gov.uk/guidance/check-and-challenge-your-business-rates-valuation-privacy-notice"
  val startNowHref = "/business-rates-property-linking/my-organisation/claim/property-links/capacity/relationship"
  val addressValue = "Test Address, Test Lane, T35 T3R"
  val localAuthValue = "2050466366770"

  "ClaimPropertyRelationship show start method" should {
    "Show an English Add a property to your account screen with the correct text for an individual" which {

      lazy val document: Document = getShowStartPage(language = English, isAgent = false)

      s"has a title of $titleText" in {
        document.title() shouldBe titleText + " - Valuation Office Agency - GOV.UK"
      }

      "has a back link which takes you to the business rates valuations page" in {
        document.select(backLinkSelector).text() shouldBe backLinkText
        document.select(backLinkSelector).attr("href") shouldBe backLinkHref
      }

      s"has a header of '$titleText'" in {
        document.select(headerSelector).text shouldBe titleText
      }

      s"has a row for '$addressText' with a value of $addressValue" in {
        document.select(addressTextSelector).text shouldBe addressText
        document.select(addressValueSelector).text shouldBe addressValue
      }

      s"has a row for '$localAuthText' with a value of $localAuthValue" in {
        document.select(localAuthTextSelector).text shouldBe localAuthText
        document.select(localAuthValueSelector).text shouldBe localAuthValue
      }

      s"has text on the screen of '$youllNeedText'" in {
        document.select(youllNeedTextSelector).text() shouldBe youllNeedText
      }

      s"has a '$howWeText' link on the screen, which takes you to the how we use info screen " in {
        document.select(howWeTextSelector).text() shouldBe howWeText
        document.select(howWeTextSelector).attr("href") shouldBe howWeWillHref
      }

      s"has text on the screen of '$weAimText'" in {
        document.select(weAimTextSelector).text() shouldBe weAimText
      }

      s"has text on the screen of '$whenWeText'" in {
        document.select(whenWeTextSelector).text() shouldBe whenWeText
      }

      s"has a bullet point list on the screen with values '$changePropertyText' and '$challengeText" in {
        document.select(bulletListSelector).get(0).text() shouldBe changePropertyText
        document.select(bulletListSelector).get(1).text() shouldBe challengeText
      }

      s"has a '$startNowText' button, which takes you to the relationship screen" in {
        document.select(startNowTextSelector).text() shouldBe startNowText
        document.select(startNowTextSelector).attr("href") shouldBe startNowHref
      }
    }

    "Show an English Add a property to your account screen with the correct text for an agent" which {

      lazy val document: Document = getShowStartPage(language = English, isAgent = true)

      s"has a title of $clientTitleText" in {
        document.title() shouldBe clientTitleText + " - Valuation Office Agency - GOV.UK"
      }

      "has a back link which takes you to the business rates valuations page" in {
        document.select(backLinkSelector).text() shouldBe backLinkText
        document.select(backLinkSelector).attr("href") shouldBe backLinkHref
      }

      s"has a header of '$clientTitleText'" in {
        document.select(headerSelector).text shouldBe clientTitleText
      }

      s"has a row for '$addressText' with a value of $addressValue" in {
        document.select(addressTextSelector).text shouldBe addressText
        document.select(addressValueSelector).text shouldBe addressValue
      }

      s"has a row for '$localAuthText' with a value of $localAuthValue" in {
        document.select(localAuthTextSelector).text shouldBe localAuthText
        document.select(localAuthValueSelector).text shouldBe localAuthValue
      }

      s"has text on the screen of '$clientYoullNeedText'" in {
        document.select(youllNeedTextSelector).text() shouldBe clientYoullNeedText
      }

      s"has a '$howWeText' link on the screen, which takes you to the how we use info screen " in {
        document.select(howWeTextSelector).text() shouldBe howWeText
        document.select(howWeTextSelector).attr("href") shouldBe howWeWillHref
      }

      s"has text on the screen of '$weAimText'" in {
        document.select(weAimTextSelector).text() shouldBe weAimText
      }

      s"has text on the screen of '$whenWeText'" in {
        document.select(whenWeTextSelector).text() shouldBe whenWeText
      }

      s"has a bullet point list on the screen with values '$changePropertyText' and '$challengeText" in {
        document.select(bulletListSelector).get(0).text() shouldBe changePropertyText
        document.select(bulletListSelector).get(1).text() shouldBe challengeText
      }

      s"has a '$startNowText' button, which takes you to the relationship screen" in {
        document.select(startNowTextSelector).text() shouldBe startNowText
        document.select(startNowTextSelector).attr("href") shouldBe startNowHref
      }
    }

    "Show a Welsh Add a property to your account screen with the correct text for an individual when the language is welsh" which {

      lazy val document: Document = getShowStartPage(language = Welsh, isAgent = false)

      s"has a title of $titleText in welsh" in {
        document.title() shouldBe titleTextWelsh + " - Valuation Office Agency - GOV.UK"
      }

      "has a back link which takes you to the business rates valuations page" in {
        document.select(backLinkSelector).text() shouldBe backLinkTextWelsh
        document.select(backLinkSelector).attr("href") shouldBe backLinkHref
      }

      s"has a header of '$titleText' in welsh" in {
        document.select(headerSelector).text shouldBe titleTextWelsh
      }

      s"has a row for '$addressText' in welsh with a value of $addressValue" in {
        document.select(addressTextSelector).text shouldBe addressTextWelsh
        document.select(addressValueSelector).text shouldBe addressValue
      }

      s"has a row for '$localAuthText' in welsh with a value of $localAuthValue" in {
        document.select(localAuthTextSelector).text shouldBe localAuthTextWelsh
        document.select(localAuthValueSelector).text shouldBe localAuthValue
      }

      s"has text on the screen of '$youllNeedText' in welsh" in {
        document.select(youllNeedTextSelector).text() shouldBe youllNeedTextWelsh
      }

      s"has a '$howWeText' link on the screen in welsh, which takes you to the how we use info screen " in {
        document.select(howWeTextSelector).text() shouldBe howWeTextWelsh
        document.select(howWeTextSelector).attr("href") shouldBe howWeWillHref
      }

      s"has text on the screen of '$weAimText' in welsh" in {
        document.select(weAimTextSelector).text() shouldBe weAimTextWelsh
      }

      s"has text on the screen of '$whenWeText' in welsh" in {
        document.select(whenWeTextSelector).text() shouldBe whenWeTextWelsh
      }

      s"has a bullet point list on the screen with values '$changePropertyText' and '$challengeText in welsh" in {
        document.select(bulletListSelector).get(0).text() shouldBe changePropertyTextWelsh
        document.select(bulletListSelector).get(1).text() shouldBe challengeTextWelsh
      }

      s"has a '$startNowText' button in welsh, which takes you to the relationship screen" in {
        document.select(startNowTextSelector).text() shouldBe startNowTextWelsh
        document.select(startNowTextSelector).attr("href") shouldBe startNowHref
      }
    }

    "Show a Welsh Add a property to your account screen with the correct text for an agent when the language is welsh" which {

      lazy val document: Document = getShowStartPage(language = Welsh, isAgent = true)

      s"has a title of $clientTitleText in welsh" in {
        document.title() shouldBe clientTitleTextWelsh + " - Valuation Office Agency - GOV.UK"
      }

      "has a back link which takes you to the business rates valuations page" in {
        document.select(backLinkSelector).text() shouldBe backLinkTextWelsh
        document.select(backLinkSelector).attr("href") shouldBe backLinkHref
      }

      s"has a header of '$clientTitleText' in welsh" in {
        document.select(headerSelector).text shouldBe clientTitleTextWelsh
      }

      s"has a row for '$addressText' in welsh with a value of $addressValue" in {
        document.select(addressTextSelector).text shouldBe addressTextWelsh
        document.select(addressValueSelector).text shouldBe addressValue
      }

      s"has a row for '$localAuthText' in welsh with a value of $localAuthValue" in {
        document.select(localAuthTextSelector).text shouldBe localAuthTextWelsh
        document.select(localAuthValueSelector).text shouldBe localAuthValue
      }

      s"has text on the screen of '$clientYoullNeedText' in welsh" in {
        document.select(youllNeedTextSelector).text() shouldBe clientYoullNeedTextWelsh
      }

      s"has a '$howWeText' link on the screen in welsh, which takes you to the how we use info screen " in {
        document.select(howWeTextSelector).text() shouldBe howWeTextWelsh
        document.select(howWeTextSelector).attr("href") shouldBe howWeWillHref
      }

      s"has text on the screen of '$weAimText' in welsh" in {
        document.select(weAimTextSelector).text() shouldBe weAimTextWelsh
      }

      s"has text on the screen of '$whenWeText' in welsh" in {
        document.select(whenWeTextSelector).text() shouldBe whenWeTextWelsh
      }

      s"has a bullet point list on the screen with values '$changePropertyText' and '$challengeText in welsh" in {
        document.select(bulletListSelector).get(0).text() shouldBe changePropertyTextWelsh
        document.select(bulletListSelector).get(1).text() shouldBe challengeTextWelsh
      }

      s"has a '$startNowText' button in welsh, which takes you to the relationship screen" in {
        document.select(startNowTextSelector).text() shouldBe startNowTextWelsh
        document.select(startNowTextSelector).attr("href") shouldBe startNowHref
      }
    }

  }

  private def getShowStartPage(language: Language, isAgent: Boolean): Document = {

    stubsSetup

    val ipUrl =
      s"http://localhost:$port/business-rates-property-linking/my-organisation/claim/property-links/capacity/2198480000?rtp=summary-valuation&valuationId=10007980"
    val agentUrl =
      s"http://localhost:$port/business-rates-property-linking/my-organisation/claim/property-links/capacity/2198480000?rtp=summary-valuation&valuationId=10007980&organisationId=1234&organisationName=Test"

    val res = await(
      ws.url(if (isAgent) agentUrl else ipUrl)
        .withCookies(languageCookie(language), getSessionCookie(testSessionId))
        .withFollowRedirects(follow = false)
        .get()
    )

    res.status shouldBe OK
    Jsoup.parse(res.body)
  }

  private def stubsSetup: StubMapping = {

    stubFor {
      get("/vmv/rating-listing/api/properties/2198480000")
        .willReturn {
          aResponse.withStatus(OK).withBody(Json.toJson(testPropertyHistory).toString())
        }
    }

    stubFor {
      get("/business-rates-authorisation/authenticate")
        .willReturn {
          aResponse.withStatus(OK).withBody(Json.toJson(testIpAccounts).toString())
        }
    }

    stubFor {
      post("/auth/authorise")
        .willReturn {
          aResponse.withStatus(OK).withBody("{}")
        }
    }

    stubFor {
      get("/property-linking/submissionId/PL")
        .willReturn {
          aResponse.withStatus(OK).withBody("\"PL1ZRPH8V\"")
        }
    }
  }

}
