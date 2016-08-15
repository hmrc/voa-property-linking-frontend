package useCaseSpecs.utils

import org.jsoup.nodes.Document
import org.scalatest.{AppendedClues, MustMatchers}

import scala.collection.JavaConverters._

case class HtmlPage(html: Document) extends MustMatchers with AppendedClues {

  def mustContainMultiFileInput(id: String) {
    mustContain1(s"input#$id[type=file][multiple]")
  }

  def mustContainSuccessSummary(msg: String) {
    val successSummary = html.select("div.success-summary").asScala.headOption.getOrElse(fail(s"No success summary in $html"))
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

  def mustContainSummaryErrors(errors: (String, String, String)*) =
    errors.foreach { e =>
      val summary = html.select(s"div#error-summary")
      if (summary.asScala.length != 1) fail(s"No error summary \n$html")
      val ses = summary.select("ul li a").asScala
      val link = ses.find(_.attr("href") == s"#${e._1}").getOrElse(fail(s"No error summary with ID ${e._1}\nError summary: ${ses.headOption.getOrElse("")}"))
      link.text.trim.toLowerCase mustEqual errorSummaryHtmlFor(e).toLowerCase withClue s"Errors $link did not have text ${errorSummaryHtmlFor(e)}"
    }

  private def errorSummaryHtmlFor(e: (String, String, String)): String = s"${e._2} - ${e._3}"

  def mustContainFieldErrors(errors: (String, String)*) =
    errors.foreach { e =>
      html.select(s"#${e._1}.has-error div.form-grouped-error p").asScala.count(_.text == e._2) mustEqual 1 withClue s"No field error for $e \n$allFieldErrors"
    }

  private def allFieldErrors = html.select(".has-error")
}
