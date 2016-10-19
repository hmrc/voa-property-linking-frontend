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
import models.{Address, GroupAccount, IndividualAccount}
import play.api.data.Forms._
import play.api.data.{Form, Mapping}

trait CreateGroupAccount extends PropertyLinkingController {
  val groups = Wiring().groupAccountConnector
  val individuals = Wiring().individualAccountConnector
  val userDetails = Wiring().userDetailsConnector
  val auth = Wiring().authConnector
  val ggAction = Wiring().ggAction

  def show = ggAction { _ => implicit request =>
    Ok(views.html.createAccount.group(form))
  }

  def submit = ggAction.async { ctx => implicit request =>
    form.bindFromRequest().fold(
      errors => BadRequest(views.html.createAccount.group(errors)),
      formData => for {
        groupId <- userDetails.getGroupId(ctx)
        userId <- auth.getInternalId(ctx)
        _ <- groups.create(groupId, formData)
        _ <- individuals.create(IndividualAccount(userId, groupId))
      } yield {
        Redirect(routes.Dashboard.home)
      }
    )
  }

  lazy val keys = new {
    val companyName = "companyName"
    val address = "address"
    val email = "email"
    val phone = "phone"
    val isSmallBusiness = "isSmallBusiness"
    val isAgent = "isAgent"
  }

  lazy val form = Form(mapping(
    keys.companyName -> nonEmptyText,
    keys.address -> address,
    keys.email -> email,
    keys.phone -> nonEmptyText,
    keys.isSmallBusiness -> mandatoryBoolean,
    keys.isAgent -> mandatoryBoolean
  )(GroupAccountDetails.apply)(GroupAccountDetails.unapply))

  def address: Mapping[Address] = mapping(
    "line1" -> nonEmptyText,
    "line2" -> text,
    "line3" -> text,
    "postcode" -> nonEmptyText
  )(Address.apply)(Address.unapply)

  val mandatoryBoolean: Mapping[Boolean] = optional(boolean).verifying("error.booleanMissing", _.isDefined).transform(_.get, Some(_))

  implicit def vm(form: Form[_]): CreateGroupAccountVM = CreateGroupAccountVM(form)
}

object CreateGroupAccount extends CreateGroupAccount

case class CreateGroupAccountVM(form: Form[_])

case class GroupAccountDetails(companyName: String, address: Address, email: String, phone: String, isSmallBusiness: Boolean, isAgent: Boolean)
