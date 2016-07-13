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

import config.Keystore
import models.{Address, CapacityType, PropertyData}
import play.api.data.{Form, FormError, Forms}
import play.api.data.Forms._
import play.api.mvc.Action
import models.JsonFormats._
import play.api.data.format.Formatter

object Search extends PropertyLinkingController {
  val propertyToClaim = "propertyToClaim"

  def show() = Action { implicit request =>
    Ok(views.html.search(pretendSearchResults))
  }

  def declareCapacity(baRef: String) = Action.async { implicit request =>
    pretendSearchResults.find(_.baRef == baRef) match {
      case Some(pd) => Keystore.cache(propertyToClaim, pd) map { _ => Ok(views.html.declareCapacity(DeclareCapacityVM(declareCapacityForm))) }
      case None => NotFound(views.html.defaultpages.notFound(request, Some(app.Routes)))
    }
  }

  def attemptLink() = Action { implicit request =>
    declareCapacityForm.bindFromRequest().fold(
      errors => BadRequest(views.html.declareCapacity(DeclareCapacityVM(errors))),
      formData => Ok(views.html.defaultpages.todo())
    )
  }

  lazy val pretendSearchResults = Seq(
    PropertyData("1", Address(Seq("1 The Road", "The Town"), "AA11 1AA", true), "World Famous Fish and Chip Shop"),
    PropertyData("2", Address(Seq("An ATM that charges you £10 just to look at it", "Some Road", "Some Town"), "AA11 1AA", false), "ATM"),
    PropertyData("3", Address(Seq("Banky McBankface", "The Town"), "AA11 1AA", true), "Bank")
  )

  implicit val capacityTypeFormatter = new Formatter[CapacityType] {
    override def bind(key: String, data: Map[String, String]) = {
      CapacityType.unapply(data.getOrElse(key, "MISSING")) match {
        case Some(ct) => Right(ct)
        case None => Left(Seq(FormError(key, "error.capacity.noValueSelected")))
      }
    }

    override def unbind(key: String, value: CapacityType) = Map(key -> value.name)
  }

  lazy val declareCapacityForm = Form(mapping(
    "capacity" -> Forms.of[CapacityType],
    "fromDate" -> nonEmptyText,
    "toDate" -> nonEmptyText
  )(CapacityDeclaration.apply)(CapacityDeclaration.unapply))
}

case class DeclareCapacityVM(form: Form[_])

case class CapacityDeclaration(capacity: CapacityType, fromDate: String, toDate: String)