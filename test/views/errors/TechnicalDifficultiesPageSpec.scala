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

package views.errors

import java.time.LocalDateTime
import play.api.test.FakeRequest
import tests.BaseUnitSpec
import utils._
import utils.StubMessageControllerComponents._
import utils.{Configs, NoMetricsOneAppPerSuite}

class TechnicalDifficultiesPageSpec extends BaseUnitSpec with NoMetricsOneAppPerSuite with FakeViews {

  "The technical difficulties page (AM)" should {
    val ref: String = shortString
    val req = FakeRequest()

    // the LAZY keyword is actually important here
    // without LAZY you get
    // An exception or error caused a run to abort: java.lang.RuntimeException was thrown inside "The technical difficulties page" should, construction cannot continue: "There is no started application"
    lazy val view = new views.html.errors.technicalDifficulties(layout)

    lazy val html =
      view(Some(ref), LocalDateTime.of(2017, 4, 1, 9, 30))(req, messagesApi.preferred(req), Configs.applicationConfig)
    lazy val page = HtmlPage(html)

    "display the correct page title" in
      page.titleShouldMatch("Sorry, we’re experiencing technical difficulties - Valuation Office - GOV.UK")

    "display the correct page heading" in
      page.verifyElementTextByClass("govuk-heading-l", "Sorry, we’re experiencing technical difficulties")

    "display the first paragraph" in
      page.shouldContainText("Please try again in a few minutes")

    "display the second paragraph" in
      page.shouldContainText(
        "If you continue to experience problems, please contact us on 03000 501 501. You’ll need to tell us the date, time and your reference number."
      )

    "display an error reference number" in
      page.shouldContainText(s"Reference number: $ref")

    "display the current date" in
      page.shouldContainText("Date: 1 April 2017")

    "display the current time in 12 hour format" in
      page.shouldContainText("Time: 9:30am")
  }

  "The technical difficulties page (PM)" should {
    val ref: String = shortString
    val req = FakeRequest()

    // the LAZY keyword is actually important here
    // without LAZY you get
    // An exception or error caused a run to abort: java.lang.RuntimeException was thrown inside "The technical difficulties page" should, construction cannot continue: "There is no started application"
    lazy val view = new views.html.errors.technicalDifficulties(layout)

    lazy val html =
      view(Some(ref), LocalDateTime.of(2017, 4, 1, 21, 30))(req, messagesApi.preferred(req), Configs.applicationConfig)
    lazy val page = HtmlPage(html)

    "display the correct page title" in
      page.titleShouldMatch("Sorry, we’re experiencing technical difficulties - Valuation Office - GOV.UK")

    "display the correct page heading" in
      page.verifyElementTextByClass("govuk-heading-l", "Sorry, we’re experiencing technical difficulties")

    "display the first paragraph" in
      page.shouldContainText("Please try again in a few minutes")

    "display the second paragraph" in
      page.shouldContainText(
        "If you continue to experience problems, please contact us on 03000 501 501. You’ll need to tell us the date, time and your reference number."
      )

    "display an error reference number" in
      page.shouldContainText(s"Reference number: $ref")

    "display the current date" in
      page.shouldContainText("Date: 1 April 2017")

    "display the current time in 12 hour format" in
      page.shouldContainText("Time: 9:30pm")
  }

  "The technical difficulties page in Welsh (AM)" should {
    val ref: String = shortString
    val reqCy = FakeRequest().withTransientLang("cy")

    lazy val viewCy = new views.html.errors.technicalDifficulties(layout)

    lazy val htmlCy =
      viewCy(Some(ref), LocalDateTime.of(2017, 4, 1, 9, 30))(
        reqCy,
        messagesApi.preferred(reqCy),
        Configs.applicationConfig
      )
    lazy val pageCy = HtmlPage(htmlCy)

    "display the correct Welsh page title" in
      pageCy.titleShouldMatch("Ymddiheuriadau, rydym yn profi anawsterau technegol - Y Swyddfa Brisio - GOV.UK")

    "display the correct Welsh page heading" in
      pageCy.verifyElementTextByClass("govuk-heading-l", "Ymddiheuriadau, rydym yn profi anawsterau technegol")

    "display the first Welsh paragraph" in
      pageCy.shouldContainText("Rhowch gynig arall arni mewn ychydig funudau")

    "display the second Welsh paragraph" in
      pageCy.shouldContainText(
        "Os ydych yn parhau i gael problemau, cysylltwch â ni ar 03000 505 505. Bydd angen i chi ddweud y dyddiad, yr amser, a’ch cyfeirnod wrthym."
      )

    "display a Welsh error reference number" in
      pageCy.shouldContainText(s"Cyfeirnod: $ref")

    "display the current date in Welsh" in
      pageCy.shouldContainText("Dyddiad: 1 Ebrill 2017")

    "display the current time in 12 hour format in Welsh" in
      pageCy.shouldContainText("Amser: 9:30y.b")
  }

  "The technical difficulties page in Welsh (PM)" should {
    val ref: String = shortString
    val reqCy = FakeRequest().withTransientLang("cy")

    lazy val viewCy = new views.html.errors.technicalDifficulties(layout)

    lazy val htmlCy =
      viewCy(Some(ref), LocalDateTime.of(2017, 4, 1, 21, 30))(
        reqCy,
        messagesApi.preferred(reqCy),
        Configs.applicationConfig
      )
    lazy val pageCy = HtmlPage(htmlCy)

    "display the correct Welsh page title" in
      pageCy.titleShouldMatch("Ymddiheuriadau, rydym yn profi anawsterau technegol - Y Swyddfa Brisio - GOV.UK")

    "display the correct Welsh page heading" in
      pageCy.verifyElementTextByClass("govuk-heading-l", "Ymddiheuriadau, rydym yn profi anawsterau technegol")

    "display the first Welsh paragraph" in
      pageCy.shouldContainText("Rhowch gynig arall arni mewn ychydig funudau")

    "display the second Welsh paragraph" in
      pageCy.shouldContainText(
        "Os ydych yn parhau i gael problemau, cysylltwch â ni ar 03000 505 505. Bydd angen i chi ddweud y dyddiad, yr amser, a’ch cyfeirnod wrthym."
      )

    "display a Welsh error reference number" in
      pageCy.shouldContainText(s"Cyfeirnod: $ref")

    "display the current date in Welsh" in
      pageCy.shouldContainText("Dyddiad: 1 Ebrill 2017")

    "display the current time in 12 hour format in Welsh" in
      pageCy.shouldContainText("Amser: 9:30y.h")
  }

}
