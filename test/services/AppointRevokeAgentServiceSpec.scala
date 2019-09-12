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

package services

import connectors.PropertyRepresentationConnector
import connectors.propertyLinking.PropertyLinkConnector
import models._
import repositories.SessionRepo
import uk.gov.hmrc.http.HeaderCarrier
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import uk.gov.hmrc.http.logging.SessionId

import scala.concurrent.Future

class AppointRevokeAgentServiceSpec extends ServiceSpec {

  private lazy val testService = new AppointRevokeAgentService(mockRepresentationConnector, mockPropertyLinkConnector, mockSessionRepo)

  private lazy val mockPropertyLinkConnector = mock[PropertyLinkConnector]

  private lazy val mockRepresentationConnector = mock[PropertyRepresentationConnector]

  private lazy val mockSessionRepo = mock[SessionRepo]

  implicit val hc = HeaderCarrier(sessionId = Some(SessionId("1111")))

  import scala.concurrent.ExecutionContext.Implicits.global

  "createAndSubmitAgentRepRequest" should "return option unit when succesful" in {

    val links = SessionPropertyLinks(Seq(SessionPropertyLink(1L, "1", Seq(Agent(None,
      AgentPermissions(AgentPermission.fromName("START_AND_CONTINUE").get, AgentPermission.fromName("START_AND_CONTINUE").get),
      1L,
      1L)))))

    when(mockSessionRepo.get[SessionPropertyLinks](any(), any())).thenReturn(Future.successful(Some(links)))
    when(mockRepresentationConnector.revoke(any())(any())).thenReturn(Future.successful())
    when(mockRepresentationConnector.create(any())(any())).thenReturn(Future.successful())


    val res = testService.createAndSubmitAgentRepRequest(
      List("1"),
      1L,
      1L,
      1L,
      AgentPermission.fromName("START_AND_CONTINUE").get,
      AgentPermission.fromName("START_AND_CONTINUE").get,
      true)

    res.futureValue must be(Some(()))

  }


  "createAndSubmitAgentRepRequest" should "return option none when link doesn't exist in cache" in {

    val links = SessionPropertyLinks(Seq(SessionPropertyLink(2L, "11111", Seq(Agent(None,
      AgentPermissions(AgentPermission.fromName("START_AND_CONTINUE").get, AgentPermission.fromName("START_AND_CONTINUE").get),
      3L,
      4L)))))

    when(mockSessionRepo.get[SessionPropertyLinks](any(), any())).thenReturn(Future.successful(Some(links)))

    val res = testService.createAndSubmitAgentRepRequest(
      List("999", "8888"),
      5L,
      6L,
      7L,
      AgentPermission.fromName("START_AND_CONTINUE").get,
      AgentPermission.fromName("START_AND_CONTINUE").get,
      true)

    res.futureValue must be(None)
  }

  "createAndSubmitAgentRepRequest" should "return option unit when session id doesn't exist in cache (handled by auth)" in {

    when(mockSessionRepo.get[SessionPropertyLinks](any(), any())).thenReturn(Future.successful(None))

    val res = testService.createAndSubmitAgentRepRequest(
      List("999", "8888"),
      5L,
      6L,
      7L,
      AgentPermission.fromName("START_AND_CONTINUE").get,
      AgentPermission.fromName("START_AND_CONTINUE").get,
      true)

    res.futureValue must be(Some())
  }

  "createAndSubmitAgentRepRequest" should "return option unit when session id can't be obtained (handled by auth)" in {

    implicit val hc = HeaderCarrier()

    val res = testService.createAndSubmitAgentRepRequest(
      List("999", "8888"),
      5L,
      6L,
      7L,
      AgentPermission.fromName("START_AND_CONTINUE").get,
      AgentPermission.fromName("START_AND_CONTINUE").get,
      true)

    res.futureValue must be(Some())
  }
}

