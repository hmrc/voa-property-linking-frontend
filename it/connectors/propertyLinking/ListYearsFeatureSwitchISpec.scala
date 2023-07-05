package connectors.propertyLinking

import base.ISpecBase
import com.github.tomakehurst.wiremock.client.WireMock.{aResponse, get, post, stubFor}
import models.propertyrepresentation.AgentSummary
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

class ListYearsFeatureSwitchISpec extends ISpecBase {

  override lazy val extraConfig: Map[String, Any] = Map(
    "feature-switch.agentListYears.enabled" -> "false"
  )

  val testSessionId = s"stubbed-${UUID.randomUUID}"

  lazy val mockRepository: ManageAgentSessionRepository = app.injector.instanceOf[ManageAgentSessionRepository]
  implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId(testSessionId)))

  "ChooseRatingListController show method" should {
    "Show a NOT_FOUND page when the agentListYears feature switch is disabled" in {
      getRequest(s"http://localhost:$port/business-rates-property-linking/my-organisation/appoint/ratings-list/choose")
    }
  }

  "ChooseRatingListController post method" should {
    "Show a NOT_FOUND page when the agentListYears feature switch is disabled" in {
      postRequest(s"http://localhost:$port/business-rates-property-linking/my-organisation/appoint/ratings-list/choose")
    }
  }

  "WhichRatingListController show method" should {
    "Show a NOT_FOUND page when the agentListYears feature switch is disabled" in {
      getRequest(s"http://localhost:$port/business-rates-property-linking/my-organisation/appoint/ratings-list/confirm")
    }
  }

  "WhichRatingListController post method" should {
    "Show a NOT_FOUND page when the agentListYears feature switch is disabled" in {
      postRequest(s"http://localhost:$port/business-rates-property-linking/my-organisation/appoint/ratings-list/confirm")
    }
  }

  "AreYouSureSingleController show method" should {
    "Show a NOT_FOUND page when the agentListYears feature switch is disabled" in {
      getRequest(s"http://localhost:$port/business-rates-property-linking/my-organisation/appoint/ratings-list/are-you-sure?chosenListYear=2017")
    }
  }

  "AreYouSureSingleController post method" should {
    "Show a NOT_FOUND page when the agentListYears feature switch is disabled" in {
      postRequest(s"http://localhost:$port/business-rates-property-linking/my-organisation/appoint/ratings-list/are-you-sure?chosenListYear=2017")
    }
  }

  "AreYouSureMultipleController show method" should {
    "Show a NOT_FOUND page when the agentListYears feature switch is disabled" in {
      getRequest(s"http://localhost:$port/business-rates-property-linking/my-organisation/appoint/ratings-list/are-you-sure-multiple")
    }
  }

  "AreYouSureMultipleController post method" should {
    "Show a NOT_FOUND page when the agentListYears feature switch is disabled" in {
      postRequest(s"http://localhost:$port/business-rates-property-linking/my-organisation/appoint/ratings-list/are-you-sure-multiple")
    }
  }

  "RatingListConfirmedController show method" should {
    "Show a NOT_FOUND page when the agentListYears feature switch is disabled" in {
      getRequest(s"http://localhost:$port/business-rates-property-linking/my-organisation/appoint/ratings-list/confirmed")
    }
  }

  private def getRequest(url: String): Document = {

    await(
      mockRepository.saveOrUpdate(
        AgentSummary(
          listYears = Some(List("2017")),
          name = "Test Agent",
          organisationId = 100L,
          representativeCode = 100L,
          appointedDate = LocalDate.now(),
          propertyCount = 1
        )))

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

    val res = await(
      ws.url(url)
        .withCookies(languageCookie(English), getSessionCookie(testSessionId))
        .withFollowRedirects(follow = false)
        .get()
    )

    res.status shouldBe NOT_FOUND
    Jsoup.parse(res.body)
  }

  private def postRequest(url: String): Document = {

    await(
      mockRepository.saveOrUpdate(
        AgentSummary(
          listYears = Some(List("2017")),
          name = "Test Agent",
          organisationId = 100L,
          representativeCode = 100L,
          appointedDate = LocalDate.now(),
          propertyCount = 1
        )))

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

    val res = await(
      ws.url(url)
        .withCookies(languageCookie(English), getSessionCookie(testSessionId))
        .withFollowRedirects(follow = false)
        .withHttpHeaders(HeaderNames.COOKIE -> "sessionId", "Csrf-Token" -> "nocheck")
        .post(body="")
    )

    res.status shouldBe NOT_FOUND
    Jsoup.parse(res.body)
  }

}
