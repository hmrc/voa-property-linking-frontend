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

import java.io.File

import com.ning.http.client.Response
import com.ning.http.client.Response.ResponseBuilder
import connectors.fileUpload.FileUploadConnector

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.ExecutionContext.Implicits.global
import uk.gov.hmrc.play.http.HeaderCarrier

object StubFileUploadConnector extends FileUploadConnector(StubHttp) {

  override def uploadFile(envelopeId: String, fileName: String, content: Array[Byte],
                          contentType: String, file: File)(implicit hc: HeaderCarrier): Future[Response] = {
    val resp = (new ResponseBuilder()).build()
    Future.successful(resp)
  }

  override def closeEnvelope(envelopeId: String)(implicit hc: HeaderCarrier): Future[Unit] = {
    Future.successful( () )
  }

}
