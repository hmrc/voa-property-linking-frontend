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

package utils

import com.typesafe.config.ConfigFactory
import config.ApplicationConfig
import connectors.SubmissionIdConnector
import org.mockito.Mockito.mock
import play.api.Configuration
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

trait Configs {
  def configuration: Configuration = Configuration(ConfigFactory.parseResources("application.conf"))

  def servicesConfig = new ServicesConfig(configuration)

  def applicationConfig: ApplicationConfig = new ApplicationConfig(configuration)
}

object Configs extends Configs

object StubSubmissionIdConnector extends SubmissionIdConnector(Configs.servicesConfig, mock(classOf[HttpClient])) {
  private var stubbedId: Option[String] = None

  override def get(prefix: String)(implicit hc: HeaderCarrier): Future[String] = Future {
    stubbedId.getOrElse(throw new Exception("submission id not stubbed"))
  }

  def stubId(submissionId: String) =
    stubbedId = Some(submissionId)

  def reset() =
    stubbedId = None
}
