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

import models.RatesBill
import play.api.libs.json._
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.http.{HeaderCarrier, HttpPut}
import serialization.JsonFormats.ratesBillCheckFormat

import scala.concurrent.{ExecutionContext, Future}

class RatesBillVerificationConnector(http: HttpPut)(implicit ec: ExecutionContext) extends ServicesConfig {
  private lazy val base = s"${baseUrl("rates-bill-verification")}/rates-bill-checks"

  def verify(billingAuthorityReference: String, ratesBill: RatesBill)(implicit hc: HeaderCarrier): Future[RatesBillCheck] =
    http.PUT[JsValue, RatesBillCheck](
      s"$base/$billingAuthorityReference/${java.util.UUID.randomUUID.toString}",
      Json.toJson(JsObject(Seq("data" -> JsString(ratesBill.toB64String))))
    ).recover { case _ =>
      RatesBillCheck(true)
    }
}

case class RatesBillCheck(isValid: Boolean)
