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

import java.net.ConnectException
import javax.xml.ws.WebServiceException

import com.ning.http.client.{AsyncHttpClient, ByteArrayPart, ListenableFuture, Response}
import play.api.Logger
import play.api.libs.json.{JsValue, Json}
import play.api.libs.ws.{WS, WSResponse}
import uk.gov.hmrc.play.http.{BadGatewayException, GatewayTimeoutException, HttpResponse}
import com.ning.http.client.RequestBuilder
import com.ning.http.multipart.StringPart

import scala.concurrent.{ExecutionException, Future, TimeoutException}
import play.api.Play.current

import scala.concurrent.ExecutionContext.Implicits.global
import java.util.concurrent.ExecutorService

import uk.gov.hmrc.play.config.ServicesConfig
import config.Environment

import scala.concurrent.Promise
import scala.util.Try

trait FusConnector extends ServicesConfig{

  def uploadFileToFusUrl(envelopeId:String,fileId:String):String =
    if (Environment.isDev)
       s"${System.getProperty("file-upload-frontend")}/upload/envelopes/$envelopeId/files/$fileId"
    else
      s"${baseUrl("file-upload-frontend")}/upload/envelopes/$envelopeId/files/$fileId"


  def addToEnvelope(envelopId:String,fileId:String,data:Array[Byte],contentType:String,fileName:String): Future[Response] ={
    val request = buildRequest( uploadFileToFusUrl(envelopId,fileId),
      Map.empty,Map(("CSRF-token", "nocheck")),fileName,fileName,data,contentType)
    val asyncHttpClient = WS.client.underlying[AsyncHttpClient]
    implicit val executor = asyncHttpClient.getConfig.executorService()
    val listenableFutureResult: ListenableFuture[Response] = asyncHttpClient.executeRequest(request)
    val futureResult = listenableFutureToFuture(listenableFutureResult)
    futureResult.map { response => is2xx(response.getStatusCode) match {
        case true => response
        case false => {
          Logger.debug(s"error uploading to fus, status '${response.getStatusCode}, for $fileName to envelop $envelopId with ${response.getResponseBody}")
          throw new Exception(s"error uploading to fus, status '${response.getStatusCode}, for $fileName to envelop $envelopId")}
      }}.recover {
      case e: ExecutionException => e.getCause match {
        case e: TimeoutException => {
          Logger.warn(s"Error while adding $fileName to $envelopId. ${e.getLocalizedMessage}. TimeoutException ${e.getLocalizedMessage}")
          throw new GatewayTimeoutException(s"POST of '${request.getUrl}' timed out with message '${e.getMessage}'")}
        case e: ConnectException => {
          Logger.warn(s"Error while adding $fileName to $envelopId. ${e.getLocalizedMessage} Connection Exception ${e.getMessage}")
          throw new BadGatewayException(s"POST of '${request.getUrl}' failed. Caused by: '${e.getMessage}'")}
        case e: Exception =>{
          Logger.warn(s"Error while adding $fileName to $envelopId. ${e.getLocalizedMessage} ${e.getMessage}")
          throw new BadGatewayException(s"POST of '${request.getUrl}' failed. Caused by: '${e.getMessage}'")}
      }
      case e:Throwable =>{
        Logger.warn(s"Exception while adding $fileName to $envelopId. ${e.getLocalizedMessage}")
        throw new Exception("Something went wrong.")
      }
    }
  }


  def listenableFutureToFuture(g: ListenableFuture[Response])(implicit executor: ExecutorService): Future[Response] = {
    val pr: Promise[Response] = Promise[Response]()
    g.addListener(new Runnable {
      def run() = pr.complete( Try(g.get()) )
    }, executor)
    pr.future
  }

  def buildRequest(url: String, fileMetadata: Map[String, String],
                   headers: Map[String, String],name:String, fileName:String, fileContent: Array[Byte],mimeType:String) = {
    val rb = new RequestBuilder("POST").setUrl(url)
     fileMetadata.foreach {
      case (key, value) =>
        rb.addBodyPart(new StringPart(key, value, "UTF-8"))
    }
    rb.addBodyPart((new ByteArrayPart(name,fileName, fileContent,mimeType,"UTF-8")))
    headers.foreach {
      case (key, value) =>
        if (addHeader(key)) {
          rb.addHeader(key, value)
        }
    }
    rb.build()
  }
    def is2xx(status: Int) = status >= 200 && status <= 299
    def addHeader(headerName: String): Boolean = !"Content-Type".equals(headerName) && !"Content-Length".equals(headerName)
  }

object FusConnector extends FusConnector

case class FusConnectionException(message:String) extends WebServiceException(message)
