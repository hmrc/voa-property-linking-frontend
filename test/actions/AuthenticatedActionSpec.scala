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

package actions

import config.ApplicationConfig
import connectors.authorisation.AuthorisationResult._
import models.Accounts
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import play.api.i18n.MessagesApi
import play.api.mvc.Results._
import play.api.mvc.{MessagesControllerComponents, Request}
import play.api.test.FakeRequest
import play.api.test.Helpers.{contentAsString, _}
import tests.{AllMocks, BaseUnitSpec}
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.authorise.Predicate
import uk.gov.hmrc.auth.core.retrieve.Retrieval
import uk.gov.hmrc.http.HeaderCarrier
import utils.{Configs, FakeObjects, FakeViews, NoMetricsOneAppPerSuite}

import scala.concurrent.{ExecutionContext, Future}

class AuthenticatedActionSpec
    extends BaseUnitSpec with MockitoSugar with BeforeAndAfterEach with AllMocks with NoMetricsOneAppPerSuite
    with FakeViews {

  implicit lazy val messageApi = app.injector.instanceOf[MessagesApi]
  implicit lazy val messagesControllerComponents: MessagesControllerComponents =
    app.injector.instanceOf[MessagesControllerComponents]

  "AuthenticatedAction" should {
    "invoke the wrapped action when the user is logged in to CCA" in new Setup {
      val accounts = Accounts(mockGroupAccount, mockDetailedIndividualAccount)
      when(mockBusinessRatesAuthorisation.authenticate(any[HeaderCarrier]))
        .thenReturn(Future.successful(Authenticated(accounts)))

      val res = testAction(_ => Ok("something"))(FakeRequest())
      status(res) shouldBe OK
      contentAsString(res) shouldBe "something"
    }

    "redirect to the login page when the user is not logged in" in new Setup {
      when(mockBusinessRatesAuthorisation.authenticate(any[HeaderCarrier]))
        .thenReturn(Future.successful(InvalidGGSession))
      when(mockGovernmentGatewayProvider.redirectToLogin(any[Request[_]]))
        .thenReturn(Future.successful(Redirect("sign-in-page")))

      val res = testAction(_ => Ok("something"))(FakeRequest())

      status(res) shouldBe SEE_OTHER
      redirectLocation(res) shouldBe Some("sign-in-page")
    }

    "redirect to the registration page when the user is logged in to GG but has not registered" in new Setup {
      when(mockBusinessRatesAuthorisation.authenticate(any[HeaderCarrier])).thenReturn(Future.successful(NoVOARecord))

      val res = testAction(_ => Ok("something"))(FakeRequest())

      status(res) shouldBe SEE_OTHER
      redirectLocation(res) shouldBe Some(controllers.registration.routes.RegistrationController.show.url)
    }

    "redirect to invalid account page when the user has an invalid account type" in new Setup {
      when(mockBusinessRatesAuthorisation.authenticate(any[HeaderCarrier]))
        .thenReturn(Future.successful(InvalidAccountType))

      val res = testAction(_ => Ok("something"))(FakeRequest())

      status(res) shouldBe SEE_OTHER
      redirectLocation(res) shouldBe Some(controllers.routes.Application.invalidAccountType.url)
    }

    "throw unauthorized when the trustId is incorrect" in new Setup {
      when(mockBusinessRatesAuthorisation.authenticate(any[HeaderCarrier]))
        .thenReturn(Future.successful(IncorrectTrustId))

      val res = testAction(_ => Ok("something"))(FakeRequest())

      status(res) shouldBe UNAUTHORIZED
      contentAsString(res) shouldBe "Trust ID does not match"
    }

    "throw forbidden when a ForbiddenResponse is thrown" in new Setup {
      when(mockBusinessRatesAuthorisation.authenticate(any[HeaderCarrier]))
        .thenReturn(Future.successful(ForbiddenResponse))

      val res = testAction(_ => Ok("something"))(FakeRequest())

      status(res) shouldBe FORBIDDEN
    }

    "redirect to invalid account page when the user is logged in to GG but does not have groupId" in new Setup {
      when(mockBusinessRatesAuthorisation.authenticate(any[HeaderCarrier]))
        .thenReturn(Future.successful(NonGroupIDAccount))

      val res = testAction(_ => Ok("something"))(FakeRequest())

      status(res) shouldBe SEE_OTHER
      redirectLocation(res) shouldBe Some(controllers.routes.Application.invalidAccountType.url)
    }

  }

  trait Setup extends FakeObjects {

    implicit val hc: HeaderCarrier = HeaderCarrier()

    def success(): Unit = ()

    def exception: Option[AuthorisationException] = None

    lazy val authConnector: AuthConnector = new AuthConnector {
      override def authorise[A](predicate: Predicate, retrieval: Retrieval[A])(
            implicit hc: HeaderCarrier,
            ec: ExecutionContext): Future[A] =
        exception.fold(Future.successful(success().asInstanceOf[A]))(Future.failed(_))
    }

    implicit val appConfig: ApplicationConfig = Configs.applicationConfig
    lazy val testAction = new AuthenticatedAction(
      messageApi,
      mockGovernmentGatewayProvider,
      mockBusinessRatesAuthorisation,
      mockEnrolmentService,
      authConnector,
      errorView = new views.html.errors.error(mainLayout),
      forbiddenView = new views.html.errors.forbidden(mainLayout),
      invalidAccountTypeView = new views.html.errors.invalidAccountType(mainLayout, govukButton),
    )
  }

}
