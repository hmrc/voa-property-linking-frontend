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

import connectors._
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
    linkedProperties(organisationId).map( links => links.find(_.authorisationId == authorisationId) )
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

  def linkedProperties(organisationId: Int)(implicit hc: HeaderCarrier): Future[Seq[PropertyLink]] = {
    val url = baseUrl + s"/property-links/$organisationId"
    http.GET[Seq[PropertyLink]](url)
  }

  def clientProperties(userOrgId: Long, agentOrgId: Int)(implicit hc: HeaderCarrier): Future[Seq[ClientProperty]] = {
    val url = baseUrl + s"/property-links/client-properties/$userOrgId/$agentOrgId"
    http.GET[Seq[ClientProperty]](url)
  }

  def assessments(authorisationId: Long)(implicit hc: HeaderCarrier): Future[Seq[Assessment]] = {
    val url = baseUrl + s"/dashboard/assessments/$authorisationId"
    http.GET[Seq[Assessment]](url)
  }
}
