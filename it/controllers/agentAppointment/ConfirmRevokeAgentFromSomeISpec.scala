package controllers.agentAppointment

import base.{HtmlComponentHelpers, ISpecBase}
import com.github.tomakehurst.wiremock.client.WireMock.{aResponse, get, post, stubFor}
import models.AgentRevokeBulkAction
import models.propertyrepresentation.RevokeAgentFromSomePropertiesSession
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.http.HeaderNames
import play.api.http.Status.{NOT_FOUND, OK}
import play.api.libs.json.Json
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import repositories.RevokeAgentPropertiesSessionRepository
import uk.gov.hmrc.http.{HeaderCarrier, SessionId}

import java.util.UUID

class ConfirmRevokeAgentFromSomeISpec extends ISpecBase with HtmlComponentHelpers {

  val testSessionId = s"stubbed-${UUID.randomUUID}"

  val agentName = "test agent name"

  lazy val mockRevokeAgentPropertiesSessionRepository: RevokeAgentPropertiesSessionRepository = app.injector.instanceOf[RevokeAgentPropertiesSessionRepository]
  implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId(testSessionId)))

  val headingSelector = "h1"
  val theAgentCanSelector = "#revoke-agent-summary-p1"
  val theAgentHasSelector = "#revoke-agent-summary-p2"
  val whatHappensNextSelector = "#main-content > div > div > h2"
  val youCanReassignSelector = "#revoke-agent-summary-p3"
  val goHomeLinkSelector = "#main-content > div > div > p:nth-child(6) > a"

  val titleText = s"$agentName has been unassigned from your selected properties - Valuation Office Agency - GOV.UK"
  val headingText = s"$agentName has been unassigned from your selected properties"
  val theAgentCanText = "The agent can no longer act for you on any of the properties you selected."
  val theAgentHasText = "The agent has not been removed from your account. They can still act for you if they add other properties to your account."
  val whatHappensNextText = "What happens next"
  val youCanReassignText = "You can reassign an agent to a property if you want them to act for you again."
  val goHomeLinkText = "Go to your account home"

  val titleTextWelsh = s"Mae $agentName wedi’i ddadneilltuo o’r eiddo a ddewiswyd gennych - Valuation Office Agency - GOV.UK"
  val headingTextWelsh = s"Mae $agentName wedi’i ddadneilltuo o’r eiddo a ddewiswyd gennych"
  val theAgentCanTextWelsh = "Ni all yr asiant weithredu ar eich rhan mwyach ar unrhyw un o’r eiddo a ddewiswyd gennych."
  val theAgentHasTextWelsh = "Nid yw’r asiant wedi’i dynnu o’ch cyfrif. Gallant barhau i weithredu ar eich rhan os ydynt yn ychwanegu eiddo eraill at eich cyfrif."
  val whatHappensNextTextWelsh = "Yr hyn sy’n digwydd nesaf"
  val youCanReassignTextWelsh = "Gallwch ailbennu asiant i eiddo os ydych am iddynt weithredu ar eich rhan eto."
  val goHomeLinkTextWelsh = "Ewch i hafan eich cyfrif"
  
  val goHomeLinkHref = "/business-rates-dashboard/home"

  "confirmRevokeAgentFromSome method displays the correct content in English" which {
    lazy val document: Document = getConfirmRevokeAgentFromSomePage(English)

    s"has a title of $titleText" in {
      document.title() shouldBe titleText
    }

    s"has a heading of $headingText" in {
      document.getElementsByTag(headingSelector).text shouldBe headingText
    }

    s"has text on the screen of $theAgentCanText" in {
      document.select(theAgentCanSelector).text shouldBe theAgentCanText
    }

    s"has text on the screen of $theAgentHasText" in {
      document.select(theAgentHasSelector).text shouldBe theAgentHasText
    }

    s"has a subheading of $whatHappensNextText" in {
      document.select(whatHappensNextSelector).text shouldBe whatHappensNextText
    }

    s"has text on the screen of $youCanReassignText" in {
      document.select(youCanReassignSelector).text shouldBe youCanReassignText
    }

    s"has a $goHomeLinkText link" in {
      document.select(goHomeLinkSelector).text shouldBe goHomeLinkText
      document.select(goHomeLinkSelector).attr("href") shouldBe goHomeLinkHref
    }
  }

  "confirmRevokeAgentFromSome method displays the correct content in Welsh" which {
    lazy val document: Document = getConfirmRevokeAgentFromSomePage(Welsh)

    s"has a title of $titleText in welsh" in {
      document.title() shouldBe titleTextWelsh
    }

    s"has a heading of $headingText in welsh" in {
      document.getElementsByTag(headingSelector).text shouldBe headingTextWelsh
    }

    s"has text on the screen of $theAgentCanText in welsh" in {
      document.select(theAgentCanSelector).text shouldBe theAgentCanTextWelsh
    }

    s"has text on the screen of $theAgentHasText in welsh" in {
      document.select(theAgentHasSelector).text shouldBe theAgentHasTextWelsh
    }

    s"has a subheading of $whatHappensNextText in welsh" in {
      document.select(whatHappensNextSelector).text shouldBe whatHappensNextTextWelsh
    }

    s"has text on the screen of $youCanReassignText in welsh" in {
      document.select(youCanReassignSelector).text shouldBe youCanReassignTextWelsh
    }

    s"has a $goHomeLinkText link in welsh" in {
      document.select(goHomeLinkSelector).text shouldBe goHomeLinkTextWelsh
      document.select(goHomeLinkSelector).attr("href") shouldBe goHomeLinkHref
    }
  }

  "confirmRevokeAgentFromSome method returns a not found if no agent revoke data is cached" in {

    await(
      mockRevokeAgentPropertiesSessionRepository.remove()
    )

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
      ws.url(s"http://localhost:$port/business-rates-property-linking/my-organisation/revoke/properties/confirm")
        .withCookies(languageCookie(English), getSessionCookie(testSessionId))
        .withFollowRedirects(follow = false)
        .withHttpHeaders(HeaderNames.COOKIE -> "sessionId")
        .get()
    )

    res.status shouldBe NOT_FOUND
  }


  private def getConfirmRevokeAgentFromSomePage(language: Language): Document = {

    await(
      mockRevokeAgentPropertiesSessionRepository.saveOrUpdate(
        RevokeAgentFromSomePropertiesSession(
          agentRevokeAction = Some(
            AgentRevokeBulkAction(
              agentCode = 123L,
              name = agentName,
              propertyLinkIds = List(),
              backLinkUrl = "/back-link"
            )
          )
        )
      )
    )

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
      ws.url(s"http://localhost:$port/business-rates-property-linking/my-organisation/revoke/properties/confirm")
        .withCookies(languageCookie(language), getSessionCookie(testSessionId))
        .withFollowRedirects(follow = false)
        .withHttpHeaders(HeaderNames.COOKIE -> "sessionId")
        .get()
    )

    res.status shouldBe OK
    Jsoup.parse(res.body)
  }

}
