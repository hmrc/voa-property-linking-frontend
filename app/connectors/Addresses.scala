/*
 * Copyright 2022 HM Revenue & Customs
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

import javax.inject.Inject
import models.registration.GroupAccountDetails
import models.{Address, DetailedAddress}
import play.api.libs.json.{JsDefined, JsNumber, JsValue}
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import scala.concurrent.{ExecutionContext, Future}

class Addresses @Inject()(config: ServicesConfig, http: HttpClient)(implicit executionContext: ExecutionContext) {

  val url: String = config.baseUrl("property-linking") + "/property-linking/address"

  def registerAddress(details: GroupAccountDetails)(implicit hc: HeaderCarrier): Future[Long] =
    details.address.addressUnitId match {
      case Some(id) => Future.successful(id)
      case None     => create(details.address)
    }

  def findByPostcode(postcode: String)(implicit hc: HeaderCarrier): Future[Seq[DetailedAddress]] =
    http.GET[Seq[DetailedAddress]](url + s"?postcode=$postcode")

  def findById(id: Long)(implicit hc: HeaderCarrier): Future[Option[Address]] =
    http.GET[Option[Address]](url + s"/$id")

  def create(address: Address)(implicit hc: HeaderCarrier): Future[Long] =
    http.POST[Address, JsValue](url, address) map { js =>
      js \ "id" match {
        case JsDefined(JsNumber(n)) => n.toLong
        case _                      => throw new Exception(s"Unexpected response: $js")
      }
    }
}
