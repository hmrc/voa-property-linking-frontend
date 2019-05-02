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

package controllers.manageDetails

import javax.inject.Inject
import actions.{AuthenticatedAction, BasicAuthenticatedRequest}
import cats.data.OptionT
import cats.implicits._
import config.ApplicationConfig
import connectors.{Addresses, MessagesConnector, VPLAuthConnector}
import controllers.PropertyLinkingController
import models.registration.UserDetails
import models.{Address, DetailedIndividualAccount}
import play.api.i18n.{Messages, MessagesApi}
import play.api.mvc.{AnyContent, Result, Results}
import uk.gov.hmrc.auth.core.AffinityGroup
import uk.gov.hmrc.auth.core.AffinityGroup._

import scala.concurrent.Future

class ViewDetails @Inject()(addressesConnector: Addresses,
                            authenticated: AuthenticatedAction,
                            messagesConnector: MessagesConnector,
                            authConnector: VPLAuthConnector,
                            details: Details
                           )(implicit val messagesApi: MessagesApi, config: ApplicationConfig) extends PropertyLinkingController {

  def show() = authenticated { implicit request =>
    Future.successful(Redirect(config.newDashboardUrl("your-details")))
  }

}


trait Details extends Results {

  def view(
            affinityGroup: AffinityGroup,
            person: DetailedIndividualAccount,
            personalAddress: Address,
            businessAddress: Address,
            userDetails: UserDetails)
          (implicit request: BasicAuthenticatedRequest[AnyContent], messages: Messages): Result
}

class VoaDetails @Inject()(implicit val messagesApi: MessagesApi, implicit val config: ApplicationConfig) extends Details {

  def view(
            affinityGroup: AffinityGroup,
            person: DetailedIndividualAccount,
            personalAddress: Address,
            businessAddress: Address,
            userDetails: UserDetails
          )
          (implicit request: BasicAuthenticatedRequest[AnyContent], messages: Messages): Result = {
    affinityGroup match {
      case Individual =>
        Ok(views.html.details.viewDetails_individual(person, request.organisationAccount, personalAddress, businessAddress))
      case Organisation =>
        Ok(views.html.details.viewDetails_organisation(person, request.organisationAccount, personalAddress, businessAddress, userDetails))
    }
  }
}