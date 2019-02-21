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

package utils

import connectors.SubmissionIdConnector
import org.scalatest.mockito.MockitoSugar
import play.api.Mode.Mode
import play.api.{Configuration, Environment, Mode, Play}
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import uk.gov.hmrc.play.config.ServicesConfig

import scala.concurrent.Future
import uk.gov.hmrc.http.HeaderCarrier


object StubSubmissionIdConnector extends SubmissionIdConnector(StubServicesConfig, StubHttp) {
  private var stubbedId: Option[String] = None

  override def get(prefix: String)(implicit hc: HeaderCarrier): Future[String] = Future {
    stubbedId.getOrElse(throw new Exception("submission id not stubbed"))
  }

  def stubId(submissionId: String) = {
    stubbedId = Some(submissionId)
  }

  def reset() = {
    stubbedId = None
  }
}


object StubServicesConfig extends ServicesConfig with MockitoSugar {
  override lazy val env = "Test"

  override protected def mode: Mode = Mode.Test

  override protected def runModeConfiguration: Configuration = Play.current.configuration
}