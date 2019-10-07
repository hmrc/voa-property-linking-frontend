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

import actions.propertylinking.{LinkingSessionRequest, WithLinkingSession}
import actions.{AuthenticatedAction, BasicAuthenticatedRequest}
import auth.GovernmentGatewayProvider
import connectors.Addresses
import connectors.authorisation.BusinessRatesAuthorisation
import actions._
import models._
import models.registration.UserDetails
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{AppendedClues, BeforeAndAfterEach, FlatSpec, MustMatchers}
import play.api.i18n.MessagesApi
import play.api.mvc.{Request, Result}
import play.api.test.{DefaultAwaitTimeout, FutureAwaits}
import session.{LinkingSessionRequest, WithLinkingSession}
import services.EnrolmentService
import tests.AllMocks
import uk.gov.hmrc.http.HeaderCarrier
import utils._

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
    with GlobalExecutionContext
    with AllMocks {

  val token = "Csrf-Token" -> "nocheck"

  implicit val ec: ExecutionContext = ExecutionContext.Implicits.global
  implicit lazy val messageApi: MessagesApi = app.injector.instanceOf[MessagesApi]
  implicit val headerCarrier: HeaderCarrier = HeaderCarrier()

  def preAuthenticatedActionBuilders(
                                      userDetails: UserDetails = individualUserDetails,
                                      userIsAgent: Boolean = true
                                    ): AuthenticatedAction =
    new AuthenticatedAction(messageApi, mockGovernmentGatewayProvider, mockBusinessRatesAuthorisation, mockEnrolmentService, mockAuthConnector) {
      override def invokeBlock[A](request: Request[A], block: BasicAuthenticatedRequest[A] => Future[Result]): Future[Result] = {
        block(new BasicAuthenticatedRequest[A](groupAccount(userIsAgent), detailedIndividualAccount, userDetails, request))
      }
    }

  def ggPreauthenticated(userDetails: UserDetails): GgAuthenticatedAction =
    new GgAuthenticatedAction(mockGovernmentGatewayProvider, mockAuthConnector) {
      override def invokeBlock[A](request: Request[A], block: RequestWithUserDetails[A] => Future[Result]): Future[Result] =
        block(new RequestWithUserDetails[A](userDetails, request))
    }

  def unauthenticatedActionBuilder(): AuthenticatedAction =
    new AuthenticatedAction(messageApi, mockGovernmentGatewayProvider, mockBusinessRatesAuthorisation, mockEnrolmentService, mockAuthConnector) {
      override def invokeBlock[A](request: Request[A], block: BasicAuthenticatedRequest[A] => Future[Result]): Future[Result] = super.invokeBlock(request, block)
    }

  def preEnrichedActionRefiner(): WithLinkingSession = {
    new WithLinkingSession(mockCustomErrorHandler, mockSessionRepository) {

      override def refine[A](request: BasicAuthenticatedRequest[A]): Future[Either[Result, LinkingSessionRequest[A]]] = {
        Future.successful(Right(LinkingSessionRequest[A](
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
            uploadEvidenceData = UploadEvidenceData(
              fileInfo = None,
              attachments = None),
            evidenceType = Some(RatesBillType)),
          organisationId = 1L,
          individualAccount = request.individualAccount,
          groupAccount = request.organisationAccount,
          request = request
        )))
      }
    }
  }

  override protected def beforeEach(): Unit = {
    StubIndividualAccountConnector.reset()
    StubGroupAccountConnector.reset()
    StubIdentityVerification.reset()
    StubPropertyLinkConnector.reset()
    StubBusinessRatesValuation.reset()
    StubSubmissionIdConnector.reset()
    StubPropertyRepresentationConnector.reset()
  }
}
