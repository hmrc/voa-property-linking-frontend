/*
 * Copyright 2022 HM Revenue & Customs
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

package utils

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.scalatest.AppendedClues
import org.scalatest.matchers.should.Matchers
import play.api.mvc.Result
import play.api.test.Helpers._
import play.twirl.api.Html

import scala.collection.JavaConverters._
import scala.concurrent.Future

case class HtmlPage(html: Document) extends Matchers with AppendedClues {
  type FieldId = String
  type FieldName = String
  type Message = String
  private def allRadios = html.select("input[type=radio]").asScala

  def shouldContainTable(selector: String): Unit =
    html.select(selector).size() shouldBe 1

  def shouldContainText(text: String) {
    html.body.text.contains(text) shouldBe true withClue s"HTML did not contain: $text\nHTML:\n${html.body.text}"
  }

  def shouldNotContainText(text: String) {
    html.body.text.contains(text) shouldBe false withClue s"HTML did contain: $text\nHTML:\n${html.body.text}"
  }

  def titleShouldMatch(text: String) {
    html.title().equals(text) shouldBe true withClue s"Title did not match. Expected:$text\nActual:\n${html.title()}"
  }

  def inputShouldContain(fieldId: String, text: String): Unit =
    Option(html.getElementById(fieldId)).map(_.`val`()) shouldBe Some(text)

  def mustContainRadioSelect(name: String, options: Seq[String]) =
    options.foreach { o =>
      html
        .select(s"input[type=radio][name=$name][value=$o]")
        .asScala
        .length shouldEqual 1 withClue s"No radio option: $name $o. All radios:\n$allRadios}"
    }

  def shouldContainDateSelect(name: String) =
    List("day", "month", "year").foreach(x => shouldContainTextInput(s"[name=$name.$x]"))

  def shouldContainTextInput(selector: String) = shouldContainInput(s"form input[type=text]$selector")

  private def shouldContainInput(selector: String) =
    if (html.select(selector).size != 1) {
      fail(
        s"Expected one element matching $selector, found inputs: \n${html.select("form input").asScala.mkString("\n\t")}")
    }

  def shouldContain(selector: String, count: Int) =
    html
      .select(selector)
      .size shouldBe count withClue s"Expected $count of: '$selector'\n ${html.select(selector)}\nFull HTML: \n$html"

  def verifyElementTextByClass(cssSelector: String, text: String): Unit =
    Option(html.getElementsByClass(cssSelector)).map(_.text()) shouldBe Some(text)

  def verifyElementText(fieldId: String, text: String): Unit =
    Option(html.getElementById(fieldId)).map(_.text()) shouldBe Some(text)

  def verifyElementTextByAttribute(attribute: String, value: String, text: String): Unit =
    Option(html.getElementsByAttributeValue(attribute, value)).map(_.text()) shouldBe Some(text)

  def shouldContainSummaryErrors(errors: (FieldId, FieldName, Message)*) =
    errors.foreach {
      case (id, name, msg) =>
        val summary = html.select(s"div#error-summary")
        if (summary.asScala.length != 1) fail(s"No error summary \n$html")
        val ses = summary.select("ul li a").asScala
        val link = ses
          .find(_.attr("href") == s"#${id}Group")
          .getOrElse(fail(s"No error summary with ID ${id}Group\nError summary: ${ses.headOption.getOrElse("")}"))
        link.text.trim.toLowerCase shouldEqual errorSummaryHtmlFor(name, msg).toLowerCase withClue s"Errors $link did not have text ${errorSummaryHtmlFor(name, msg)}"
    }

  private def errorSummaryHtmlFor(name: FieldName, msg: Message): String = s"$name - $msg"

  def shouldContainFieldErrors(errors: (FieldId, Message)*) =
    errors.foreach { e =>
      html
        .select(s"#${e._1.replace("_", "")}Group .error-message")
        .asScala
        .map(_.text) should contain(e._2) withClue s"No field error for $e \n$allFieldErrors"
    }

  private def allFieldErrors = html.select(".has-error")
}

object HtmlPage {
  def apply(res: Future[Result]): HtmlPage = HtmlPage(Jsoup.parse(contentAsString(res)))
  def apply(content: Html): HtmlPage = HtmlPage(Jsoup.parse(content.toString))
}
