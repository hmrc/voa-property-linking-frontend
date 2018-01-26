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

import java.time.{Clock, Instant}

import actions.{AuthenticatedAction, BasicAuthenticatedRequest}
import javax.inject.Inject

import connectors.{Addresses, GroupAccounts}
import controllers.PropertyLinkingController
import form.{Mappings, TextMatching}
import models.{GroupAccount, UpdatedOrganisationAccount}
import play.api.data.Form
import play.api.data.Forms._
import play.api.mvc.{AnyContent, Result}
import services.{EnrolmentResult, ManageDetails, Success}

import scala.concurrent.Future

class UpdateOrganisationDetails @Inject()(
  authenticated: AuthenticatedAction, groups: GroupAccounts,
  addresses: Addresses, manageDetails: ManageDetails)
  (implicit clock: Clock) extends PropertyLinkingController {

  def viewBusinessName = authenticated { implicit request =>
    Ok(views.html.details.updateBusinessName(UpdateOrganisationDetailsVM(businessNameForm, request.organisationAccount)))
  }

  def updateBusinessName = authenticated { implicit request =>
    businessNameForm.bindFromRequest().fold(
      errors => BadRequest(views.html.details.updateBusinessName(UpdateOrganisationDetailsVM(errors, request.organisationAccount))),
      businessName => updateDetails(name = Some(businessName))
    )
  }

  def viewBusinessAddress = authenticated { implicit request =>
    Ok(views.html.details.updateBusinessAddress(UpdateOrganisationDetailsVM(addressForm, request.organisationAccount)))
  }

  def updateBusinessAddress = authenticated { implicit request =>
    addressForm.bindFromRequest().fold(
      errors => BadRequest(views.html.details.updateBusinessAddress(UpdateOrganisationDetailsVM(errors, request.organisationAccount))),
      address => address.addressUnitId match {
        case Some(id) => updateDetails(addressId = Some(id))
        case _ => addresses.create(address) flatMap { id => updateDetails(addressId = Some(id)) }
      }
    )
  }

  def viewBusinessPhone = authenticated { implicit request =>
    Ok(views.html.details.updateBusinessPhone(UpdateOrganisationDetailsVM(phoneForm, request.organisationAccount)))
  }

  def updateBusinessPhone = authenticated { implicit request =>
    phoneForm.bindFromRequest().fold(
      errors => BadRequest(views.html.details.updateBusinessPhone(UpdateOrganisationDetailsVM(errors, request.organisationAccount))),
      phone => updateDetails(phone = Some(phone))
    )
  }

  def viewBusinessEmail = authenticated { implicit request =>
    Ok(views.html.details.updateBusinessEmail(UpdateOrganisationDetailsVM(emailForm, request.organisationAccount)))
  }

  def updateBusinessEmail = authenticated { implicit request =>
    emailForm.bindFromRequest().fold(
      errors => BadRequest(views.html.details.updateBusinessEmail(UpdateOrganisationDetailsVM(errors, request.organisationAccount))),
      email => updateDetails(email = Some(email))
    )
  }

  private def updateDetails(name: Option[String] = None,
                            addressId: Option[Int] = None,
                            email: Option[String] = None,
                            phone: Option[String] = None)(implicit request: BasicAuthenticatedRequest[AnyContent]): Future[Result] = {
    val current = request.organisationAccount
    val details = UpdatedOrganisationAccount(
      current.groupId,
      addressId.getOrElse(current.addressId),
      current.isAgent,
      name.getOrElse(current.companyName),
      email.getOrElse(current.email),
      phone.getOrElse(current.phone),
      Instant.now(clock),
      request.individualAccount.externalId)

    groups.update(current.id, details)
      .flatMap(_ => addressId.fold[Future[EnrolmentResult]]
        (Future.successful(Success))
        (manageDetails.updatePostcode(request.individualAccount.individualId, current.addressId, _)(_ => true)))
      .map(_ => Redirect(controllers.manageDetails.routes.ViewDetails.show()))
  }

  lazy val businessNameForm = Form(single("businessName" -> nonEmptyText(maxLength = 45)))

  lazy val addressForm = Form(single("address" -> Mappings.addressMapping))

  lazy val phoneForm = Form(single("phone" -> nonEmptyText(maxLength = 15)))

  lazy val emailForm = Form(mapping(
    "email" -> email,
    "confirmedEmail" -> TextMatching("email", "error.emailsMustMatch")
  ){ case (e, _) => e }(e => Some((e, e))))
}

case class UpdateOrganisationDetailsVM(form: Form[_], currentDetails: GroupAccount)
