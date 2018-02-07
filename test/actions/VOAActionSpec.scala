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

package actions


import auth.{GGActionEnrolment, GovernmentGatewayProvider}
import connectors.VPLAuthConnector
import models.enrolment.{UserDetails, UserInfo}
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar
import play.api.mvc.Results.Ok
import play.api.test.FakeRequest
import play.api.test.Helpers.{contentAsString, _}
import uk.gov.hmrc.auth.core.AffinityGroup.Individual
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import uk.gov.hmrc.play.test.UnitSpec
import utils.NoMetricsOneAppPerSuite

import scala.concurrent.Future

class VOAActionSpec extends UnitSpec with MockitoSugar with NoMetricsOneAppPerSuite {

  override val additionalAppConfig: Seq[(String, String)] = Seq("featureFlags.enrolment" -> "false")

  "when false provided but session is not stored, goes through process of getting details again" in {
    when(mockVplAuthConnector.getUserDetails(any())).thenReturn(Future.successful(UserDetails("123456", "654321", UserInfo(None, None, "", None, "", "654321", Individual))))
    println(Individual.toJson)
    val res = testActionEnrolment.async(false) { _ =>
      _ =>
        Future.successful(Ok("something"))
    }(FakeRequest())

    status(res) shouldBe OK
    contentAsString(res) shouldBe "something"
  }

  lazy val testActionEnrolment = new GGActionEnrolment(mockGG, mockAuthConnector, mockVplAuthConnector)
  lazy val mockAuthConnector = mock[AuthConnector]
  lazy val mockVplAuthConnector = mock[VPLAuthConnector]
  lazy val mockGG = mock[GovernmentGatewayProvider]

}
