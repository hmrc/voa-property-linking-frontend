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

import models.{NamedEnum, NamedEnumSupport}
import org.joda.time.DateTime
import org.scalatest.{AppendedClues, MustMatchers}
import play.api.data.Form
import views.helpers.Errors

object FormBindingVerification extends BasicVerification with DateVerification {

  def verifyMultiChoice[A <: NamedEnumSupport[_]](form: Form[_], validData: Map[String, String], field: String, a: A) {
    a.all.asInstanceOf[Seq[NamedEnum]].map(_.name).foreach { c =>
      mustBind(form, validData.updated(field, c))
    }
  }

  def verifyTrue(form: Form[_], field: String, error: String) {
    verifyRange(form, Map(field -> "true"), field, Seq.empty, Seq("false", "flase", "abc"), error)
  }

  def verifyMandatoryMultiChoice(form: Form[_], validData: Map[String, String], field: String) {
    val d = validData - field
    mustOnlyContainError(form.bind(d), field, Errors.noValueSelected)
  }

  def mustBindTo[A](form: Form[A], data: Map[String, String], result: A) {
    val f = form.bind(data)
    f.errors.isEmpty mustBe true withClue s"Form unexpectedly contained errors.${diagnostics(f)}"
    f.value.foreach(_ mustEqual result withClue s"Form did not bind to $result.${diagnostics(f)}")
  }

}

trait DateVerification { this: BasicVerification =>

  def verifyMandatoryDate(form: Form[_], validData: Map[String, String], dateField: String) {
    Seq("day", "month", "year").foreach { x =>
      verifyMandatory(form, validData, s"$dateField.$x")
    }
  }

  def verifyOptionalDate(form: Form[_], validData: Map[String, String], dateField: String) {
    val d = validData - s"$dateField.day" - s"$dateField.month" - s"$dateField.year"
    mustBind(form, d)
  }

  def verifyddmmyy(form: Form[_], validData: Map[String, String], dateField: String) {
    validateDay(form, validData, dateField)
    validateMonth(form, validData, dateField)
    validateYearUpto(form, validData, dateField, 3000)
    verifyRealDatesOnly(form, validData, dateField)
  }

  def verifyPastddmmyy(form: Form[_], validData: Map[String, String], dateField: String) {
    validateDay(form, validData, dateField)
    validateMonth(form, validData, dateField)
    validateYearUpto(form, validData, dateField, DateTime.now.year.get - 1)
    verifyPastDatesOnly(form, validData, dateField)
  }

  private def validateDay[T](form: Form[T], validData: Map[String, String], field: String) {
    verifyRange(form, validData, s"$field.day", (1 to 31).map(_.toString), Seq("0"), Errors.belowMinimum)
    verifyRange(form, validData, s"$field.day", Seq.empty, Seq("32", "33", "55", "101", "58987"), Errors.aboveMaximum)
    verifyNonEmptyString(form, validData, s"$field.day", Errors.invalidNumber)
    verifyAcceptsLeadingAndTrailingWhitespace(form, validData, s"$field.day")
  }

  private def verifyRealDatesOnly[T](form: Form[T], validData: Map[String, String], field: String) {
    def withDate(dt: (String, String, String)) =
      validData.updated(field + ".day", dt._1).updated(field + ".month", dt._2).updated(field + ".year", dt._3)

    val invalid = Seq(("29", "2", "2015"),("31", "9", "2015"))
    invalid foreach { iv =>  verifyOnlyError(form, withDate(iv), field, Errors.invalidDate) }

    val valid = Seq(("28", "2", "2012"), ("31", "8", "2015"), ("30", "9", "2015"))
    valid foreach { v => mustBind(form, withDate(v)) }
  }

  private def validateMonth(form: Form[_], validData: Map[String, String], field: String) {
    verifyRange(form, validData, s"$field.month", (1 to 12).map(_.toString), Seq("0", "-1"), Errors.belowMinimum)
    verifyRange(form, validData, s"$field.month", Seq.empty, Seq("13", "999"), Errors.aboveMaximum)
    verifyNonEmptyString(form, validData, s"$field.month", Errors.invalidNumber)
    verifyAcceptsLeadingAndTrailingWhitespace(form, validData, s"$field.month")
  }

