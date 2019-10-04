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

package connectors.test

import javax.inject.Inject
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.play.bootstrap.http.HttpClient
import uk.gov.hmrc.play.config.ServicesConfig

import scala.concurrent.{ExecutionContext, Future}

class TestPropertyLinkingConnector @Inject()(config: ServicesConfig, http: HttpClient)(implicit ec: ExecutionContext) {

  lazy val url = config.baseUrl("property-linking") + "/property-linking/"

  def deRegister(organisationId: Long)(implicit hc: HeaderCarrier): Future[HttpResponse] = {
    http.DELETE[HttpResponse](url + s"test-only/de-register/$organisationId")
  }

  def clearDvrRecords(organisationId: Long)(implicit hc: HeaderCarrier): Future[HttpResponse] = {
    http.DELETE[HttpResponse](url + s"test-only/clear-dvr-records/$organisationId")
  }

  def deleteCheckCases(plSubmissionId: String)(implicit hc: HeaderCarrier): Future[HttpResponse] = {
    http.DELETE[HttpResponse](url + s"test-only/delete-check-cases/$plSubmissionId")
  }

}
