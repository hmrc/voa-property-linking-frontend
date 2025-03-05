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

package base

import org.jsoup.nodes.{Document, Element}

import scala.jdk.CollectionConverters._

trait HtmlComponentHelpers { self: ISpecBase =>
  def testPageTitle(
        expectedEnglishText: String,
        expectedWelshText: String,
        document: Document,
        titleClass: String = "govuk-heading-l"
  )(implicit language: Language): PartialFunction[Element, Unit] = { element =>
    val expectedText = language match {
      case English => expectedEnglishText
      case Welsh   => expectedWelshText
    }
    language match {
      case English => document.title shouldBe s"$expectedText - Valuation Office Agency - GOV.UK"
      case Welsh   => document.title shouldBe s"$expectedText - Asiantaeth y Swyddfa Brisio - GOV.UK"
    }
    element.text shouldBe expectedText
    element.classNames should contain(titleClass)
  }

  def testParagraph(expectedEnglishText: String, expectedWelshText: String)(implicit
        language: Language
  ): PartialFunction[Element, Unit] = { element =>
    element.tagName shouldBe "p"
    element.classNames should contain("govuk-body")
    language match {
      case English => element.text shouldBe expectedEnglishText
      case Welsh   => element.text shouldBe expectedWelshText
    }
  }

  def testLink(
        expectedEnglishText: String,
        expectedWelshText: String,
        href: String,
        expectedClasses: Seq[String] = Seq("govuk-link")
  )(implicit
        language: Language
  ): PartialFunction[Element, Unit] = { element =>
    element.tagName shouldBe "a"
    element.classNames should contain allElementsOf expectedClasses
    element.attr("href") shouldBe href
    language match {
      case English => element.text shouldBe expectedEnglishText
      case Welsh   => element.text shouldBe expectedWelshText
    }
  }

  def testCaption(expectedEnglishText: String, expectedWelshText: String, captionSize: String = "l")(implicit
        language: Language
  ): PartialFunction[Element, Unit] = { element =>
    element.tagName shouldBe "span"
    element.classNames should contain(s"govuk-caption-$captionSize")
    language match {
      case English => element.text shouldBe expectedEnglishText
      case Welsh   => element.text shouldBe expectedWelshText
    }
  }

  def testBulletList(expectedEnglishBullets: Seq[String], expectedWelshBullets: Seq[String])(implicit
        language: Language
  ): PartialFunction[Element, Unit] = { element =>
    element.tagName shouldBe "ul"
    element.classNames should contain allElementsOf Seq("govuk-list", "govuk-list--bullet")
    val bulletList = language match {
      case English => expectedEnglishBullets
      case Welsh   => expectedWelshBullets
    }

    bulletList.zipWithIndex.foreach { case (bulletText, index) =>
      element.child(index).tagName shouldBe "li"
      element.child(index).text shouldBe bulletText
    }

    element.children().asScala.drop(expectedEnglishBullets.size).foreach { missingElement =>
      fail(s"$missingElement bullet point was returned but not expected")
    }
  }

  def testRadio(
        expectedEnglishText: String,
        expectedWelshText: String,
        expectedValue: String,
        expectedName: String,
        checked: Boolean,
        conditionalHtmlTest: Option[PartialFunction[Element, Unit]] = None
  )(implicit language: Language): PartialFunction[Element, Unit] = { element =>
    element.tagName shouldBe "input"
    element.attr("type") shouldBe "radio"
    element.attr("name") shouldBe expectedName
    element.hasAttr("checked") shouldBe checked
    val expectedText = language match {
      case English => expectedEnglishText
      case Welsh   => expectedWelshText
    }
    element.siblingElements.first.text shouldBe expectedText
    element.`val` shouldBe expectedValue
    conditionalHtmlTest.fold((): Unit)(
      _.apply(
        element.parent.parent.getElementById(element.attr("data-aria-controls")).child(0)
      )
    )
  }

  def testInput(
        expectedName: String,
        expectedValue: String = "",
        labelTest: Option[PartialFunction[Element, Unit]] = None,
        hintTest: Option[PartialFunction[Element, Unit]] = None
  ): PartialFunction[Element, Unit] = { element =>
    element.tagName shouldBe "input"
    element.classNames should contain("govuk-input")
    element.attr("type") shouldBe "text"
    element.attr("name") shouldBe expectedName
    element.`val` shouldBe expectedValue

    labelTest.fold((): Unit)(
      _.apply(element.siblingElements.asScala.find(_.tagName == "label").getOrElse(fail("Could not find label")))
    )
    hintTest.fold((): Unit)(
      _.apply(
        element.siblingElements.asScala
          .find(elem => elem.tagName == "div" && elem.hasClass("govuk-hint"))
          .getOrElse(fail("Could not find input hint"))
      )
    )
  }

  def testLabel(expectedEnglishText: String, expectedWelshText: String)(implicit
        language: Language
  ): PartialFunction[Element, Unit] = { element =>
    element.tagName shouldBe "label"
    element.classNames should contain("govuk-label")
    language match {
      case English => element.text shouldBe expectedEnglishText
      case Welsh   => element.text shouldBe expectedWelshText
    }
  }

  def testHint(expectedEnglishText: String, expectedWelshText: String)(implicit
        language: Language
  ): PartialFunction[Element, Unit] = { element =>
    element.tagName shouldBe "div"
    element.classNames should contain("govuk-hint")
    language match {
      case English => element.text shouldBe expectedEnglishText
      case Welsh   => element.text shouldBe expectedWelshText
    }
  }

  def testButton(expectedEnglishText: String, expectedWelshText: String)(implicit
        language: Language
  ): PartialFunction[Element, Unit] = { element =>
    element.tagName shouldBe "button"
    element.classNames should contain("govuk-button")
    language match {
      case English => element.text shouldBe expectedEnglishText
      case Welsh   => element.text shouldBe expectedWelshText
    }
  }
}
