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

package connectors.fileUpload

import java.io.File
import java.nio.charset.Charset
import javassist.NotFoundException

import com.ning.http.client.Response
import config.Environment
import play.api.libs.json.Json
import serialization.JsonFormats
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.http._
import play.api.Play.current
import uk.gov.hmrc.play.http.HttpException

import scala.concurrent.{ExecutionContext, Future}
import play.api.Logger
import play.api.mvc.MultipartFormData.FilePart
import play.api.libs.ws._

case class NewEnvelope(envelopeId: String)
object NewEnvelope{
  implicit lazy val newEnvelope = Json.format[NewEnvelope]
}
case class RoutingRequest(envelopeId: String, application:String = "application/json", destination: String = "VOA_CCA")
object RoutingRequest {
  implicit lazy val routingRequest = Json.format[RoutingRequest]
}

class FileUploadConnector(http: HttpGet with HttpPut with HttpPost)(implicit ec: ExecutionContext)
    extends ServicesConfig with JsonHttpReads {

    implicit val rds: HttpReads[Unit] = new HttpReads[Unit] {
      override def read(method: String, url: String, response: HttpResponse): Unit = Unit
    }
    def createEnvelope()(implicit hc: HeaderCarrier): Future[String] = {
      val url = if (Environment.isDev)
        s"${System.getProperty("file-upload-backend")}/create-envelope"
      else
        s"${baseUrl("file-upload-backend")}/create-envelope"
      val res = http.POSTEmpty[NewEnvelope](s"$url")

      res map (envelope => {
        Logger.debug(s"env id: ${envelope.envelopeId}")
        envelope.envelopeId
      })
    }

    def uploadFile(envelopeId: String, fileName: String, content: Array[Byte], contentType: String, file: File)(implicit hc: HeaderCarrier) = {
      FusConnector.addToEnvelope(envelopeId, fileName, content, contentType, fileName)
    }

    def closeEnvelope(envelopeId: String)(implicit hc: HeaderCarrier) = {
      val url = if (Environment.isDev)
        s"${System.getProperty("file-upload-backend")}/routing/requests"
      else
        s"${baseUrl("file-upload-backend")}/routing/requests"
      http.POST[RoutingRequest, Unit](url, RoutingRequest(envelopeId)).map ( _=>
        ()
      ).recover{
        case ex: HttpException => Logger.debug(s"${ex.responseCode}: ${ex.message}")
      }

    }

}


