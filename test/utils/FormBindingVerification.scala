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

import models.registration.keys
import models.{NamedEnum, NamedEnumSupport}
import org.scalatest.AppendedClues
import org.scalatest.matchers.should.Matchers
import play.api.data.Form
import views.helpers.Errors

import java.time.LocalDate

object FormBindingVerification extends BasicVerification with DateVerification with ContactDetailsVerification {

  def verifyMultiChoice[A <: NamedEnumSupport[_]](
        form: Form[_],
        validData: Map[String, String],
        field: String,
        a: A): Unit =
    a.all.asInstanceOf[Seq[NamedEnum]].map(_.name).foreach { c =>
      shouldBind(form, validData.updated(field, c))
    }

  def verifyBoolean(
        form: Form[_],
        validData: Map[String, String],
        field: String,
        error: String = "error.boolean"): Unit =
    verifyRange(form, validData, field, Seq("true", "false"), Seq("treu", "flase", "abc"), error)

  def verifyMandatoryMultiChoice(form: Form[_], validData: Map[String, String], field: String): Unit = {
    val d = validData - field
    shouldOnlyContainError(form.bind(d), field, Errors.noValueSelected)
  }

  def shouldBindTo[A](form: Form[A], data: Map[String, String], result: A): Unit = {
    val f = form.bind(data)
    f.errors.isEmpty shouldBe true withClue s"Form unexpectedly contained errors.${diagnostics(f)}"
    f.value.foreach(_ shouldEqual result withClue s"Form did not bind to $result.${diagnostics(f)}")
  }

}

trait ContactDetailsVerification { this: BasicVerification =>

  def verifyValidNino(
        form: Form[_],
        validData: Map[String, String],
        ninoField: String = keys.nino,
        ninoError: String = "error.nino.invalid"): Unit = {
    val valid = Seq("AA 12 34 56 D", "AA123456D")
    val invalid = Seq("BG 12 34 56 D", "BG123456D", "ABCDEFGH", "12345678", "ABC")
    verifyRange(form, validData, ninoField, valid, invalid, ninoError)
  }

  def verifyMandatoryAddress(
        form: Form[_],
        validData: Map[String, String],
        addressField: String = keys.address): Unit = {
    verifyMandatoryFields(form, validData, addressField)
    verifyAddressFieldCharacterLimits(form, validData, addressField)
  }

  def verifyMandatoryFields(form: Form[_], validData: Map[String, String], field: String): Unit = {
    val data = validData - s"$field.line1" - s"$field.postcode"
    verifyError(form, data, s"$field.line1", Errors.required)
    verifyError(form, data, s"$field.postcode", Errors.required)
  }

  def verifyAddressFieldCharacterLimits(form: Form[_], validData: Map[String, String], field: String): Unit = {
    verifyCharacterLimit(form, validData, s"$field.line1", 36)
    verifyCharacterLimit(form, validData, s"$field.line2", 36)
    verifyCharacterLimit(form, validData, s"$field.line3", 36)
    verifyCharacterLimit(form, validData, s"$field.line4", 36)
  }
}
trait DateVerification { this: BasicVerification =>

  def verifyMandatoryDate(
        form: Form[_],
        validData: Map[String, String],
        dateField: String,
        exclusive: Boolean = true): Unit =
    Seq("day", "month", "year").foreach { x =>
      verifyMandatory(form, validData, s"$dateField.$x", exclusive)
    }

  def verifyOptionalDate(form: Form[_], validData: Map[String, String], dateField: String): Unit = {
    val d = validData - s"$dateField.day" - s"$dateField.month" - s"$dateField.year"
    shouldBind(form, d)
  }

  def verifyddmmyy(form: Form[_], validData: Map[String, String], dateField: String, maxYear: Int = 3000): Unit = {
    validateDay(form, validData, dateField)
    validateMonth(form, validData, dateField)
    validateYearUpto(form, validData, dateField, maxYear)
    verifyRealDatesOnly(form, validData, dateField)
  }

  def verifyDateIsBefore(
        form: Form[_],
        validData: Map[String, String],
        dateField: String,
        futureDate: LocalDate): Unit = {
    verifyddmmyy(form, validData, dateField, futureDate.getYear)
    val data = validData
      .updated(s"$dateField.day", futureDate.getDayOfMonth.toString)
      .updated(s"$dateField.month", futureDate.getMonthValue.toString)
      .updated(s"$dateField.year", futureDate.getYear.toString)
    verifyError(form, data, dateField, Errors.dateMustBeInPast)
  }

  private def validateDay[T](form: Form[T], validData: Map[String, String], field: String): Unit = {
    verifyRange(form, validData, s"$field.day", (1 to 31).map(_.toString), Seq("0"), Errors.belowMinimum)
    verifyRange(form, validData, s"$field.day", Seq.empty, Seq("32", "33", "55", "101", "58987"), Errors.aboveMaximum)
    verifyNonEmptyString(form, validData, s"$field.day", Errors.invalidNumber)
    verifyAcceptsLeadingAndTrailingWhitespace(form, validData, s"$field.day")
  }

  private def verifyRealDatesOnly[T](form: Form[T], validData: Map[String, String], field: String): Unit = {
    def withDate(dt: (String, String, String)) =
      validData.updated(field + ".day", dt._1).updated(field + ".month", dt._2).updated(field + ".year", dt._3)

    val invalid = Seq(("29", "2", "2015"), ("31", "9", "2015"))
    invalid foreach { iv =>
      verifyOnlyError(form, withDate(iv), field, Errors.invalidDate)
    }

    val valid = Seq(("28", "2", "2012"), ("31", "8", "2015"), ("30", "9", "2015"))
    valid foreach { v =>
      shouldBind(form, withDate(v))
    }
  }

