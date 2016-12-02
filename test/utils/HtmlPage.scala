/*
 * Copyright 2016 HM Revenue & Customs
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

import org.scalatest.{AppendedClues, MustMatchers}
import org.jsoup.Jsoup
import play.api.test.Helpers._
import scala.collection.JavaConverters._
import org.jsoup.nodes.Document
import play.api.mvc.Result

import scala.concurrent.Future

case class HtmlPage(html: Document) extends MustMatchers with AppendedClues {
  type FieldId = String
  type FieldName = String
  type Message = String

  def mustContainFileInput(id: String) {

    mustContain1(s"input#$id[type=file]")
  }

  def mustContainSuccessSummary(msg: String) {
    val successSummary = html.select("h1.heading-confirmation").asScala.headOption.getOrElse(fail(s"No success summary in $html"))
    successSummary.text mustEqual msg withClue s"Success summary contained:\n${successSummary.text}"
  }

  def mustContainText(text: String) {
    html.body.text.contains(text) mustBe true withClue s"HTML did not contain: $text\nHTML:\n${html.body.text}"
  }

  def mustContainLink(selector: String, href: String) = mustContain1(s"a$selector[href=$href]")

  def mustContainDataRow(cellValues: String*) {
    val rows = html.select("table tbody tr").asScala
    rows.exists { r =>
      val values = r.select("td").asScala.map(_.text)
      values == cellValues
    } mustBe true withClue s"No row with cell values: $cellValues.\nAll rows:\n ${rows.mkString("\n")}"
  }

  def mustContainDataInRow(cellValues: String*) {
    val rows = html.select("table tbody tr").asScala
    cellValues.foreach(cell => {
      rows.mkString("\n").contains(cell) mustBe true withClue s"No row with cell value: $cell.\nAll rows:\n ${rows.mkString("\n")}"
    })
  }

  def mustContainCheckbox(name: String) {
    html.select(s"form input[type=checkbox][name=$name]")
  }

  def mustContainRadioSelect(name: String, options: Seq[String]) =
    options.foreach { o =>
      html.select(s"input[type=radio][name=$name][value=$o]").asScala.length mustEqual 1 withClue s"No radio option: $name $o. All radios:\n$allRadios}"
    }

  private def allRadios = html.select("input[type=radio]").asScala

  def mustContainDateSelect(name: String) =
    List("day", "month", "year").foreach( x =>
      mustContainTextInput(s"[name=$name.$x]")
    )

  def mustContainTextInput(selector: String) = mustContain1(s"form input[type=text]$selector")

  def mustContain1(selector: String) =
    html.select(selector).size mustBe 1 withClue s"Expected 1 of: '$selector'\n ${html.select(selector)}\nFull HTML: \n$html"

  def mustContainSummaryErrors(errors: (FieldId, FieldName, Message)*) =
    errors.foreach { case (id, name, msg) =>
      val summary = html.select(s"div#error-summary")
      if (summary.asScala.length != 1) fail(s"No error summary \n$html")
      val ses = summary.select("ul li a").asScala
      val link = ses.find(_.attr("href") == s"#$id").getOrElse(fail(s"No error summary with ID $id\nError summary: ${ses.headOption.getOrElse("")}"))
      link.text.trim.toLowerCase mustEqual errorSummaryHtmlFor(name, msg).toLowerCase withClue s"Errors $link did not have text ${errorSummaryHtmlFor(name, msg)}"
    }

  private def errorSummaryHtmlFor(name: FieldName, msg: Message): String = s"$name - $msg"

  def mustContainFieldErrors(errors: (FieldId, Message)*) =
    errors.foreach { e =>
      html.select(s"#${e._1} p.error-message").asScala.count(_.text == e._2) mustEqual 1 withClue s"No field error for $e \n$allFieldErrors"
    }

  private def allFieldErrors = html.select(".has-error")
}

object HtmlPage {
  def apply(res: Future[Result]): HtmlPage = HtmlPage(Jsoup.parse(contentAsString(res)))
}
