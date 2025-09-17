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

package controllers.registration.newRegistrationJourneyDisabled

import base.{HtmlComponentHelpers, ISpecBase}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.http.Status.OK
import play.api.test.Helpers._
import utils.ListYearsHelpers

class RegisterISpec extends ISpecBase with HtmlComponentHelpers with ListYearsHelpers {

  override lazy val extraConfig: Map[String, String] =
    Map("featureFlags.newRegistrationJourneyEnabled" -> "false")

  val titleText = "Register to use this service - Valuation Office Agency - GOV.UK"
  val headingText = "Register to use this service"
  val returningUserText = "If you’re a returning user, you can sign in to use this service."
  val alreadyRegisteredText =
    "If you’ve already registered for this service and other people from your business want to register, you need to add them to your business Government Gateway account."
  val toRegisterText =
    "To register for this service you’ll need to verify your identity by providing your National Insurance number, date of birth and details from at least one of the following:"
  val paySlipsText = "payslips"
  val ukPassportText = "UK passport"
  val p60Text = "P60"
  val insetText = "You should allow approximately 15 minutes without interruption to complete the registration process."
  val iDontHaveTheseDetailsText = "I don’t have these details"
  val iDontHaveTheseDetailsExpandedText =
    "If you don’t have the details that you need to register (for example, you haven’t been assigned a National Insurance number, or you don’t have a passport, P60 or payslip, you’ll need to contact the Valuation Office Agency (VOA)."
  val whichTypeOfAccountText = "Which type of account would you like to create?"
  val individualText = "Individual (you represent yourself) – you won’t be able to add others to this account"
  val organisationText =
    "Organisation (you represent a business, charity or other organisation) – you’ll be able to add others to this account"
  val iWantToRegisterUsingGGText = "I want to register using my existing Government Gateway account"

  val titleTextWelsh = "Cofrestru i ddefnyddio’r gwasanaeth hwn - Valuation Office Agency - GOV.UK"
  val headingTextWelsh = "Cofrestru i ddefnyddio’r gwasanaeth hwn"
  val returningUserTextWelsh =
    "Os ydych chi’n ddefnyddiwr sy’n dychwelyd, gallwch fewngofnodi i ddefnyddio’r gwasanaeth."
  val alreadyRegisteredTextWelsh =
    "Os ydych chi eisoes wedi cofrestru ar gyfer y gwasanaeth hwn a bod pobl eraill o’ch busnes eisiau cofrestru, mae angen i chi eu hychwanegu at eich cyfrif busnes Porth y Llywodraeth."
  val toRegisterTextWelsh =
    "I gofrestru ar gyfer y gwasanaeth hwn bydd angen i chi ddilysu eich hunaniaeth drwy ddarparu eich rhif Yswiriant Gwladol, dyddiad geni a manylion o leiaf un o’r canlynol:"
  val paySlipsTextWelsh = "slipiau cyflog"
  val ukPassportTextWelsh = "Pasbort y DU"
  val p60TextWelsh = "P60"
  val insetTextWelsh = "Dylech ganiatáu tua 15 munud yn ddi-dor er mwyn cwblhau’r broses gofrestru."
  val iDontHaveTheseDetailsTextWelsh = "Does gen i ddim y manylion yma"
  val iDontHaveTheseDetailsExpandedTextWelsh =
    "Os nad oes gennych y manylion er mwyn gallu cofrestru (er enghraifft, nid ydych wedi cael rhif Yswiriant Gwladol, neu nad oes gennych basbort, P60 neu slip cyflog, bydd angen i chi cysylltu ag Asiantaethy Swyddfa Brisio (VOA)."
  val whichTypeOfAccountTextWelsh = "Pa fath o gyfrif hoffech chi ei greu?"
  val individualTextWelsh =
    "Unigolyn (rydych yn cynrychioli chi’ch hunain) - ni fyddwch yn gallu ychwanegu eraill i’r cyfrif hwn"
  val organisationTextWelsh =
    "Sefydliad (rydych yn cynrychioli busnes, elusen neu sefydliad arall) – byddwch yn gallu ychwanegu eraill at y cyfrif hwn"
  val iWantToRegisterUsingGGTextWelsh = "Rwyf am gofrestru gan ddefnyddio fy nghyfrif Porth y Llywodraeth presennol"

