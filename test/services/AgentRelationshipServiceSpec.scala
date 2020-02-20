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

package services

import config.ApplicationConfig
import connectors.PropertyRepresentationConnector
import connectors.propertyLinking.PropertyLinkConnector
import models._
import models.searchApi.OwnerAuthAgent
import repositories.SessionRepo
import uk.gov.hmrc.http.HeaderCarrier
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import uk.gov.hmrc.http.logging.SessionId

import scala.concurrent.Future
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import tests.AllMocks

class AgentRelationshipServiceSpec extends ServiceSpec with AllMocks {

  import scala.concurrent.ExecutionContext.Implicits.global

  private val mockApplicationConfig = mock[ApplicationConfig]
  when(mockApplicationConfig.agentAppointDelay).thenReturn(0)

  private lazy val testService = new AgentRelationshipService(
    mockAuditingService,
    mockRepresentationConnector,
    mockPropertyLinkConnector,
    mockSessionRepo,
    mockApplicationConfig)

  private lazy val mockPropertyLinkConnector = mock[PropertyLinkConnector]

  private lazy val mockRepresentationConnector = mock[PropertyRepresentationConnector]

  private lazy val mockSessionRepo = mock[SessionRepo]

  implicit val hc = HeaderCarrier(sessionId = Some(SessionId("1111")))

  "createAndSubmitAgentRepRequest" should "return option unit when successful" in {

    val links = SessionPropertyLinks(
      Seq(
        SessionPropertyLink(
          1L,
          "1",
          Seq(OwnerAuthAgent(1l, 1l, "organisationName", "APPROVED", NotPermitted, StartAndContinue, 1l)))
      )
    )

    when(mockSessionRepo.get[SessionPropertyLinks](any(), any())).thenReturn(Future.successful(Some(links)))
    when(mockRepresentationConnector.revoke(any())(any())).thenReturn(Future.successful())
    when(mockRepresentationConnector.create(any())(any())).thenReturn(Future.successful())

    val res = testService.createAndSubmitAgentRepRequest(List("1"), 1L, 1L, 1L, StartAndContinue, NotPermitted, true)

    res.futureValue must be(())

    verify(mockRepresentationConnector, times(1)).create(any())(any())
  }

  "createAndSubitAgentRevokeRequest" should "return option unit when successful" in {

    val links = SessionPropertyLinks(
      Seq(
        SessionPropertyLink(
          1L,
          "1",
          Seq(OwnerAuthAgent(1l, 1l, "organisationName", "APPROVED", StartAndContinue, StartAndContinue, 1l)))))

    when(mockSessionRepo.get[SessionPropertyLinks](any(), any())).thenReturn(Future.successful(Some(links)))
    when(mockRepresentationConnector.revoke(any())(any())).thenReturn(Future.successful())
    when(mockRepresentationConnector.create(any())(any())).thenReturn(Future.successful())

    val res = testService.createAndSubitAgentRevokeRequest(List("1"), 1L)

    res.futureValue must be(())
  }

  "createAndSubmitAgentRepRequest" should "throw exception when link doesn't exist in cache" in {

    val links = SessionPropertyLinks(
      Seq(
        SessionPropertyLink(
          2L,
          "11111",
          Seq(OwnerAuthAgent(3L, 4L, "", "APPROVED", StartAndContinue, StartAndContinue, 1l)))))

    when(mockSessionRepo.get[SessionPropertyLinks](any(), any())).thenReturn(Future.successful(Some(links)))

    val res = testService
      .createAndSubmitAgentRepRequest(List("999", "8888"), 5L, 6L, 7L, StartAndContinue, StartAndContinue, true)

    res.failed.futureValue must be(
      new services.AppointRevokeException("Property link 999 not found in property links cache."))
  }

  "createAndSubitAgentRevokeRequest" should "throw exception when link doesn't exist in cache" in {

    val links = SessionPropertyLinks(
      Seq(
        SessionPropertyLink(
          2L,
          "11111",
          Seq(OwnerAuthAgent(3L, 4L, "", "APPROVED", StartAndContinue, StartAndContinue, 1l)))))

    when(mockSessionRepo.get[SessionPropertyLinks](any(), any())).thenReturn(Future.successful(Some(links)))

    val res = testService.createAndSubitAgentRevokeRequest(List("999", "8888"), 5L)

    res.failed.futureValue must be(
      new services.AppointRevokeException(
        "Agent 5 for the property link with subission ID 999 doesn't exist in cache - this shouldn't be possible."))
  }

  "createAndSubmitAgentRepRequest" should "throw exception when session id doesn't exist in cache (handled by auth)" in {

    when(mockSessionRepo.get[SessionPropertyLinks](any(), any())).thenReturn(Future.successful(None))

    val res = testService
      .createAndSubmitAgentRepRequest(List("999", "8888"), 5L, 6L, 7L, StartAndContinue, StartAndContinue, true)

    res.failed.futureValue must be(
      new services.AppointRevokeException(
        "Session ID SessionId(1111) no longer in property links cache - should be redirected to login by auth."))
  }

  "createAndSubitAgentRevokeRequest" should "throw exception when session id doesn't exist in cache (handled by auth)" in {

    when(mockSessionRepo.get[SessionPropertyLinks](any(), any())).thenReturn(Future.successful(None))

    val res = testService.createAndSubitAgentRevokeRequest(List("999", "8888"), 5L)

    res.failed.futureValue must be(
      new services.AppointRevokeException(
        "Session ID SessionId(1111) no longer in property links cache - should be redirected to login by auth."))
  }

  "createAndSubmitAgentRepRequest" should "throw exception session id can't be obtained (handled by auth)" in {

    implicit val hc = HeaderCarrier()

    val res = testService
      .createAndSubmitAgentRepRequest(List("999", "8888"), 5L, 6L, 7L, StartAndContinue, StartAndContinue, true)

    res.failed.futureValue must be(new services.AppointRevokeException(
      "Unable to obtain session ID from request to retrieve property links cache - should be redirected to login by auth."))
  }

  "createAndSubitAgentRevokeRequest" should "throw exception session id can't be obtained (handled by auth)" in {

    implicit val hc = HeaderCarrier()

    val res = testService.createAndSubitAgentRevokeRequest(List("999", "8888"), 5L)

    res.failed.futureValue must be(new services.AppointRevokeException(
      "Unable to obtain session ID from request to retrieve property links cache - should be redirected to login by auth."))
  }
}
