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

import javax.inject.Inject
import models.searchApi.OwnerAgents
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.http.HttpClient
import uk.gov.hmrc.play.config.ServicesConfig

import scala.concurrent.{ExecutionContext, Future}

class AgentsConnector @Inject()(http: HttpClient, conf: ServicesConfig)(implicit ec: ExecutionContext) {

  lazy val baseUrl: String = conf.baseUrl("property-linking") + "/property-linking"

  def ownerAgents(organisationId: Long)(implicit hc: HeaderCarrier): Future[OwnerAgents] =
    http.GET[OwnerAgents](s"$baseUrl/manage-agents/$organisationId/agents")
}
