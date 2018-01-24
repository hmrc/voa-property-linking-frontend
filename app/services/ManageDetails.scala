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

import connectors.{Addresses, TaxEnrolmentConnector, VPLAuthConnector}
import models.Address
import play.api.Logger
import play.api.mvc.Request
import uk.gov.hmrc.auth.core.AffinityGroup
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future

trait ManageDetails {
  def updatePostcode(personId: Long, currentAddressId: Int, addressId: Int)(predicate:AffinityGroup=>Boolean)(implicit hc: HeaderCarrier, request: Request[_]): Future[Any]
}

class ManageDetailsWithEnrolments @Inject() (taxEnrolments: TaxEnrolmentConnector, addresses: Addresses, vPLAuthConnector: VPLAuthConnector) extends ManageDetails with RequestContext {
  def updatePostcode(personId: Long, currentAddressId: Int, addressId: Int)(predicate:AffinityGroup=>Boolean)(implicit hc: HeaderCarrier, request: Request[_]): Future[Any] = {
    def withAddress(addressId:Int, addressType:String)(action:Address => Future[Any]):Future[Any] =
      addresses.findById(addressId).flatMap {
        case Some(address) => action(address)
        case None => {
          Logger.error(s"ManageDetails: Could not find address for $addressType addressId $addressId")
          succeed
        }
      }

    vPLAuthConnector.getUserDetails.flatMap { x =>
      if (!predicate(x.userInfo.affinityGroup)) {
        succeed
      }
      else {
        withAddress(currentAddressId, "current") { currentAddress =>
          withAddress(addressId, "updated") { updatedAddress =>
            taxEnrolments.updatePostcode(personId = personId, postcode = updatedAddress.postcode, previousPostcode = currentAddress.postcode)
          }
        }
      }
    }
  }
}

class ManageDetailsWithoutEnrolments extends ManageDetails with RequestContext {
  def updatePostcode(personId: Long, currentAddressId: Int, addressId: Int)(predicate: AffinityGroup => Boolean)(implicit hc: HeaderCarrier, request: Request[_]): Future[Any] = succeed
}
