package connectors

import javax.inject.{Inject, Singleton}

import models.attachment.InitiateAttachmentRequest
import models.attachment.SubmissionTypesValues.ChallengeCaseEvidence
import models.upscan.PreparedUpload
import play.api.Logger
import play.api.libs.json.{JsObject, JsString, Json}
import uk.gov.hmrc.http.{HeaderCarrier, HttpErrorFunctions}
import models.attachment._
import uk.gov.hmrc.play.bootstrap.http.HttpClient
import uk.gov.hmrc.http._
import uk.gov.hmrc.play.config.ServicesConfig

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.ExecutionContext.Implicits.global

@Singleton
class BusinessRatesAttachmentConnector @Inject()(val http: HttpClient)(implicit val appConfig: ServicesConfig, executionContext: ExecutionContext)
  extends JsonHttpReads with OptionHttpReads with RawReads with AttachmentHttpErrorFunctions {

  val baseURL: String = appConfig.baseUrl("business-rates-attachments")
  def initiateAttachmentUpload(uploadSettings: InitiateAttachmentRequest)(
        implicit headerCarrier: HeaderCarrier): Future[PreparedUpload] = {
    http.POST[InitiateAttachmentRequest, PreparedUpload](s"$baseURL/business-rates-attachments/initiate", uploadSettings)

  }

  def submitFile(fileReference: String, submissionId: String)(
    implicit headerCarrier: HeaderCarrier): Future[Option[Attachment]] = {
    http.PATCH[JsObject, Attachment](s"$baseURL/business-rates-attachments/attachments/${fileReference}",
      Json.obj(ChallengeCaseEvidence.submissionId.toString -> JsString(submissionId))).map(Some.apply).recover {
      case ex: Exception =>
        Logger.warn(s"File Submission failed for File Reference: ${fileReference} Response body: ${ex.printStackTrace()}")
        None
    }
  }

}


case class FileAttachmentFailed(errorMessage: String) extends Exception

import play.api.http.Status.BAD_REQUEST
import uk.gov.hmrc.http._

trait AttachmentHttpErrorFunctions extends HttpErrorFunctions {

  override def handleResponse(httpMethod: String, url: String)(response: HttpResponse): HttpResponse =
    response.status match {
      case BAD_REQUEST =>
        Logger.warn(s"Upload failed with status ${response.status}. Response body: ${response.body}")
        throw FileAttachmentFailed(response.body)
      case _           =>
        super.handleResponse(httpMethod, url)(response)
    }
}
