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

import connectors.errorhandler.exceptions.ExceptionThrowingReadsInstances

import javax.inject.Inject
import models.propertyrepresentation.AgentDetails
import play.api.Logging
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpResponse, UpstreamErrorResponse}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import scala.concurrent.{ExecutionContext, Future}

class PropertyRepresentationConnector @Inject()(serverConfig: ServicesConfig, http: HttpClient)(
      implicit ec: ExecutionContext)
    extends Logging {
  lazy val baseUrl: String = s"${serverConfig.baseUrl("property-linking")}/property-linking"
  def revokeClientProperty(submissionId: String)(implicit hc: HeaderCarrier): Future[Unit] = {
    import ExceptionThrowingReadsInstances._
    http.DELETE[HttpResponse](s"$baseUrl/property-representations/revoke-client-property/$submissionId") map { _ =>
      ()
    }
  }
  def getAgentDetails(agentCode: Long)(implicit hc: HeaderCarrier): Future[Option[AgentDetails]] =
    http.GET[Option[AgentDetails]](s"$baseUrl/my-organisation/agent/$agentCode") recover {
      case err @ UpstreamErrorResponse.WithStatusCode(403) =>
        logger.info(s"Agent code: $agentCode does not belong to an agent organisation")
        None
    }
}
