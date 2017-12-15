/*
 * Copyright 2017 HM Revenue & Customs
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

package actions

import javax.inject.Inject

import config.WSHttp
import connectors.{Addresses, VPLAuthConnector}
import controllers.EnrolmentPayload._
import controllers.{EnrolmentPayload, KeyValuePair}
import models.Address
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.play.config.ServicesConfig

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class EnrolmentService @Inject()(taxEnrolmentsConnector: TaxEnrolmentConnector, auth: VPLAuthConnector, addresses: Addresses) {

  def enrol(personId: Long, addressId: Int)(implicit hc: HeaderCarrier): Future[EnrolmentResult] = {
    (for {
      optPostCode <- addresses.findById(addressId)
      y      <- x(optPostCode)
      userId <- auth.getUserId
      _ <- taxEnrolmentsConnector.enrol(personId, y.postcode, userId)
    } yield Success)
      .recover{
        case _: Throwable => Failure
      }
  } //Test what happens on NONE.

  def x(opt: Option[Address]): Future[Address] = opt match {
    case None => Future.failed(throw new IllegalArgumentException())
    case Some(x) => Future.successful(x)
  }
}

class TaxEnrolmentConnector @Inject()(wSHttp: WSHttp) extends ServicesConfig {

  val serviceUrl = baseUrl("tax-enrolments")

  private def enrolMaybe(enrolmentPayload: EnrolmentPayload)(implicit hc: HeaderCarrier): Future[HttpResponse] =
    wSHttp.PUT[EnrolmentPayload, HttpResponse](s"$serviceUrl/tax-enrolments/service/HMRC-VOA-CCA/enrolment", enrolmentPayload)

  def enrol(personId: Long, postcode: String, userId: String)(implicit hc: HeaderCarrier): Future[HttpResponse] =
    enrolMaybe(EnrolmentPayload(identifiers = List(KeyValuePair("PersonID", personId.toString)), verifiers = List(KeyValuePair("BusPostcode", postcode))))

}

sealed trait EnrolmentResult
case object Success extends EnrolmentResult
case object Failure extends EnrolmentResult