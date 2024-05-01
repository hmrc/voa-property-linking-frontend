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

package controllers.registration

import base.{HtmlComponentHelpers, ISpecBase}
import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.http.Status.OK
import play.api.libs.json.Json
import play.api.test.Helpers._
import uk.gov.hmrc.auth.core.AffinityGroup
import uk.gov.hmrc.auth.core.AffinityGroup.Individual
import utils.ListYearsHelpers

class RegistrationConfirmationISpec extends ISpecBase with HtmlComponentHelpers with ListYearsHelpers {

  val titleText = "Registration successful - Valuation Office Agency - GOV.UK"
  val headingText = "Registration successful"
  val yourVoaIdText = "Your VOA personal ID number 3164"
  val yourAgentCodeText = "Your Agent code 100"
  val weHaveSentText = "We have sent these details to individual@test.com"
  val whatHappensNextText = "What happens next"
  val nextTimeYouText = "Next time you sign in to the service, use your Government Gateway details."
  val ifYouNeedToText = "If you need to reset your password, you’ll need to provide your VOA personal ID number."
  val giveYourAgentText = "Give your Agent code to your client so they can appoint you to act for them."
  val ifAnyoneElseText =
    "If anyone else wants to register on your behalf of the organisation, you’ll need to add them to your Government Gateway account."
  val addThemTooText = "add them to your Government Gateway account"
  val weUseYourText = "We use your contact details to send you correspondence related to the service and your account."
  val termsOfUseText = "Terms of use"
  val infoProvidedText =
    "Information provided using this service is only for the purpose of checking and, if necessary, challenging and appealing the rating of non-domestic property. Use for any other purpose is a breach of the terms and conditions of service. Unlawful access may be prosecuted under the relevant legislation, including the Computer Misuse Act 1990 or the Fraud Act 2006."
  val termsLinkText = "terms and conditions"
  val goToDashboardText = "Go To Dashboard"

  val titleTextWelsh = "Wedi cofrestru’n llwyddiannus - Valuation Office Agency - GOV.UK"
  val headingTextWelsh = "Wedi cofrestru’n llwyddiannus"
  val yourVoaIdTextWelsh = "Eich rhif adnabod personol (ID) VOA 3164"
  val yourAgentCodeTextWelsh = "Eich cod Asiant 100"
  val weHaveSentTextWelsh = "Rydym wedi anfon y manylion yma at individual@test.com"
  val whatHappensNextTextWelsh = "Beth sy’n digwydd nesaf"
  val nextTimeYouTextWelsh =
    "Y tro nesaf y byddwch yn mewngofnodi I ddefnyddio’r gwasanaeth, defnyddiwch eich manylion Porth y Llywodraeth."
  val ifYouNeedToTextWelsh =
    "Os oes angen i chi ailosod eich cyfrinair, bydd angen i chi ddarparu eich rhif adnabod (ID) personol y VOA."
  val giveYourAgentTextWelsh = "Rhowch eich cod Asiant i’ch cleient fel y gallant eich penodi i weithredu ar eich rhan."
  val ifAnyoneElseTextWelsh =
    "Os oes unrhyw un arall eisiau cofrestru ar eich rhan chi o’r mudiad, bydd angen i chi eu hychwanegu i’ch cyfrif Porth y Llywodraeth."
  val addThemTooTextWelsh = "eu hychwanegu i’ch cyfrif Porth y Llywodraeth"
  val weUseYourTextWelsh =
    "Rydym yn defnyddio’ch manylion cyswllt i anfon gohebiaeth atoch sy’n ymwneud â’ch cyfrif a’r gwasanaeth."
  val termsOfUseTextWelsh = "Telerau defnyddio"
  val infoProvidedTextWelsh =
    "Dim ond at ddibenion gwirio y defnyddir y wybodaeth a ddarperir, ac os oes angen, herio ac apelio ardrethi eiddo annomestig. Mae’r defnydd ar gyfer unrhyw bwrpas arall yn torri telerau ac amodau’r gwasanaeth. Gellir erlyn mynediad anghyfreithlon dan y ddeddfwriaeth berthnasol, gan gynnwys Deddf Camddefnyddio Cyfrifiaduron 1990 neu Ddeddf Twyll 2006."
  val termsLinkTextWelsh = "telerau ac amodau’r"
  val goToDashboardTextWelsh = "Ewch i’r dangosfwrdd"

