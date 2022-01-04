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

package connectors.challenge

import javax.inject.Inject
import models.challenge.myclients.ChallengeCasesWithClient
import models.challenge.myorganisations.ChallengeCasesWithAgent
import models.dvr.cases.check.projection.CaseDetails
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import scala.concurrent.{ExecutionContext, Future}

class ChallengeConnector @Inject()(config: ServicesConfig, http: HttpClient)(implicit ec: ExecutionContext) {
  lazy val baseUrl: String = config.baseUrl("business-rates-challenge") + s"/business-rates-challenge"

  def getMyClientsChallengeCases(propertyLinkSubmissionId: String)(
        implicit hc: HeaderCarrier): Future[List[CaseDetails]] =
    http
      .GET[ChallengeCasesWithClient](
        s"$baseUrl/my-organisation/clients/all/challenge-cases",
        queryParams = Seq("submissionId" -> propertyLinkSubmissionId, "projection" -> "clientsPropertyLink")
      )
      .map(_.challengeCases.map(CaseDetails.apply))

  def getMyOrganisationsChallengeCases(propertyLinkSubmissionId: String)(
        implicit hc: HeaderCarrier): Future[List[CaseDetails]] =
    http
      .GET[ChallengeCasesWithAgent](
        s"$baseUrl/my-organisations/challenge-cases",
        queryParams = Seq("submissionId" -> propertyLinkSubmissionId)
      )
      .map(_.challengeCases.map(CaseDetails.apply))

}
