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

  // These are all the scenarios where the agentListYears flag is disabled, see ConfirmAgentAppointControllerFSOnISpec for when its enabled
  override lazy val extraConfig: Map[String, Any] = Map(
    "feature-switch.agentListYears.enabled" -> "false"
  )

  val testSessionId = s"stubbed-${UUID.randomUUID}"
  val agentCode = 1001
  val agentName = "Test Agent"

  lazy val mockAppointAgentSessionRepository: AppointAgentSessionRepository =
    app.injector.instanceOf[AppointAgentSessionRepository]
  lazy val mockAppointAgentPropertiesSessionRepository: AppointAgentPropertiesSessionRepository =
    app.injector.instanceOf[AppointAgentPropertiesSessionRepository]
  implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId(testSessionId)))

  val titleText = s"$agentName has been appointed to your account - Valuation Office Agency - GOV.UK"
  val headingText = s"$agentName has been appointed to your account"
  val somePropertiesText = "They have also been assigned to the properties you selected."
  val allPropertiesText = "They have also been assigned to all your properties."
  val onePropertyText = "They have also been assigned to your property."
  val thisAgentCanText = "This agent can:"
  val addPropertiesText = "add properties to your account"
  val whatHappensNextText = "What happens next"
  val youCanAssignText = "You can assign or unassign this agent from your properties by managing your agents."
  val managingAgentsText = "managing your agents"
  val goToHomeText = "Go to your account home"

  val titleTextWelsh = s"Mae $agentName wedi’i benodi i’ch cyfrif - Valuation Office Agency - GOV.UK"
  val headingTextWelsh = s"Mae $agentName wedi’i benodi i’ch cyfrif"
  val somePropertiesTextWelsh = "Maent hefyd wedi’u neilltuo i’r eiddo a ddewiswyd gennych."
  val allPropertiesTextWelsh = "Maent hefyd wedi’u neilltuo i’ch holl eiddo."
  val onePropertyTextWelsh = "Maent hefyd wedi’u neilltuo i’ch eiddo."
  val thisAgentCanTextWelsh = "Gall yr asiant hwn:"
  val addPropertiesTextWelsh = "ychwanegu eiddo at eich cyfrif"
  val whatHappensNextTextWelsh = "Beth sy’n digwydd nesaf"
  val youCanAssignTextWelsh = "Gallwch neilltuo neu ddadneilltuo’r asiant hwn o’ch eiddo trwy reoli eich asiantiaid."
  val managingAgentsTextWelsh = "reoli eich asiantiaid"
  val goToHomeTextWelsh = "Ewch i hafan eich cyfrif"

  val headingSelector = "h1"
  val somePropertiesSelector = "#assigned-to"
  val allPropertiesSelector = "#assigned-to"
  val onePropertySelector = "#assigned-to"
  val thisAgentCanSelector = "#agent-can-text"
  val addPropertiesSelector = "#agent-can-list > li"
  val whatHappensNextSelector = "#what-happens-next-title"
  val youCanAssignSelector = "#what-happens-next-text"
  val managingAgentsLinkSelector = "#manage-agents-link"
  val goToHomeSelector = "#go-home-link"

  val managingAgentsLinkHref = "/business-rates-property-linking/my-organisation/agents"
  val goToHomeLinkHref = "/business-rates-dashboard/home"


  "onPageLoad" should {
    "return 200 & display the correct English content when the agent has been assigned to some properties" when {

      lazy val document: Document = getDocument(English, "Some")

      s"has a title of $titleText" in {
        document.title() shouldBe titleText
      }

      s"has a header of '$headingText'" in {
        document.select(headingSelector).text shouldBe headingText
      }

      s"has text on the screen of $somePropertiesText" in {
        document.select(somePropertiesSelector).text shouldBe somePropertiesText
      }

      s"has text on the screen of $thisAgentCanText" in {
        document.select(thisAgentCanSelector).text shouldBe thisAgentCanText
      }

      s"has a bullet point on the screen of $addPropertiesText" in {
        document.select(addPropertiesSelector).text shouldBe addPropertiesText
      }

      s"has a subheading on the screen of $whatHappensNextText" in {
        document.select(whatHappensNextSelector).text shouldBe whatHappensNextText
      }

      s"has text on the screen of $youCanAssignText" in {
        document.select(youCanAssignSelector).text shouldBe youCanAssignText
      }

      s"has a $managingAgentsText link which takes you to Manage agent properties screen" in {
        document.select(managingAgentsLinkSelector).text() shouldBe managingAgentsText
        document.select(managingAgentsLinkSelector).attr("href") shouldBe managingAgentsLinkHref
      }

      s"has a $goToHomeText link which takes you to the home screen" in {
        document.select(goToHomeSelector).text() shouldBe goToHomeText
        document.select(goToHomeSelector).attr("href") shouldBe goToHomeLinkHref
      }

    }

    "return 200 & display the correct English content when the agent has been assigned to all properties" when {

      lazy val document: Document = getDocument(English, "All")

      s"has a title of $titleText" in {
        document.title() shouldBe titleText
      }

      s"has a header of '$headingText'" in {
        document.select(headingSelector).text shouldBe headingText
      }

      s"has text on the screen of $allPropertiesText" in {
        document.select(allPropertiesSelector).text shouldBe allPropertiesText
      }

      s"has text on the screen of $thisAgentCanText" in {
        document.select(thisAgentCanSelector).text shouldBe thisAgentCanText
      }

      s"has a bullet point on the screen of $addPropertiesText" in {
        document.select(addPropertiesSelector).text shouldBe addPropertiesText
      }

      s"has a subheading on the screen of $whatHappensNextText" in {
        document.select(whatHappensNextSelector).text shouldBe whatHappensNextText
      }

      s"has text on the screen of $youCanAssignText" in {
        document.select(youCanAssignSelector).text shouldBe youCanAssignText
      }

      s"has a $managingAgentsText link which takes you to Manage agent properties screen" in {
        document.select(managingAgentsLinkSelector).text() shouldBe managingAgentsText
        document.select(managingAgentsLinkSelector).attr("href") shouldBe managingAgentsLinkHref
      }

      s"has a $goToHomeText link which takes you to the home screen" in {
        document.select(goToHomeSelector).text() shouldBe goToHomeText
        document.select(goToHomeSelector).attr("href") shouldBe goToHomeLinkHref
      }

    }

    "return 200 & display the correct English content when the agent has been assigned to only one property" when {

      lazy val document: Document = getDocument(English, "One")

      s"has a title of $titleText" in {
        document.title() shouldBe titleText
      }

      s"has a header of '$headingText'" in {
        document.select(headingSelector).text shouldBe headingText
      }

      s"has text on the screen of $onePropertyText" in {
        document.select(onePropertySelector).text shouldBe onePropertyText
      }

      s"has text on the screen of $thisAgentCanText" in {
        document.select(thisAgentCanSelector).text shouldBe thisAgentCanText
      }

      s"has a bullet point on the screen of $addPropertiesText" in {
        document.select(addPropertiesSelector).text shouldBe addPropertiesText
      }

      s"has a subheading on the screen of $whatHappensNextText" in {
        document.select(whatHappensNextSelector).text shouldBe whatHappensNextText
      }

      s"has text on the screen of $youCanAssignText" in {
        document.select(youCanAssignSelector).text shouldBe youCanAssignText
      }

      s"has a $managingAgentsText link which takes you to Manage agent properties screen" in {
        document.select(managingAgentsLinkSelector).text() shouldBe managingAgentsText
        document.select(managingAgentsLinkSelector).attr("href") shouldBe managingAgentsLinkHref
      }

      s"has a $goToHomeText link which takes you to the home screen" in {
        document.select(goToHomeSelector).text() shouldBe goToHomeText
        document.select(goToHomeSelector).attr("href") shouldBe goToHomeLinkHref
      }

    }

    "return 200 & display the correct Welsh content when the agent has been assigned to some properties" when {

      lazy val document: Document = getDocument(Welsh, "Some")

      s"has a title of $titleText in welsh" in {
        document.title() shouldBe titleTextWelsh
      }

      s"has a header of '$headingText' in welsh" in {
        document.select(headingSelector).text shouldBe headingTextWelsh
      }

      s"has text on the screen of $somePropertiesText in welsh" in {
        document.select(somePropertiesSelector).text shouldBe somePropertiesTextWelsh
      }

      s"has text on the screen of $thisAgentCanText in welsh" in {
        document.select(thisAgentCanSelector).text shouldBe thisAgentCanTextWelsh
      }

      s"has a bullet point on the screen of $addPropertiesText in welsh" in {
        document.select(addPropertiesSelector).text shouldBe addPropertiesTextWelsh
      }

      s"has a subheading on the screen of $whatHappensNextText in welsh" in {
        document.select(whatHappensNextSelector).text shouldBe whatHappensNextTextWelsh
      }

      s"has text on the screen of $youCanAssignText in welsh" in {
        document.select(youCanAssignSelector).text shouldBe youCanAssignTextWelsh
      }

      s"has a $managingAgentsText link which takes you to Manage agent properties screen in welsh" in {
        document.select(managingAgentsLinkSelector).text() shouldBe managingAgentsTextWelsh
        document.select(managingAgentsLinkSelector).attr("href") shouldBe managingAgentsLinkHref
      }

      s"has a $goToHomeText link which takes you to the home screen in welsh" in {
        document.select(goToHomeSelector).text() shouldBe goToHomeTextWelsh
        document.select(goToHomeSelector).attr("href") shouldBe goToHomeLinkHref
      }

    }

    "return 200 & display the correct Welsh content when the agent has been assigned to all properties" when {

      lazy val document: Document = getDocument(Welsh, "All")

      s"has a title of $titleText  in welsh" in {
        document.title() shouldBe titleTextWelsh
      }

      s"has a header of '$headingText' in welsh" in {
        document.select(headingSelector).text shouldBe headingTextWelsh
      }

      s"has text on the screen of $allPropertiesText in welsh" in {
        document.select(allPropertiesSelector).text shouldBe allPropertiesTextWelsh
      }

      s"has text on the screen of $thisAgentCanText in welsh" in {
        document.select(thisAgentCanSelector).text shouldBe thisAgentCanTextWelsh
      }

      s"has a bullet point on the screen of $addPropertiesText in welsh" in {
        document.select(addPropertiesSelector).text shouldBe addPropertiesTextWelsh
      }

      s"has a subheading on the screen of $whatHappensNextText in welsh" in {
        document.select(whatHappensNextSelector).text shouldBe whatHappensNextTextWelsh
      }

      s"has text on the screen of $youCanAssignText in welsh" in {
        document.select(youCanAssignSelector).text shouldBe youCanAssignTextWelsh
      }

      s"has a $managingAgentsText link which takes you to Manage agent properties screen in welsh" in {
        document.select(managingAgentsLinkSelector).text() shouldBe managingAgentsTextWelsh
        document.select(managingAgentsLinkSelector).attr("href") shouldBe managingAgentsLinkHref
      }

      s"has a $goToHomeText link which takes you to the home screen in welsh" in {
        document.select(goToHomeSelector).text() shouldBe goToHomeTextWelsh
        document.select(goToHomeSelector).attr("href") shouldBe goToHomeLinkHref
      }

    }

    "return 200 & display the correct Welsh content when the agent has been assigned to only one property" when {

      lazy val document: Document = getDocument(Welsh, "One")

      s"has a title of $titleText in welsh" in {
        document.title() shouldBe titleTextWelsh
      }

      s"has a header of '$headingText' in welsh" in {
        document.select(headingSelector).text shouldBe headingTextWelsh
      }

      s"has text on the screen of $onePropertyText in welsh" in {
        document.select(onePropertySelector).text shouldBe onePropertyTextWelsh
      }

      s"has text on the screen of $thisAgentCanText in welsh" in {
        document.select(thisAgentCanSelector).text shouldBe thisAgentCanTextWelsh
      }

      s"has a bullet point on the screen of $addPropertiesText in welsh" in {
        document.select(addPropertiesSelector).text shouldBe addPropertiesTextWelsh
      }

      s"has a subheading on the screen of $whatHappensNextText in welsh" in {
        document.select(whatHappensNextSelector).text shouldBe whatHappensNextTextWelsh
      }

      s"has text on the screen of $youCanAssignText in welsh" in {
        document.select(youCanAssignSelector).text shouldBe youCanAssignTextWelsh
      }

      s"has a $managingAgentsText link which takes you to Manage agent properties screen in welsh" in {
        document.select(managingAgentsLinkSelector).text() shouldBe managingAgentsTextWelsh
        document.select(managingAgentsLinkSelector).attr("href") shouldBe managingAgentsLinkHref
      }

      s"has a $goToHomeText link which takes you to the home screen in welsh" in {
        document.select(goToHomeSelector).text() shouldBe goToHomeTextWelsh
        document.select(goToHomeSelector).attr("href") shouldBe goToHomeLinkHref
      }

    }

  }

  def getDocument(language: Language, properties: String): Document = {

    val (managingPropertyChoice, singlePropertyChoice): (String, Boolean) = properties match {
      case "One" => (ChooseFromList.name, true)
      case "Some" => (ChooseFromList.name, false)
      case "All" => (All.name, false)
      case _ => ("Invalid", false)
    }

    val managingPropertyData: ManagingProperty = ManagingProperty(
      agentCode = agentCode,
      agentOrganisationName = agentName,
      isCorrectAgent = true,
      managingPropertyChoice = managingPropertyChoice,
      agentAddress = "An Address",
      backLink = None,
      totalPropertySelectionSize = 2,
      propertySelectedSize = 2,
      singleProperty = singlePropertyChoice
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

    val res = await(
      ws.url(s"http://localhost:$port/business-rates-property-linking/my-organisation/confirm-appoint-agent")
        .withCookies(languageCookie(language), getSessionCookie(testSessionId))
        .withFollowRedirects(follow = false)
        .withHttpHeaders(HeaderNames.COOKIE -> "sessionId", "Csrf-Token" -> "nocheck")
        .get()
    )
    res.status shouldBe OK
    Jsoup.parse(res.body)
  }
}
