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

import auth.GovernmentGatewayProvider
import connectors._
import models.{Accounts, DetailedIndividualAccount, GroupAccount}
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar
import play.api.mvc.Request
import play.api.mvc.Results._
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.play.test.UnitSpec
import utils.NoMetricsOneAppPerSuite

import scala.concurrent.Future
import uk.gov.hmrc.http.{BadRequestException, HeaderCarrier, NotFoundException}
import uk.gov.hmrc.play.config.ServicesConfig

class AuthenticatedActionSpec extends UnitSpec with MockitoSugar with NoMetricsOneAppPerSuite {

  "AuthenticatedAction" should {
    "invoke the wrapped action when the user is logged in to CCA" in {
      when(mockAuth.authenticate(any[HeaderCarrier])).thenReturn(Future.successful(Authenticated(accounts)))

      val res = testAction { _ =>
        Ok("something")
      }(FakeRequest())

      status(res) shouldBe OK
      contentAsString(res) shouldBe "something"
    }

    "redirect to the login page when the user is not logged in" in {
      when(mockAuth.authenticate(any[HeaderCarrier])).thenReturn(Future.successful(InvalidGGSession))
      when(mockGG.redirectToLogin(any[Request[_]])).thenReturn(Future.successful(Redirect("sign-in-page")))

      val res = testAction { _ =>
        Ok("something")
      }(FakeRequest())

      status(res) shouldBe SEE_OTHER
      redirectLocation(res) shouldBe Some("sign-in-page")
    }

    "redirect to the registration page when the user is logged in to GG but has not registered" in {
      when(mockAuth.authenticate(any[HeaderCarrier])).thenReturn(Future.successful(NoVOARecord))

      val res = testAction { _ =>
        Ok("something")
      }(FakeRequest())

      status(res) shouldBe SEE_OTHER
      redirectLocation(res) shouldBe Some(controllers.routes.CreateIndividualAccount.show().url)
    }

    "redirect to the invalid account page when the user is logged in with a non-organisation account" in {
      when(mockAuth.authenticate(any[HeaderCarrier])).thenReturn(Future.successful(NonOrganisationAccount))

      val res = testAction { _ =>
        Ok("something")
      }(FakeRequest())

      status(res) shouldBe SEE_OTHER
      redirectLocation(res) shouldBe Some(controllers.routes.Application.invalidAccountType().url)
    }

    "return a 400 response when the wrapped action throws a BadRequestException" in {
      when(mockAuth.authenticate(any[HeaderCarrier])).thenReturn(Future.successful(Authenticated(accounts)))

      val res = testAction { _ =>
        throw new BadRequestException("the request was bad")
      }(FakeRequest())

      status(res) shouldBe BAD_REQUEST
    }

    "return a 404 response when the wrapped action throws a NotFoundException" in {
      when(mockAuth.authenticate(any[HeaderCarrier])).thenReturn(Future.successful(Authenticated(accounts)))

      val res = testAction { _ =>
        throw new NotFoundException("not found")
      }(FakeRequest())

      status(res) shouldBe NOT_FOUND
    }

    "return a 500 response when the wrapped action throws any other unhandled exception" in {
      when(mockAuth.authenticate(any[HeaderCarrier])).thenReturn(Future.successful(Authenticated(accounts)))

      val res = testAction { _ =>
        throw new Exception("bad stuff happened")
      }(FakeRequest())

      status(res) shouldBe INTERNAL_SERVER_ERROR
    }
  }

  lazy val testAction = new AuthenticatedAction(mockGG, mockAuth, mockServiceConfig, )
  lazy val mockServiceConfig = mock[ServicesConfig]
  lazy val mockAuth = mock[BusinessRatesAuthorisation]
  lazy val mockGG = mock[GovernmentGatewayProvider]

  lazy val accounts = Accounts(mock[GroupAccount], mock[DetailedIndividualAccount])
}
