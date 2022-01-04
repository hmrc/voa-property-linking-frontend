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

package connectors.identityVerificationProxy

import javax.inject.Inject
import models.identityVerificationProxy._
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import scala.concurrent.{ExecutionContext, Future}

class IdentityVerificationProxyConnector @Inject()(serverConfig: ServicesConfig, http: HttpClient)(
      implicit ec: ExecutionContext) {
  private lazy val url = serverConfig.baseUrl("identity-verification-proxy")
  private val path = "identity-verification-proxy/journey"

  def start(journey: Journey)(implicit hc: HeaderCarrier): Future[Link] =
    http.POST[Journey, Link](s"$url/$path/start", journey)

}
