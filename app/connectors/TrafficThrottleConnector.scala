/*
 * Copyright 2018 HM Revenue & Customs
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

import uk.gov.hmrc.play.config.inject.ServicesConfig
import config.WSHttp
import scala.concurrent.{ExecutionContext, Future}
import uk.gov.hmrc.http.HeaderCarrier

class TrafficThrottleConnector @Inject()(serverConfig: ServicesConfig, http: WSHttp)(implicit ec: ExecutionContext) {
  val trafficRouter = "voa-traffic-throttle"

  lazy val serviceUrl = s"${serverConfig.baseUrl(trafficRouter)}/$trafficRouter"

  def isThrottled(route: String)(implicit hc: HeaderCarrier): Future[Boolean] = {
    val url = s"$serviceUrl/cca/$route/throttled"

    http.GET[Boolean](url)
  }

}
