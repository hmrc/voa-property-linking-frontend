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

package services

import config.ApplicationConfig
import connectors.{Addresses, TaxEnrolmentConnector, VPLAuthConnector}
import javax.inject.Inject
import models.Address
import play.api.mvc.Request
import uk.gov.hmrc.auth.core.AffinityGroup
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

trait ManageDetails {
  def updatePostcode(personId: Long, currentAddressId: Long, addressId: Long)(predicate: AffinityGroup => Boolean)
                    (implicit hc: HeaderCarrier, request: Request[_]): Future[EnrolmentResult]
}

class ManageVoaDetails @Inject()(
                                  taxEnrolments: TaxEnrolmentConnector,
                                  addresses: Addresses,
                                  vPLAuthConnector: VPLAuthConnector,
                                  config: ApplicationConfig
                                ) extends ManageDetails with RequestContext {

  def updatePostcode(personId: Long, currentAddressId: Long, addressId: Long)(predicate: AffinityGroup => Boolean)
                    (implicit hc: HeaderCarrier, request: Request[_]): Future[EnrolmentResult] = {
    def withAddress(addressId: Long, addressType: String): Future[Address] =
      addresses.findById(addressId)

    for {
      current       <- withAddress(currentAddressId, "current")
      updated       <- withAddress(addressId, "updated")
      affinityGroup <- vPLAuthConnector.getUserDetails.map(_.userInfo.affinityGroup)
      is            = predicate(affinityGroup)
      result        <- if (config.stubEnrolment) Future.successful(Success) else update(current, updated, personId, is)
    } yield result
  }

  private def update(current: Address, updated: Address, personId: Long, predicate: Boolean)(implicit hc: HeaderCarrier): Future[EnrolmentResult] =
    if(predicate) {
      taxEnrolments.updatePostcode(personId = personId, postcode = updated.postcode, previousPostcode = current.postcode)
    } else {
      Future.successful(Failure)
    }
}