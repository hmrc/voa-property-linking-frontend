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

package connectors

import config.ApplicationConfig
import javax.inject.Inject
import play.api.libs.json.JsValue
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.play.bootstrap.http.HttpClient

import scala.concurrent.{ExecutionContext, Future}

class IdentityVerification @Inject()(serverConfig: ServicesConfig, config: ApplicationConfig, http: HttpClient)(implicit val executionContext: ExecutionContext) {

  def verifySuccess(journeyId: String)(implicit hc: HeaderCarrier): Future[Boolean] = {
    if (config.ivEnabled) {
      http.GET[JsValue](s"${config.ivBaseUrl}/mdtp/journey/journeyId/$journeyId").map(r => (r \ "result").asOpt[String].contains("Success"))
    } else {
      Future.successful(true)
    }
  }
}
