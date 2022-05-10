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

import models.{Address, PropertyAddress}

import java.text.NumberFormat.getCurrencyInstance
import java.time.{LocalDate, LocalDateTime, LocalTime}
import java.time.format.DateTimeFormatter

object Formatters {

  // Copied from Dashboard frontend for consistency
  // https://github.com/hmrc/business-rates-dashboard-frontend/blob/a1b1807f1a5595915d0a9e3ea111469acbc1bd24/app/uk/gov/voa/businessrates/dashboard/models/propertyLinks/owner/OwnerAuthorisation.scala#L21
  def capitalisedAddress(s: String) =
    s"${s.toLowerCase.trim.split(" ").dropRight(2).map(_.capitalize).mkString(" ")} ${s.toLowerCase.trim.split(",").last.toUpperCase()}"

  def capitalizedAddress(address: PropertyAddress): String =
    address
      .copy(
        lines = address.lines.map(_.toLowerCase().trim.split(" ").map(_.capitalize).mkString(" ")).filter(_.nonEmpty),
        postcode = address.postcode.toUpperCase()
      )
      .toString

  def capitalizedAddress(address: String): String =
    capitalizedAddress(PropertyAddress.fromString(address))

  def capitalizedAddress(address: Address): String =
    Seq(address.line1, address.line2, address.line3, address.line4, address.postcode)
      .filterNot(_.isEmpty)
      .map(_.capitalize) mkString ", "

  def formatDate(date: LocalDate): String =
    date.format(DateTimeFormatter.ofPattern("d MMMM yyyy"))

  def formatDateTimeToDate(date: LocalDateTime): String =
    date.format(DateTimeFormatter.ofPattern("d MMMM yyyy"))

  def formatDateHint(date: LocalDate): String =
    date.format(DateTimeFormatter.ofPattern("d M yyyy"))

  def formatTime(time: LocalTime): String =
    time.format(DateTimeFormatter.ofPattern("hh:mm a"))

  def buildQueryParams(name: String, value: Option[String]): String =
    value match { case Some(paramValue) if paramValue != "" => s"&$name=${paramValue.trim}"; case _ => "" }

  def buildUppercaseQueryParams(name: String, value: Option[String]): String =
    value match { case Some(paramValue) if paramValue != "" => s"&$name=${paramValue.toUpperCase.trim}"; case _ => "" }

  def formatCurrency[A: Numeric](total: A, maximumFractionDigits: Option[Int] = None): String = {
    val currencyFormatter = getCurrencyInstance(java.util.Locale.UK)
    currencyFormatter.setRoundingMode(java.math.RoundingMode.DOWN)
    maximumFractionDigits.foreach(digits => currencyFormatter.setMaximumFractionDigits(digits))

    currencyFormatter.format(total)
  }

  def formatCurrencyRoundedToPounds[A: Numeric](total: A): String =
    formatCurrency(total, Some(0))
}
