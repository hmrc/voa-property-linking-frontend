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

import models.CapacityType
import org.joda.time.DateTime
import serialization.JsonFormats._
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.http.{HeaderCarrier, HttpPut, HttpReads, HttpResponse}

import scala.concurrent.{ExecutionContext, Future}

object ServiceContract {
  case class LinkToProperty(capacityDeclaration: CapacityDeclaration)
  case class CapacityDeclaration(capacity: CapacityType, fromDate: DateTime, toDate: Option[DateTime] = None)
}

class PropertyLinkConnector(http: HttpPut)(implicit ec: ExecutionContext) extends ServicesConfig {
  private val url = baseUrl("property-linking")

  implicit val rds: HttpReads[Unit] = new HttpReads[Unit] {
    override def read(method: String, url: String, response: HttpResponse): Unit = Unit
  }

  // TODO - will not be passing in accountId once auth solution is confirmed
  def linkToProperty(billingAuthorityRef: String, accountId: String, request: ServiceContract.LinkToProperty, submissionId: String)
                    (implicit hc: HeaderCarrier): Future[Unit] =
    http.PUT[ServiceContract.LinkToProperty, Unit](s"$url/property-links/$billingAuthorityRef/$accountId/$submissionId", request).recoverWith {
      // TODO - ignore errors until the backend is hooked up so we can demo frontend
      case _ => Future.successful(Unit)
    }


}