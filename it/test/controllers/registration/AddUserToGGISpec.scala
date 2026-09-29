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
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.http.Status.OK
import play.api.test.Helpers._
import utils.ListYearsHelpers

class AddUserToGGISpec extends ISpecBase with HtmlComponentHelpers with ListYearsHelpers {

  val titleText = "Add another user to your GOV.UK account - Valuation Office - GOV.UK"
  val headingText = "Add another user to your GOV.UK account"
  val p1Text =
    "You’ll need to be an administrator for the Government Gateway account to add users. You’re an administrator if you created the Government Gateway account, or if you’ve been added as an administrator to the account. We recommend you have at least 2 administrators for your organisation."
  val p2Text =
    "You should be aware that if you add an agent to your Government Gateway account, they’ll have the same access rights as other assistant users within your organisation."
  val p3Text = "You can add a user on the next screen by selecting “Account users” then “Manage users”."
  val addUserButtonText = "Add a user"

  val titleTextWelsh = "Ychwanegu defnyddiwr arall at eich cyfrif GOV.UK - Y Swyddfa Brisio - GOV.UK"
  val headingTextWelsh = "Ychwanegu defnyddiwr arall at eich cyfrif GOV.UK"
  val p1TextWelsh =
    "Bydd angen i chi fod yn weinyddwr ar gyfer cyfrif Porth y Llywodraeth i ychwanegu defnyddwyr. Rydych chi’n weinyddwr os wnaethoch chi greu y cyfrif Porth y Llywodraeth, neu os ydych chi wedi cael eich ychwanegu fel gweinyddwr i’r cyfrif. Rydym yn argymell bod gennych o leiaf 2 weinyddwr ar gyfer eich sefydliad."
  val p2TextWelsh =
    "Os ydych yn ychwanegu asiant at eich cyfrif Porth y Llywodraeth dylech fod yn ymwybodol bydd ganddynt yr un hawliau mynediad â defnyddwyr cynorthwyol eraill yn eich sefydliad."
  val p3TextWelsh =
    "Gallwch ychwanegu defnyddiwr ar y sgrin nesaf drwy ddewis ’Defnyddwyr cyfrif’ yna \"Rheoli defnyddwyr’"
  val addUserButtonTextWelsh = "Ychwanegu defnyddiwr"

  val headingSelector = "#main-content > div > div > h1"
  val p1Selector = "#main-content > div > div > p:nth-child(2)"
  val p2Selector = "#main-content > div > div > p:nth-child(3)"
  val p3Selector = "#main-content > div > div > p:nth-child(4)"
  val addUserButtonSelector = "#main-content > div > div > a"
  val returningUserHref = "/business-rates-property-linking/login"

  "Add user to GG controller method" should {
    "Show an Add user to GG screen with the correct text" which {

      lazy val document: Document = getAddToGGPage(English)

      s"has a title of $titleText" in {
        document.title() shouldBe titleText
      }

      s"has a header of $headingText" in {
        document.select(headingSelector).text() shouldBe headingText
      }

      s"has '$p1Text' text" in {
        document.select(p1Selector).text() shouldBe p1Text
      }

      s"has '$p2Text' text" in {
        document.select(p2Selector).text() shouldBe p2Text
      }

      s"has '$p3Text' text" in {
        document.select(p3Selector).text() shouldBe p3Text
      }
      s"has a button with text '$addUserButtonText'" in {
        document.select(addUserButtonSelector).text() shouldBe addUserButtonText
      }

    }

    "Show a Welsh Add user to GG page with the correct text" which {

      lazy val document: Document = getAddToGGPage(Welsh)

      s"has a title of $titleText in Welsh" in {
        document.title() shouldBe titleTextWelsh
      }

      s"has a header of $headingText in Welsh" in {
        document.select(headingSelector).text() shouldBe headingTextWelsh
      }

      s"has '$p1Text' text in Welsh" in {
        document.select(p1Selector).text() shouldBe p1TextWelsh
      }

      s"has '$p2Text' text in Welsh" in {
        document.select(p2Selector).text() shouldBe p2TextWelsh
      }

      s"has '$p3Text' text in Welsh" in {
        document.select(p3Selector).text() shouldBe p3TextWelsh
      }
      s"has a button with text '$addUserButtonText' in Welsh" in {
        document.select(addUserButtonSelector).text() shouldBe addUserButtonTextWelsh
      }

    }
  }

  private def getAddToGGPage(language: Language): Document = {

    val res = await(
      ws.url(s"http://localhost:$port/business-rates-property-linking/add-user-to-gg")
        .withCookies(languageCookie(language), getSessionCookie(testSessionId))
        .withFollowRedirects(follow = false)
        .get()
    )

    res.status shouldBe OK
    Jsoup.parse(res.body)
  }
}
