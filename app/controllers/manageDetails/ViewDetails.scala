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

package controllers.manageDetails

import javax.inject.Inject

import actions.{AuthenticatedAction, BasicAuthenticatedRequest}
import cats.data.OptionT
import cats.implicits._
import connectors.{Addresses, MessagesConnector, VPLAuthConnector}
import controllers.PropertyLinkingController
import models.{Address, DetailedIndividualAccount}
import play.api.i18n.Messages
import play.api.mvc.{AnyContent, Result, Results}
import uk.gov.hmrc.auth.core.AffinityGroup
import uk.gov.hmrc.auth.core.AffinityGroup._
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector

import scala.concurrent.Future

class ViewDetails @Inject()(
                             addressesConnector: Addresses,
                             authenticated: AuthenticatedAction,
                             messagesConnector: MessagesConnector,
                             authConnector: VPLAuthConnector,
                             details: Details
                           ) extends PropertyLinkingController {

  def show() = authenticated { implicit request =>
    val person = request.individualAccount
    (for {
      personalAddress <- OptionT(addressesConnector.findById(person.details.addressId))
      businessAddress <- OptionT(addressesConnector.findById(request.organisationAccount.addressId))
      msgCount        <- OptionT.liftF(messagesConnector.countUnread(request.organisationId))
      affinityGroup   <- OptionT.liftF(authConnector.getAffinityGroup)
    } yield details.view(affinityGroup, person, personalAddress, businessAddress, msgCount.unread)
      ).getOrElse(throw new Exception(
      s"Unable to lookup address: Individual address ID: ${person.details.addressId}; Organisation address Id: ${request.organisationAccount.addressId}")
    )
  }

}


trait Details extends Results {

  def view(
            affinityGroup: AffinityGroup,
            person: DetailedIndividualAccount,
            personalAddress: Address,
            businessAddress: Address,
            msgCount: Int)
          (implicit request: BasicAuthenticatedRequest[AnyContent], messages: Messages): Result
}

class NonEnrolmentDetails extends Details {

  def view(
            affinityGroup: AffinityGroup,
            person: DetailedIndividualAccount,
            personalAddress: Address,
            businessAddress: Address,
            msgCount: Int)
          (implicit request: BasicAuthenticatedRequest[AnyContent], messages: Messages): Result = {
    Ok(views.html.details.viewDetails(person, request.organisationAccount, personalAddress, businessAddress, msgCount))
  }
}

class EnrolmentDetails extends Details {

  def view(
            affinityGroup: AffinityGroup,
            person: DetailedIndividualAccount,
            personalAddress: Address,
            businessAddress: Address,
            msgCount: Int)
          (implicit request: BasicAuthenticatedRequest[AnyContent], messages: Messages): Result = {
    affinityGroup match {
      case Individual =>
        Ok(views.html.details.viewDetails_individual(person, request.organisationAccount, personalAddress, businessAddress, msgCount))
      case Organisation =>
        Ok(views.html.details.viewDetails_organisation(person, request.organisationAccount, personalAddress, businessAddress, msgCount))
    }
  }
}