/*
 * Copyright 2020 HM Revenue & Customs
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

import java.time.{LocalDate, LocalDateTime, LocalTime}
import java.time.format.DateTimeFormatter

import models.{Address, PropertyAddress}

object Formatters {

  def capitalizedAddress(address: PropertyAddress): String = {
    address.copy(
      lines = address.lines.map(_.toLowerCase().trim.split(" ").map(_.capitalize).mkString(" ")).filter(_.nonEmpty),
      postcode = address.postcode.toUpperCase()
    ).toString
  }

  def capitalizedAddress(address: String): String = {
    capitalizedAddress(PropertyAddress.fromString(address))
  }

  def capitalizedAddress(address: Address): String = {
    Seq(address.line1, address.line2, address.line3, address.line4, address.postcode).filterNot(_.isEmpty).map(_.capitalize) mkString ", "
  }

  def formatDate(date: LocalDate): String = {
    date.format(DateTimeFormatter.ofPattern("d MMMM yyyy"))
  }

  def formatDateTimeToDate(date: LocalDateTime): String = {
    date.format(DateTimeFormatter.ofPattern("d MMMM yyyy"))
  }

  def formatTime(time: LocalTime): String = {
    time.format(DateTimeFormatter.ofPattern("hh:mm a"))
  }
  def buildQueryParams(name : String, value : Option[String]) : String = {
    value match { case Some(paramValue) if paramValue != "" => s"&$name=${paramValue.trim}" ; case _ => ""}
  }

  def buildUppercaseQueryParams(name : String, value : Option[String]) : String = {
    value match { case Some(paramValue) if paramValue != "" => s"&$name=${paramValue.toUpperCase.trim}" ; case _ => ""}
  }

}
