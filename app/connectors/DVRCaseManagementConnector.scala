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

package connectors

import javax.inject.Inject
import models.dvr.DetailedValuationRequest
import models.dvr.documents.DvrDocumentFiles
import play.api.libs.ws.{WSClient, WSResponse}
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http._
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import scala.concurrent.{ExecutionContext, Future}

class DVRCaseManagementConnector @Inject()(config: ServicesConfig, val wsClient: WSClient, http: HttpClient)(
      implicit val executionContext: ExecutionContext) {

  val url = config.baseUrl("property-linking") + "/property-linking"

  def requestDetailedValuation(dvr: DetailedValuationRequest)(implicit hc: HeaderCarrier): Future[Unit] =
    http.POST[DetailedValuationRequest, HttpResponse](url + "/request-detailed-valuation", dvr) map { _ =>
      ()
    }

  def requestDetailedValuationV2(dvr: DetailedValuationRequest)(implicit hc: HeaderCarrier): Future[Unit] =
    http.POST[DetailedValuationRequest, HttpResponse](url + "/detailed-valuation", dvr) map { _ =>
      ()
    }

  def dvrExists(organisationId: Long, assessmentRef: Long)(implicit hc: HeaderCarrier): Future[Boolean] =
    http.GET[Boolean](url + s"/dvr-exists?organisationId=$organisationId&assessmentRef=$assessmentRef")

  def getDvrDocuments(uarn: Long, valuationId: Long, propertyLinkId: String)(
        implicit hc: HeaderCarrier): Future[Option[DvrDocumentFiles]] =
    http.GET[Option[DvrDocumentFiles]](
      s"$url/properties/$uarn/valuation/$valuationId/files",
      Seq("propertyLinkId" -> propertyLinkId)
    )

  def getDvrDocument(uarn: Long, valuationId: Long, propertyLinkId: String, fileRef: String)(
        implicit hc: HeaderCarrier): Future[WSResponse] =
    wsClient
      .url(s"$url/properties/$uarn/valuation/$valuationId/files/$fileRef?propertyLinkId=$propertyLinkId")
      .withMethod("GET")
      .withHttpHeaders(hc.headers(HeaderNames.explicitlyIncludedHeaders): _*)
      .stream()

}
