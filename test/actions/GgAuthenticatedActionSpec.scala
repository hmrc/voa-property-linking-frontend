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

package actions

import actions.registration.GgAuthenticatedAction
import models.registration.UserDetails
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar
import play.api.i18n.MessagesApi
import play.api.mvc.Results.{Ok, Redirect}
import play.api.mvc._
import play.api.test.FakeRequest
import play.api.test.Helpers._
import tests.AllMocks
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.authorise.Predicate
import uk.gov.hmrc.auth.core.retrieve.{Name, Retrieval, ~}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.test.UnitSpec
import utils.{FakeObjects, GlobalExecutionContext, NoMetricsOneAppPerSuite}

import scala.concurrent.{ExecutionContext, Future}

class GgAuthenticatedActionSpec
  extends UnitSpec
    with MockitoSugar
    with ScalaFutures
    with BeforeAndAfterEach
    with AllMocks
    with NoMetricsOneAppPerSuite
    with GlobalExecutionContext {

  implicit lazy val messageApi = app.injector.instanceOf[MessagesApi]

  "GgAuthenticatedAction" should {
    "invoke the action block when user is authorised" in new Setup {
      val res = testAction(_ => Ok("something"))(FakeRequest())
      status(res) shouldBe OK
      contentAsString(res) shouldBe "something"
    }

    "redirect to the login page when the user is not logged in (NoActiveSession)" in new Setup {
      override def exception: Option[Throwable] = Some(new MissingBearerToken("error"))

      when(mockGovernmentGatewayProvider.redirectToLogin(any[Request[_]])).thenReturn(Future.successful(Redirect("sign-in-page")))

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

  }

  trait Setup extends FakeObjects {

    implicit val hc: HeaderCarrier = HeaderCarrier()

    def user: UserDetails = userDetails()

    def success: Option[Name] ~ Option[String] ~ Option[String] ~ Option[String] ~ Option[String] ~ Option[AffinityGroup] ~ Option[CredentialRole] =
      new ~(new ~(new ~(new ~(new ~(new ~(Option(Name(user.firstName, user.lastName)), Option(user.email)), user.postcode), Option(user.groupIdentifier)), Option(user.externalId)), Option(user.affinityGroup)), Option(user.credentialRole))

    def exception: Option[Throwable] = None

    lazy val authConnector: AuthConnector = new AuthConnector {
      override def authorise[A](predicate: Predicate, retrieval: Retrieval[A])(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[A] =
        exception.fold(Future.successful(success.asInstanceOf[A]))(Future.failed(_))
    }

    lazy val testAction = new GgAuthenticatedAction(mockGovernmentGatewayProvider, authConnector)
  }

}