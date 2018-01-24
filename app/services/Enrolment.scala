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

package services

import javax.inject.Inject

import auditing.AuditingService
import connectors.{Addresses, TaxEnrolmentConnector, VPLAuthConnector}
import models.Address
import play.api.libs.json.Json
import play.api.mvc.Request
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}

class EnrolmentService @Inject()(taxEnrolmentsConnector: TaxEnrolmentConnector, addresses: Addresses) {

  def enrol(personId: Long, addressId: Int)(implicit hc: HeaderCarrier, ex: ExecutionContext, request: Request[_]): Future[EnrolmentResult] = {
    (for {
      optAddress <- addresses.findById(addressId)
      address      <- getAddress(optAddress)
      _ <- taxEnrolmentsConnector.enrol(personId, address.postcode)
    } yield Success).recover{
        case _: Throwable =>
          AuditingService.sendEvent("Enrolment Failure", Json.obj("personId" -> personId))
          Failure
      }
  }

  def deEnrolUser(personID: Long)(implicit hc: HeaderCarrier) =
    taxEnrolmentsConnector
      .deEnrol(personID)
      .map(_ => Success)
      .recover{
        case _ : Throwable => Failure
      }

  private def getAddress(opt: Option[Address]): Future[Address] = opt match {
    case None => Future.failed(throw new IllegalArgumentException())
    case Some(x) => Future.successful(x)
  }
}

sealed trait EnrolmentResult
case object Success extends EnrolmentResult
case object Failure extends EnrolmentResult