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

import config.WSHttp
import controllers.{EnrolmentPayload, KeyValuePair}
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.play.config.ServicesConfig

import scala.concurrent.{ExecutionContext, Future}

class TaxEnrolmentConnector @Inject()(wSHttp: WSHttp) extends ServicesConfig {
  private val serviceUrl = baseUrl("tax-enrolments")

  def enrol(personId: Long, postcode: String)(implicit hc: HeaderCarrier, executionContext: ExecutionContext): Future[HttpResponse] =
    enrolMaybe(
      EnrolmentPayload(
        identifiers = List(KeyValuePair("VOAPersonID", personId.toString)),
        verifiers = List(KeyValuePair("BusPostcode", postcode))
      )
    )

  private def enrolMaybe(enrolmentPayload: EnrolmentPayload)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[HttpResponse] =
    wSHttp.PUT[EnrolmentPayload, HttpResponse](s"$serviceUrl/tax-enrolments/service/HMRC-VOA-CCA/enrolment", enrolmentPayload)

}
