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

import connectors._
import models.{LinkBasis, Property}
import org.joda.time.DateTime
import serialization.JsonFormats._
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.http._

import scala.concurrent.{ExecutionContext, Future}

class PropertyLinkConnector(http: HttpGet with HttpPut with HttpPost)(implicit ec: ExecutionContext)
  extends ServicesConfig {
  lazy val baseUrl: String = baseUrl("property-representations") + s"/property-linking"

  def get(linkId: String)(implicit hc: HeaderCarrier): Future[Option[PropertyLink]] = {
    val url = baseUrl + s"/property-link/$linkId"
    http.GET[Option[PropertyLink]](url)
  }

  def linkToProperty(property: Property, groupId: String,
                     capacityDeclaration: CapacityDeclaration, linkId: String, basis: LinkBasis,
                     fileInfo: Option[FileInfo])
                    (implicit hc: HeaderCarrier): Future[Unit] = {
    val url = baseUrl + s"/property-links/$linkId"
    val request = PropertyLinkRequest(property.uarn, groupId, capacityDeclaration,
      DateTime.now, basis, property.specialCategoryCode, property.description, property.bulkClassIndicator, fileInfo)
    http.POST[PropertyLinkRequest, HttpResponse](s"$url", request) map { _ => () }
  }

  def linkedProperties(groupId: String)(implicit hc: HeaderCarrier): Future[LinkedProperties] = {
    val url = baseUrl + s"/property-links/$groupId"
    http.GET[Seq[PropertyLink]](url).map(seq => {
      val tmp: (Seq[PropertyLink], Seq[PropertyLink]) = seq.partition(!_.pending)
      LinkedProperties(tmp._1, tmp._2)
    })
  }
}
