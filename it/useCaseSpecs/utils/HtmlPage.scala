package useCaseSpecs.utils

import org.jsoup.nodes.Document
import org.scalatest.{AppendedClues, MustMatchers}
import scala.collection.JavaConverters._

case class HtmlPage(html: Document) extends MustMatchers with AppendedClues {

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
    html.select(selector).size mustBe 1 withClue s"Expected 1 of: '$selector'\n ${html.select(selector)}"

  def mustContainSummaryErrors(errors: (String, String)*) =
    errors.foreach { e =>
      html.select(s"div.form-error ul li a[href=${e._1}]").asScala.count(_.text == e._2) mustEqual 1 withClue s"No Summary error for $e"
    }

  def mustContainFieldErrors(errors: (String, String)*) =
    errors.foreach { e =>
      html.select(s"div#${e._1}.has-error div.form-grouped-error p").asScala.count(_.text == e._2) mustEqual 1 withClue s"No field error for $e"
    }

}
