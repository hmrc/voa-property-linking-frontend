package useCaseSpecs.utils

import org.jsoup.nodes.Document
import org.scalatest.{AppendedClues, MustMatchers}

import scala.collection.JavaConverters._

case class HtmlPage(html: Document) extends MustMatchers with AppendedClues {

  def mustContainCheckbox(name: String) {
    html.select(s"form input[type=checkbox][name=$name]")
  }

  def mustContainRadioSelect(name: String, options: Seq[String]) = {
    options.foreach { o =>
      html.select(s"input[type=radio][name=$name][value=$o]").asScala.length mustEqual 1 withClue s"No radio option: $name $o"
    }
  }

  def mustContainDateSelect(name: String) =
    List("day", "month", "year").foreach( x =>
      mustContainTextInput(s"[name=$name.$x]")
    )

  def mustContainTextInput(selector: String) = mustContain1(s"form input[type=text]$selector")

  def mustContain1(selector: String) =
    html.select(selector).size mustBe 1 withClue s"Expected 1 of: '$selector'\n ${html.select(selector)}\nFull HTML: \n$html"

  def mustContainSummaryErrors(errors: (String, String)*) =
    errors.foreach { e =>
      val summary = html.select(s"div#error-summary")
      if (summary.asScala.length != 1) fail(s"No error summary \n$html")
      val ses = summary.select("ul li a").asScala
      ses.exists { x =>
        println(s"Text: \n${x.text.trim.toLowerCase} \n${errorSummaryHtmlFor(e).toLowerCase}")
        x.attr("href") == s"#${e._1}" && x.text.trim.toLowerCase == errorSummaryHtmlFor(e).toLowerCase
      } mustBe true withClue s"No error summary for $e in $ses"
    }

  private def errorSummaryHtmlFor(e: (String, String)): String = s"${e._1} - ${e._2}"

  def mustContainFieldErrors(errors: (String, String)*) =
    errors.foreach { e =>
      html.select(s"div#${e._1}.has-error div.form-grouped-error p").asScala.count(_.text == e._2) mustEqual 1 withClue s"No field error for $e \n$allFieldErrors"
    }

  private def allFieldErrors = html.select(".has-error")
}
