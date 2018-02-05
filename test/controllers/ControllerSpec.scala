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

package controllers

import org.scalatest.mockito.MockitoSugar
import org.scalatest.{AppendedClues, BeforeAndAfterEach, FlatSpec, MustMatchers}
import play.api.test.{DefaultAwaitTimeout, FutureAwaits}
import utils._

trait ControllerSpec extends FlatSpec with MustMatchers with FutureAwaits with DefaultAwaitTimeout with BeforeAndAfterEach with AppendedClues with MockitoSugar with NoMetricsOneAppPerSuite {

  override val additionalAppConfig: Seq[(String, String)] = Seq("featureFlags.enrolment" -> "false", "micsroservice.services.email.host" -> "testhost", "microservice.services.email.port" -> "1234")
  val token = "Csrf-Token" -> "nocheck"
  override protected def beforeEach(): Unit = {
    StubIndividualAccountConnector.reset()
    StubGroupAccountConnector.reset()
    StubAuthConnector.reset()
    StubIdentityVerification.reset()
    StubPropertyLinkConnector.reset()
    StubAuthentication.reset()
    StubBusinessRatesValuation.reset()
    StubSubmissionIdConnector.reset()
    StubPropertyRepresentationConnector.reset()
  }
}
