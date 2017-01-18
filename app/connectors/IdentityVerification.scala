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

import config.ApplicationConfig
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.{JsDefined, JsString, JsValue}
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.http._

import scala.concurrent.Future

class IdentityVerification(http: HttpGet with HttpPost) extends ServicesConfig {

  val url = baseUrl("identity-verification")
  private val successUrl = ApplicationConfig.baseUrl + controllers.routes.IdentityVerification.restoreSession.url
  private val failureUrl = ApplicationConfig.baseUrl + controllers.routes.IdentityVerification.fail.url

  def verifyUrl = s"$url/mdtp/confirm?origin=voa&completionURL=$successUrl&failureURL=$failureUrl&confidenceLevel=200"

  def verifySuccess(journeyId: String)(implicit hc: HeaderCarrier) = {
    if (ApplicationConfig.ivEnabled) {
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
