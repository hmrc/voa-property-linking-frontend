/*
 * Copyright 2024 HM Revenue & Customs
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
import play.api.mvc.MessagesControllerComponents
import play.api.test.FakeRequest
import play.api.test.Helpers.{contentAsString, _}
import tests.{AllMocks, BaseUnitSpec}
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.authorise.Predicate
import uk.gov.hmrc.auth.core.retrieve.Retrieval
import uk.gov.hmrc.http.HeaderCarrier
import utils.{Configs, FakeObjects, FakeViews, NoMetricsOneAppPerSuite}

import scala.concurrent.{ExecutionContext, Future}

class StaticPageActionSpec
    extends BaseUnitSpec with MockitoSugar with BeforeAndAfterEach with AllMocks with NoMetricsOneAppPerSuite
    with FakeViews {

  implicit lazy val messageApi = app.injector.instanceOf[MessagesApi]
  implicit lazy val messagesControllerComponents: MessagesControllerComponents =
    app.injector.instanceOf[MessagesControllerComponents]

  "StaticPageAction" should {
    "invoke the wrapped action when the user is logged in to CCA" in new Setup {
      val accounts = Accounts(mockGroupAccount, mockDetailedIndividualAccount)
      when(mockBusinessRatesAuthorisation.authenticate(any[HeaderCarrier]))
        .thenReturn(Future.successful(Authenticated(accounts)))

      val res = testAction(_ => Ok("something"))(FakeRequest())
      status(res) shouldBe OK
      contentAsString(res) shouldBe "something"
    }

    "invoke the wrapped action even when the user is NOT logged in to CCA" in new Setup {
      when(mockBusinessRatesAuthorisation.authenticate(any[HeaderCarrier]))
        .thenReturn(Future.successful(InvalidGGSession))

      val res = testAction(_ => Ok("something"))(FakeRequest())
      status(res) shouldBe OK
      contentAsString(res) shouldBe "something"
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
    lazy val testAction = new StaticPageAction(
      messageApi,
      mockBusinessRatesAuthorisation,
      authConnector,
    )
  }

}
