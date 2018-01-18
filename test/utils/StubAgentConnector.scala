/*
 * Copyright 2018 HM Revenue & Customs
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

package utils

import connectors.AgentsConnector
import models.searchApi.OwnerAgents
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future

object StubAgentConnector extends AgentsConnector(StubHttp, StubServicesConfig) {

  override def ownerAgents(organisationId: Long)(implicit hc: HeaderCarrier): Future[OwnerAgents] =
    Future.successful {
      OwnerAgents(Seq())
    }
}
