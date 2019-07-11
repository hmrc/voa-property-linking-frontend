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

///*
// * Copyright 2019 HM Revenue & Customs
// *
// * Licensed under the Apache License, Version 2.0 (the "License");
// * you may not use this file except in compliance with the License.
// * You may obtain a copy of the License at
// *
// *     http://www.apache.org/licenses/LICENSE-2.0
// *
// * Unless required by applicable law or agreed to in writing, software
// * distributed under the License is distributed on an "AS IS" BASIS,
// * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// * See the License for the specific language governing permissions and
// * limitations under the License.
// */
//
//package controllers.agentAppointment
//
//import connectors.propertyLinking.PropertyLinkConnector
//import connectors.{Authenticated, PropertyRepresentationConnector}
//import controllers.VoaPropertyLinkingSpec
//import models._
//import org.jsoup.Jsoup
//import org.mockito.ArgumentMatchers.{eq => matching, _}
//import org.mockito.Mockito._
//import org.scalatest.mockito.MockitoSugar
//import play.api.test.FakeRequest
//import play.api.test.Helpers._
//import resources._
//import utils.StubAuthentication
//import scala.collection.JavaConverters._
//
//import scala.concurrent.Future
//import uk.gov.hmrc.http.HeaderCarrier
//
//class RevokeAgentSpec extends VoaPropertyLinkingSpec with MockitoSugar {
//
//  "Viewing the revoke agent page when the agent is not appointed for the property link" should "return a Not Found response" in {
//    val (org, _) = stubLogin()
//    val authId: Long = positiveLong
//    val agentId: Long = positiveLong
//    val link: PropertyLink = propertyLinkGen.retryUntil(_.agents.forall(_.authorisedPartyId != agentId))
//
//    //when(mockPropertyLinks.get(matching(org.id), matching(authId))(any[HeaderCarrier])).thenReturn(Future.successful(Some(link)))
//
//    val res = testController.revokeAgent("111", authId, agentId)(FakeRequest())
//    status(res) mustBe NOT_FOUND
//  }
//
//  "Viewing the revoke agent page when the user is not authorised for the property link" should "return a Not Found response" in {
//    val (org, _) = stubLogin()
//    val authId: Long = positiveLong
//
//    //when(mockPropertyLinks.get(matching(org.id), matching(authId))(any[HeaderCarrier])).thenReturn(Future.successful(None))
//
//    val res = testController.revokeAgent("11111", authId, positiveLong)(FakeRequest())
//    status(res) mustBe NOT_FOUND
//  }
//
//  "Viewing the revoke agent page for a valid agent appointment" should "display the revoke agent page" in {
//    val (org, _) = stubLogin()
//    val authId: Long = positiveLong
//    val agentId: Long = positiveLong
//    val agentCode: Long = positiveLong
//
//    val link = propertyLinkGen.copy( agents = Seq(Party(agentId, agentCode, "Some agent", positiveLong, agentPermissionGen, agentPermissionGen)))
//    //when(mockPropertyLinks.get(matching(org.id), matching(authId))(any[HeaderCarrier])).thenReturn(Future.successful(Some(link)))
//
//    val res = testController.revokeAgent("1111", authId, agentCode)(FakeRequest())
//    status(res) mustBe OK
//
//    val html = Jsoup.parse(contentAsString(res))
//    html.select("p").asScala.map(_.text) must contain ("Are you sure you no longer want Some agent to act on behalf of this property?")
//    html.select("a#confirm").attr("href") mustBe routes.RevokeAgentController.revokeAgentConfirmed("111111", authId, agentCode).url
//  }
//
//  "Revoking an agent when the agent is not appointed for the property link" should "return a Not Found response" in {
//    val (org, _) = stubLogin()
//    val authId: Long = positiveLong
//    val agentId: Long = positiveLong
//    val link: PropertyLink = propertyLinkGen.retryUntil(_.agents.forall(_.authorisedPartyId != agentId))
//
//    //when(mockPropertyLinks.get(matching(org.id), matching(authId))(any[HeaderCarrier])).thenReturn(Future.successful(Some(link)))
//
//    val res = testController.revokeAgentConfirmed("11111", authId, agentId)(FakeRequest())
//    status(res) mustBe NOT_FOUND
//  }
//
//  "Revoking an agent when the user is not authorised for the property link" should "return a Not Found response" in {
//    val (org, _) = stubLogin()
//    val authId: Long = positiveLong
//
//    //when(mockPropertyLinks.get(matching(org.id), matching(authId))(any[HeaderCarrier])).thenReturn(Future.successful(None))
//
//    val res = testController.revokeAgent("111111", authId, positiveLong)(FakeRequest())
//    status(res) mustBe NOT_FOUND
//  }
//
//  "Revoking an agent from a valid agent appointment" should "show the confirmation page" in {
//    val (org, _) = stubLogin()
//    val authId: Long = positiveLong
//    val agentId: Long = positiveLong
//    val agentCode: Long = positiveLong
//
//    val link = propertyLinkGen.copy( agents = Seq(Party(agentId, agentCode, "Some agent", positiveLong, agentPermissionGen, agentPermissionGen)))
//    //when(mockPropertyLinks.get(matching(org.id), matching(authId))(any[HeaderCarrier])).thenReturn(Future.successful(Some(link)))
//    when(mockRepresentationConnector.revoke(any[Long])(any[HeaderCarrier])).thenReturn(Future.successful(()))
//
//    val res = testController.revokeAgentConfirmed("111111", authId, agentCode)(FakeRequest())
//    status(res) mustBe OK
//
//    verify(mockRepresentationConnector, times(1)).revoke(matching(agentId))(any[HeaderCarrier])
//  }
//
//  private lazy val testController = new RevokeAgentController(StubAuthentication, mockPropertyLinks, mockRepresentationConnector)
//
//  private lazy val mockPropertyLinks = mock[PropertyLinkConnector]
//
//  private lazy val mockRepresentationConnector = mock[PropertyRepresentationConnector]
//
//  private def stubLogin() = {
//    val accounts = Accounts(groupAccountGen, individualGen)
//    StubAuthentication.stubAuthenticationResult(Authenticated(accounts))
//    (accounts.organisation, accounts.person)
//  }
//}
