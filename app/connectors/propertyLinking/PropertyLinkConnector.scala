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

import connectors.{CapacityDeclaration, LinkedProperties, PropertyLink, RequestFlag}
import org.joda.time.DateTime
import serialization.JsonFormats._
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.http._

import scala.concurrent.{ExecutionContext, Future}

class PropertyLinkConnector(http: HttpGet with HttpPut with HttpPost)(implicit ec: ExecutionContext)
  extends ServicesConfig with JsonHttpReads {
  lazy val baseUrl: String = baseUrl("property-representations") + s"/property-linking"

  implicit val rds: HttpReads[Unit] = new HttpReads[Unit] {
    override def read(method: String, url: String, response: HttpResponse): Unit = Unit
  }

  def linkToProperty(uarn: String, billingAuthorityRef: String, userId: String,
                     capacityDeclaration: CapacityDeclaration, submissionId: String, flag: RequestFlag)
                    (implicit hc: HeaderCarrier): Future[Unit] = {
    val url = baseUrl + s"/property-links/$uarn/$userId/$submissionId"
    val request = PropertyLink(uarn, userId, capacityDeclaration,
      DateTime.now, Seq(2017), true, flag)
    http.POST[PropertyLink, HttpResponse](s"$url", request) map { _ => () }
  }

  def linkedProperties(id: String)(implicit hc: HeaderCarrier): Future[LinkedProperties] = {
    val url = baseUrl + s"/property-links/$id"
    http.GET[Seq[PropertyLink]](url).map(seq => {
      val tmp:(Seq[PropertyLink], Seq[PropertyLink]) = seq.partition( !_.pending)
       LinkedProperties(tmp._1, tmp._2)
    })
  }
}