  val headingSelector = "#main-content > div > div > h1"
  val returningUserSelector = "#main-content > div > div > p:nth-child(2)"
  val returningUserHrefSelector = "#main-content > div > div > p:nth-child(2) > a"
  val alreadyRegisteredSelector = "#main-content > div > div > p:nth-child(3)"
  val alreadyRegisteredHrefSelector = "#main-content > div > div > p:nth-child(3) > a"
  val toRegisterSelector = "#main-content > div > div > p:nth-child(4)"
  val paySlipsSelector = "#main-content > div > div > ul > li:nth-child(1)"
  val ukPassportSelector = "#main-content > div > div > ul > li:nth-child(2)"
  val p60Selector = "#main-content > div > div > ul > li:nth-child(3)"
  val insetTextSelector = "#main-content > div > div > div"
  val iDontHaveTheseDetailsSelector = "#main-content > div > div > details:nth-child(7) > summary > span"
  val iDontHaveTheseDetailsExpandedSelector = "#main-content > div > div > details:nth-child(7) > div"
  val iDontHaveTheseDetailsExpandedHrefSelector = "#main-content > div > div > details:nth-child(7) > div > a"
  val whichTypeOfAccountSelector = "#main-content > div > div > h4"
  val individualSelector = "#main-content > div > div > p:nth-child(9)"
  val individualHrefSelector = "#main-content > div > div > p:nth-child(9) > a"
  val organisationSelector = "#main-content > div > div > p:nth-child(10)"
  val organisationHrefSelector = "#main-content > div > div > p:nth-child(10) > a"
  val iWantToRegisterUsingGGSelector = "#main-content > div > div > p:nth-child(11) > a"

  val returningUserHref = "/business-rates-property-linking/login"
  val alreadyRegisteredHref = "/business-rates-property-linking/add-user-to-gg"
  val iDontHaveTheseDetailsExpandedHref = "https://www.gov.uk/contact-voa"
  val iWantToRegisterUsingGGHref = "/business-rates-property-linking/login"

