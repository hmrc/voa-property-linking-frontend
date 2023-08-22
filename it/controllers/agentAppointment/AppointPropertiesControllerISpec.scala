package controllers.agentAppointment

import base.{HtmlComponentHelpers, ISpecBase}
import com.github.tomakehurst.wiremock.client.WireMock._
import models.propertyrepresentation.{All, ManagingProperty}
import play.api.http.HeaderNames
import play.api.http.Status.OK
import play.api.libs.json.Json
import play.api.test.Helpers._
import repositories.{AppointAgentPropertiesSessionRepository, AppointAgentSessionRepository}
import uk.gov.hmrc.http.{HeaderCarrier, SessionId}

import java.util.UUID

class AppointPropertiesControllerISpec extends ISpecBase with HtmlComponentHelpers {

  "onSubmit" should {
    "return 303 & redirect to 'check your answers page'" in new AppointSomePropertiesSetup {

      val requestBody = Json.obj(
        "agentCode" -> agentCode,
        "name" -> agentName,
        "linkIds" -> List(123L),
        "backLinkUrl" -> "some/back/link"
      )

      val res = await(
        ws.url(s"http://localhost:$port/business-rates-property-linking/my-organisation/appoint/properties?agentCode=$agentCode&backLinkUrl=$backLinkUrl&fromManageAgentJourney=true")
          .withCookies(languageCookie(English), getSessionCookie(testSessionId))
          .withFollowRedirects(follow = false)
          .withHttpHeaders(HeaderNames.COOKIE -> "sessionId", "Csrf-Token" -> "nocheck")
          .post(body = requestBody)
      )

      res.status shouldBe SEE_OTHER
      res.headers("Location").head shouldBe "/business-rates-property-linking/my-organisation/appoint-new-agent/check-your-answers"
    }

    "return 400 Bad_Request when no agent code is supplied" in new AppointSomePropertiesSetup {

      val requestBody = Json.obj(
        "agentCode" -> agentCode,
        "name" -> agentName,
        "linkIds" -> List(123L),
        "backLinkUrl" -> "some/back/link"
      )

      val res = await(
        ws.url(s"http://localhost:$port/business-rates-property-linking/my-organisation/appoint/properties?agentCode=&backLinkUrl=$backLinkUrl")
          .withCookies(languageCookie(English), getSessionCookie(testSessionId))
          .withFollowRedirects(follow = false)
          .withHttpHeaders(HeaderNames.COOKIE -> "sessionId", "Csrf-Token" -> "nocheck")
          .post(body = requestBody)
      )

      res.status shouldBe BAD_REQUEST
    }

    "return 404 when agent data is found in cache" in new AppointSomePropertiesSetup {

      await(mockAppointAgentSessionRepository.remove())

      val requestBody = Json.obj(
        "agentCode" -> agentCode,
        "name" -> agentName,
        "linkIds" -> List(123L),
        "backLinkUrl" -> "some/back/link"
      )

      val res = await(
        ws.url(s"http://localhost:$port/business-rates-property-linking/my-organisation/appoint/properties?agentCode=&backLinkUrl=$backLinkUrl")
          .withCookies(languageCookie(English), getSessionCookie(testSessionId))
          .withFollowRedirects(follow = false)
          .withHttpHeaders(HeaderNames.COOKIE -> "sessionId", "Csrf-Token" -> "nocheck")
          .post(body = requestBody))

      res.status shouldBe BAD_REQUEST
    }
  }

  class AppointSomePropertiesSetup {
    val testSessionId = s"stubbed-${UUID.randomUUID}"
    val account = groupAccount(true)
    val agentCode = 1001
    val agentName = "Test Agent"
    val backLinkUrl = "some/url"

    lazy val mockAppointAgentSessionRepository: AppointAgentSessionRepository = app.injector.instanceOf[AppointAgentSessionRepository]
    lazy val mockAppointAgentPropertiesSessionRepository: AppointAgentPropertiesSessionRepository = app.injector.instanceOf[AppointAgentPropertiesSessionRepository]
    implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId(testSessionId)))

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

    await(mockAppointAgentSessionRepository.saveOrUpdate(managingPropertyData))

    stubFor {
      get("/property-linking/owner/agents")
        .willReturn {
          aResponse.withStatus(OK).withBody(Json.toJson(testAgentListFor2017).toString())
        }
    }

    stubFor {
      get(s"/property-linking/groups/agentCode/$agentCode")
        .willReturn {
          aResponse.withStatus(OK).withBody(Json.toJson(account).toString())
        }
    }

    stubFor {
      get("/property-linking/owner/property-links?sortField=ADDRESS&sortOrder=ASC&startPoint=1&pageSize=100&requestTotalRowCount=false")
        .willReturn {
          aResponse.withStatus(OK).withBody(Json.toJson(testOwnerAuthResult).toString())
        }
    }

    stubFor {
      get("/property-linking/owner/property-links/count")
        .willReturn {
          aResponse.withStatus(OK).withBody(Json.toJson(testResultCount).toString())
        }
    }

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
