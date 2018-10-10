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

package connectors

import javax.inject.Inject

import actions.BasicAuthenticatedRequest
import config.{ApplicationConfig, WSHttp}
import models._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import uk.gov.hmrc.http.NotFoundException
import uk.gov.hmrc.play.HeaderCarrierConverter
import uk.gov.hmrc.play.config.inject.ServicesConfig

import scala.concurrent.Future

class CheckCaseConnector @Inject()(config: ServicesConfig, http: WSHttp){
  lazy val baseUrl: String = config.baseUrl("external-business-rates-data-platform")


  def getCheckCases(authorisationId: Long, isAgentOwnProperty: Boolean)(implicit request: BasicAuthenticatedRequest[_]): Future[Option[CheckCasesResponse]] = {
    implicit val hc = HeaderCarrierConverter.fromHeadersAndSession(request.request.headers, Some(request.request.session))
      .withExtraHeaders(("GG-EXTERNAL-ID" -> request.individualAccount.externalId))
      .withExtraHeaders(("GG-GROUP-ID" -> request.organisationAccount.groupId))

    if(request.organisationAccount.isAgent && !isAgentOwnProperty) {
      http.GET[Option[AgentCheckCasesResponse]](s"$baseUrl/external-case-management-api/my-organisation/clients/all/property-links/$authorisationId/check-cases?start=1&size=15&sortField=createdDateTime&sortOrder=ASC") recover { case _: NotFoundException => None }
    }else{
      http.GET[Option[OwnerCheckCasesResponse]](s"$baseUrl/external-case-management-api/my-organisation/property-links/$authorisationId/check-cases?start=1&size=15&sortField=createdDateTime&sortOrder=ASC") recover { case _: NotFoundException => None }
    }

  }
}
