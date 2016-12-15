/*
 * Copyright 2016 HM Revenue & Customs
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

import config.Wiring
import models.{IndividualAccount, SimpleAddress}
import play.api.data.Forms._
import play.api.data.{Form, Mapping}
import form.Mappings._
import form.TextMatching
import play.api.data.validation.Constraints
import views.helpers.Errors

trait CreateGroupAccount extends PropertyLinkingController {
  val groups = Wiring().groupAccountConnector
  val individuals = Wiring().individualAccountConnector
  val userDetails = Wiring().userDetailsConnector
  val auth = Wiring().authConnector
  val ggAction = Wiring().ggAction
  val keystore = Wiring().sessionCache

  def show = ggAction { _ => implicit request =>
    Ok(views.html.createAccount.group(form))
  }

  def submit = ggAction.async { ctx => implicit request =>
    form.bindFromRequest().fold(
      errors => BadRequest(views.html.createAccount.group(errors)),
      formData => for {
        groupId <- userDetails.getGroupId(ctx)
        userId <- auth.getExternalId(ctx)
        details <- keystore.getIndividualDetails
        id <- groups.create(groupId, formData)
        journeyId = request.session.get("journeyId").getOrElse("no-id")
        _ <- individuals.create(IndividualAccount(userId, journeyId, id, details))
      } yield {
        Ok(views.html.createAccount.confirmation())
      }
    )
  }

  lazy val keys = new {
    val companyName = "companyName"
    val address = "address"
    val email = "businessEmail"
    val confirmEmail = "confirmedEmail"
    val phone = "businessPhone"
    val isSmallBusiness = "isSmallBusiness"
    val isAgent = "isAgent"
  }

  lazy val form = Form(mapping(
    keys.companyName -> nonEmptyText(maxLength = 45),
    keys.address -> address,
    keys.email -> email.verifying(Constraints.maxLength(45)),
    keys.confirmEmail -> TextMatching(keys.email, Errors.emailsMustMatch),
    keys.phone -> nonEmptyText(maxLength = 20),
    keys.isSmallBusiness -> mandatoryBoolean,
    keys.isAgent -> mandatoryBoolean
  )(GroupAccountDetails.apply)(GroupAccountDetails.unapply))

  implicit def vm(form: Form[_]): CreateGroupAccountVM = CreateGroupAccountVM(form)
}

object CreateGroupAccount extends CreateGroupAccount

case class CreateGroupAccountVM(form: Form[_])

case class GroupAccountDetails(companyName: String, address: SimpleAddress, email: String, confirmedEmail: String,
                               phone: String, isSmallBusiness: Boolean, isAgent: Boolean)