  private def validateYearUpto[T](form: Form[T], validData: Map[String, String], field: String, maxYear: Int) {
    verifyRange(form, validData, s"$field.year", (1900 to maxYear).map(_.toString), Seq("1899", "200", "1"), Errors.belowMinimum)
    verifyRange(form, validData, s"$field.year", Seq.empty, Seq("3001", "4000", "999999"), Errors.aboveMaximum)
    verifyNonEmptyString(form, validData, s"$field.year", Errors.invalidNumber)
    verifyAcceptsLeadingAndTrailingWhitespace(form, validData, s"$field.year")
  }

  private def verifyPastDatesOnly[T](form: Form[T], validData: Map[String, String], field: String) {
    def withDate(dt: (String, String, String)) =
      validData.updated(field + ".day", dt._1).updated(field + ".month", dt._2).updated(field + ".year", dt._3)

    val tomorrow = DateTime.now.plusDays(1)
    val invalid = Seq(
      ("28", "2", "2225"), ("23", "5", "2115"), (tomorrow.getDayOfMonth.toString, tomorrow.getMonthOfYear.toString, tomorrow.getYear.toString)
    )
    invalid foreach { iv => verifyOnlyError(form, withDate(iv), field, Errors.dateMustBeInPast) }

    val yday = DateTime.now.minusDays(1)
    val valid = Seq(
      ("1", "1", "2015"), ("31", "10", "2014"), (yday.getDayOfMonth.toString, yday.getMonthOfYear.toString, yday.getYear.toString)
    )
    valid foreach { v => mustBind(form, withDate(v)) }
  }
}

trait BasicVerification extends MustMatchers with AppendedClues with FormChecking {

  def verifyMandatory(form: Form[_], validData: Map[String, String], field: String) {
    val data = validData - field
    mustOnlyContainRequiredError(form.bind(data), field)
  }

  def verifyOptional(form: Form[_], validData: Map[String, String], field: String) {
    mustBind(form, validData - field)
  }

  def verifyCharacterLimit(form: Form[_], validData: Map[String, String], field: String, limit: Int) {
    mustBind(form, validData.updated(field, 1 to limit map { _ => "a" } mkString))

    val f = form.bind(validData.updated(field, (1 to limit + 1) map { _ => "b"} mkString))
    mustContainError(f, field, "error.maxLength")
  }

  protected def verifyNonEmptyString[T](form: Form[T], validData: Map[String, String], field: String, error: String) {
    val data = validData.updated(field, "")
    mustOnlyContainError(form.bind(data), field, error)
  }

  def verifyOnlyError(form: Form[_], invalidData: Map[String, String], field: String, error: String) {
     mustOnlyContainError(form.bind(invalidData), field, error)
  }

  def verifyError(form: Form[_], invalidData: Map[String, String], field: String, error: String) {
    mustContainError(form.bind(invalidData), field, error)
  }

  protected def verifyAcceptsLeadingAndTrailingWhitespace[T](form: Form[T], validData: Map[String, String], field: String) {
    mustBind(form, validData.updated(field, s" ${validData(field)} "))
  }

  protected def verifyRange(form: Form[_], validData: Map[String, String], field: String, valid: Seq[String], invalid: Seq[String], error: String) {
    valid.foreach(x => mustBind(form, validData.updated(field, x)))
    invalid.foreach(x => mustContainError(form.bind(validData.updated(field, x)), field, error))
  }
}

trait FormChecking extends MustMatchers with AppendedClues {

  def mustBind[A](form: Form[A], data: Map[String, String]): Form[A] = {
    val f = form.bind(data)
    f.bind(data).errors.isEmpty  mustBe true withClue s"Form did not bind.${diagnostics(f)}"
    f.value.getOrElse(fail(s"Form has errors:${diagnostics(f)}"))
    f
  }

  protected def mustOnlyContainRequiredError(form: Form[_], field: String) {
    mustOnlyContainError(form, field, Errors.required)
  }

  protected def mustOnlyContainError[T](form: Form[T], field: String, error: String) {
    mustContainError(form, field, error)
    if (form.errors.length > 1) {
      fail(s"Form contained too many errors. Expected only: $field - $error.${diagnostics(form)}")
    }
  }

  protected def mustContainError(form: Form[_], field: String, error: String) {
    val xists = form.errors.exists(e => e.key == field && e.messages.head == error)
    xists mustEqual true withClue s"No error for $field - $error${diagnostics(form)}"
  }

  protected def diagnostics(f: Form[_]) = s"\nErrors: ${f.errors} \nData: ${f.data}"
}
