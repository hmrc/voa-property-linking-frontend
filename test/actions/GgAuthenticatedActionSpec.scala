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

import actions.registration.GgAuthenticatedAction
import config.ApplicationConfig
import models.registration.UserDetails
import org.jsoup.Jsoup
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar
import play.api.i18n.MessagesApi
import play.api.mvc.Results.{Ok, Redirect}
import play.api.mvc._
import play.api.test.FakeRequest
import play.api.test.Helpers._
import tests.{AllMocks, BaseUnitSpec}
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.authorise.Predicate
import uk.gov.hmrc.auth.core.retrieve.{ItmpName, Retrieval, ~}
import uk.gov.hmrc.http.HeaderCarrier
import utils._

import java.time.LocalDate
import scala.concurrent.{ExecutionContext, Future}

class GgAuthenticatedActionSpec
    extends BaseUnitSpec with MockitoSugar with ScalaFutures with BeforeAndAfterEach with AllMocks
    with NoMetricsOneAppPerSuite with GlobalExecutionContext with FakeViews {

  implicit lazy val messageApi: MessagesApi = app.injector.instanceOf[MessagesApi]
  implicit lazy val controllerComponents: ControllerComponents = app.injector.instanceOf[ControllerComponents]
  implicit lazy val messagesControllerComponents: MessagesControllerComponents =
    app.injector.instanceOf[MessagesControllerComponents]

  "GgAuthenticatedAction" should {
    "invoke the action block when user is authorised" in new Setup {
      val res = testAction(_ => Ok("something"))(FakeRequest())
      status(res) shouldBe OK
      contentAsString(res) shouldBe "something"
    }

    "redirect to the login page when the user is not logged in (NoActiveSession)" in new Setup {
      override def exception: Option[Throwable] = Some(MissingBearerToken("error"))

      when(mockGovernmentGatewayProvider.redirectToLogin(any[Request[_]]))
        .thenReturn(Future.successful(Redirect("sign-in-page")))

      val res = testAction(_ => Ok("something"))(FakeRequest())

      status(res) shouldBe SEE_OTHER
      redirectLocation(res) shouldBe Some("sign-in-page")
    }

    "pass through an exception if retrieval fails for a other thanAuthorisationException" in new Setup {
      override def exception: Option[Throwable] = Some(new RuntimeException("error"))

      val res = testAction(_ => Ok("something"))(FakeRequest())

      whenReady(res.failed) { e =>
        e.getMessage shouldBe "error"
        e shouldBe a[RuntimeException]
      }
    }

    "redirect to invalid account page when the user has an invalid account type" in new Setup {
      override def exception: Option[Throwable] = Some(new UnsupportedAffinityGroup("error"))

      val res = testAction(_ => Ok("something"))(FakeRequest())

      status(res) shouldBe OK

      val page = HtmlPage(Jsoup.parse(contentAsString(res)))
      page.shouldContainText("You can’t use that Government Gateway account to register for this service.")
    }

  }

  trait Setup extends FakeObjects {

    implicit val hc: HeaderCarrier = HeaderCarrier()

    def user: UserDetails = userDetails()

    def success: Option[ItmpName] ~ Option[String] ~ Option[String] ~ Option[String] ~ Option[String] ~ Option[
      AffinityGroup
    ] ~ Option[CredentialRole] ~ ConfidenceLevel ~ Option[LocalDate] ~ Option[String] =
      new ~(
        new ~(
          new ~(
            new ~(
              new ~(
                new ~(
                  new ~(
                    new ~(
                      new ~(
                        Option(ItmpName(user.firstName, None, user.lastName)),
                        Option(user.email)
                      ),
                      user.postcode
                    ),
                    Option(user.groupIdentifier)
                  ),
                  Option(user.externalId)
                ),
                Option(user.affinityGroup)
              ),
              Option(user.credentialRole)
            ),
            user.confidenceLevel
          ),
          user.dob
        ),
        user.nino
      )

    def exception: Option[Throwable] = None

    lazy val authConnector: AuthConnector = new AuthConnector {
      override def authorise[A](predicate: Predicate, retrieval: Retrieval[A])(implicit
            hc: HeaderCarrier,
            ec: ExecutionContext
      ): Future[A] =
        exception.fold(Future.successful(success.asInstanceOf[A]))(Future.failed(_))
    }

    implicit val appConfig: ApplicationConfig = Configs.applicationConfig
    lazy val testAction = new GgAuthenticatedAction(
      messagesApi = messageApi,
      provider = mockGovernmentGatewayProvider,
      authConnector = authConnector,
      invalidAccountTypeView = invalidAccountTypeView
    )
  }

}
