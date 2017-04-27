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

import controllers.Pagination
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
    val url = s"$baseUrl/property-links"
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

  def linkedProperties(organisationId: Int, pagination: Pagination)
                      (implicit hc: HeaderCarrier): Future[PropertyLinkResponse] = {
    http.GET[PropertyLinkResponse](s"$baseUrl/property-links/?organisationId=$organisationId&$pagination")
  }

  def clientProperties(clientOrgId: Long, agentOrgId: Int, pagination: Pagination)
                      (implicit hc: HeaderCarrier): Future[ClientPropertyResponse] = {
    http.GET[ClientPropertyResponse](s"""$baseUrl/property-links/client-properties?
                                        |clientOrganisationId=$clientOrgId&
                                        |agentOrganisationId=$agentOrgId&
                                        |$pagination""".stripMargin)
  }

  def clientProperty(authorisationId: Long, clientOrgId: Long, agentOrgId: Long)(implicit hc: HeaderCarrier): Future[Option[ClientProperty]] = {
    val url =
      s"""$baseUrl/property-links/client-property/$authorisationId?
         |clientOrganisationId=$clientOrgId&
         |agentOrganisationId=$agentOrgId""".stripMargin

    http.GET[Option[ClientProperty]](url) recover { case _: NotFoundException => None }
  }

  def assessments(authorisationId: Long)(implicit hc: HeaderCarrier): Future[Seq[Assessment]] = {
    http.GET[Seq[Assessment]](s"$baseUrl/dashboard/assessments/$authorisationId")
  }
}
