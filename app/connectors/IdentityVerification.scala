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

import javax.inject.Inject

import config.ApplicationConfig
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.{JsDefined, JsString, JsValue}
import uk.gov.hmrc.play.config.inject.ServicesConfig
import uk.gov.hmrc.play.http._
import config.WSHttp
import scala.concurrent.Future
import uk.gov.hmrc.http.HeaderCarrier

class IdentityVerification @Inject()(serverConfig: ServicesConfig, config: ApplicationConfig, http: WSHttp) {

  val url = serverConfig.baseUrl("identity-verification")

  def verifySuccess(journeyId: String)(implicit hc: HeaderCarrier) = {
    if (config.ivEnabled) {
      http.GET[JsValue](s"$url/mdtp/journey/journeyId/$journeyId") map { r =>
        r \ "result" match {
          case JsDefined(JsString("Success")) => true
          case _ => false
        }
      }
    } else {
      Future.successful(true)
    }
  }
}
