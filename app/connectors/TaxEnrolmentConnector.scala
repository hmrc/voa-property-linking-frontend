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

import auditing.AuditingService
import config.WSHttp
import controllers.{EnrolmentPayload, KeyValuePair, PayLoad, Previous}
import play.api.libs.json.{JsValue, Json, Reads, Writes}
import services.{EnrolmentResult, Success}
import play.api.mvc.{AnyContent, Request}
import uk.gov.hmrc.http.{HeaderCarrier, HttpReads, HttpResponse}
import uk.gov.hmrc.play.config.ServicesConfig

import scala.concurrent.{ExecutionContext, Future}

class TaxEnrolmentConnector @Inject()(wSHttp: WSHttp) extends ServicesConfig {
  private val serviceUrl = baseUrl("tax-enrolments")
  private val emacUrl = baseUrl("emac") + "/enrolment-store-proxy"

  private val updateUrl: Long => String = personId => s"$serviceUrl/tax-enrolments/enrolments/HMRC-VOA-CCA~VOAPersonID~${personId.toString}"
  private val enrolUrl = s"$serviceUrl/tax-enrolments/service/HMRC-VOA-CCA/enrolment"

  def enrol(personId: Long, postcode: String)(implicit hc: HeaderCarrier, ex: ExecutionContext): Future[HttpResponse] = {
    val payload = EnrolmentPayload(
      identifiers = List(KeyValuePair("VOAPersonID", personId.toString)),
      verifiers = List(KeyValuePair("BusPostcode", postcode))
    )
    wSHttp.PUT[EnrolmentPayload, HttpResponse](enrolUrl, payload).map { result =>
      AuditingService.sendEvent[EnrolmentPayload]("Enrolment Success", payload)
      result
    }.recover{case exception: Throwable =>
      AuditingService.sendEvent("Enrolment failed to update", payload)
      throw exception
    }
  }

  def deEnrol(personID: Long)(implicit hc: HeaderCarrier, ex: ExecutionContext): Future[Future[HttpResponse]] =
    wSHttp.POST[JsValue, HttpResponse](s"$serviceUrl/tax-enrolments/de-enrol/HMRC-VOA-CCA", Json.obj("keepAgentAllocations" -> true))(
      implicitly[Writes[JsValue]], implicitly[HttpReads[HttpResponse]], hc.withExtraHeaders("Content-Type" -> "application/json"), ex)
      .map(_ => wSHttp.DELETE[HttpResponse](s"$emacUrl/enrolment-store/enrolments/HMRC-VOA-CCA~VOAPersonID~$personID"))

  def updatePostcode(personId:Long, postcode:String, previousPostcode:String)(implicit hc: HeaderCarrier, executionContext: ExecutionContext): Future[EnrolmentResult] = {
    val payload = PayLoad(verifiers = Seq(KeyValuePair(key="BusPostcode",value=postcode)),
      legacy = Some(Previous(previousVerifiers = List(KeyValuePair(key="BusPostcode", value=previousPostcode)))))
    wSHttp.PUT[PayLoad, HttpResponse](s"$serviceUrl/tax-enrolments/enrolments/HMRC-VOA-CCA~VOAPersonID~${personId.toString}", payload)
      .map{_ =>
        AuditingService.sendEvent("Enrolment Updated", payload)
        Success
      }.recover{case exception: Throwable =>
      AuditingService.sendEvent("Enrolment failed to update", payload)
      throw exception
    }
  }
}
