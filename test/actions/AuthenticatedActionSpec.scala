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
import play.api.i18n.MessagesApi
import play.api.mvc.Request
import play.api.mvc.Results._
import play.api.test.FakeRequest
import play.api.test.Helpers.{contentAsString, _}
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.http.{BadRequestException, HeaderCarrier, NotFoundException}
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.test.UnitSpec
import utils.{NoMetricsOneAppPerSuite, StubAuth, StubAuthConnector}

import scala.concurrent.Future

class AuthenticatedActionSpec extends UnitSpec with MockitoSugar with NoMetricsOneAppPerSuite {

  implicit lazy val messageApi = app.injector.instanceOf[MessagesApi]

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
      redirectLocation(res) shouldBe Some(controllers.registration.routes.RegistrationController.show().url)
    }

    "redirect to invalid account page when the user has an invalid account type" in {
      when(mockAuth.authenticate(any[HeaderCarrier])).thenReturn(Future.successful(InvalidAccountType))

      val res = testAction { _ =>
        Ok("something")
      }(FakeRequest())

      status(res) shouldBe SEE_OTHER
      redirectLocation(res) shouldBe Some(controllers.routes.Application.invalidAccountType().url)
    }

    "throw unauthorized when the trustId is incorrect" in {
      when(mockAuth.authenticate(any[HeaderCarrier])).thenReturn(Future.successful(IncorrectTrustId))

      val res = testAction { _ =>
        Ok("something")
      }(FakeRequest())

      status(res) shouldBe UNAUTHORIZED
      contentAsString(res) shouldBe "Trust ID does not match"
    }

    "throw forbidden when a ForbiddenResponse is thrown" in {
      when(mockAuth.authenticate(any[HeaderCarrier])).thenReturn(Future.successful(ForbiddenResponse))

      val res = testAction { _ =>
        Ok("something")
      }(FakeRequest())

      status(res) shouldBe FORBIDDEN
    }

    "redirect to invalid account page when the user is logged in to GG but does not have groupId" in {
      when(mockAuth.authenticate(any[HeaderCarrier])).thenReturn(Future.successful(NonGroupIDAccount))

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

  lazy val testAction = new AuthenticatedAction(mockGG, mockAuth, StubAuth, mockAddresses, StubAuthConnector)
  lazy val mockAuthConnector = mock[AuthConnector]
  lazy val mockAddresses = mock[Addresses]
  lazy val mockServiceConfig = mock[ServicesConfig]
  lazy val mockAuth = mock[BusinessRatesAuthorisation]
  lazy val mockGG = mock[GovernmentGatewayProvider]

  lazy val accounts = Accounts(mock[GroupAccount], mock[DetailedIndividualAccount])
}
