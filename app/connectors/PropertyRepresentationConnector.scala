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

import ServiceContract.PropertyRepresentation
import serialization.JsonFormats._
import uk.gov.hmrc.http.cache.client.{CacheMap, SessionCache}
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.http._

import scala.concurrent.{ExecutionContext, Future}

class PropertyRepresentationConnector(http: HttpGet with HttpPut, cache: SessionCache)(implicit ec: ExecutionContext) extends ServicesConfig {
  lazy val baseUrl: String = baseUrl("property-representations") + s"/property-linking"

  implicit val rds: HttpReads[Unit] = new HttpReads[Unit] {
    override def read(method: String, url: String, response: HttpResponse): Unit = Unit
  }

  def get(userId: String, uarn: String)(implicit hc: HeaderCarrier): Future[Seq[PropertyRepresentation]] = {
    val url = baseUrl + s"/property-representations/${userId}/${uarn}"
    http.GET[Seq[PropertyRepresentation]](url)
      .recoverWith {
        case _ => existingPropertyRepresentations
      }
  }

  def create(reprRequest: PropertyRepresentation)(implicit hc: HeaderCarrier): Future[Unit] = {
    val url = baseUrl + s"TODO"
    http.PUT[PropertyRepresentation, Unit](url, reprRequest)
      .recoverWith{ case _ =>
        existingPropertyRepresentations flatMap (reprs => {
            cache.cache("propertyrepresentations", reprs ++ Seq(reprRequest)).map(_ => ())
          }
        )
    }
  }
  def update(reprRequest: PropertyRepresentation)(implicit hc: HeaderCarrier): Future[Unit] = {
    val url = baseUrl + s"TODO"
    http.PUT[PropertyRepresentation, Unit](url, reprRequest)
      .recoverWith{ case _ =>
        existingPropertyRepresentations flatMap (reprs => {
          val updatedReprs = reprs.filter(repr => repr.representationId != reprRequest.representationId) ++ Seq(reprRequest)
          cache.cache("propertyrepresentations", updatedReprs).map(_ => ())
        })
      }
  }

  def existingPropertyRepresentations(implicit hc: HeaderCarrier) = {
    cache.fetchAndGetEntry[Seq[PropertyRepresentation]]("propertyrepresentations")
      .map( _.getOrElse(Seq[PropertyRepresentation]() ))

  }
}

