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
import connectors.{LinkedProperties, PropertyRepresentation}
import models.CapacityType
import org.joda.time.DateTime
import uk.gov.hmrc.play.http.HeaderCarrier

import scala.concurrent.Future

trait Dashboard extends PropertyLinkingController {
  val propLinkedConnector = Wiring().propertyLinkConnector
  val reprConnector = Wiring().propertyRepresentationConnector
  val propConnector = Wiring().propertyConnector
  val individuals = Wiring().individualAccountConnector
  val groups = Wiring().groupAccountConnector
  val userDetails = Wiring().userDetailsConnector
  val auth = Wiring().authConnector
  val ggAction = Wiring().ggAction

  def home() = ggAction.async { ctx => implicit request =>
    for {
      userId <- auth.getExternalId(ctx)
      groupId <- userDetails.getGroupId(ctx)
      individualAccount <- individuals.get(userId)
      groupAccount <- groups.get(groupId)
    } yield {
      (individualAccount, groupAccount) match {
        case (Some(i), Some(g)) if g.isAgent => Redirect(agent.routes.Dashboard.home())
        case (Some(i), Some(g)) => Ok(views.html.dashboard.home())
        case (None, _) => Redirect(routes.CreateIndividualAccount.show)
      }
    }
  }

  def manageProperties() = ggAction.async { ctx => implicit request =>
    for {
      groupId <- userDetails.getGroupId(ctx)
      props <- propLinkedConnector.linkedProperties(groupId)
      added <- propertyLinkRepresentations(props, groupId)
      pending <- pendingPropertyLinkRepresentations(props, groupId)
    } yield {
      Ok(views.html.dashboard.manageProperties(ManagePropertiesVM(LinkedPropertiesRepresentations(added, pending))))
    }
  }

  private def pendingPropertyLinkRepresentations(lps: LinkedProperties, id: String)
                                                (implicit hc: HeaderCarrier): Future[Seq[PendingPropertyLinkRepresentations]] = {
    val pending: Seq[Future[PendingPropertyLinkRepresentations]] = lps.pending.map { p =>
      for {
        reps <- reprConnector.get(id, p.uarn)
        shortAddress <- shortAddress(p.uarn)
      } yield {
        PendingPropertyLinkRepresentations(shortAddress, p.uarn, p.capacityDeclaration.capacity, p.linkedDate, reps)
      }
    }
    Future.sequence(pending)
  }

  private def propertyLinkRepresentations(lps: LinkedProperties, id: String)(implicit hc: HeaderCarrier): Future[Seq[PropertyLinkRepresentations]] = {
    Future.sequence(lps.added.map { p =>
      for {
        reps <- reprConnector.get(id, p.uarn)
        shortAddress <- shortAddress(p.uarn)
      } yield {
        PropertyLinkRepresentations(shortAddress, p.uarn, p.capacityDeclaration.capacity, p.linkedDate, reps)
      }
    })
  }

  private def shortAddress(uarn: Long)(implicit hc: HeaderCarrier) = propConnector.find(uarn) map {
    case Some(p) => p.address.line1 + ", " + p.address.postcode
    case None => "No address found"
  }
}

object Dashboard extends Dashboard

case class ManagePropertiesVM(properties: LinkedPropertiesRepresentations)

case class PropertyLinkRepresentations(name: String, uarn: Long, capacity: CapacityType, linkedDate: DateTime,
                                       representations: Seq[PropertyRepresentation])

case class PendingPropertyLinkRepresentations(name: String, uarn: Long, capacity: CapacityType,
                                              linkedDate: DateTime, representations: Seq[PropertyRepresentation])

case class LinkedPropertiesRepresentations(added: Seq[PropertyLinkRepresentations], pending: Seq[PendingPropertyLinkRepresentations])
