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
import uk.gov.hmrc.auth.core.AffinityGroup.Organisation
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

import scala.concurrent.ExecutionContext.Implicits.global
trait ManageDetails {
  def updatePostcode(personId: Long, currentAddressId: Int, addressId: Int)(predicate: AffinityGroup => Boolean)
                    (implicit hc: HeaderCarrier, request: Request[_]): Future[EnrolmentResult]
}

class ManageDetailsWithEnrolments @Inject() (taxEnrolments: TaxEnrolmentConnector, addresses: Addresses, vPLAuthConnector: VPLAuthConnector) extends ManageDetails with RequestContext {
  def updatePostcode(personId: Long, currentAddressId: Int, addressId: Int)(predicate: AffinityGroup => Boolean)
                    (implicit hc: HeaderCarrier, request: Request[_]): Future[EnrolmentResult] = {
    def withAddress(addressId:Int, addressType:String):Future[Option[Address]] =
      addresses.findById(addressId)

    for {
      currentOpt    <- withAddress(currentAddressId, "current")
      updatedOpt    <- withAddress(addressId, "updated")
      affinityGroup <- vPLAuthConnector.getUserDetails.map(_.userInfo.affinityGroup)
      is            = predicate(affinityGroup)
      result        <- update(currentOpt, updatedOpt, personId, is)
    } yield result
  }

  private def update(currentOpt: Option[Address], updatedOpt: Option[Address], personId: Long, predicate: Boolean)(implicit hc: HeaderCarrier): Future[EnrolmentResult] =
    (currentOpt, updatedOpt, predicate) match {
      case (Some(current), Some(updated), true) =>
        taxEnrolments.updatePostcode(personId = personId, postcode = updated.postcode, previousPostcode = current.postcode)
      case _ =>
        Future.successful(Failure)
    }
}

class ManageDetailsWithoutEnrolments extends ManageDetails with RequestContext {
  def updatePostcode(personId: Long, currentAddressId: Int, addressId: Int)(predicate: AffinityGroup => Boolean)
                    (implicit hc: HeaderCarrier, request: Request[_]): Future[EnrolmentResult] = Future.successful(Success)
}
