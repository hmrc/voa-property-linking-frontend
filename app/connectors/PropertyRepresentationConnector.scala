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

package connectors

import play.api.libs.json.{JsNull, JsValue}
import serialization.JsonFormats._
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.http._

import scala.concurrent.{ExecutionContext, Future}

class PropertyRepresentationConnector(http: HttpGet with HttpPut)(implicit ec: ExecutionContext)
  extends ServicesConfig {
  lazy val baseUrl: String = baseUrl("property-representations") + s"/property-linking"

  implicit val rds: HttpReads[Unit] = new HttpReads[Unit] {
    override def read(method: String, url: String, response: HttpResponse): Unit = Unit
  }

  def get(userId: String, uarn: String)(implicit hc: HeaderCarrier): Future[Seq[PropertyRepresentation]] = {
    val url = baseUrl + s"/property-representations/$userId/$uarn"
    http.GET[Seq[PropertyRepresentation]](url)
  }

  def forAgent(agentId: String)(implicit hc: HeaderCarrier): Future[Seq[PropertyRepresentation]] = {
    val url = baseUrl + s"/property-representations/$agentId"
    http.GET[Seq[PropertyRepresentation]](url)
  }

  def create(reprRequest: PropertyRepresentation)(implicit hc: HeaderCarrier): Future[Unit] = {
    val url = baseUrl + s"/property-representations/create"
    http.PUT[PropertyRepresentation, Unit](url, reprRequest)
  }

  def accept(reprId: String)(implicit hc: HeaderCarrier): Future[Unit] = {
    val url = baseUrl + s"/property-representations/accept/$reprId"
    http.PUT[JsValue, Unit](url, JsNull)
  }

  def reject(reprId: String)(implicit hc: HeaderCarrier): Future[Unit] = {
    val url = baseUrl + s"/property-representations/reject/$reprId"
    http.PUT[JsValue, Unit](url, JsNull)
  }

  def update(reprRequest: PropertyRepresentation)(implicit hc: HeaderCarrier): Future[Unit] = {
    val url = baseUrl + s"/property-representations/update"
    http.PUT[PropertyRepresentation, Unit](url, reprRequest)
  }

}

