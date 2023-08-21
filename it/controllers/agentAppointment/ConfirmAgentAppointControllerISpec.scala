package controllers.agentAppointment

import base.{HtmlComponentHelpers, ISpecBase}
import com.github.tomakehurst.wiremock.client.WireMock._
import models.AgentAppointBulkAction
import models.propertyrepresentation._
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.http.HeaderNames
import play.api.http.Status.OK
import play.api.libs.json.Json
import play.api.test.Helpers._
import repositories.{AppointAgentPropertiesSessionRepository, AppointAgentSessionRepository}
import uk.gov.hmrc.http.{HeaderCarrier, SessionId}

import java.util.UUID

class ConfirmAgentAppointControllerISpec extends ISpecBase with HtmlComponentHelpers {

  val testSessionId = s"stubbed-${UUID.randomUUID}"
  val assignedToId = "assigned-to"

  "onPageLoad" should {
    "return 200 & display correct content (Some properties)" in new TestSetup {

      await(mockAppointAgentSessionRepository.saveOrUpdate(managingPropertyData.copy(managingPropertyChoice =  ChooseFromList.name)))
      await(mockAppointAgentPropertiesSessionRepository
        .saveOrUpdate(propertiesSessionData.agentAppointAction.map(_.copy(propertyLinkIds = List("123")))))

      lazy val document = getDocument(English)
      document.getElementById(assignedToId).text() shouldBe "They have also been assigned to the properties you selected."

      //Verify that the ManagingProperty cache data is updated with appointmentSubmitted = true
      val cacheResult = await(mockAppointAgentSessionRepository.get[ManagingProperty])
      cacheResult shouldBe Some(managingPropertyData.copy(managingPropertyChoice =  ChooseFromList.name)
        .copy(appointmentSubmitted = true))
    }

    "return 200 & display correct content (Some properties) - Welsh" in new TestSetup {

      await(mockAppointAgentSessionRepository.saveOrUpdate(managingPropertyData.copy(managingPropertyChoice =  ChooseFromList.name)))
      await(mockAppointAgentPropertiesSessionRepository
        .saveOrUpdate(propertiesSessionData.agentAppointAction.map(_.copy(propertyLinkIds = List("123")))))

      lazy val document = getDocument(Welsh)
      document.getElementById(assignedToId).text() shouldBe "Maent hefyd wedi’u neilltuo i’r eiddo a ddewiswyd gennych."

    }

    "return 200 & display correct content (All properties)" in new TestSetup {

      lazy val document = getDocument(English)
      document.getElementById(assignedToId).text() shouldBe "They have also been assigned to all your properties."

    }

    "return 200 & display correct content (All properties) - Welsh" in new TestSetup {

      lazy val document = getDocument(Welsh)
      document.getElementById(assignedToId).text() shouldBe "Maent hefyd wedi’u neilltuo i’ch holl eiddo."
    }

    "return 200 & display correct content (Assigned to only property)" in new TestSetup {
      await(mockAppointAgentSessionRepository.saveOrUpdate(managingPropertyData
        .copy(totalPropertySelectionSize = 1).copy(singleProperty =  true)))

      lazy val document = getDocument(English)
      document.getElementById(assignedToId).text() shouldBe "They have also been assigned to your property."
    }

    "return 200 & display correct content (Assigned to only property) - Welsh" in new TestSetup {
      await(mockAppointAgentSessionRepository.saveOrUpdate(managingPropertyData
        .copy(singleProperty =  true)))

      lazy val document = getDocument(Welsh)
      document.getElementById(assignedToId).text() shouldBe "Maent hefyd wedi’u neilltuo i’ch eiddo."
    }
  }

  class TestSetup {

    lazy val mockAppointAgentSessionRepository: AppointAgentSessionRepository = app.injector.instanceOf[AppointAgentSessionRepository]
    lazy val mockAppointAgentPropertiesSessionRepository: AppointAgentPropertiesSessionRepository = app.injector.instanceOf[AppointAgentPropertiesSessionRepository]
    implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId(testSessionId)))

    val account = groupAccount(true)
    val agentCode = 1001
    val agentName = "Test Agent"
    val backLinkUrl = "some/url"

    val managingPropertyData: ManagingProperty = ManagingProperty(
      agentCode = agentCode,
      agentOrganisationName = "Some Org",
      isCorrectAgent = true,
      managingPropertyChoice = All.name,
      agentAddress = "An Address",
      backLink = None,
      totalPropertySelectionSize = 2,
      propertySelectedSize = 2
    )

    val propertiesSessionData: AppointAgentToSomePropertiesSession = AppointAgentToSomePropertiesSession(agentAppointAction =
      Some(AgentAppointBulkAction(agentCode = agentCode, name = agentName, propertyLinkIds = List("123", "321"), backLinkUrl = "some-back-link")),
      filters = FilterAppointProperties(None, None))

    await(mockAppointAgentSessionRepository.saveOrUpdate(managingPropertyData))
    await(mockAppointAgentPropertiesSessionRepository.saveOrUpdate(propertiesSessionData))

    stubFor {
      get("/business-rates-authorisation/authenticate")
        .willReturn {
          aResponse.withStatus(OK).withBody(Json.toJson(testAccounts).toString())
        }
    }

    stubFor {
      post("/auth/authorise")
        .willReturn {
          aResponse.withStatus(OK).withBody("{}")
        }
    }
  }

  def getDocument(language: Language): Document = {
    val res = await(
      ws.url(s"http://localhost:$port/business-rates-property-linking/my-organisation/confirm-appoint-agent")
        .withCookies(languageCookie(language), getSessionCookie(testSessionId))
        .withFollowRedirects(follow = false)
        .withHttpHeaders(HeaderNames.COOKIE -> "sessionId", "Csrf-Token" -> "nocheck")
        .get()
    )
    res.status shouldBe OK
    val document = Jsoup.parse(res.body)

    if (language == English) {
      document.title() shouldBe "Some Org has been appointed to your account - Valuation Office Agency - GOV.UK"
      document.getElementsByClass("govuk-panel__title").text() shouldBe "Some Org has been appointed to your account"
      document.getElementById("agent-can-text").text() shouldBe "This agent can:"
      document.select("#agent-can-list > li").text() shouldBe "add properties to your account"
      document.getElementById("what-happens-next-title").text() shouldBe "What happens next"
      document.getElementById("what-happens-next-text").text() shouldBe "You can assign or unassign this agent from your properties by managing your agents."
      document.getElementById("go-home-link").text() shouldBe "Go to your account home"
    } else {
      document.title() shouldBe "Mae Some Org wedi’i benodi i’ch cyfrif - Valuation Office Agency - GOV.UK"
      document.getElementsByClass("govuk-panel__title").text() shouldBe "Mae Some Org wedi’i benodi i’ch cyfrif"
      document.getElementById("agent-can-text").text() shouldBe "Gall yr asiant hwn:"
      document.select("#agent-can-list > li").text() shouldBe "ychwanegu eiddo at eich cyfrif"
      document.getElementById("what-happens-next-title").text() shouldBe "Beth sy’n digwydd nesaf"
      document.getElementById("what-happens-next-text").text() shouldBe "Gallwch neilltuo neu ddadneilltuo’r asiant hwn o’ch eiddo trwy reoli eich asiantiaid."
      document.getElementById("go-home-link").text() shouldBe "Ewch i hafan eich cyfrif"
    }

    document
  }
}
