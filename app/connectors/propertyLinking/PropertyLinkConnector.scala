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

package connectors.propertyLinking

import models._
import org.joda.time.DateTime
import session.LinkingSessionRequest
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.http._

import scala.concurrent.{ExecutionContext, Future}

class PropertyLinkConnector(http: HttpGet with HttpPut with HttpPost)(implicit ec: ExecutionContext)
  extends ServicesConfig {
  lazy val baseUrl: String = baseUrl("property-linking") + s"/property-linking"

  def get(organisationId: Int, authorisationId: Long)(implicit hc: HeaderCarrier): Future[Option[PropertyLink]] = {
    val url = s"$baseUrl/property-links/$authorisationId"

    http.GET[Option[PropertyLink]](url) map { link =>
      link.find(_.organisationId == organisationId)
    } recover {
      case _: NotFoundException => None
    }
  }

  def linkToProperty(linkBasis: LinkBasis)(implicit request: LinkingSessionRequest[_]): Future[Unit] = {
    implicit val hc = HeaderCarrier.fromHeadersAndSession(request.request.headers, Some(request.request.session))
    val url = baseUrl + s"/property-links"
    val linkRequest = PropertyLinkRequest(
      request.ses.uarn,
      request.organisationId,
      request.ses.personId,
      Capacity.fromDeclaration(request.ses.declaration),
      DateTime.now,
      linkBasis,
      request.ses.fileInfo.toSeq,
      request.ses.submissionId
    )
    http.POST[PropertyLinkRequest, HttpResponse](url, linkRequest) map { _ => () }
  }

  def linkedProperties(organisationId: Int, startPoint: Int, pageSize: Int, requestTotalRowCount: Boolean)
                      (implicit hc: HeaderCarrier): Future[PropertyLinkResponse] = {
    val url = baseUrl + s"/property-links/" +
      s"?organisationId=$organisationId" +
      s"&startPoint=$startPoint" +
      s"&pageSize=$pageSize" +
      s"&requestTotalRowCount=$requestTotalRowCount"

    http.GET[PropertyLinkResponse](url)
  }

  def clientProperties(clientOrgId: Long, agentOrgId: Int, startPoint: Int, pageSize: Int, requestTotalRowCount: Boolean)
                      (implicit hc: HeaderCarrier): Future[ClientPropertyResponse] = {
    val url = baseUrl + s"/property-links/client-properties" +
      s"?clientOrganisationId=$clientOrgId" +
      s"&agentOrganisationId=$agentOrgId" +
      s"&startPoint=$startPoint" +
      s"&pageSize=$pageSize" +
      s"&requestTotalRowCount=$requestTotalRowCount"

    http.GET[ClientPropertyResponse](url)
  }

  def clientProperty(authorisationId: Long, clientOrgId: Long, agentOrgId: Long)(implicit hc: HeaderCarrier): Future[Option[ClientProperty]] = {
    val url = baseUrl + s"/property-links/client-property/$authorisationId" +
      s"?clientOrganisationId=$clientOrgId" +
      s"&agentOrganisationId=$agentOrgId"

    http.GET[Option[ClientProperty]](url) recover { case _: NotFoundException => None }
  }

  def assessments(authorisationId: Long)(implicit hc: HeaderCarrier): Future[Seq[Assessment]] = {
    val url = baseUrl + s"/dashboard/assessments/$authorisationId"
    http.GET[Seq[Assessment]](url)
  }
}
