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

import form.Mappings._
import config.Wiring
import models.Address
import play.api.data.{Form, Mapping}
import play.api.data.Forms._
import play.api.data.validation.Constraints
import utils.Conditionals.IfCondition

class DetailsController extends PropertyLinkingController {

  val authenticated = Wiring().authenticated
  val addressesConnector = Wiring().addresses
  val individualAccountConnector = Wiring().individualAccountConnector

  def show() = authenticated { implicit request =>
    for {
      maybeAddress <- addressesConnector.findById(request.individualAccount.details.addressId)
    } yield {
      maybeAddress.map( address =>
        Ok(views.html.details.details(request.individualAccount, request.organisationAccount, address))
      )
    }.getOrElse(BadRequest)
  }

  def personalEmail() = authenticated { implicit request =>
    Ok(views.html.details.email(PersonalEmailVM(emailForm)))
  }

  def personalEmailSubmit() = authenticated { implicit request =>
    emailForm.bindFromRequest().fold(
    errors => BadRequest(views.html.details.email(PersonalEmailVM(errors))),
    formData => {
      val updatedDetails = request.individualAccount.details.copy(email = formData)
      val updatedAccount = request.individualAccount.copy(details = updatedDetails)
      individualAccountConnector.update(updatedAccount.individualId, updatedAccount.toIndividualAccount()) map { _ =>
        Redirect(routes.DetailsController.show())
      }
    }
    )
  }

  def personalAddress() = authenticated { implicit request =>
    Ok(views.html.details.personalAddress(PersonalAddressVM(addressForm)))
  }

  def personalAddressSubmit() = authenticated { implicit request =>
    addressForm.bindFromRequest().fold(
      errors => BadRequest(views.html.details.personalAddress(PersonalAddressVM(errors))),
      formData => {
        formData.address.addressUnitId match {
          case Some(id) =>  {
            val updatedDetails = request.individualAccount.details.copy(addressId = id )
            val updatedAccount = request.individualAccount.copy(details = updatedDetails)
            individualAccountConnector.update(updatedAccount.individualId, updatedAccount.toIndividualAccount()) map { _ =>
              Redirect(routes.DetailsController.show())
            }
          }
          case None => {
            for {
              id <- addressesConnector.create(formData.address)
              updatedDetails = request.individualAccount.details.copy(addressId = id )
              updatedAccount = request.individualAccount.copy(details = updatedDetails)
              _ <- individualAccountConnector.update(updatedAccount.individualId, updatedAccount.toIndividualAccount())
            } yield {
              Redirect(routes.DetailsController.show() )
            }
          }
        }
      }
    )
  }

  def personalTelephone() = authenticated { implicit request =>
    Ok(views.html.details.telephone(PersonalTelephoneVM(telephoneForm)))
  }

  def personalTelephoneSubmit() = authenticated { implicit request =>
    telephoneForm.bindFromRequest().fold(
      errors => BadRequest(views.html.details.telephone(PersonalTelephoneVM(errors))),
      formData => {
        val updatedDetails = request.individualAccount.details.copy(phone1 = formData)
        val updatedAccount = request.individualAccount.copy(details = updatedDetails)
        individualAccountConnector.update(updatedAccount.individualId, updatedAccount.toIndividualAccount()) map { _ =>
          Redirect(routes.DetailsController.show())
        }
      }
    )
  }

  def personalName() = authenticated { implicit request =>
    Ok(views.html.details.name(PersonalNameVM(nameForm)))
  }

  def personalNameSubmit() = authenticated { implicit request =>
    nameForm.bindFromRequest().fold(
      errors => BadRequest(views.html.details.name(PersonalNameVM(errors))),
      formData => {
        val updatedDetails = request.individualAccount.details.copy(firstName = formData.firstName, lastName = formData.lastName)
        val updatedAccount = request.individualAccount.copy(details = updatedDetails)
        individualAccountConnector.update(updatedAccount.individualId, updatedAccount.toIndividualAccount()) map { _ =>
          Redirect(routes.DetailsController.show())
        }
      }
    )
  }

  val emailForm = Form(
    single(
      "email" -> email.verifying(Constraints.maxLength(150))
    )
  )

  val telephoneForm = Form(
    single(
      "telephone" -> nonEmptyText(maxLength = 20)
    )
  )

  val nameForm = Form(
    mapping(
      "firstName" -> nonEmptyText(maxLength = 100),
      "lastName" -> nonEmptyText(maxLength = 100)
    )(Name.apply)(Name.unapply)
  )

  lazy val addressForm = Form(mapping(
    "address" -> address
  )(PersonalAddress.apply)(PersonalAddress.unapply))

  case class PersonalAddress(address: Address)

  case class Name(firstName: String, lastName: String)
}

case class PersonalEmailVM(form: Form[_])
case class PersonalTelephoneVM(form: Form[_])
case class PersonalNameVM(form: Form[_])
case class PersonalAddressVM(form: Form[_])
