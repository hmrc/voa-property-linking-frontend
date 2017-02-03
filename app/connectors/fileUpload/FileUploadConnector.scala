/*
 * Copyright 2017 HM Revenue & Customs
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
import javax.inject.Inject

import akka.stream.scaladsl._
import com.google.inject.{ImplementedBy, Singleton}
import config.{Environment, Wiring}
import connectors.EnvelopeConnector
import play.api.Logger
import play.api.libs.json.{JsValue, Json}
import play.api.libs.ws.WSClient
import play.api.mvc.MultipartFormData.FilePart
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.http.{HttpException, HttpResponse, _}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}

case class NewEnvelope(envelopeId: String)

object NewEnvelope {
  implicit lazy val newEnvelope = Json.format[NewEnvelope]
}

case class RoutingRequest(envelopeId: String, application: String = "application/json", destination: String = "VOA_CCA")

object RoutingRequest {
  implicit lazy val routingRequest = Json.format[RoutingRequest]
}

@ImplementedBy(classOf[FileUploadConnector])
trait FileUpload {
  def createEnvelope()(implicit hc: HeaderCarrier): Future[String]

  def uploadFile(envelopeId: String, fileName: String, contentType: String, file: File)(implicit hc: HeaderCarrier)

  def closeEnvelope(envelopeId: String)(implicit hc: HeaderCarrier)
}

@Singleton
class FileUploadConnector @Inject()(val ws: WSClient, val envelopeConnector: EnvelopeConnector)(implicit ec: ExecutionContext)
  extends FileUpload with ServicesConfig with JsonHttpReads {
  lazy val http = Wiring().http

  def createEnvelope()(implicit hc: HeaderCarrier): Future[String] = {
    http.POST[JsValue, HttpResponse](s"${baseUrl("file-upload-backend")}/file-upload/envelopes", Json.obj()) map { r =>
      r.header("location")
        .flatMap(l => l.split("/").lastOption)
        .getOrElse(throw new Exception("No envelope id"))
    } flatMap { envId => envelopeConnector.storeEnvelope(envId) }
  }

  def uploadFile(envelopeId: String, fileName: String, contentType: String, file: File)(implicit hc: HeaderCarrier) = {
    val url = s"${baseUrl("file-upload-frontend")}/file-upload/upload/envelopes/$envelopeId/files/$fileName"
    ws.url(url)
      .withHeaders(("X-Requested-With", "VOA_CCA"))
      .post(Source(FilePart(fileName, fileName, Option(contentType), FileIO.fromFile(file)) :: List()))
  }

  def closeEnvelope(envelopeId: String)(implicit hc: HeaderCarrier) = {
    val url = s"${baseUrl("file-upload-backend")}/file-routing/requests"
    http.POST[RoutingRequest, HttpResponse](url, RoutingRequest(envelopeId))
      .map(_ => ())
      .recover {
        case ex: HttpException => Logger.error(s"${ex.responseCode}: ${ex.message}")
      }

  }

}
