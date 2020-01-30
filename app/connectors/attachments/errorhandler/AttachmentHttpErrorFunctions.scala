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

package connectors.attachments.errorhandler

import connectors.attachments.errorhandler.exceptions.FileAttachmentFailed
import play.api.Logger
import play.api.http.Status.BAD_REQUEST
import uk.gov.hmrc.http._

trait AttachmentHttpErrorFunctions extends HttpErrorFunctions {

  override def handleResponse(httpMethod: String, url: String)(response: HttpResponse): HttpResponse =
    response.status match {
      case BAD_REQUEST =>
        Logger.warn(s"Upload failed with status ${response.status}. Response body: ${response.body}")
        throw FileAttachmentFailed(response.body)
      case _ =>
        super.handleResponse(httpMethod, url)(response)
    }
}
