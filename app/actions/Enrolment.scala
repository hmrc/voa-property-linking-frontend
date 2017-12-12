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
import models.{Accounts, DetailedIndividualAccount, GroupAccount, PersonalDetails}
import play.api.libs.json.Json
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.play.config.ServicesConfig

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class EnrolmentService @Inject()(taxEnrolmentsConnector: TaxEnrolmentConnector, auth: VPLAuthConnector, addresses: Addresses) {

  def enrol(personId: Long, addressId: Int)(implicit hc: HeaderCarrier): Future[EnrolmentResult] = {
    for {
      optPostCode <- addresses.findById(addressId)
      userId <- auth.getUserId
      result <- taxEnrolmentsConnector.enrol(personId, optPostCode.map(_.postcode), userId)
    } yield result
  } //Test what happens on NONE.
}

class TaxEnrolmentConnector @Inject()(serviceConfig: ServicesConfig, wSHttp: WSHttp) {
  implicit val keyValue = Json.format[KeyValuePair]
  implicit val previous = Json.format[Previous]
  implicit val format = Json.format[PayLoad]

  val baseUrl = serviceConfig.baseUrl("tax-enrolments")

  def enrolMaybe(enrolmentPayload: EnrolmentPayload) = {
    wSHttp.PUT(s"$baseUrl/tax-enrolments/service/HMRC-VOA-CCA/enrolment", enrolmentPayload)
  }

  def enrol(personId: Long, maybePostCode: Option[String], userId: String)(implicit hc: HeaderCarrier): Future[EnrolmentResult] = maybePostCode match {
    case Some(x) =>
      wSHttp
      .PUT[PayLoad, HttpResponse](s"$baseUrl/tax-enrolments/enrolments/HMRC-VOA-CCA~PersonID~$personId", PayLoad(List(KeyValuePair("BusPostcode", x)))).flatMap{
      case HttpResponse(201, _, _, _) => assign(personId, userId)
      case _ => Future.successful(Failure)
    }
    case None => Future.successful(Failure)
  }

  private def assign(personId: Long, userId: String)(implicit hc: HeaderCarrier): Future[EnrolmentResult] =
    wSHttp
      .POSTEmpty[HttpResponse](s"$baseUrl /tax-enrolments/users/$userId/enrolments/HMRC-VOA-CCA~PersonID~$personId")
      .map(_ => Success)
      .recover{case _ : Throwable => Failure}

}

case class EnrolmentPayload(identifiers: List[KeyValuePair], verifiers: List[KeyValuePair])
case class PayLoad(verifiers: Seq[KeyValuePair], legacy: Option[Previous] = None)

case class KeyValuePair(key: String, value: String)

case class Previous(previousVerifiers: List[KeyValuePair])

sealed trait EnrolmentResult
case object Success extends EnrolmentResult
case object Failure extends EnrolmentResult