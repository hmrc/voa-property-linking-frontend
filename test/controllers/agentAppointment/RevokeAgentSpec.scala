package controllers.agentAppointment

import connectors.propertyLinking.PropertyLinkConnector
import connectors.{Authenticated, PropertyRepresentationConnector}
import controllers.ControllerSpec
import models._
import org.jsoup.Jsoup
import org.mockito.ArgumentMatchers.{eq => matching, _}
import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar
import play.api.test.FakeRequest
import play.api.test.Helpers._
import resources._
import uk.gov.hmrc.play.http.HeaderCarrier
import utils.StubAuthentication
import scala.collection.JavaConverters._

import scala.concurrent.Future

class RevokeAgentSpec extends ControllerSpec with MockitoSugar {

  "Viewing the revoke agent page when the agent is not appointed for the property link" should "return a Not Found response" in {
    val (org, _) = stubLogin()
    val authId: Long = positiveLong
    val agentId: Long = positiveLong
    val link: PropertyLink = propertyLinkGen.retryUntil(_.agents.forall(_.authorisedPartyId != agentId))

    when(mockPropertyLinks.get(matching(org.id), matching(authId))(any[HeaderCarrier])).thenReturn(Future.successful(Some(link)))

    val res = testController.revokeAgent(authId, agentId, positiveLong)(FakeRequest())
    status(res) mustBe NOT_FOUND
  }

  "Viewing the revoke agent page when the user is not authorised for the property link" should "return a Not Found response" in {
    val (org, _) = stubLogin()
    val authId: Long = positiveLong

    when(mockPropertyLinks.get(matching(org.id), matching(authId))(any[HeaderCarrier])).thenReturn(Future.successful(None))

    val res = testController.revokeAgent(authId, positiveLong, positiveLong)(FakeRequest())
    status(res) mustBe NOT_FOUND
  }

  "Viewing the revoke agent page for a valid agent appointment" should "display the revoke agent page" in {
    val (org, _) = stubLogin()
    val authId: Long = positiveLong
    val agentId: Long = positiveLong
    val agentCode: Long = positiveLong

    val link = propertyLinkGen.copy(organisationId = org.id, agents = Seq(Party(agentId, agentCode, "Some agent", positiveLong, agentPermissionGen, agentPermissionGen)))
    when(mockPropertyLinks.get(matching(org.id), matching(authId))(any[HeaderCarrier])).thenReturn(Future.successful(Some(link)))

    val res = testController.revokeAgent(authId, agentId, agentCode)(FakeRequest())
    status(res) mustBe OK

    val html = Jsoup.parse(contentAsString(res))
    html.select("p").asScala.map(_.text) must contain ("Are you sure you no longer want Some agent to act on behalf of this property?")
    html.select("a#confirm").attr("href") mustBe routes.RevokeAgentController.revokeAgentConfirmed(authId, agentId, agentCode).url
  }

  "Revoking an agent when the agent is not appointed for the property link" should "return a Not Found response" in {
    val (org, _) = stubLogin()
    val authId: Long = positiveLong
    val agentId: Long = positiveLong
    val link: PropertyLink = propertyLinkGen.retryUntil(_.agents.forall(_.authorisedPartyId != agentId))

    when(mockPropertyLinks.get(matching(org.id), matching(authId))(any[HeaderCarrier])).thenReturn(Future.successful(Some(link)))

    val res = testController.revokeAgentConfirmed(authId, agentId, positiveLong)(FakeRequest())
    status(res) mustBe NOT_FOUND
  }

  "Revoking an agent when the user is not authorised for the property link" should "return a Not Found response" in {
    val (org, _) = stubLogin()
    val authId: Long = positiveLong

    when(mockPropertyLinks.get(matching(org.id), matching(authId))(any[HeaderCarrier])).thenReturn(Future.successful(None))

    val res = testController.revokeAgent(authId, positiveLong, positiveLong)(FakeRequest())
    status(res) mustBe NOT_FOUND
  }

  "Revoking an agent from a valid agent appointment" should "show the confirmation page" in {
    val (org, _) = stubLogin()
    val authId: Long = positiveLong
    val agentId: Long = positiveLong
    val agentCode: Long = positiveLong

    val link = propertyLinkGen.copy(organisationId = org.id, agents = Seq(Party(agentId, agentCode, "Some agent", positiveLong, agentPermissionGen, agentPermissionGen)))
    when(mockPropertyLinks.get(matching(org.id), matching(authId))(any[HeaderCarrier])).thenReturn(Future.successful(Some(link)))
    when(mockRepresentationConnector.revoke(any[Long])(any[HeaderCarrier])).thenReturn(Future.successful(()))

    val res = testController.revokeAgentConfirmed(authId, agentId, agentCode)(FakeRequest())
    status(res) mustBe OK

    verify(mockRepresentationConnector, times(1)).revoke(matching(agentId))(any[HeaderCarrier])
  }

  private lazy val testController = new RevokeAgentController {
    override val authenticated = StubAuthentication
    override val propertyLinks = mockPropertyLinks
    override val representations = mockRepresentationConnector
  }

  private lazy val mockPropertyLinks = mock[PropertyLinkConnector]

  private lazy val mockRepresentationConnector = mock[PropertyRepresentationConnector]

  private def stubLogin() = {
    val accounts = Accounts(groupAccountGen, individualGen)
    StubAuthentication.stubAuthenticationResult(Authenticated(accounts))
    (accounts.organisation, accounts.person)
  }
}
