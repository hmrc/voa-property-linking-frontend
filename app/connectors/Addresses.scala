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

import com.google.inject.Inject
import models.Address
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.{JsDefined, JsNumber, JsValue}
import uk.gov.hmrc.play.config.inject.ServicesConfig
import uk.gov.hmrc.play.http.{HeaderCarrier, HttpGet, HttpPost}

import scala.concurrent.Future

class Addresses @Inject()(config: ServicesConfig, http: HttpGet with HttpPost) {

  val url = config.baseUrl("property-linking") + "/property-linking/address"

  def findByPostcode(postcode: String)(implicit hc: HeaderCarrier): Future[Seq[Address]] = {
    http.GET[Seq[Address]](url + s"?postcode=$postcode")
  }

  def findById(id: Int)(implicit hc: HeaderCarrier): Future[Option[Address]] = {
    http.GET[Option[Address]](url + s"/$id")
  }

  def create(address: Address)(implicit hc: HeaderCarrier): Future[Int] = {
    http.POST[Address, JsValue](url, address) map { js =>
      js \ "id" match {
        case JsDefined(JsNumber(n)) => n.toInt
        case _ => throw new Exception(s"Unexpected response: $js")
      }
    }
  }
}
