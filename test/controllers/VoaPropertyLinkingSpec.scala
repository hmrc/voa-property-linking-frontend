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

package controllers

import actions.{AuthenticatedAction, StaticPageAction}
import actions.propertylinking.{WithLinkingSession, WithSubmittedLinkingSession}
import actions.propertylinking.requests.LinkingSessionRequest
import actions.registration.requests.{RequestWithSessionPersonDetails, RequestWithUserDetails}
import actions.registration.{GgAuthenticatedAction, SessionUserDetailsAction}
import actions.requests.{BasicAuthenticatedRequest, StaticPageRequest}
import org.apache.pekko.stream.Materializer
import binders.propertylinks.ClaimPropertyReturnToPage
import config.ApplicationConfig
import models._
import models.registration.UserDetails
import org.scalatest._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.time.{Millis, Seconds, Span}
import org.scalatestplus.mockito.MockitoSugar
import play.api.i18n.MessagesApi
import play.api.mvc.{AnyContent, Cookie, MessagesControllerComponents, Request, Result}
import play.api.test.{DefaultAwaitTimeout, FakeRequest, FutureAwaits}
import tests.AllMocks
import uk.gov.hmrc.http.{HeaderCarrier, SessionId}
import utils._

import java.time.{Clock, Instant, LocalDate, ZoneId}
import org.jsoup.nodes.Document

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
  lazy val welshFakeRequest = FakeRequest().withCookies(cookies = Cookie("PLAY_LANG", "cy"))

  sealed trait RequestLang {
    def fakeRequest: FakeRequest[AnyContent]
  }

  trait EnglishRequest extends RequestLang {
    lazy val fakeRequest: FakeRequest[AnyContent] = FakeRequest()
  }

  trait WelshRequest extends RequestLang {
    lazy val fakeRequest: FakeRequest[AnyContent] = welshFakeRequest
  }

  def preAuthenticatedStaticPage(
        accounts: Option[Accounts] = Some(Accounts(groupAccount(false), detailedIndividualAccount))
  ): StaticPageAction =
    new StaticPageAction(
      messageApi,
      mockBusinessRatesAuthorisation,
      mockAuthConnector
    ) {
      override def invokeBlock[A](request: Request[A], block: StaticPageRequest[A] => Future[Result]): Future[Result] =
        block(new StaticPageRequest[A](accounts, request))
    }

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
            block: BasicAuthenticatedRequest[A] => Future[Result]
      ): Future[Result] =
        block(new BasicAuthenticatedRequest[A](groupAccount(userIsAgent), detailedIndividualAccount, request))
    }

  def ggPreauthenticated(userDetails: UserDetails): GgAuthenticatedAction =
    new GgAuthenticatedAction(
      messageApi,
      mockGovernmentGatewayProvider,
      mockAuthConnector,
      invalidAccountTypeView = new views.html.errors.invalidAccountType(mainLayout, govukButton)
    ) {
      override def invokeBlock[A](
            request: Request[A],
            block: RequestWithUserDetails[A] => Future[Result]
      ): Future[Result] =
        block(new RequestWithUserDetails[A](userDetails, request))
    }

  def sessionUserDetailsAction: SessionUserDetailsAction =
    new SessionUserDetailsAction(mockPersonalDetailsSessionRepository) {
      override def transform[A](request: RequestWithUserDetails[A]): Future[RequestWithSessionPersonDetails[A]] =
        Future successful new RequestWithSessionPersonDetails[A](None, request)
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
            block: BasicAuthenticatedRequest[A] => Future[Result]
      ): Future[Result] = super.invokeBlock(request, block)
    }

  def preEnrichedActionRefiner(): WithLinkingSession =
    preEnrichedActionRefiner(UploadEvidenceData(fileInfo = None, attachments = None))

  def preEnrichedActionRefinerWithStartDate(
        earliestStartDate: LocalDate,
        userIsAgent: Boolean = true
  ): WithLinkingSession =
    preEnrichedActionRefiner(
      UploadEvidenceData(fileInfo = None, attachments = None),
      earliestStartDate = earliestStartDate,
      userIsAgent = userIsAgent
    )

  def preEnrichedActionRefinerFromCya(
        earliestStartDate: LocalDate = earliestEnglishStartDate,
        relationshipCapacity: CapacityType = Owner,
        userIsAgent: Boolean = true
  ): WithLinkingSession =
    preEnrichedActionRefiner(
      UploadEvidenceData(fileInfo = None, attachments = None),
      earliestStartDate = earliestStartDate,
      fromCya = Some(true),
      relationshipCapacity = Some(relationshipCapacity),
      userIsAgent = userIsAgent
    )

  def preEnrichedActionRefiner(
        evidenceData: UploadEvidenceData,
        relationshipCapacity: Option[CapacityType] = Some(Owner),
        userIsAgent: Boolean = true,
        earliestStartDate: LocalDate = earliestEnglishStartDate,
        propertyOwnership: Option[PropertyOwnership] = Some(PropertyOwnership(fromDate = LocalDate.of(2017, 1, 1))),
        propertyOccupancy: Option[PropertyOccupancy] = Some(
          PropertyOccupancy(stillOccupied = false, lastOccupiedDate = None)
        ),
        fromCya: Option[Boolean] = Some(false),
        isSubmitted: Option[Boolean] = None
  ): WithLinkingSession =
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
                propertyRelationship = relationshipCapacity.map(capacity => PropertyRelationship(capacity, 1L)),
                propertyOwnership = propertyOwnership,
                propertyOccupancy = propertyOccupancy,
                hasRatesBill = Some(true),
                uploadEvidenceData = evidenceData,
                evidenceType = Some(RatesBillType),
                clientDetails = if (userIsAgent) Some(ClientDetails(100, "ABC")) else None,
                localAuthorityReference = "12341531531",
                rtp = ClaimPropertyReturnToPage.FMBR,
                fromCya = fromCya,
                isSubmitted = isSubmitted
              ),
              organisationId = 1L,
              individualAccount = request.individualAccount,
              organisationAccount = request.organisationAccount,
              request = request
            )
          )
        )
    }

  def submittedActionRefiner(
        evidenceData: UploadEvidenceData,
        relationshipCapacity: Option[CapacityType] = Some(Owner),
        userIsAgent: Boolean = true,
        earliestStartDate: LocalDate = earliestEnglishStartDate,
        propertyOwnership: Option[PropertyOwnership] = Some(PropertyOwnership(fromDate = LocalDate.of(2017, 1, 1))),
        propertyOccupancy: Option[PropertyOccupancy] = Some(
          PropertyOccupancy(stillOccupied = false, lastOccupiedDate = None)
        ),
        fromCya: Option[Boolean] = Some(true)
  ): WithSubmittedLinkingSession =
    new WithSubmittedLinkingSession(mockCustomErrorHandler, mockSessionRepository) {
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
                propertyRelationship = relationshipCapacity.map(capacity => PropertyRelationship(capacity, 1L)),
                propertyOwnership = propertyOwnership,
                propertyOccupancy = propertyOccupancy,
                hasRatesBill = Some(true),
                uploadEvidenceData = evidenceData,
                evidenceType = Some(RatesBillType),
                clientDetails = if (userIsAgent) Some(ClientDetails(100, "ABC")) else None,
                localAuthorityReference = "12341531531",
                rtp = ClaimPropertyReturnToPage.FMBR,
                fromCya = fromCya,
                isSubmitted = Some(true)
              ),
              organisationId = 1L,
              individualAccount = request.individualAccount,
              organisationAccount = request.organisationAccount,
              request = request
            )
          )
        )
    }

  def verifyLoggedIn(html: Document, pageTitle: String): Unit = {
    html.title shouldBe pageTitle
    html.getElementById("home-link").text() shouldBe "Home"
    html.getElementById("sign-out-link").text() shouldBe "Sign out"
  }

  def verifyNotLoggedIn(html: Document, pageTitle: String): Unit = {
    html.title shouldBe pageTitle
    html.getElementById("login-link").text() shouldBe "Login"
    html.getElementById("register-link").text() shouldBe "Register"
  }

  def verifyUnassignedPrivilegesDisplayed(html: Document, isWelsh: Boolean = false) =
    if (isWelsh) verifyUnassignedPrivilegesDisplayedInWelsh(html)
    else verifyUnassignedPrivilegesDisplayedInEnglish(html)

  def verifyUnassignedPrivilegesDisplayedInEnglish(html: Document) = {
    html.getElementById("unassigned-privilege-1").text() shouldBe "send or continue Check and Challenge cases"
    html
      .getElementById("unassigned-privilege-2")
      .text() shouldBe "see new Check and Challenge case correspondence, such as messages and emails"
    html.getElementById("unassigned-privilege-3").text() shouldBe "see detailed property information"
    html
      .getElementById("warning-text")
      .text() shouldBe "! Warning Unassigning an agent that has Check and Challenge cases in progress means they will no longer be able to act on them for you."
  }

  def verifyUnassignedPrivilegesDisplayedInWelsh(html: Document) = {
    html.getElementById("unassigned-privilege-1").text() shouldBe "anfon neu barhau ag achosion Gwirio a Herio"
    html
      .getElementById("unassigned-privilege-2")
      .text() shouldBe "gweld gohebiaeth achos Gwirio a Herio newydd, er enghraifft negeseuon ac e-byst"
    html.getElementById("unassigned-privilege-3").text() shouldBe "gweld gwybodaeth eiddo fanwl"
    html
      .getElementById("warning-text")
      .text() shouldBe "! Rhybudd Mae dadneilltuo asiant sydd ag achosion Gwirio a Herio ar y gweill yn golygu na fydd yn gallu gweithredu arnynt ar eich rhan mwyach."
  }
}
