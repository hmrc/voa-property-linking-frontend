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

class ConfirmAgentAppointControllerFSOnISpec extends ISpecBase with HtmlComponentHelpers {

  val testSessionId = s"stubbed-${UUID.randomUUID}"
  val assignedToId = "rates-for"

  val linkID = "showAgent"

  "onPageLoad" should {
    "return 200 & display correct content (both tax year rates)" in new TestSetup(Some(true), None) {

      await(
        mockAppointAgentSessionRepository.saveOrUpdate(
          managingPropertyData.copy(managingPropertyChoice = ChooseFromList.name)))
      await(
        mockAppointAgentPropertiesSessionRepository
          .saveOrUpdate(propertiesSessionData.agentAppointAction.map(_.copy(propertyLinkIds = List("123")))))

      lazy val document = getDocument(English)
      document
        .getElementById(assignedToId)
        .text() shouldBe "act for you on your property valuations on the 2023 and 2017 rating lists, for properties that you assign to them or they add to your account"

    }

    "return 200 & display correct content (2017 tax year rates)" in new TestSetup(Some(false), Some("2017")) {

      lazy val document = getDocument(English)
      document
        .getElementById(assignedToId)
        .text() shouldBe "act for you on your property valuations on the 2017 rating list, for properties that you assign to them or they add to your account"

    }

    "return 200 & display correct content (2023 tax year rates)" in new TestSetup(Some(false), Some("2023")) {
      await(
        mockAppointAgentSessionRepository.saveOrUpdate(
          managingPropertyData
            .copy(totalPropertySelectionSize = 1)
            .copy(singleProperty = true)))

      lazy val document = getDocument(English)
      document
        .getElementById(assignedToId)
        .text() shouldBe "act for you on your property valuations on the 2023 rating list, for properties that you assign to them or they add to your account"
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
      document.select("#agent-can-list > li:nth-child(1)").text() shouldBe "add properties to your account"
      document.getElementById("what-happens-next-title").text() shouldBe "What happens next"
      document
        .select("#main-content > div > div > p:nth-child(5)")
        .text() shouldBe "You can assign or unassign this agent from your properties or change the rating lists they can act for you on by managing your agents."
      document.getElementById("go-home-link").text() shouldBe "Go to your account home"
    } else {
      document.title() shouldBe "Mae Some Org wedi’i benodi i’ch cyfrif - Valuation Office Agency - GOV.UK"
      document.getElementsByClass("govuk-panel__title").text() shouldBe "Mae Some Org wedi’i benodi i’ch cyfrif"
      document.getElementById("agent-can-text").text() shouldBe "Gall yr asiant hwn:"
      document.select("#agent-can-list > li:nth-child(1)").text() shouldBe "ychwanegu eiddo at eich cyfrif"
      document.getElementById("what-happens-next-title").text() shouldBe "Beth sy’n digwydd nesaf"
      document
        .getElementById("what-happens-next-text")
        .text() shouldBe "Gallwch neilltuo neu ddadneilltuo’r asiant hwn o’ch eiddo trwy reoli eich asiantiaid."
      document.getElementById("go-home-link").text() shouldBe "Ewch i hafan eich cyfrif"
    }

    document
  }

  class TestSetup(bothRatingList: Option[Boolean], specificRatingList: Option[String]) {

    lazy val mockAppointAgentSessionRepository: AppointAgentSessionRepository =
      app.injector.instanceOf[AppointAgentSessionRepository]
    lazy val mockAppointAgentPropertiesSessionRepository: AppointAgentPropertiesSessionRepository =
      app.injector.instanceOf[AppointAgentPropertiesSessionRepository]
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
      propertySelectedSize = 2,
      bothRatingLists = bothRatingList,
      specificRatingList = specificRatingList
    )

    val propertiesSessionData: AppointAgentToSomePropertiesSession = AppointAgentToSomePropertiesSession(
      agentAppointAction = Some(
        AgentAppointBulkAction(
          agentCode = agentCode,
          name = agentName,
          propertyLinkIds = List("123", "321"),
          backLinkUrl = "some-back-link")),
      filters = FilterAppointProperties(None, None)
    )

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
}
