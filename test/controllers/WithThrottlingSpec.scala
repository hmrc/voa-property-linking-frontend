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

package controllers

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import connectors.TrafficThrottleConnector
import controllers.Application.Ok
import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar
import play.api.mvc.Action
import play.api.test.Helpers._
import play.api.test.{FakeHeaders, FakeRequest}
import uk.gov.hmrc.play.http.HeaderCarrier
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Await, Future}
import scala.concurrent.duration._

class WithThrottlingSpec extends UnitSpec with MockitoSugar {

  implicit val app = TestApp.app

  implicit val hc: HeaderCarrier = mock[HeaderCarrier]
  implicit val system = ActorSystem("test-system")
  implicit val materializer = ActorMaterializer()

  val mockTrafficThrottleConnector = mock[TrafficThrottleConnector]

  object ThrottledController extends WithThrottling {
    override lazy val trafficThrottleConnector = mockTrafficThrottleConnector

    def index() = Action.async { implicit request =>
      withThrottledHoldingPage(Ok("Holding page")) {
        Ok("Normal page")
      }
    }
  }

  "when the controller is not being throttled" must {
    "return the normal page" in {
      when(mockTrafficThrottleConnector.isThrottled()).thenReturn(Future(false))

      val fakeRequest = new FakeRequest(
        "GET",
        "http://localhost:9000/voa-property-linking-frontend/index",
        FakeHeaders(Seq()),
        "RequestBody"
      )

      val result = Await.result(ThrottledController.index()(fakeRequest).run(), 60 seconds)

      contentAsString(result) shouldBe "Normal page"
    }
  }

  "when the controller is being throttled" should {
    "return the holding page" in {
      when(mockTrafficThrottleConnector.isThrottled()).thenReturn(Future(true))

      val fakeRequest = new FakeRequest(
        "GET",
        "http://localhost:9000/voa-property-linking-frontend/index",
        FakeHeaders(Seq()),
        "RequestBody"
      )

      val result = Await.result(ThrottledController.index()(fakeRequest).run(), 60 seconds)

      contentAsString(result) shouldBe "Holding page"
    }
  }

  "when an exception occurs" should {
    "return the normal page" in {
      when(mockTrafficThrottleConnector.isThrottled()).thenReturn(Future.failed(new Exception))

      val fakeRequest = new FakeRequest(
        "GET",
        "http://localhost:9000/voa-property-linking-frontend/index",
        FakeHeaders(Seq()),
        "RequestBody"
      )

      val result = Await.result(ThrottledController.index()(fakeRequest).run(), 60 seconds)

      contentAsString(result) shouldBe "Normal page"
    }
  }

}
