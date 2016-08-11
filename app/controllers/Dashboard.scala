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
import connectors.propertyLinking.ServiceContract.PropertyRepresentation
import session.WithAuthentication
import play.api.mvc.Action
import org.joda.time.DateTime
import play.api.data.Form

import scala.concurrent.Future

object Dashboard extends PropertyLinkingController {
  val connector = Wiring().propertyLinkConnector
  val reprConnector = Wiring().propertyRepresentationConnector

  def home() = Action { implicit request =>
    Ok(views.html.dashboard.home())
  }

  def manageProperties() = WithAuthentication.async { implicit request =>

    connector.linkedProperties(request.accountId).map { ps =>
      val added = ps.added.map(p=> PropertyLinkRepresentations(p.name, p.billingAuthorityReference, p.capacity, p.linkedDate,
                                                               p.assessmentYears, Seq(PropertyRepresentation("a", "b", "c", "d", true, true, true))))
      val pending = ps.pending.map(p=> PendingPropertyLinkRepresentations(p.name, p.billingAuthorityReference, p.capacity, p.linkedDate, Seq()))
      Ok(views.html.dashboard.manageProperties(ManagePropertiesVM(LinkedPropertiesRepresentations(added, pending))))
    }
  }

  case class PropertyLinkRepresentations(name: String, billingAuthorityReference: String, capacity: String, linkedDate: DateTime,
                               assessmentYears: Seq[Int], representations: Seq[PropertyRepresentation])
  case class PendingPropertyLinkRepresentations(name: String, billingAuthorityReference: String, capacity: String,
                                                linkedDate: DateTime, representations: Seq[PropertyRepresentation])
  case class LinkedPropertiesRepresentations(added: Seq[PropertyLinkRepresentations], pending: Seq[PendingPropertyLinkRepresentations])
  case class ManagePropertiesVM(properties: LinkedPropertiesRepresentations)

}

