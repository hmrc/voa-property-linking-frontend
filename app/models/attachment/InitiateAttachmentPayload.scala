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

package models.attachment

import ai.x.play.json.Jsonx
import models.attachment.Destinations.Destinations
import models.attachment.request.InitiateAttachmentRequest
import play.api.libs.json.OFormat

case class InitiateAttachmentPayload(
      fileName: String,
      mimeType: String,
      successRedirect: String, //Labelling as optional's as these should not be on the screen
      errorRedirect: String, //Labelling as optional's as these should not be on the screen
      destination: Destinations, //Labelling as optional's as these should not be on the screen
      data: Map[String, String] = Map()) {}

object InitiateAttachmentPayload {

  implicit val format: OFormat[InitiateAttachmentPayload] = Jsonx.formatCaseClassUseDefaults

  def apply(
        request: InitiateAttachmentRequest,
        successRedirectUrl: String,
        errorRedirectUrl: String
  ): InitiateAttachmentPayload =
    InitiateAttachmentPayload(
      request.fileName,
      request.mimeType,
      successRedirectUrl,
      errorRedirectUrl,
      Destinations.PROPERTY_LINK_EVIDENCE_DFE
    )
}
