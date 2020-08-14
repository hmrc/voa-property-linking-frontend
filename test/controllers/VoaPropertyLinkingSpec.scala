/*
 * Copyright 2020 HM Revenue & Customs
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

import java.time.{Clock, Instant, ZoneId}

import actions.AuthenticatedAction
import actions.agentrelationship.request.AppointAgentSessionRequest
import actions.propertylinking.WithLinkingSession
import actions.propertylinking.requests.LinkingSessionRequest
import actions.registration.requests.{RequestWithSessionPersonDetails, RequestWithUserDetails}
import actions.registration.{GgAuthenticatedAction, SessionUserDetailsAction}
import actions.requests.BasicAuthenticatedRequest
import config.ApplicationConfig
import models._
import models.registration.{User, UserDetails}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{AppendedClues, BeforeAndAfterEach, FlatSpec, MustMatchers}
import org.scalatestplus.mockito.MockitoSugar
import play.api.i18n.MessagesApi
import play.api.mvc.{MessagesControllerComponents, Request, Result}
import play.api.test.{DefaultAwaitTimeout, FutureAwaits}
import tests.AllMocks
import uk.gov.hmrc.http.HeaderCarrier
import utils._

import scala.concurrent.{ExecutionContext, Future}

trait VoaPropertyLinkingSpec
    extends FlatSpec with MustMatchers with FutureAwaits with DefaultAwaitTimeout with BeforeAndAfterEach
    with AppendedClues with MockitoSugar with NoMetricsOneAppPerSuite with StubMessageControllerComponents
    with WSHTTPMock with ScalaFutures with Configs with FakeObjects with GlobalExecutionContext with AllMocks {

  val token: (String, String) = "Csrf-Token" -> "nocheck"

  implicit val ec: ExecutionContext = ExecutionContext.Implicits.global
  implicit val headerCarrier: HeaderCarrier = HeaderCarrier()
  implicit val messagesControllerComponents: MessagesControllerComponents = stubMessagesControllerComponents()
  implicit lazy val messageApi: MessagesApi = messagesControllerComponents.messagesApi
  implicit val clock: Clock = Clock.fixed(Instant.now(), ZoneId.systemDefault())
  override implicit lazy val applicationConfig: ApplicationConfig = app.injector.instanceOf[ApplicationConfig]

  def preAuthenticatedActionBuilders(
        userIsAgent: Boolean = true
  ): AuthenticatedAction =
    new AuthenticatedAction(
      messageApi,
      mockGovernmentGatewayProvider,
      mockBusinessRatesAuthorisation,
      mockEnrolmentService,
      mockAuthConnector) {
      override def invokeBlock[A](
            request: Request[A],
            block: BasicAuthenticatedRequest[A] => Future[Result]): Future[Result] =
        block(new BasicAuthenticatedRequest[A](groupAccount(userIsAgent), detailedIndividualAccount, request))
    }

  def ggPreauthenticated(userDetails: UserDetails): GgAuthenticatedAction =
    new GgAuthenticatedAction(messageApi, mockGovernmentGatewayProvider, mockAuthConnector) {
      override def invokeBlock[A](
            request: Request[A],
            block: RequestWithUserDetails[A] => Future[Result]): Future[Result] =
        block(new RequestWithUserDetails[A](userDetails, request))
    }

  def sessionUserDetailsAction(details: User): SessionUserDetailsAction =
    new SessionUserDetailsAction(mockPersonalDetailsSessionRepository) {
      override def transform[A](request: RequestWithUserDetails[A]): Future[RequestWithSessionPersonDetails[A]] =
        Future successful new RequestWithSessionPersonDetails[A](Some(details), request)
    }

  def unauthenticatedActionBuilder(): AuthenticatedAction =
    new AuthenticatedAction(
      messageApi,
      mockGovernmentGatewayProvider,
      mockBusinessRatesAuthorisation,
      mockEnrolmentService,
      mockAuthConnector) {
      override def invokeBlock[A](
            request: Request[A],
            block: BasicAuthenticatedRequest[A] => Future[Result]): Future[Result] = super.invokeBlock(request, block)
    }

  def preEnrichedActionRefiner(clientId :Option[Long] = None): WithLinkingSession =
    new WithLinkingSession(mockCustomErrorHandler, mockSessionRepository) {

      override def refine[A](request: BasicAuthenticatedRequest[A]): Future[Either[Result, LinkingSessionRequest[A]]] =
        Future.successful(
          Right(
            LinkingSessionRequest[A](
              LinkingSession(
                address = "",
                uarn = 1L,
                submissionId = "PL-123456",
                personId = 1L,
                declaration = CapacityDeclaration(
                  capacity = Owner,
                  interestedBefore2017 = true,
                  fromDate = None,
                  stillInterested = false,
                  toDate = None),
                uploadEvidenceData = UploadEvidenceData(fileInfo = None, attachments = None),
                evidenceType = Some(RatesBillType),
                clientId = clientId
              ),
              organisationId = 1L,
              individualAccount = request.individualAccount,
              groupAccount = request.organisationAccount,
              request = request
            )))
    }
}
