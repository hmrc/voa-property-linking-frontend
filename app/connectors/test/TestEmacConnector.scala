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

import config.WSHttp
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.play.config.ServicesConfig

import scala.concurrent.ExecutionContext

class TestEmacConnector @Inject()(wSHttp: WSHttp) extends ServicesConfig {

  private val serviceUrl = baseUrl("emac")

  def removeEnrolment(personId: Long, userId: String, groupId: String)(implicit hc: HeaderCarrier, ex: ExecutionContext) = {
    wSHttp
      .DELETE[HttpResponse](s"$serviceUrl/enrolment-store-proxy/enrolment-store/users/$userId/enrolments/HMRC-VOA-CCA~VOAPersonID~$personId")
      .map(_ => wSHttp.DELETE[HttpResponse](s"$serviceUrl/enrolment-store-proxy/enrolment-store/groups/$groupId/enrolments/HMRC-VOA-CCA~VOAPersonID~$personId"))
  }

}
