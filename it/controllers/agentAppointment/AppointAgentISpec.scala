package controllers.agentAppointment

import base.{HtmlComponentHelpers, ISpecBase}
import com.github.tomakehurst.wiremock.client.WireMock._
import models.propertyrepresentation.{AgentAppointmentChangesResponse, AgentList, AgentSummary, AppointmentScope}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.http.HeaderNames
import play.api.http.Status.OK
import play.api.libs.json.Json
import play.api.test.Helpers._
import repositories.ManageAgentSessionRepository
import uk.gov.hmrc.http.{HeaderCarrier, SessionId}

import java.time.LocalDate
import java.util.UUID

class AppointAgentISpec extends ISpecBase with HtmlComponentHelpers {

  "appointAgentSummary" should {
    "return 303 SEE OTHER with valid submission & assert that 2017 listYears are submitted to backend when agent summary returns 2017" in new AssignSomeSetup {

      stubFor {
        get("/property-linking/owner/agents")
          .willReturn {
            aResponse.withStatus(OK).withBody(Json.toJson(testAgentListFor2017).toString())
          }
      }

      val requestBody = Json.obj(
        "agentCode" -> agentCode,
        "name" -> agentName,
        "linkIds" -> List(123L),
        "backLinkUrl" -> "some/back/link"
      )

      val res = await(
        ws.url(s"http://localhost:$port/business-rates-property-linking/my-organisation/appoint/properties/create?agentCode=$agentCode&agentName=$agentName&backLinkUrl=$backLinkUrl")
          .withCookies(languageCookie(English), getSessionCookie(testSessionId))
          .withFollowRedirects(follow = false)
          .withHttpHeaders(HeaderNames.COOKIE -> "sessionId", "Csrf-Token" -> "nocheck")
          .post(body = requestBody)
      )

      val jsonRequest = Json.parse(
        """{
          |   "agentRepresentativeCode":1001,
          |   "action":"APPOINT",
          |   "scope":"PROPERTY_LIST",
          |   "propertyLinks":[
          |      "123"
          |   ],
          |   "listYears":[
          |      "2017"
          |   ]
          |}""".stripMargin)

      //Check that the listYears returned from agent summary call are included in request body to backend
      verify(1, postRequestedFor(urlEqualTo("/property-linking/my-organisation/agent/submit-appointment-changes"))
        .withRequestBody(equalToJson(jsonRequest.toString())))

      res.status shouldBe SEE_OTHER

    }

        "return 303 SEE OTHER assert that 2017&2023 listYears are submitted to backend when agent summary returns none for listYears" in new AssignSomeSetup {

          //Return agent in summary list that has no listYears assigned
          stubFor {
            get("/property-linking/owner/agents")
              .willReturn {
                aResponse.withStatus(OK).withBody(Json.toJson(testAgentNoListYears).toString())
              }
          }

          val requestBody = Json.obj(
            "agentCode" -> agentCode,
            "name" -> agentName,
            "linkIds" -> List(123L),
            "backLinkUrl" -> "some/back/link"
          )

          val res = await(
            ws.url(s"http://localhost:$port/business-rates-property-linking/my-organisation/appoint/properties/create?agentCode=$agentCode&agentName=$agentName&backLinkUrl=$backLinkUrl")
              .withCookies(languageCookie(English), getSessionCookie(testSessionId))
              .withFollowRedirects(follow = false)
              .withHttpHeaders(HeaderNames.COOKIE -> "sessionId", "Csrf-Token" -> "nocheck")
              .post(body = requestBody)
          )

          val jsonRequest = Json.parse(
            """{
              |   "agentRepresentativeCode":1001,
              |   "action":"APPOINT",
              |   "scope":"PROPERTY_LIST",
              |   "propertyLinks":[
              |      "123"
              |   ],
              |   "listYears":[
              |      "2017","2023"
              |   ]
              |}""".stripMargin)

          //Check that the when no listYears returned from agent summary call that listYears is defaulted to 2017&2023
          verify(1, postRequestedFor(urlEqualTo("/property-linking/my-organisation/agent/submit-appointment-changes"))
            .withRequestBody(equalToJson(jsonRequest.toString())))

          res.status shouldBe SEE_OTHER
        }
  }

  class AssignSomeSetup {
    val testSessionId = s"stubbed-${UUID.randomUUID}"
    val account = groupAccount(true)
    val agentCode = 1001
    val agentName = "Test Agent"
    val backLinkUrl = "some/url"

    stubFor {
      post("/property-linking/my-organisation/agent/submit-appointment-changes")
        .willReturn {
          aResponse.withStatus(OK).withBody(Json.toJson(AgentAppointmentChangesResponse("some-id")).toString())
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
