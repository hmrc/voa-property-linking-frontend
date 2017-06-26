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

package controllers

import actions.BasicAuthenticatedRequest
import config.Wiring
import form.Mappings._
import form.TextMatching
import models.IndividualDetails
import play.api.data.Form
import play.api.data.Forms._
import play.api.data.validation.Constraints
import play.api.mvc.Result

import scala.concurrent.Future

class UpdatePersonalDetails extends PropertyLinkingController {

  val authenticated = Wiring().authenticated
  val addressesConnector = Wiring().addresses
  val individualAccountConnector = Wiring().individualAccountConnector

  def show() = authenticated { implicit request =>
    addressesConnector.findById(request.individualAccount.details.addressId) map {
      case Some(address) => Ok(views.html.details.personalDetails(request.individualAccount, request.organisationAccount, address))
      case None => throw new Exception(s"Unable to lookup address for individual; ID: ${request.individualAccount.individualId}")
    }
  }

  def viewEmail() = authenticated { implicit request =>
    Ok(views.html.details.updateEmail(UpdateDetailsVM(emailForm, request.individualAccount.details)))
  }

  def updateEmail() = authenticated { implicit request =>
    emailForm.bindFromRequest().fold(
      errors => BadRequest(views.html.details.updateEmail(UpdateDetailsVM(errors, request.individualAccount.details))),
      email => updateDetails(email = Some(email))
    )
  }

  def viewAddress() = authenticated { implicit request =>
    Ok(views.html.details.updateAddress(UpdateDetailsVM(addressForm, request.individualAccount.details)))
  }

  def updateAddress() = authenticated { implicit request =>
    addressForm.bindFromRequest().fold(
      errors => BadRequest(views.html.details.updateAddress(UpdateDetailsVM(errors, request.individualAccount.details))),
      address => {
        address.addressUnitId match {
          case Some(id) => updateDetails(addressId = Some(id))
          case None => addressesConnector.create(address) flatMap { id => updateDetails(addressId = Some(id)) }
        }
      }
    )
  }

  def viewPhone() = authenticated { implicit request =>
    Ok(views.html.details.updatePhone(UpdateDetailsVM(telephoneForm, request.individualAccount.details)))
  }

  def updatePhone() = authenticated { implicit request =>
    telephoneForm.bindFromRequest().fold(
      errors => BadRequest(views.html.details.updatePhone(UpdateDetailsVM(errors, request.individualAccount.details))),
      phone => updateDetails(phone = Some(phone))
    )
  }

  def viewName() = authenticated { implicit request =>
    Ok(views.html.details.updateName(UpdateDetailsVM(nameForm, request.individualAccount.details)))
  }

  def updateName() = authenticated { implicit request =>
    nameForm.bindFromRequest().fold(
      errors => BadRequest(views.html.details.updateName(UpdateDetailsVM(errors, request.individualAccount.details))),
      name => updateDetails(firstName = Some(name.firstName), lastName = Some(name.lastName))
    )
  }

  def viewMobile() = authenticated { implicit request =>
    Ok(views.html.details.updateMobile(UpdateDetailsVM(telephoneForm, request.individualAccount.details)))
  }

  def updateMobile() = authenticated { implicit request =>
    telephoneForm.bindFromRequest().fold(
      errors => BadRequest(views.html.details.updateMobile(UpdateDetailsVM(errors, request.individualAccount.details))),
      mobile => updateDetails(mobile = Some(mobile))
    )
  }

  private def updateDetails(firstName: Option[String] = None,
                            lastName: Option[String] = None,
                            email: Option[String] = None,
                            phone: Option[String] = None,
                            mobile: Option[String] = None,
                            addressId: Option[Int] = None)
                           (implicit request: BasicAuthenticatedRequest[_]): Future[Result] = {

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

    individualAccountConnector.update(updatedAccount) map { _ => Redirect(routes.UpdatePersonalDetails.show()) }
  }

  private lazy val emailForm = Form(
    mapping(
      "email" -> email.verifying(Constraints.maxLength(150)),
      "confirmedEmail" -> TextMatching("email", "error.emailsMustMatch")
    ) { case (e, _) => e }(email => Some((email, email)))
  )

  private lazy val telephoneForm = Form(
    single(
      "phone" -> nonEmptyText(maxLength = 20)
    )
  )

  private lazy val nameForm = Form(
    mapping(
      "firstName" -> nonEmptyText(maxLength = 100),
      "lastName" -> nonEmptyText(maxLength = 100)
    )(Name.apply)(Name.unapply)
  )

  private lazy val addressForm = Form(
    single("address" -> addressMapping)
  )

  case class Name(firstName: String, lastName: String)

}

case class UpdateDetailsVM(form: Form[_], currentDetails: IndividualDetails)