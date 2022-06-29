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

package controllers.manageDetails

import java.time.Instant

import actions.AuthenticatedAction
import actions.requests.BasicAuthenticatedRequest
import config.ApplicationConfig
import connectors.{Addresses, GroupAccounts, IndividualAccounts}
import controllers.PropertyLinkingController
import form.Mappings._
import form.TextMatching
import javax.inject.Inject
import models.{DetailedIndividualAccount, GroupAccount, IndividualDetails, UpdatedOrganisationAccount}
import play.api.data.Form
import play.api.data.Forms._
import play.api.i18n.MessagesApi
import play.api.mvc.{MessagesControllerComponents, Result}
import services.{EnrolmentResult, ManageDetails, Success}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.propertylinking.errorhandler.CustomErrorHandler
import utils.EmailAddressValidation
import utils.PhoneNumberValidation.validatePhoneNumber

import scala.concurrent.{ExecutionContext, Future}

class UpdatePersonalDetails @Inject()(
      val errorHandler: CustomErrorHandler,
      authenticated: AuthenticatedAction,
      addressesConnector: Addresses,
      individualAccountConnector: IndividualAccounts,
      manageDetails: ManageDetails,
      groupAccounts: GroupAccounts,
      updateAddressView: views.html.details.updateAddress,
      updatePhoneView: views.html.details.updatePhone,
      updateMobileView: views.html.details.updateMobile,
      updateEmailView: views.html.details.updateEmail,
      updateNameView: views.html.details.updateName
)(
      implicit executionContext: ExecutionContext,
      override val messagesApi: MessagesApi,
      override val controllerComponents: MessagesControllerComponents,
      val config: ApplicationConfig
) extends PropertyLinkingController {

  def viewEmail() = authenticated { implicit request =>
    Ok(updateEmailView(UpdateDetailsVM(emailForm, request.individualAccount.details)))
  }

  def updateEmail() = authenticated.async { implicit request =>
    emailForm
      .bindFromRequest()
      .fold(
        errors =>
          Future.successful(BadRequest(updateEmailView(UpdateDetailsVM(errors, request.individualAccount.details)))),
        email => updateDetails(email = Some(email))
      )
  }

  def viewAddress() = authenticated { implicit request =>
    Ok(updateAddressView(UpdateDetailsVM(addressForm, request.individualAccount.details)))
  }

  def updateAddress() = authenticated.async { implicit request =>
    addressForm
      .bindFromRequest()
      .fold(
        errors =>
          Future.successful(BadRequest(updateAddressView(UpdateDetailsVM(errors, request.individualAccount.details)))),
        address =>
          address.addressUnitId match {
            case Some(id) => updateDetails(addressId = Some(id))
            case None =>
              addressesConnector.create(address) flatMap { id =>
                updateDetails(addressId = Some(id))
              }
        }
      )
  }

  def viewPhone() = authenticated { implicit request =>
    Ok(updatePhoneView(UpdateDetailsVM(telephoneForm, request.individualAccount.details)))
  }

  def updatePhone() = authenticated.async { implicit request =>
    telephoneForm
      .bindFromRequest()
      .fold(
        errors =>
          Future.successful(BadRequest(updatePhoneView(UpdateDetailsVM(errors, request.individualAccount.details)))),
        phone => updateDetails(phone = Some(phone))
      )
  }

  def viewName() = authenticated { implicit request =>
    Ok(updateNameView(UpdateDetailsVM(nameForm, request.individualAccount.details)))
  }

  def updateName() = authenticated.async { implicit request =>
    nameForm
      .bindFromRequest()
      .fold(
        errors =>
          Future.successful(BadRequest(updateNameView(UpdateDetailsVM(errors, request.individualAccount.details)))),
        name => updateDetails(firstName = Some(name.firstName), lastName = Some(name.lastName))
      )
  }

  def viewMobile() = authenticated { implicit request =>
    Ok(updateMobileView(UpdateDetailsVM(telephoneForm, request.individualAccount.details)))
  }

  def updateMobile() = authenticated.async { implicit request =>
    telephoneForm
      .bindFromRequest()
      .fold(
        errors =>
          Future.successful(BadRequest(updateMobileView(UpdateDetailsVM(errors, request.individualAccount.details)))),
        mobile => updateDetails(mobile = Some(mobile))
      )
  }

  private def updateDetails(
        firstName: Option[String] = None,
        lastName: Option[String] = None,
        email: Option[String] = None,
        phone: Option[String] = None,
        mobile: Option[String] = None,
        addressId: Option[Long] = None)(implicit request: BasicAuthenticatedRequest[_]): Future[Result] = {

    val currentDetails = request.individualAccount.details
    val updatedDetails = currentDetails.copy(
      firstName = firstName.getOrElse(currentDetails.firstName),
      lastName = lastName.getOrElse(currentDetails.lastName),
      email = email.getOrElse(currentDetails.email),
      phone1 = phone.getOrElse(currentDetails.phone1),
      phone2 = mobile.orElse(currentDetails.phone2),
      addressId = addressId.getOrElse(currentDetails.addressId)
    )
    val updatedAccount = request.individualAccount.copy(details = updatedDetails)

    individualAccountConnector
      .update(updatedAccount)
      .flatMap(_ =>
        addressId.fold[Future[EnrolmentResult]](Future.successful(Success))(
          manageDetails.updatePostcode(request.individualAccount.individualId, currentDetails.addressId, _)))
      .map { _ =>
        updateGroup(request.organisationAccount, updatedAccount)
        Redirect(controllers.manageDetails.routes.ViewDetails.show())
      }
  }

  private def updateGroup(group: GroupAccount, updatedAccount: DetailedIndividualAccount)(
        implicit hc: HeaderCarrier): Future[Unit] = {
    val account =
      UpdatedOrganisationAccount(
        group.groupId,
        updatedAccount.details.addressId,
        group.isAgent,
        companyName(group.companyName, updatedAccount.details.firstName, updatedAccount.details.lastName),
        updatedAccount.details.email,
        updatedAccount.details.phone1,
        Instant.now(),
        updatedAccount.externalId
      )

    groupAccounts.update(updatedAccount.organisationId, account)
  }

  private def companyName(companyName: String, firstName: String, lastName: String) =
    if (companyName.isEmpty) s"$firstName $lastName" else companyName

  private lazy val emailForm = Form(
    mapping(
      "email"          -> text.verifying("error.invalidEmail", EmailAddressValidation.isValid(_)),
      "confirmedEmail" -> TextMatching("email", "error.emailsMustMatch")
    ) { case (e, _) => e }(email => Some((email, email)))
  )

  private lazy val telephoneForm = Form(
    single(
      "phone" -> validatePhoneNumber
    )
  )

  private lazy val nameForm = Form(
    mapping(
      "firstName" -> nonEmptyText(maxLength = 100),
      "lastName"  -> nonEmptyText(maxLength = 100)
    )(Name.apply)(Name.unapply)
  )

  private lazy val addressForm = Form(
    single("address" -> addressMapping)
  )

  case class Name(firstName: String, lastName: String)

}

case class UpdateDetailsVM(form: Form[_], currentDetails: IndividualDetails)