  "Register controller method" should {
    "Show an English registration screen with the correct text" which {

      lazy val document: Document = getRegisterPage(English)

      s"has a title of $titleText" in {
        document.title() shouldBe titleText
      }

      s"has a header of $headingText" in {
        document.select(headingSelector).text() shouldBe headingText
      }

      s"has '$returningUserText' text" in {
        document.select(returningUserSelector).text() shouldBe returningUserText
        document.select(returningUserHrefSelector).attr("href") shouldBe returningUserHref
      }

      s"has '$alreadyRegisteredText' text" in {
        document.select(alreadyRegisteredSelector).text() shouldBe alreadyRegisteredText
        document.select(alreadyRegisteredHrefSelector).attr("href") shouldBe alreadyRegisteredHref
      }

      s"has '$toRegisterText' text" in {
        document.select(toRegisterSelector).text() shouldBe toRegisterText
      }

      s"has '$paySlipsText' text" in {
        document.select(paySlipsSelector).text() shouldBe paySlipsText
      }

      s"has '$ukPassportText' text" in {
        document.select(ukPassportSelector).text() shouldBe ukPassportText
      }

      s"has '$p60Text' text" in {
        document.select(p60Selector).text() shouldBe p60Text
      }

      s"has '$insetText' text" in {
        document.select(insetTextSelector).text() shouldBe insetText
      }

      s"has '$iDontHaveTheseDetailsText' text" in {
        document.select(iDontHaveTheseDetailsSelector).text() shouldBe iDontHaveTheseDetailsText
      }

      s"has '$iDontHaveTheseDetailsExpandedText' text" in {
        document.select(iDontHaveTheseDetailsExpandedSelector).text() shouldBe iDontHaveTheseDetailsExpandedText
        document
          .select(iDontHaveTheseDetailsExpandedHrefSelector)
          .attr("href") shouldBe iDontHaveTheseDetailsExpandedHref
      }

      s"has '$whichTypeOfAccountText' text" in {
        document.select(whichTypeOfAccountSelector).text() shouldBe whichTypeOfAccountText
      }

      s"has '$individualText' text" in {
        document.select(individualSelector).text() shouldBe individualText
      }

      s"has '$organisationText' text" in {
        document.select(organisationSelector).text() shouldBe organisationText
      }

      s"has '$iWantToRegisterUsingGGText' text" in {
        document.select(iWantToRegisterUsingGGSelector).text() shouldBe iWantToRegisterUsingGGText
        document.select(iWantToRegisterUsingGGSelector).attr("href") shouldBe iWantToRegisterUsingGGHref
      }

    }

    "Show a Welsh registration page with the correct text" which {

      lazy val document: Document = getRegisterPage(Welsh)

      s"has a title of $titleText in Welsh" in {
        document.title() shouldBe titleTextWelsh
      }

      s"has a header of $headingText in Welsh" in {
        document.select(headingSelector).text() shouldBe headingTextWelsh
      }

      s"has '$returningUserText' text in Welsh" in {
        document.select(returningUserSelector).text() shouldBe returningUserTextWelsh
        document.select(returningUserHrefSelector).attr("href") shouldBe returningUserHref
      }

      s"has '$alreadyRegisteredText' text in Welsh" in {
        document.select(alreadyRegisteredSelector).text() shouldBe alreadyRegisteredTextWelsh
        document.select(alreadyRegisteredHrefSelector).attr("href") shouldBe alreadyRegisteredHref
      }

      s"has '$toRegisterText' text in Welsh" in {
        document.select(toRegisterSelector).text() shouldBe toRegisterTextWelsh
      }

      s"has '$paySlipsText' text in Welsh" in {
        document.select(paySlipsSelector).text() shouldBe paySlipsTextWelsh
      }

      s"has '$ukPassportText' text in Welsh" in {
        document.select(ukPassportSelector).text() shouldBe ukPassportTextWelsh
      }

      s"has '$p60Text' text in Welsh" in {
        document.select(p60Selector).text() shouldBe p60TextWelsh
      }

      s"has '$insetText' text in Welsh" in {
        document.select(insetTextSelector).text() shouldBe insetTextWelsh
      }

      s"has '$iDontHaveTheseDetailsText' text in Welsh" in {
        document.select(iDontHaveTheseDetailsSelector).text() shouldBe iDontHaveTheseDetailsTextWelsh
      }

      s"has '$iDontHaveTheseDetailsExpandedText' text in Welsh" in {
        document.select(iDontHaveTheseDetailsExpandedSelector).text() shouldBe iDontHaveTheseDetailsExpandedTextWelsh
        document
          .select(iDontHaveTheseDetailsExpandedHrefSelector)
          .attr("href") shouldBe iDontHaveTheseDetailsExpandedHref
      }

      s"has '$whichTypeOfAccountText' text in Welsh" in {
        document.select(whichTypeOfAccountSelector).text() shouldBe whichTypeOfAccountTextWelsh
      }

      s"has '$individualText' text in Welsh" in {
        document.select(individualSelector).text() shouldBe individualTextWelsh
      }

      s"has '$organisationText' text in Welsh" in {
        document.select(organisationSelector).text() shouldBe organisationTextWelsh
      }

      s"has '$iWantToRegisterUsingGGText' text in Welsh" in {
        document.select(iWantToRegisterUsingGGSelector).text() shouldBe iWantToRegisterUsingGGTextWelsh
        document.select(iWantToRegisterUsingGGSelector).attr("href") shouldBe iWantToRegisterUsingGGHref
      }
    }
  }

  private def getRegisterPage(language: Language): Document = {

    val res = await(
      ws.url(s"http://localhost:$port/business-rates-property-linking/")
        .withCookies(languageCookie(language), getSessionCookie(testSessionId))
        .withFollowRedirects(follow = false)
        .get()
    )

    res.status shouldBe OK
    Jsoup.parse(res.body)
  }
}
