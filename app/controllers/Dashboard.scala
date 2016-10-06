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

import auth.GGAction
import config.Wiring
import connectors.ServiceContract.PropertyRepresentation
import org.joda.time.DateTime

import scala.concurrent.Future

object Dashboard extends PropertyLinkingController {
  val propLinkedConnector = Wiring().propertyLinkConnector
  val reprConnector = Wiring().propertyRepresentationConnector
  val propConnector = Wiring().propertyConnector

  def home() = GGAction { _ => implicit request =>
    Ok(views.html.dashboard.home())
  }

  def manageProperties() = GGAction.async { ctx => implicit request =>
    propLinkedConnector.linkedProperties(ctx.user.oid).flatMap { ps =>
      val added: Seq[Future[PropertyLinkRepresentations]] = ps.added.map { prop =>
        for {
          repr <- reprConnector.get(ctx.user.oid, prop.uarn)
          name <- propConnector.find(prop.uarn)
            .map( x => x.map(prop => prop.address.lines.head + ", " + prop.address.postcode).getOrElse("No Address found"))
        } yield
          PropertyLinkRepresentations(name, prop.uarn,
            prop.capacityDeclaration.capacity.name, prop.linkedDate, prop.assessmentYears, repr)
      }
      val pending: Seq[Future[PendingPropertyLinkRepresentations]] = ps.pending.map { prop =>
        reprConnector.get(ctx.user.oid, prop.uarn).map { rep =>
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

