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

import actions.requests.BasicAuthenticatedRequest
import config.ApplicationConfig
import connectors.{Addresses, TaxEnrolmentConnector}
import javax.inject.Inject
import models.Address
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

trait ManageDetails {
  def updatePostcode(personId: Long, currentAddressId: Long, addressId: Long)(
        implicit hc: HeaderCarrier,
        request: BasicAuthenticatedRequest[_]): Future[EnrolmentResult]
}

class ManageVoaDetails @Inject()(taxEnrolments: TaxEnrolmentConnector, addresses: Addresses, config: ApplicationConfig)(
      implicit executionContext: ExecutionContext)
    extends ManageDetails with RequestContext {
  def updatePostcode(personId: Long, currentAddressId: Long, addressId: Long)(
        implicit hc: HeaderCarrier,
        request: BasicAuthenticatedRequest[_]): Future[EnrolmentResult] = {
    def withAddress(addressId: Long, addressType: String): Future[Option[Address]] =
      addresses.findById(addressId)

    for {
      currentOpt <- withAddress(currentAddressId, "current")
      updatedOpt <- withAddress(addressId, "updated")
      result     <- if (config.stubEnrolment) Future.successful(Success) else update(currentOpt, updatedOpt, personId)
    } yield result
  }

  private def update(currentOpt: Option[Address], updatedOpt: Option[Address], personId: Long)(
        implicit hc: HeaderCarrier): Future[EnrolmentResult] =
    (currentOpt, updatedOpt) match {
      case (Some(current), Some(updated)) =>
        taxEnrolments
          .updatePostcode(personId = personId, postcode = updated.postcode, previousPostcode = current.postcode)
      case _ =>
        Future.successful(Failure)
    }
}
