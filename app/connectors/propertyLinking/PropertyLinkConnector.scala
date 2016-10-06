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

import connectors.{PrototypeTestData, ServiceContract}
import connectors.ServiceContract.{LinkedProperties, PropertyLink}
import controllers.Account
import org.joda.time.DateTime
import serialization.JsonFormats
import serialization.JsonFormats._
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.http._

import scala.concurrent.{ExecutionContext, Future}



class PropertyLinkConnector(http: HttpGet with HttpPut with HttpPost)(implicit ec: ExecutionContext)
  extends ServicesConfig with JsonHttpReads {
  implicit val fmt = JsonFormats.linkedProperties
  lazy val baseUrl: String = baseUrl("property-representations") + s"/property-linking"

  implicit val rds: HttpReads[Unit] = new HttpReads[Unit] {
    override def read(method: String, url: String, response: HttpResponse): Unit = Unit
  }

  // TODO - will not be passing in accountId once auth solution is confirmed
  def linkToProperty(uarn: String, billingAuthorityRef: String, account: Account, linkToProperty: ServiceContract.LinkToProperty, submissionId: String)
                    (implicit hc: HeaderCarrier): Future[Unit] = {
    val url = baseUrl + s"/property-links/$uarn/${account.companyName}/$submissionId"
    val request = PropertyLink(uarn, account.companyName, linkToProperty.capacityDeclaration,
      DateTime.now, Seq(2017), true )
    http.POST[ServiceContract.PropertyLink, Unit](s"$url", request)
  }

  def linkedProperties(account: Account)(implicit hc: HeaderCarrier): Future[ServiceContract.LinkedProperties] = {
    val url = baseUrl + s"/property-links/${account.companyName}"
    http.GET[Seq[PropertyLink]](url).map(seq => {
      val tmp:(Seq[PropertyLink], Seq[PropertyLink]) = seq.partition( !_.pending)
       LinkedProperties(tmp._1, tmp._2)
    })
  }
}
