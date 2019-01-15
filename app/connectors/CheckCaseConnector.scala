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

import actions.BasicAuthenticatedRequest
import config.{ApplicationConfig, WSHttp}
import models._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import uk.gov.hmrc.play.config.inject.ServicesConfig
import uk.gov.hmrc.http.HeaderCarrier
import play.api.Logger

import scala.concurrent.Future

class CheckCaseConnector @Inject()(config: ServicesConfig, http: WSHttp){
  lazy val baseUrl: String = s"${config.baseUrl("property-linking")}/property-linking"


  def getCheckCases(propertyLink: Option[PropertyLink], isAgentOwnProperty: Boolean)(implicit request: BasicAuthenticatedRequest[_], hc: HeaderCarrier): Future[Option[CheckCasesResponse]] = {

    propertyLink match {
      case Some(link) => {
          val interestedParty =  request.organisationAccount.isAgent && !isAgentOwnProperty match {
            case true => "agent"
            case false => "client"
          }
          http.GET[CheckCasesResponse](s"$baseUrl/check-cases/${link.submissionId}/${interestedParty}").map{
            case ownerResponse: OwnerCheckCasesResponse =>
              Logger.debug("FilterTotal Owner CheckCases: " + ownerResponse.filterTotal)
              Logger.debug("Print Owner CheckCases: " + ownerResponse.checkCases.mkString)
                Some(ownerResponse)
            case agentResponse: AgentCheckCasesResponse =>
              Logger.debug("FilterTotal Agent CheckCases: " + agentResponse.filterTotal)
              Logger.debug("Print Agent CheckCases: "+ agentResponse.checkCases.mkString)
              Some(agentResponse)
            case _ =>
              Logger.debug("No CheckCases Found")
              None
          }recover{
             case _ => None
          }
      }
      case _ => Future.successful(None)
    }

  }
}