  val headingSelector = "h1.govuk-panel__title"
  val yourVoaIdSelector = "#registration-confirmation-panel > div"
  val yourAgentCodeSelector = "#registration-confirmation-panel > div > span"
  val weHaveSentTextSelector = "#email-sent"
  val whatHappensNextSelector = "#what-next"
  val nextTimeYouSelector = "#main-content > div > div > p:nth-child(4)"
  val ifYouNeedToSelector = "#main-content > div > div > p:nth-child(5)"
  val giveYourAgentSelector = "#main-content > div > div > p:nth-child(6)"
  val ifAnyoneElseSelector = "#main-content > div > div > p:nth-child(7)"
  val addThemToSelector = "#main-content > div > div > p:nth-child(7) > a"
  val weUseYourSelector = "#main-content > div > div > p:nth-child(8)"
  val termsOfUseSelector = "#terms-of-use"
  val infoProvidedSelector = "#main-content > div > div > p:nth-child(10)"
  val termsLinkSelector = "#main-content > div > div > p:nth-child(10) > a"
  val goToDashboardSelector = "#continue"

  val addThemTooHref = "/business-rates-property-linking/add-user-to-gg"
  val termsLinkHref = "terms-and-conditions"
  val goToDashboardButtonHref = "/business-rates-dashboard/home"

