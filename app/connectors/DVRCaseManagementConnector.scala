/*
 * Copyright 2019 HM Revenue & Customs
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

package connectors

import akka.stream.scaladsl.Source
import akka.util.ByteString
import javax.inject.Inject
import uk.gov.hmrc.play.config.ServicesConfig
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import config.WSHttp
import models.dvr.documents.DvrDocumentFiles
import models.dvr.{DetailedValuationRequest, StreamedDocument}
import play.api.http.HeaderNames.{CONTENT_LENGTH, CONTENT_TYPE}
import play.api.libs.ws.{StreamedResponse, WSClient}

import scala.concurrent.Future
import uk.gov.hmrc.http._

class DVRCaseManagementConnector @Inject()(
                                            config: ServicesConfig,
                                            val wsClient: WSClient,
                                            http: WSHttp) extends HttpErrorFunctions {
  val url = config.baseUrl("property-linking") + "/property-linking"

  def requestDetailedValuation(dvr: DetailedValuationRequest)(implicit hc: HeaderCarrier): Future[Unit] = {
    http.POST[DetailedValuationRequest, HttpResponse](url + "/request-detailed-valuation", dvr) map { _ => () }
  }

  def requestDetailedValuationV2(dvr: DetailedValuationRequest)(implicit hc: HeaderCarrier): Future[Unit] = {
    http.POST[DetailedValuationRequest, HttpResponse](url + "/detailed-valuation", dvr) map { _ => () }
  }

  def dvrExists(organisationId: Long, assessmentRef: Long)(implicit hc: HeaderCarrier): Future[Boolean] = {
    http.GET[Boolean](url + s"/dvr-exists?organisationId=$organisationId&assessmentRef=$assessmentRef")
  }

  def getDvrDocuments(uarn: Long, valuationId: Long, propertyLinkId: String)(implicit hc: HeaderCarrier): Future[Option[DvrDocumentFiles]] = {
    http.GET[DvrDocumentFiles](s"$url/properties/$uarn/valuation/$valuationId/files", Seq("propertyLinkId" -> propertyLinkId)).map(Some.apply).recover {
      case _: NotFoundException =>
        None
      case e =>
        throw e
    }
  }

  def getDvrDocument(uarn: Long, valuationId: Long, propertyLinkId: String, fileRef: String)(implicit hc: HeaderCarrier): Future[StreamedDocument] =
    wsClient
      .url(s"$url/properties/$uarn/valuation/$valuationId/files/$fileRef?propertyLinkId=$propertyLinkId")
      .withMethod("GET").withHeaders(hc.headers: _*)
      .stream().flatMap {
      case StreamedResponse(hs, body) =>

        val headers = hs.headers.mapValues(_.mkString(","))
        hs.status match {
          case s if is4xx(s) => Future.failed(Upstream4xxResponse(s"Upload failed with status ${hs.status}.", s, s))
          case s if is5xx(s) => Future.failed(Upstream5xxResponse(s"Upload failed with status ${hs.status}.", s, s))
          case _ => Future.successful(
            StreamedDocument(headers.get(CONTENT_TYPE), headers.get(CONTENT_LENGTH).map(_.toLong), headers.filter(_._1 != CONTENT_TYPE), body)
          )
        }
    }
}
