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

import config.ApplicationConfig
import javax.inject.Inject
import models.identityVerificationProxy.IvResult
import models.identityVerificationProxy.IvResult.IvFailure
import play.api.libs.json.{JsObject, JsValue}
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import scala.concurrent.{ExecutionContext, Future}

class IdentityVerificationConnector @Inject()(
      serverConfig: ServicesConfig,
      config: ApplicationConfig,
      http: HttpClient
)(implicit val executionContext: ExecutionContext) {

  val baseUrl = serverConfig.baseUrl("identity-verification")

  def verifySuccess(journeyId: String)(implicit hc: HeaderCarrier): Future[Boolean] =
    if (config.ivEnabled) {
      http
        .GET[JsValue](s"$baseUrl/mdtp/journey/journeyId/$journeyId")
        .map(r => (r \ "result").asOpt[String].contains("Success"))
    } else {
      Future.successful(true)
    }

  def journeyStatus(journeyId: String)(implicit hc: HeaderCarrier): Future[IvResult] =
    if (config.ivEnabled) {
      http.GET[JsObject](s"$baseUrl/mdtp/journey/journeyId/$journeyId").map { returnedObject =>
        IvResult.fromString((returnedObject \ "result").as[String]).getOrElse(IvFailure.TechnicalIssue)
      }
    } else {
      Future.successful(IvFailure.TechnicalIssue)
    }
}
