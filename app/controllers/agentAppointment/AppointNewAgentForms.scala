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

package controllers.agentAppointment

import form.EnumMapping
import models.propertyrepresentation.AddAgentOptions
import play.api.data.Forms.{boolean, optional, single, text}
import play.api.data.{Form, Forms}

object AppointNewAgentForms {
  val agentCode: Form[String] =
    Form(single("agentCode" -> {
      text.verifying("error.agentCode.required", s => s.matches("^[0-9]+$"))
    }))

  val isThisTheCorrectAgent: Form[Boolean] =
    Form(
      single(
        "isThisYourAgent" -> optional(boolean)
          .verifying("error.isThisYourAgent.required", _.isDefined)
          .transform(_.get, Some.apply(_: Boolean))))

  val manageOneProperty: Form[AddAgentOptions] = Form(
    Forms.single("oneProperty" -> EnumMapping(AddAgentOptions, "error.oneProperty.required")))
  val manageMultipleProperties: Form[AddAgentOptions] = Form(
    Forms.single("multipleProperties" -> EnumMapping(AddAgentOptions, "error.multipleProperties.required")))
}
