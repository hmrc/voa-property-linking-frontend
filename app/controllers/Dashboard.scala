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
import connectors.ServiceContract.PropertyRepresentation
import session.WithAuthentication
import play.api.mvc.Action
import org.joda.time.DateTime
import play.api.data.Form

import scala.concurrent.Future

object Dashboard extends PropertyLinkingController {
  val propLinkedConnector = Wiring().propertyLinkConnector
  val reprConnector = Wiring().propertyRepresentationConnector
  val propConnector = Wiring().propertyConnector

  def home() = Action { implicit request =>
    Ok(views.html.dashboard.home())
  }

  def manageProperties() = WithAuthentication.async { implicit request =>
    propLinkedConnector.linkedProperties(request.account).flatMap { ps =>
      val added: Seq[Future[PropertyLinkRepresentations]] = ps.added.map { prop =>
        for {
          repr <- reprConnector.get(request.account.companyName, prop.uarn)
          name <- propConnector.find(prop.uarn)
            .flatMap( x => x.map(prop => prop.address.lines.head + ", " + prop.address.postcode).getOrElse("No Address found"))
        } yield
          PropertyLinkRepresentations(name, prop.uarn,
            prop.capacityDeclaration.capacity.name, prop.linkedDate, prop.assessmentYears, repr)
      }
      val pending: Seq[Future[PendingPropertyLinkRepresentations]] = ps.pending.map { prop =>
        reprConnector.get(request.account.companyName, prop.uarn).map { rep =>
          PendingPropertyLinkRepresentations("TODO", prop.uarn,
            prop.capacityDeclaration.capacity.name, prop.linkedDate, rep)
        }
      }
      val fsAdded: Future[Seq[PropertyLinkRepresentations]] = Future.sequence(added)
      val psPending: Future[Seq[PendingPropertyLinkRepresentations]] = Future.sequence(pending)
      for { a <- fsAdded
            p <- psPending
      } yield {
        Ok(views.html.dashboard.manageProperties(ManagePropertiesVM(LinkedPropertiesRepresentations(a, p))))
      }
    }
  }

  case class PropertyLinkRepresentations(name: String, uarn: String, capacity: String, linkedDate: DateTime,
                               assessmentYears: Seq[Int], representations: Seq[PropertyRepresentation])
  case class PendingPropertyLinkRepresentations(name: String, uarn: String, capacity: String,
                                                linkedDate: DateTime, representations: Seq[PropertyRepresentation])
  case class LinkedPropertiesRepresentations(added: Seq[PropertyLinkRepresentations], pending: Seq[PendingPropertyLinkRepresentations])
  case class ManagePropertiesVM(properties: LinkedPropertiesRepresentations)

}

