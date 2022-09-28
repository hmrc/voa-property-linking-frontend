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

package controllers

import actions.AuthenticatedAction
import actions.propertylinking.WithLinkingSession
import actions.propertylinking.requests.LinkingSessionRequest
import actions.registration.requests.{RequestWithSessionPersonDetails, RequestWithUserDetails}
import actions.registration.{GgAuthenticatedAction, SessionUserDetailsAction}
import actions.requests.BasicAuthenticatedRequest
import akka.stream.Materializer
import binders.propertylinks.ClaimPropertyReturnToPage
import config.ApplicationConfig
import models._
import models.registration.{User, UserDetails}
import org.scalatest._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.time.{Millis, Seconds, Span}
import org.scalatestplus.mockito.MockitoSugar
import play.api.i18n.MessagesApi
import play.api.mvc.{MessagesControllerComponents, Request, Result}
import play.api.test.{DefaultAwaitTimeout, FutureAwaits}
import tests.AllMocks
import uk.gov.hmrc.http.{HeaderCarrier, SessionId}
import utils._
import java.time.{Clock, Instant, LocalDate, ZoneId}

import scala.concurrent.{ExecutionContext, Future}

trait VoaPropertyLinkingSpec
    extends AnyFlatSpec with Matchers with FutureAwaits with DefaultAwaitTimeout with BeforeAndAfterEach
    with AppendedClues with MockitoSugar with NoMetricsOneAppPerSuite with StubMessageControllerComponents
    with HTTPClientMock with ScalaFutures with Configs with FakeObjects with GlobalExecutionContext with AllMocks
    with HttpResponseUtils with Inside with FakeViews {

  override implicit val patienceConfig: PatienceConfig =
    PatienceConfig(timeout = Span(10, Seconds), interval = Span(20, Millis))

  implicit val ec: ExecutionContext = ExecutionContext.Implicits.global
  implicit val headerCarrier: HeaderCarrier = HeaderCarrier().copy(sessionId = Some(SessionId("my-session")))
  implicit val messagesControllerComponents: MessagesControllerComponents = stubMessagesControllerComponents()
  implicit lazy val messageApi: MessagesApi = messagesControllerComponents.messagesApi
  implicit val clock: Clock = Clock.fixed(Instant.now(), ZoneId.systemDefault())

  override implicit lazy val applicationConfig: ApplicationConfig = app.injector.instanceOf[ApplicationConfig]
  implicit def materializer: Materializer = app.injector.instanceOf[Materializer]

  def preAuthenticatedActionBuilders(
        userIsAgent: Boolean = true
  ): AuthenticatedAction =
    new AuthenticatedAction(
      messageApi,
      mockGovernmentGatewayProvider,
      mockBusinessRatesAuthorisation,
      mockEnrolmentService,
      mockAuthConnector,
      errorView = new views.html.errors.error(mainLayout),
      forbiddenView = new views.html.errors.forbidden(mainLayout),
      invalidAccountTypeView = new views.html.errors.invalidAccountType(mainLayout, govukButton)
    ) {
      override def invokeBlock[A](
            request: Request[A],
            block: BasicAuthenticatedRequest[A] => Future[Result]): Future[Result] =
        block(new BasicAuthenticatedRequest[A](groupAccount(userIsAgent), detailedIndividualAccount, request))
    }

  def ggPreauthenticated(userDetails: UserDetails): GgAuthenticatedAction =
    new GgAuthenticatedAction(
      messageApi,
      mockGovernmentGatewayProvider,
      mockAuthConnector,
      invalidAccountTypeView = new views.html.errors.invalidAccountType(mainLayout, govukButton)) {
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
      mockAuthConnector,
      errorView = new views.html.errors.error(mainLayout),
      forbiddenView = new views.html.errors.forbidden(mainLayout),
      invalidAccountTypeView = new views.html.errors.invalidAccountType(mainLayout, govukButton)
    ) {
      override def invokeBlock[A](
            request: Request[A],
            block: BasicAuthenticatedRequest[A] => Future[Result]): Future[Result] = super.invokeBlock(request, block)
    }

  def preEnrichedActionRefiner(): WithLinkingSession =
    preEnrichedActionRefiner(UploadEvidenceData(fileInfo = None, attachments = None))

  def preEnrichedActionRefinerWithStartDate(earliestStartDate: LocalDate): WithLinkingSession =
    preEnrichedActionRefiner(
      UploadEvidenceData(fileInfo = None, attachments = None),
      earliestStartDate = earliestStartDate)

  def preEnrichedActionRefinerFromCya(earliestStartDate: LocalDate = earliestEnglishStartDate): WithLinkingSession =
    preEnrichedActionRefiner(
      UploadEvidenceData(fileInfo = None, attachments = None),
      earliestStartDate = earliestStartDate,
      fromCya = Some(true)
    )

  def preEnrichedActionRefiner(
        evidenceData: UploadEvidenceData,
        relationshipCapacity: CapacityType = Owner,
        userIsAgent: Boolean = true,
        earliestStartDate: LocalDate = earliestEnglishStartDate,
        propertyOwnership: Option[PropertyOwnership] = Some(
          PropertyOwnership(interestedOnOrBefore = true, fromDate = Some(LocalDate.of(2017, 1, 1)))),
        propertyOccupancy: Option[PropertyOccupancy] = Some(
          PropertyOccupancy(stillOccupied = false, lastOccupiedDate = None)),
        fromCya: Option[Boolean] = Some(false)): WithLinkingSession =
    new WithLinkingSession(mockCustomErrorHandler, mockSessionRepository) {

      override def refine[A](request: BasicAuthenticatedRequest[A]): Future[Either[Result, LinkingSessionRequest[A]]] =
        Future.successful(
          Right(
            LinkingSessionRequest[A](
              LinkingSession(
                address = "LS",
                uarn = 1L,
                submissionId = "PL-123456",
                personId = 1L,
                earliestStartDate = earliestStartDate,
                propertyRelationship = Some(PropertyRelationship(relationshipCapacity)),
                propertyOwnership = propertyOwnership,
                propertyOccupancy = propertyOccupancy,
                hasRatesBill = Some(true),
                uploadEvidenceData = evidenceData,
                evidenceType = Some(RatesBillType),
                clientDetails = if (userIsAgent) Some(ClientDetails(100, "ABC")) else None,
                localAuthorityReference = "12341531531",
                rtp = ClaimPropertyReturnToPage.FMBR,
                fromCya = fromCya
              ),
              organisationId = 1L,
              individualAccount = request.individualAccount,
              organisationAccount = request.organisationAccount,
              request = request
            )))
    }

}
