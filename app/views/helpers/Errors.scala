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

package views.helpers

object Errors {
  // Common Play
  val required = "error.required"
  val invalidNumber = "error.number"
  val belowMinimum = "error.min"
  val aboveMaximum = "error.max"

  // Common Custom
  val noValueSelected = "error.common.noValueSelected"
  val invalidDay = "error.common.date.invalidDay"
  val invalidMonth = "error.common.date.invalidMonth"
  val invalidYear = "error.common.date.invalidYear"
  val invalidDate = "error.invalidDate"
  val dateMustBeInPast = "error.common.date.mustBeInPast"
  val dateMustBeAfter1stApril2017 = "error.date.mustBeAfter2017"
  val dateMustBeAfter = "error.date.mustBeAfter"
  val dateMustBeAfterOtherDate = "error.date.mustBeAfterOther"
  val invalidPostcode = "error.postcode"

  // Capacity Declaration
  val capacityRequired = "error.capacityDeclaration.capacityRequired"
  val invalidCapacity = "error.capacityDeclaration.invalidCapacity"

  // Self Certification
  val mustAgreeToSelfCert = "error.mustAgreeToSelfCert"

  // Evidence Upload
  val uploadedFiles = "error.evidenceUploadFiles"
  val missingFiles = "error.missingFiles"

  //validation errors
  val invalidEmail = "error.invalidEmail"
  val emailsMustMatch = "error.emailsMustMatch"
}
