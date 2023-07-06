package connectors.propertyLinking

import base.{HtmlComponentHelpers, ISpecBase}
import com.github.tomakehurst.wiremock.client.WireMock.{aResponse, get, post, stubFor}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.http.Status.OK
import play.api.libs.json.Json
import play.api.test.Helpers._
import repositories.ManageAgentSessionRepository
import uk.gov.hmrc.http.{HeaderCarrier, SessionId}
import java.util.UUID

class MyAgentPropertiesISpec extends ISpecBase with HtmlComponentHelpers {

  val testSessionId = s"stubbed-${UUID.randomUUID}"

  lazy val mockRepository: ManageAgentSessionRepository = app.injector.instanceOf[ManageAgentSessionRepository]
  implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId(testSessionId)))

  val titleText = "Your agent - Valuation Office Agency - GOV.UK"
  val backLinkText = "Back"
  val captionText = "Agent"
  val noOrMultipleRatingListText = "This agent can act for you on your property valuations on the 2023 and 2017 rating lists, for properties that you assign them to or they add to your account."
  val manageButtonText = "Appoint an agent"
  val ratingListSectionHeading = "Rating lists they can act on for you"
  val assignedPropertiesHeading = "Assigned properties"
  val assignedToNoProperties = "This agent is not assigned to any properties"

  def headingText(name: String) = s"Agent $name"
  def singleRatingListText(ratingList: String) = s"This agent can act for you on your property valuations on the $ratingList, for properties that you assign them to or they add to your account."

  def assignedPropertyListElement(address: String) = address

  def headingTextWelsh(name: String) = s"Agent $name"
  def singleRatingListTextWelsh(ratingList: String) = s"This agent can act for you on your property valuations on the $ratingList, for properties that you assign them to or they add to your account."

  val titleTextWelsh = "Eich asiant - Valuation Office Agency - GOV.UK"
  val backLinkTextWelsh = "Yn ôl"
  val captionTextWelsh = "Asiant"
  val noOrMultipleRatingListTextWelsh = "This agent can act for you on your property valuations on the 2023 and 2017 rating lists, for properties that you assign them to or they add to your account."
  val manageButtonTextWelsh = "Rheoli’r asiant hwn"
  val ratingListSectionHeadingWelsh = "Rating lists they can act on for you"
  val assignedPropertiesHeadingWelsh = "Eiddo wedi’u neilltuo"
  val assignedToNoPropertiesWelsh = "This agent is not assigned to any properties"

  "ManageAgentController showAgents method" should {
    "display 'Your agents' screen with the correct text and the language is set to English" which {

      lazy val document = getYourAgentsPropertiesPage(English)

      s"has a title of $titleText" in {
        document.title() shouldBe titleText
      }
    }
    s"displays a correct header with agent name included" in {
      document.select(headerSelector).text() shouldBe headerText(name = "Test Agent")
    }

    s"displays a correct caption above the header" in {
      document.select(captionSelector).text shouldBe captionText
    }

    s"displays a correct text in continue button" in {
      document.select(continueButtonText).text shouldBe continueButtonText
    }

  }

  private def getYourAgentsPropertiesPage(language: Language, listYears: List[String]): Document = {


    stubFor {
      get("/property-linking/my-organisation/agents/1001/property-links?sortField=ADDRESS&sortOrder=ASC&startPoint=1&pageSize=100&requestTotalRowCount=true")
        .willReturn {
          aResponse.withStatus(OK).withBody(Json.toJson(testOwnerAuthResult).toString())
        }
    }

    stubFor {
      get("/property-linking/my-organisation/agent/1001")
        .willReturn {
          aResponse.withStatus(OK).withBody(Json.toJson(testAgentDetails).toString())
        }
    }


    stubFor {
      get("/property-linking/owner/agents")
        .willReturn {
          aResponse.withStatus(OK).withBody(Json.toJson(testAgentList).toString())
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

    val res = await(
      ws.url(s"http://localhost:$port/business-rates-property-linking/my-organisation/manage-agent/property-links?agentCode=1001")
        .withCookies(languageCookie(language), getSessionCookie(testSessionId))
        .withFollowRedirects(follow = false)
        .get()
    )

    res.status shouldBe OK
    Jsoup.parse(res.body)

  }
}