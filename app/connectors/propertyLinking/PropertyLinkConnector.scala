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

import config.Wiring

import scala.collection.mutable.{HashMap => mMap}
import connectors.{PrototypeTestData, ServiceContract}
import connectors.ServiceContract.{LinkedProperties, PendingPropertyLink, PropertyLink}
import controllers.Account
import models.CapacityType
import org.joda.time.DateTime
import serialization.JsonFormats
import serialization.JsonFormats._
import uk.gov.hmrc.http.cache.client.SessionCache
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.http._

import scala.concurrent.{ExecutionContext, Future}



class PropertyLinkConnector(http: HttpGet with HttpPut, cache: mMap[Account, LinkedProperties])(implicit ec: ExecutionContext)
  extends ServicesConfig with JsonHttpReads {
  implicit val fmt = JsonFormats.linkedProperties
  private val url = baseUrl("property-linking") + "/property-links"

  implicit val rds: HttpReads[Unit] = new HttpReads[Unit] {
    override def read(method: String, url: String, response: HttpResponse): Unit = Unit
  }

  // TODO - will not be passing in accountId once auth solution is confirmed
  def linkToProperty(uarn: String, billingAuthorityRef: String, account: Account, request: ServiceContract.LinkToProperty, submissionId: String)
                    (implicit hc: HeaderCarrier): Future[Unit] =
    http.PUT[ServiceContract.LinkToProperty, Unit](s"$url/$billingAuthorityRef/${account.companyName}/$submissionId", request).recoverWith {
      // TODO - use caching for temporary prototype persistence
      case _ =>
        val prop = PrototypeTestData.pretendSearchResults.find(_.billingAuthorityReference == billingAuthorityRef).get
        if (PrototypeTestData.canBeLinkedTo(billingAuthorityRef)) {
          val x = PropertyLink(
            prop.address.lines.head + ", " + prop.address.postcode, uarn, billingAuthorityRef, request.capacityDeclaration.capacity.name,
            DateTime.now, Seq(2009, 20015)
          )
          linkedProperties(account).flatMap(ps =>
            Future.successful(Wiring().tmpInMemoryLinkedPropertyDb(account) = ps.copy(added=ps.added :+ x)))
            .map(_ => ())
        } else {
          val x = PendingPropertyLink(
            prop.address.lines.head + ", " + prop.address.postcode, uarn,
            billingAuthorityRef, request.capacityDeclaration.capacity.name, DateTime.now
          )
          linkedProperties(account).flatMap(ps =>
            Future.successful(Wiring().tmpInMemoryLinkedPropertyDb(account)= ps.copy(pending = ps.pending :+ x)))
            .map(_ => ())
        }
    }

  def linkedProperties(account: Account)(implicit hc: HeaderCarrier): Future[ServiceContract.LinkedProperties] =
    http.GET[ServiceContract.LinkedProperties](s"$url/${account.companyName}").recoverWith {
      // TODO -  use caching for temporary prototype persistence
      case _ => {
        val ret = Wiring().tmpInMemoryLinkedPropertyDb.getOrElse(account, LinkedProperties(Seq.empty, Seq.empty))
        //cache.fetchAndGetEntry[LinkedProperties]("linkedproperties").map(_.getOrElse(LinkedProperties(Seq.empty, Seq.empty)))
        Future.successful(ret)
      }
    }
}