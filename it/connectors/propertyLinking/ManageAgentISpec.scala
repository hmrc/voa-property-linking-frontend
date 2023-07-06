package connectors.propertyLinking

import base.{HtmlComponentHelpers, ISpecBase}
import com.github.tomakehurst.wiremock.client.WireMock.{aResponse, get, post, stubFor}
import models.propertyrepresentation.AgentSummary
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.http.Status.OK
import play.api.libs.json.Json
import play.api.test.Helpers._
import repositories.ManageAgentSessionRepository
import uk.gov.hmrc.http.{HeaderCarrier, SessionId}

import java.time.LocalDate
import java.util.UUID

class ManageAgentISpec extends ISpecBase with HtmlComponentHelpers {

  val testSessionId = s"stubbed-${UUID.randomUUID}"

  lazy val mockRepository: ManageAgentSessionRepository = app.injector.instanceOf[ManageAgentSessionRepository]
  implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId(testSessionId)))

  val titleText = "Manage agent - Valuation Office Agency - GOV.UK"
  val backLinkText ="Back"
  val continueButtonText = "Continue"
  val captionText = "Manage agent"

  def headerText(name: String) = s"What do you want to do to the agent $name?"

  val radioAssignAllText= "Assign to all properties"
  val radioAssignASomeText = "Assign to some properties"
  val radioUnassignedAllText = "Unassign from all properties"
  val radioUnassignedASomeText = "Unassign from some properties"
  val radioChangeText = "Change which rating list they can act on for you"
  val radioRemoveText = "Remove from your account"

  val titleTextWelsh  ="Rheoli asiant- Valuation Office Agency - GOV.UK"
  val backLinkTextWelsh  ="Yn ôl"
  val continueButtonTextWelsh ="Parhau"
  val captionTextWelsh  = "Rheoli asiant"

  def headerTextWelsh(name: String) = s"Beth ydych chi eisiau ei wneud i’r asiant  $name?"

  val radioAssignAllTextWelsh = "Neilltuo i bob eiddo"
  val radioAssignASomeTextWelsh  = "Neilltuo i ambell eiddo"
  val radioUnassignedAllTextWelsh  = "Dad-neilltuo o’ch holl eiddo"
  val radioUnassignedASomeTextWelsh  = "Dad-neilltuo o rhai eiddo"
  val radioChangeTextWelsh  = "Change which rating list they can act on for you"
  val radioRemoveTextWelsh  = "Dileu o’ch cyfrif"



  val backLinkSelector = "#back-link"
  val captionSelector = ".govuk-caption-l"
  val headerSelector = "h1.govuk-heading-l"
  val continueButtonSelector ="button.govuk-button"
  val firstRadioLabelSelector =".govuk-radios__item:nth-child(1) > .govuk-label"
  val secondRadioLabelSelector =".govuk-radios__item:nth-child(2) > .govuk-label"
  val thirdRadioLabelSelector =".govuk-radios__item:nth-child(3) > .govuk-label"
  val fourthRadioLabelSelector =".govuk-radios__item:nth-child(4) > .govuk-label"
  val fifthRadioLabelSelector =".govuk-radios__item:nth-child(5) > .govuk-label"

  val backLinkHref = s"/business-rates-property-linking/my-organisation/manage-agent/property-links?agentCode=100"
  val continueHref = ""
  val radioAssignAllHref = "business-rates-property-linking/my-organisation/manage-agent/assign/to-all-properties"
  val radioAssignASomeHref = "http://localhost:9523/business-rates-property-linking/my-organisation/appoint/properties?page=1&pageSize=15&agentCode=1001&agentAppointed=BOTH&backLink=%2Fbusiness-rates-property-linking%2Fmy-organisation%2Fmanage-agent"
  val radioUnassignedAllHref = "/business-rates-property-linking/my-organisation/manage-agent/unassign/from-all-properties"
  val radioUnassignedSomeHref = "/business-rates-property-linking/my-organisation/revoke/properties?page=1&pageSize=15&agentCode=36"
  val radioChangeHref = "Change which rating list they can act on for you"
  val radioRemoveHref = "/business-rates-property-linking/my-organisation/manage-agent/remove/from-organisation"

  "ManageAgentController showManageAgent method" should {
    "display 'Manage agent' screen with the correct text and the language is set to English" which {

      lazy val document = getManageAgentsPage(English)

      s"has a title of $titleText" in {
        document.title() shouldBe titleText
      }

      "has a back link which takes you to Manage agent properties screen" in {
        document.select(backLinkSelector).text() shouldBe backLinkText
        document.select(backLinkSelector).attr("href") shouldBe backLinkHref
      }

      s"displays a correct header with agent name included" in {
        document.select(headerSelector).text() shouldBe headerText(name = "Test Agent")
      }

      s"displays a correct caption above the header" in{
        document.select(captionSelector).text shouldBe captionText
      }

      s"displays a correct text in continue button" in {
        document.select(continueButtonSelector).text shouldBe continueButtonText
      }

      s"displays a correct text in first radio label" in {
        document.select(firstRadioLabelSelector).text shouldBe radioAssignAllText
      }

      }
    }

  private def getManageAgentsPage(language: Language): Document = {

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
      get("/property-linking/owner/property-links?sortField=ADDRESS&sortOrder=ASC&startPoint=1&pageSize=100&requestTotalRowCount=false")
        .willReturn {
          aResponse.withStatus(OK).withBody(Json.toJson(testOwnerAuthResult).toString())
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
      ws.url(s"http://localhost:$port/business-rates-property-linking/my-organisation/manage-agent")
        .withCookies(languageCookie(language), getSessionCookie(testSessionId))
        .withFollowRedirects(follow = false)
        .get()
    )

    res.status shouldBe OK
    Jsoup.parse(res.body)
  }
}
