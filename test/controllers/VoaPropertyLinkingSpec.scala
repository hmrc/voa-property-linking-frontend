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

package controllers

import actions.{AgentRequest, AuthenticatedAction, BasicAuthenticatedRequest}
import akka.stream.Materializer
import auth.GovernmentGatewayProvider
import connectors.{Addresses, BusinessRatesAuthorisation}
import models._
import org.scalacheck.Arbitrary._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{AppendedClues, BeforeAndAfterEach, FlatSpec, MustMatchers}
import play.api.i18n.{Messages, MessagesApi}
import play.api.mvc.Results._
import play.api.mvc.{Action, AnyContent, Request, Result}
import play.api.test.{DefaultAwaitTimeout, FutureAwaits}
import repositories.{SessionRepo, SessionRepository}
import services.EnrolmentService
import session.{LinkingSessionRequest, WithLinkingSession}
import tests.AllMocks
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.play.config.ServicesConfig
import utils._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}

trait VoaPropertyLinkingSpec
  extends FlatSpec
    with MustMatchers
    with FutureAwaits
    with DefaultAwaitTimeout
    with BeforeAndAfterEach
    with AppendedClues
    with MockitoSugar
    with NoMetricsOneAppPerSuite
    with WSHTTPMock
    with ScalaFutures
    with FakeObjects
    with AllMocks {

  val token = "Csrf-Token" -> "nocheck"

  implicit val ec: ExecutionContext = ExecutionContext.Implicits.global
  implicit lazy val messageApi = app.injector.instanceOf[MessagesApi]

  def preAuthenticatedActionBuilders(
                                      externalId: String = "gg_external_id",
                                      groupId: String = "gg_group_id"
                                    ): AuthenticatedAction =
    new AuthenticatedAction(messageApi, mockGG, mockAuth, mockEnrolmentService, StubAuthConnector) {
      override def invokeBlock[A](request: Request[A], block: BasicAuthenticatedRequest[A] => Future[Result]): Future[Result] = {
        block(BasicAuthenticatedRequest[A](groupAccount, detailedIndividualAccount, request))
      }
    }

  def preEnrichedActionRefiner(): WithLinkingSession = {
    new WithLinkingSession(mockCustomErrorHandler, mockSessionRepository) {

      override def refine[A](request: BasicAuthenticatedRequest[A]): Future[Either[Result, LinkingSessionRequest[A]]] = {
        Future.successful(Right(LinkingSessionRequest[A](
          LinkingSession("", 1L, "PL-123456", 1L, CapacityDeclaration(Owner, true, None, false, None), UploadEvidenceData(fileInfo = None, attachments = None), Some(RatesBillType)),
          1L,
          request.individualAccount,
          request.organisationAccount,
          request
        )))
      }
    }
  }

  lazy val testAction = new AuthenticatedAction(messageApi, mockGG, mockAuth, mockEnrolmentService, StubAuthConnector)
  lazy val mockAuthConnector = mock[AuthConnector]
  lazy val mockAddresses = mock[Addresses]
  lazy val mockServiceConfig = mock[ServicesConfig]
  lazy val mockAuth = mock[BusinessRatesAuthorisation]
  lazy val mockGG = mock[GovernmentGatewayProvider]
  lazy val mockEnrolmentService = mock[EnrolmentService]
  lazy val accounts = Accounts(mock[GroupAccount], mock[DetailedIndividualAccount])
  lazy val mockDetailedIndividualAccount =  mock[DetailedIndividualAccount]
  lazy val mockGroupAccount = mock[GroupAccount]
  lazy val mockFakeRequest = mock[Request[_]]

  override protected def beforeEach(): Unit = {
    StubIndividualAccountConnector.reset()
    StubGroupAccountConnector.reset()
    StubVplAuthConnector.reset()
    StubIdentityVerification.reset()
    StubPropertyLinkConnector.reset()
    StubAuthentication.reset()
    StubBusinessRatesValuation.reset()
    StubSubmissionIdConnector.reset()
    StubPropertyRepresentationConnector.reset()
  }
}
