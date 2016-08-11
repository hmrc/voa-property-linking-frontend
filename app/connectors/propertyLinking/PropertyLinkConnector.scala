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

package connectors.propertyLinking

import connectors.PrototypeTestData
import connectors.propertyLinking.ServiceContract.{LinkedProperties, PendingPropertyLink, PropertyLink}
import models.CapacityType
import org.joda.time.DateTime
import serialization.JsonFormats
import serialization.JsonFormats._
import uk.gov.hmrc.http.cache.client.SessionCache
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.http._

import scala.concurrent.{ExecutionContext, Future}

object ServiceContract {
  case class LinkToProperty(capacityDeclaration: CapacityDeclaration)
  case class CapacityDeclaration(capacity: CapacityType, fromDate: DateTime, toDate: Option[DateTime] = None)

  case class PropertyLink(name: String, uarn: String, billingAuthorityReference: String, capacity: String, linkedDate: DateTime, assessmentYears: Seq[Int])
  case class PendingPropertyLink(name: String, uarn: String, billingAuthorityReference: String, capacity: String, linkedDate: DateTime)
  case class LinkedProperties(added: Seq[PropertyLink], pending: Seq[PendingPropertyLink])
  case class PropertyRepresentation(representationId: String, agentId: String, userId: String, uarn: String,
                                    canCheck: Boolean, canChallenge: Boolean, pending: Boolean)

}

class PropertyLinkConnector(http: HttpGet with HttpPut, cache: SessionCache)(implicit ec: ExecutionContext) extends ServicesConfig with JsonHttpReads {
  implicit val fmt = JsonFormats.linkedProperties
  private val url = baseUrl("property-linking") + "/property-links"

  implicit val rds: HttpReads[Unit] = new HttpReads[Unit] {
    override def read(method: String, url: String, response: HttpResponse): Unit = Unit
  }

  // TODO - will not be passing in accountId once auth solution is confirmed
  def linkToProperty(uarn: String, billingAuthorityRef: String, accountId: String, request: ServiceContract.LinkToProperty, submissionId: String)
                    (implicit hc: HeaderCarrier): Future[Unit] =
    http.PUT[ServiceContract.LinkToProperty, Unit](s"$url/$billingAuthorityRef/$accountId/$submissionId", request).recoverWith {
      // TODO - use caching for temporary prototype persistence
      case _ =>
        val prop = PrototypeTestData.pretendSearchResults.find(_.billingAuthorityReference == billingAuthorityRef).get
        if (PrototypeTestData.canBeLinkedTo(billingAuthorityRef)) {
          val x = PropertyLink(
            prop.address.lines.head + ", " + prop.address.postcode, uarn, billingAuthorityRef, request.capacityDeclaration.capacity.name,
            DateTime.now, Seq(2009, 20015)
          )
          linkedProperties(accountId).flatMap(ps => cache.cache("linkedproperties", ps.copy(added = ps.added :+ x))).map(_ => ())
        } else {
          val x = PendingPropertyLink(
            prop.address.lines.head + ", " + prop.address.postcode, uarn, billingAuthorityRef, request.capacityDeclaration.capacity.name, DateTime.now
          )
          linkedProperties(accountId).flatMap(ps => cache.cache("linkedproperties", ps.copy(pending = ps.pending :+ x))).map(_ => ())
        }
    }

  def linkedProperties(accountId: String)(implicit hc: HeaderCarrier): Future[ServiceContract.LinkedProperties] =
    http.GET[ServiceContract.LinkedProperties](s"$url/$accountId").recoverWith {
      // TODO -  use caching for temporary prototype persistence
      case _ => cache.fetchAndGetEntry[LinkedProperties]("linkedproperties").map(_.getOrElse(LinkedProperties(Seq.empty, Seq.empty)))
    }
}