  private def validateMonth(form: Form[_], validData: Map[String, String], field: String): Unit = {
    verifyRange(form, validData, s"$field.month", (1 to 12).map(_.toString), Seq("0", "-1"), Errors.belowMinimum)
    verifyRange(form, validData, s"$field.month", Seq.empty, Seq("13", "999"), Errors.aboveMaximum)
    verifyNonEmptyString(form, validData, s"$field.month", Errors.invalidNumber)
    verifyAcceptsLeadingAndTrailingWhitespace(form, validData, s"$field.month")
  }

  private def validateYearUpto[T](form: Form[T], validData: Map[String, String], field: String, maxYear: Int): Unit = {
    verifyRange(
      form,
      validData,
      s"$field.year",
      (1900 to maxYear - 1).map(_.toString),
      Seq("1899", "200", "1"),
      Errors.belowMinimum)
    verifyRange(form, validData, s"$field.year", Seq.empty, Seq("3001", "4000", "999999"), Errors.aboveMaximum)
    verifyNonEmptyString(form, validData, s"$field.year", Errors.invalidNumber)
    verifyAcceptsLeadingAndTrailingWhitespace(form, validData, s"$field.year")
  }
}

trait BasicVerification extends Matchers with AppendedClues with FormChecking {

  def verifyMandatory(form: Form[_], validData: Map[String, String], field: String, exclusive: Boolean = true): Unit = {
    val data = validData - field
    shouldContainRequiredError(form.bind(data), field, exclusive)
  }

  def verifyNonEmptyText(
        form: Form[_],
        validData: Map[String, String],
        field: String,
        exclusive: Boolean = true): Unit = {
    verifyMandatory(form, validData, field, exclusive)
    val data = validData.updated(field, " ")
    shouldContainRequiredError(form.bind(data), field, exclusive)
  }

  def verifyOptional(form: Form[_], validData: Map[String, String], field: String): Unit =
    shouldBind(form, validData - field)

  def verifyCharacterLimit(form: Form[_], validData: Map[String, String], field: String, limit: Int): Unit = {
    shouldBind(form, validData.updated(field, (1 to limit).map(_ => "a").mkString))

    val f = form.bind(validData.updated(field, (1 to limit + 1).map(_ => "b").mkString))
    shouldContainError(f, field, "error.maxLength", Some(Seq(limit)))
  }

  def verifyPhoneCharacterLimit(form: Form[_], validData: Map[String, String], field: String, limit: Int): Unit = {
    shouldBind(form, validData.updated(field, (1 to limit).map(_ => "0").mkString))

    val f = form.bind(validData.updated(field, (1 to limit + 1).map(_ => "1").mkString))
    shouldContainError(f, field, "error.maxLength", Some(Seq(limit)))
  }

  protected def verifyNonEmptyString[T](
        form: Form[T],
        validData: Map[String, String],
        field: String,
        error: String): Unit = {
    val data = validData.updated(field, "")
    shouldOnlyContainError(form.bind(data), field, error)
  }

  def verifyOnlyError(form: Form[_], invalidData: Map[String, String], field: String, error: String): Unit =
    shouldOnlyContainError(form.bind(invalidData), field, error)

  def verifyError(
        form: Form[_],
        invalidData: Map[String, String],
        field: String,
        error: String,
        args: Option[Seq[Any]] = None): Unit =
    shouldContainError(form.bind(invalidData), field, error, args)

  def verifyNoErrors(form: Form[_], validData: Map[String, String]): Unit = {
    val f = form.bind(validData)

    if (f.hasErrors) {
      fail(s"Form unexpectedly contained errors: ${diagnostics(f)}")
    }
  }

  protected def verifyAcceptsLeadingAndTrailingWhitespace[T](
        form: Form[T],
        validData: Map[String, String],
        field: String): Unit =
    shouldBind(form, validData.updated(field, s" ${validData(field)} "))

  protected def verifyRange(
        form: Form[_],
        validData: Map[String, String],
        field: String,
        valid: Seq[String],
        invalid: Seq[String],
        error: String): Unit = {
    valid.foreach(x => shouldBind(form, validData.updated(field, x)))
    invalid.foreach(x => shouldContainError(form.bind(validData.updated(field, x)), field, error))
  }
}

trait FormChecking extends Matchers with AppendedClues {

  def shouldBind[A](form: Form[A], data: Map[String, String]): Form[A] = {
    val f = form.bind(data)
    f.bind(data).errors.isEmpty shouldBe true withClue s"Form did not bind.${diagnostics(f)}"
    f.value.getOrElse(fail(s"Form has errors:${diagnostics(f)}"))
    f
  }

  protected def shouldContainRequiredError(form: Form[_], field: String, exclusive: Boolean = true): Unit =
    if (exclusive) {
      shouldOnlyContainError(form, field, Errors.required)
    } else {
      shouldContainError(form, field, Errors.required)
    }

  protected def shouldOnlyContainError[T](form: Form[T], field: String, error: String): Unit = {
    shouldContainError(form, field, error)
    if (form.errors.length > 1) {
      fail(s"Form contained too many errors. Expected only: $field - $error.${diagnostics(form)}")
    }
  }

  protected def shouldContainError(form: Form[_], field: String, error: String, args: Option[Seq[Any]] = None): Unit = {
    val exists = form.errors.exists(e => e.key == field && e.messages.head == error && args.fold(true)(_ == e.args))
    exists shouldEqual true withClue s"No matching error for $field - $error${diagnostics(form)}"
  }

  protected def diagnostics(f: Form[_]) = s"\nErrors: ${f.errors} \nData: ${f.data}"
}