  "RegistrationController confirmation method" should {
    "Show an English confirmation screen with the correct text" which {

      lazy val document: Document = getSuccessPage(English, Individual)

      s"has a title of $titleText" in {
        document.title() shouldBe titleText
      }

      s"has a header of $headingText" in {
        document.select(headingSelector).text() shouldBe headingText
      }

      s"has a small header of the $yourVoaIdText" in {
        document.select(yourVoaIdSelector).text() shouldBe yourVoaIdText + " " + yourAgentCodeText
      }

      s"has text on the screen of $yourAgentCodeText" in {
        document.select(yourAgentCodeSelector).text() shouldBe yourAgentCodeText
      }

      s"has text on the screen of $weHaveSentText" in {
        document.select(weHaveSentTextSelector).text() shouldBe weHaveSentText
      }

      s"has text on the screen of $whatHappensNextText" in {
        document.select(whatHappensNextSelector).text() shouldBe whatHappensNextText
      }

      s"has text on the screen of $nextTimeYouText" in {
        document.select(nextTimeYouSelector).text() shouldBe nextTimeYouText
      }

      s"has text on the screen of $ifYouNeedToText" in {
        document.select(ifYouNeedToSelector).text() shouldBe ifYouNeedToText
      }

      s"has text on the screen of $giveYourAgentText" in {
        document.select(giveYourAgentSelector).text() shouldBe giveYourAgentText
      }

      s"has text on the screen of $ifAnyoneElseText" in {
        document.select(ifAnyoneElseSelector).text() shouldBe ifAnyoneElseText
      }

      s"has a $addThemTooText link" in {
        document.select(addThemToSelector).text() shouldBe addThemTooText
        document.select(addThemToSelector).attr("href") shouldBe addThemTooHref
      }

      s"has text on the screen of $weUseYourText" in {
        document.select(weUseYourSelector).text() shouldBe weUseYourText
      }

      s"has text on the screen of $termsOfUseText" in {
        document.select(termsOfUseSelector).text() shouldBe termsOfUseText
      }

      s"has text on the screen of $infoProvidedText" in {
        document.select(infoProvidedSelector).text() shouldBe infoProvidedText
      }

      s"has a $termsLinkText link" in {
        document.select(termsLinkSelector).text() shouldBe termsLinkText
        document.select(termsLinkSelector).attr("href") shouldBe termsLinkHref
      }

      s"has a $goToDashboardText button" in {
        document.select(goToDashboardSelector).text() shouldBe goToDashboardText
        document.select(goToDashboardSelector).attr("href") shouldBe goToDashboardButtonHref
      }
    }

    "Show a Welsh confirmation screen with the correct text" which {

      lazy val document: Document = getSuccessPage(Welsh, Individual)

      s"has a title of $titleText in welsh" in {
        document.title() shouldBe titleTextWelsh
      }

      s"has a header of $headingText in welsh" in {
        document.select(headingSelector).text() shouldBe headingTextWelsh
      }

      s"has a small header of the $yourVoaIdText in welsh" in {
        document.select(yourVoaIdSelector).text() shouldBe yourVoaIdTextWelsh + " " + yourAgentCodeTextWelsh
      }

      s"has text on the screen of $yourAgentCodeText in welsh" in {
        document.select(yourAgentCodeSelector).text() shouldBe yourAgentCodeTextWelsh
      }

      s"has text on the screen of $weHaveSentText in welsh" in {
        document.select(weHaveSentTextSelector).text() shouldBe weHaveSentTextWelsh
      }

      s"has text on the screen of $whatHappensNextText in welsh" in {
        document.select(whatHappensNextSelector).text() shouldBe whatHappensNextTextWelsh
      }

      s"has text on the screen of $nextTimeYouText in welsh" in {
        document.select(nextTimeYouSelector).text() shouldBe nextTimeYouTextWelsh
      }

      s"has text on the screen of $ifYouNeedToText in welsh" in {
        document.select(ifYouNeedToSelector).text() shouldBe ifYouNeedToTextWelsh
      }

      s"has text on the screen of $giveYourAgentText in welsh" in {
        document.select(giveYourAgentSelector).text() shouldBe giveYourAgentTextWelsh
      }

      s"has text on the screen of $ifAnyoneElseText in welsh" in {
        document.select(ifAnyoneElseSelector).text() shouldBe ifAnyoneElseTextWelsh
      }

      s"has a $addThemTooText link in welsh" in {
        document.select(addThemToSelector).text() shouldBe addThemTooTextWelsh
        document.select(addThemToSelector).attr("href") shouldBe addThemTooHref
      }

      s"has text on the screen of $weUseYourText in welsh" in {
        document.select(weUseYourSelector).text() shouldBe weUseYourTextWelsh
      }

      s"has text on the screen of $termsOfUseText in welsh" in {
        document.select(termsOfUseSelector).text() shouldBe termsOfUseTextWelsh
      }

      s"has text on the screen of $infoProvidedText in welsh" in {
        document.select(infoProvidedSelector).text() shouldBe infoProvidedTextWelsh
      }

      s"has a $termsLinkText link in welsh" in {
        document.select(termsLinkSelector).text() shouldBe termsLinkTextWelsh
        document.select(termsLinkSelector).attr("href") shouldBe termsLinkHref
      }

      s"has a $goToDashboardText button in welsh" in {
        document.select(goToDashboardSelector).text() shouldBe goToDashboardTextWelsh
        document.select(goToDashboardSelector).attr("href") shouldBe goToDashboardButtonHref
      }
    }

  }

  private def getSuccessPage(language: Language, affinityGroup: AffinityGroup): Document = {

    stubsSetup(affinityGroup)

    val res = await(
      ws.url(s"http://localhost:$port/business-rates-property-linking/create-confirmation?personId=3164")
        .withCookies(languageCookie(language), getSessionCookie(testSessionId))
        .withFollowRedirects(follow = false)
        .get()
    )

    res.status shouldBe OK
    Jsoup.parse(res.body)
  }

  private def stubsSetup(affinityGroup: AffinityGroup): StubMapping = {

    stubFor {
      get("/business-rates-authorisation/authenticate")
        .willReturn {
          aResponse.withStatus(OK).withBody(Json.toJson(testAccounts).toString())
        }
    }

    val authResponseBody =
      s"""{ "affinityGroup": "$affinityGroup", "credentialRole": "Admin", "optionalName": {"name": "Test Name", "lastName": "whatever"}, "email": "test@test.com", "groupIdentifier": "group-id", "externalId": "external-id", "confidenceLevel": 200}"""

    stubFor {
      post("/auth/authorise")
        .willReturn {
          aResponse.withStatus(OK).withBody(authResponseBody)
        }
    }

  }

}
