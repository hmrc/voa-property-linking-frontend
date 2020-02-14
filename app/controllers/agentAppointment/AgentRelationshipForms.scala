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

package controllers.agentAppointment

import form.{EnumMapping, Mappings}
import models.propertyrepresentation.{ManageMultiplePropertiesOptions, ManageOnePropertyOptions}
import play.api.data.{Form, Forms}
import play.api.data.Forms.{boolean, longNumber, optional, single}

object AgentRelationshipForms {
  val agentCode: Form[Long] =
    Form(single("agentCode" -> {
      longNumber.verifying("error.agentCode.invalid", n => n > 0)
    }))

  val isThisYourAgent: Form[Boolean] =
    Form(single("isThisYourAgent" -> optional(boolean).verifying("error.isThisYourAgent.required", _.isDefined).transform(_.get, Some.apply(_: Boolean))))

  val manageOnePropertyNoAgent: Form[Boolean] = Form(Forms.single("onePropertyNoAgent" -> Mappings.mandatoryBoolean))
  val manageOnePropertyExistingAgent: Form[ManageOnePropertyOptions] = Form(Forms.single("onePropertyWithAgent" -> EnumMapping(ManageOnePropertyOptions)))
  val manageMultipleProperties: Form[ManageMultiplePropertiesOptions] = Form(Forms.single("multipleProperties" -> EnumMapping(ManageMultiplePropertiesOptions)))

}
