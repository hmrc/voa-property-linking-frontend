/*
 * Copyright 2019 HM Revenue & Customs
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

package connectors.identityVerificationProxy

import javax.inject.Inject

import models.IVDetails
import models.identityVerificationProxy._
import uk.gov.hmrc.play.config.inject.ServicesConfig
import uk.gov.hmrc.play.frontend.auth.connectors.domain.ConfidenceLevel
import uk.gov.hmrc.play.http._
import config.WSHttp

import scala.concurrent.{ExecutionContext, Future}
import uk.gov.hmrc.http.HeaderCarrier

//completionURL: String, failureURL:  String, userData: IVDetails
class IdentityVerificationProxyConnector @Inject()(serverConfig: ServicesConfig, http: WSHttp)(implicit ec: ExecutionContext) {
  private lazy val url = serverConfig.baseUrl("identity-verification-proxy")
  private val path = "identity-verification-proxy/journey"

  def start(journey: Journey)(implicit hc: HeaderCarrier): Future[Link] = {
    http.POST[Journey, Link](s"$url/$path/start", journey)
  }
}
