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

package services

import auditing.AuditingService
import connectors.{Addresses, TaxEnrolmentConnector}
import form.Mappings.postcodeRegex
import models.Address
import org.apache.commons.lang3.StringUtils.isNotBlank
import play.api.libs.json.Json
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class EnrolmentService @Inject()(
      taxEnrolmentsConnector: TaxEnrolmentConnector,
      addresses: Addresses,
      auditingService: AuditingService) {

  private val logger = play.api.Logger(this.getClass)

  def enrol(personId: Long, addressId: Long)(
        implicit hc: HeaderCarrier,
        ex: ExecutionContext): Future[EnrolmentResult] = {

    def skipEnrolmentForBlankPostcode: Future[Unit] = {
      logger.info(s"Skipping enrolment for personId $personId, addressId $addressId because of a blank postcode")
      Future.successful((): Unit)
    }

    def canEnrol(postcode: String): Boolean =
      isNotBlank(postcode) && postcodeRegex.pattern.matcher(postcode).matches()

    val enrol = for {
      optAddress <- addresses.findById(addressId)
      address    <- getAddress(optAddress)
      _ <- if (canEnrol(address.postcode)) taxEnrolmentsConnector.enrol(personId, address.postcode)
          else skipEnrolmentForBlankPostcode
    } yield Success

    enrol.recover {
      case _: Throwable =>
        auditingService.sendEvent("Enrolment Failure", Json.obj("personId" -> personId))
        Failure
    }
  }

  private def getAddress(opt: Option[Address]): Future[Address] = opt match {
    case None    => Future.failed(throw new IllegalArgumentException())
    case Some(x) => Future.successful(x)
  }
}

sealed trait EnrolmentResult

case object Success extends EnrolmentResult

case object Failure extends EnrolmentResult
