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

package connectors

import auditing.AuditingService
import connectors.errorhandler.exceptions.ExceptionThrowingReadsInstances
import controllers.{EnrolmentPayload, KeyValuePair, PayLoad, Previous}
import javax.inject.Inject
import services.{EnrolmentResult, Success}
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpResponse}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import scala.concurrent.{ExecutionContext, Future}

class TaxEnrolmentConnector @Inject()(
      http: HttpClient,
      auditingService: AuditingService,
      servicesConfig: ServicesConfig
) {
  private val serviceUrl = servicesConfig.baseUrl("tax-enrolments")
  private val enrolUrl = s"$serviceUrl/tax-enrolments/service/HMRC-VOA-CCA/enrolment"

  def enrol(personId: Long, postcode: String)(
        implicit hc: HeaderCarrier,
        ex: ExecutionContext): Future[HttpResponse] = {

    val payload = EnrolmentPayload(
      identifiers = List(KeyValuePair("VOAPersonID", personId.toString)),
      verifiers = List(KeyValuePair("BusPostcode", postcode))
    )
    import ExceptionThrowingReadsInstances._
    http
      .PUT[EnrolmentPayload, HttpResponse](enrolUrl, payload)
      .map { result =>
        auditingService.sendEvent[EnrolmentPayload]("Enrolment Success", payload)
        result
      }
      .recover {
        case exception: Throwable =>
          auditingService.sendEvent("Enrolment failed to update", payload)
          throw exception
      }
  }

  def updatePostcode(personId: Long, postcode: String, previousPostcode: String)(
        implicit hc: HeaderCarrier,
        executionContext: ExecutionContext): Future[EnrolmentResult] = {

    val payload = PayLoad(
      verifiers = Seq(KeyValuePair(key = "BusPostcode", value = postcode)),
      legacy = Some(Previous(previousVerifiers = List(KeyValuePair(key = "BusPostcode", value = previousPostcode))))
    )
    import ExceptionThrowingReadsInstances._
    http
      .PUT[PayLoad, HttpResponse](
        s"$serviceUrl/tax-enrolments/enrolments/HMRC-VOA-CCA~VOAPersonID~${personId.toString}",
        payload)
      .map { _ =>
        auditingService.sendEvent("Enrolment Updated", payload)
        Success
      }
      .recover {
        case exception: Throwable =>
          auditingService.sendEvent("Enrolment failed to update", payload)
          throw exception
      }
  }
}
