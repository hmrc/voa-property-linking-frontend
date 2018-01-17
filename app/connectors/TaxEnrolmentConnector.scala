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
import controllers.{EnrolmentPayload, KeyValuePair, PayLoad}
import play.api.libs.json.{JsValue, Json}
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.play.config.ServicesConfig

import scala.concurrent.{ExecutionContext, Future}

class TaxEnrolmentConnector @Inject()(wSHttp: WSHttp) extends ServicesConfig {
  private val serviceUrl = baseUrl("tax-enrolments")
  private val emacUrl = baseUrl("emac") + "/enrolment-store-proxy"

  def enrol(personId: Long, postcode: String)(implicit hc: HeaderCarrier, executionContext: ExecutionContext): Future[HttpResponse] =
    enrolMaybe(
      EnrolmentPayload(
        identifiers = List(KeyValuePair("VOAPersonID", personId.toString)),
        verifiers = List(KeyValuePair("BusPostcode", postcode))
      )
    )

  def deEnrol(personID: Long)(implicit hc: HeaderCarrier, ex: ExecutionContext) =
    wSHttp.POST[JsValue, HttpResponse](s"$serviceUrl/tax-enrolments/de-enrol/HMRC-VOA-CCA", Json.obj("keepAgentAllocations" ->  true))
      .map(_ => wSHttp.DELETE(s"$emacUrl/enrolment-store/enrolments/HMRC-VOA-CCA~VOAPersonID~$personID"))

  def updatePostcode(personId:Long, postcode:String)(implicit hc: HeaderCarrier, executionContext: ExecutionContext): Future[HttpResponse] =
    wSHttp.PUT[PayLoad, HttpResponse](
      s"$serviceUrl/tax-enrolments/enrolments/HMRC-VOA-CCA~VOAPersonID~${personId.toString}",
      PayLoad(verifiers = Seq(KeyValuePair(key="BusPostcode",value=postcode))))

  private def enrolMaybe(enrolmentPayload: EnrolmentPayload)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[HttpResponse] =
    wSHttp.PUT[EnrolmentPayload, HttpResponse](s"$serviceUrl/tax-enrolments/service/HMRC-VOA-CCA/enrolment", enrolmentPayload)
}
