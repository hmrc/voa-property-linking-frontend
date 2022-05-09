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

import models.PropertyAddress
import tests.BaseUnitSpec

import java.time.{LocalDateTime, LocalTime}

class FormattersSpec extends BaseUnitSpec {

  "capitalisedAddress" should {
    "capitalise the first letter of each word in a string, with the postcode in caps" in {
      val address = "THE OLD WAREHOUSE, CHALFONT STATION ROAD, LITTLE CHALFONT, AMERSHAM, BUCKS, HP7 9PS"
      Formatters.capitalisedAddress(address) shouldBe "The Old Warehouse, Chalfont Station Road, Little Chalfont, Amersham, Bucks,  HP7 9PS"
    }
  }

  "capitalizedAddress" should {
    "capitalise the first letter of each word in a string, with the postcode in caps" in {
      val address = "THE OLD WAREHOUSE, CHALFONT STATION ROAD, LITTLE CHALFONT, AMERSHAM, BUCKS, HP7 9PS"
      Formatters.capitalizedAddress(address) shouldBe "The Old Warehouse, Chalfont Station Road, Little Chalfont, Amersham, Bucks,  HP7 9PS"
    }

    "capitalise the first letter of each word in a PropertyAddress, with the postcode in caps" in {
      val propertyAddress = PropertyAddress(
        Seq("THE OLD WAREHOUSE, CHALFONT STATION ROAD", "LITTLE CHALFONT", "AMERSHAM", "BUCKS"),
        "HP7 9PS")
      Formatters.capitalizedAddress(propertyAddress) shouldBe "The Old Warehouse, Chalfont Station Road, Little Chalfont, Amersham, Bucks, HP7 9PS"
    }
  }

  "formatDate" should {
    "format LocalDates correctly" in {
      Formatters.formatDate(april2017) shouldBe "1 April 2017"
    }
  }

  "formatDateTimeToDate" should {
    "format LocalDateTimes correctly" in {
      val testDateTime = LocalDateTime.of(april2017, LocalTime.of(12, 30))
      Formatters.formatDateTimeToDate(testDateTime) shouldBe "1 April 2017"
    }
  }

  "formatTime" should {
    "format LocalTimes correctly" in {
      Formatters.formatTime(LocalTime.of(12, 30)) shouldBe "12:30 PM"
    }
  }

  "buildQueryParams" should {
    "format a query parameter" in {
      val actual = Formatters.buildQueryParams("email", Some(email))
      val expected = "&email=some@email.com"
      actual shouldBe expected
    }
  }

  "buildUppercaseQueryParams" should {
    "format an uppercase query parameter" in {
      val actual = Formatters.buildUppercaseQueryParams("email", Some(email))
      val expected = "&email=SOME@EMAIL.COM"
      actual shouldBe expected
    }
  }

  "formatCurrency" should {
    "format doubles correctly" in {
      Formatters.formatCurrency(12345.678) shouldBe "£12,345.67"
      Formatters.formatCurrency(12345.675) shouldBe "£12,345.67"
      Formatters.formatCurrency(12345.671) shouldBe "£12,345.67"
    }

    "format floats correctly" in {
      Formatters.formatCurrency(12345.678f) shouldBe "£12,345.67"
      Formatters.formatCurrency(12345.675f) shouldBe "£12,345.67"
      Formatters.formatCurrency(12345.671f) shouldBe "£12,345.67"
    }

    "format ints correctly" in {
      Formatters.formatCurrency(1234567) shouldBe "£1,234,567.00"
    }

    "format longs correctly" in {
      Formatters.formatCurrency(1234567L) shouldBe "£1,234,567.00"
    }
  }

  "formatCurrencyRoundedToPounds" should {
    "format doubles correctly" in {
      Formatters.formatCurrencyRoundedToPounds(12345.678) shouldBe "£12,345"
      Formatters.formatCurrencyRoundedToPounds(12345.500) shouldBe "£12,345"
      Formatters.formatCurrencyRoundedToPounds(12345.123) shouldBe "£12,345"
    }

    "format floats correctly" in {
      Formatters.formatCurrencyRoundedToPounds(12345.678f) shouldBe "£12,345"
      Formatters.formatCurrencyRoundedToPounds(12345.500f) shouldBe "£12,345"
      Formatters.formatCurrencyRoundedToPounds(12345.123f) shouldBe "£12,345"
    }

    "format ints correctly" in {
      Formatters.formatCurrencyRoundedToPounds(1234567) shouldBe "£1,234,567"
    }

    "format longs correctly" in {
      Formatters.formatCurrencyRoundedToPounds(1234567L) shouldBe "£1,234,567"
    }
  }
}
