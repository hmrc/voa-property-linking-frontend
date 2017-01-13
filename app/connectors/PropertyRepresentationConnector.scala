/*
 * Copyright 2017 HM Revenue & Customs
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

  def get(representationId: String)(implicit hc: HeaderCarrier): Future[Option[PropertyRepresentation]] = {
    val url = baseUrl + s"/property-representations/$representationId"
    http.GET[Option[PropertyRepresentation]](url)
  }

  def forAgent(agentOrganisationId: Int)(implicit hc: HeaderCarrier): Future[Seq[PropertyRepresentation]] = {
    val url = baseUrl + s"/property-representations/agent/$agentOrganisationId"
    http.GET[Seq[PropertyRepresentation]](url)
  }

  def find(linkId: Int)(implicit hc: HeaderCarrier): Future[Seq[PropertyRepresentation]] = {
    val url = baseUrl + s"/property-representations/linkId/$linkId"
    http.GET[Seq[PropertyRepresentation]](url)
  }

  def create(reprRequest: PropertyRepresentation)(implicit hc: HeaderCarrier): Future[Unit] = {
    val url = baseUrl + s"/property-representations/create"
    http.PUT[PropertyRepresentation, HttpResponse](url, reprRequest) map { _ => () }
  }

  def accept(reprId: String)(implicit hc: HeaderCarrier): Future[Unit] = {
    val url = baseUrl + s"/property-representations/accept/$reprId"
    http.PUT[JsValue, HttpResponse](url, JsNull) map { _ => () }
  }

  def reject(reprId: String)(implicit hc: HeaderCarrier): Future[Unit] = {
    val url = baseUrl + s"/property-representations/reject/$reprId"
    http.PUT[JsValue, HttpResponse](url, JsNull) map { _ => () }
  }

  def update(updated: UpdatedRepresentation)(implicit hc: HeaderCarrier): Future[Unit] = {
    val url = baseUrl + s"/property-representations/update"
    http.PUT[UpdatedRepresentation, HttpResponse](url, updated) map { _ => () }
  }
}
