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

package controllers.manageDetails

import javax.inject.Inject

import actions.AuthenticatedAction
import cats.data.OptionT
import cats.implicits._
import connectors.{Addresses, MessagesConnector}
import controllers.PropertyLinkingController

class ViewDetails @Inject()(addressesConnector: Addresses, authenticated: AuthenticatedAction, messagesConnector: MessagesConnector) extends PropertyLinkingController {

  def show() = authenticated { implicit request =>
    val person = request.individualAccount

    (for {
      personalAddress <- OptionT(addressesConnector.findById(person.details.addressId))
      businessAddress <- OptionT(addressesConnector.findById(request.organisationAccount.addressId))
      msgCount <- OptionT.liftF(messagesConnector.countUnread(request.organisationId))
    } yield Ok(views.html.details.viewDetails(person, request.organisationAccount, personalAddress, businessAddress, msgCount.unread))
      ).getOrElse(throw new Exception(
      s"Unable to lookup address: Individual address ID: ${person.details.addressId}; Organisation address Id: ${request.organisationAccount.addressId}")
    )
  }

}
