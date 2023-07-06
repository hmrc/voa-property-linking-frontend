package controllers.propertyLinking

import base.{HtmlComponentHelpers, ISpecBase}
import com.github.tomakehurst.wiremock.client.WireMock.{aResponse, get, post, stubFor}
import models.propertyrepresentation.AgentList
import models.searchApi.OwnerAuthResult
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.http.Status.OK
import play.api.libs.json.Json
import play.api.test.Helpers._
import repositories.ManageAgentSessionRepository
import uk.gov.hmrc.http.{HeaderCarrier, SessionId}

import java.util.UUID

class ManageAgentPropertiesISpec extends ISpecBase with HtmlComponentHelpers {

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
  val testAddress = "TEST ADDRESS"
  val testAddress2 = "ADDRESSTEST 2"

  def headerText(name: String) = s"$name"
  def singleRatingListText(ratingList: String) = s"This agent can act for you on your property valuations on the $ratingList rating list, for properties that you assign them to or they add to your account."

  def assignedPropertyListElement(address: String) = address

  def singleRatingListTextWelsh(ratingList: String) = s"This agent can act for you on your property valuations on the $ratingList rating list, for properties that you assign them to or they add to your account."

  val titleTextWelsh = "Eich asiant - Valuation Office Agency - GOV.UK"
  val backLinkTextWelsh = "Yn ôl"
  val captionTextWelsh = "Asiant"
  val noOrMultipleRatingListTextWelsh = "This agent can act for you on your property valuations on the 2023 and 2017 rating lists, for properties that you assign them to or they add to your account."
  val manageButtonTextWelsh = "Rheoli’r asiant hwn"
  val ratingListSectionHeadingWelsh = "Rating lists they can act on for you"
  val assignedPropertiesHeadingWelsh = "Eiddo wedi’u neilltuo"
  val assignedToNoPropertiesWelsh = "This agent is not assigned to any properties"

  val backLinkSelector = "#back-link"
  val captionSelector = ".govuk-caption-l"
  val headerSelector = "h1.govuk-heading-l"
  val manageAgentButtonSelector = ".govuk-button"
  val ratingListHeadingSelector = ".govuk-heading-m:nth-of-type(2)"
  val ratingListTextSelector = "#ratingListText"
  val assignedPropertiesHeadingSelector = "h2.govuk-heading-m:nth-of-type(3)"
  val assignedPropertiesTextSelectorSomePropertiesFirst =".govuk-list--bullet > li:nth-child(1)"
  val assignedPropertiesTextSelectorSomePropertiesSecond =".govuk-list--bullet > li:nth-child(2)"
  val assignedPropertiesTextSelectorNoProperties =".govuk-body:nth-child(7)"

  val backLinkHref="/business-rates-property-linking/my-organisation/agents"


  "ManageAgentController manageAgentProperties method" should {
    "display 'Manage Agent Properties' screen with the correct text and the language is set to English and Agent got multiple properties assigned " which {

      lazy val document = getYourAgentsPropertiesPage(English, testOwnerAuthResultMultipleProperty, testAgentList)

      "has a title of $titleText" in {
        document.title() shouldBe titleText
      }
      "displays a correct header with agent name included" in {
        document.select(headerSelector).text() shouldBe headerText(name = "Test Agent")
      }

      "displays a correct caption above the header" in {
        document.select(captionSelector).text shouldBe captionText
      }

      "has a back link which takes you to 'Agent List' page" in {
        document.select(backLinkSelector).text() shouldBe backLinkText
        document.select(backLinkSelector).attr("href") shouldBe backLinkHref
      }

      "has heading above rating list info with correct text" in {
        document.select(ratingListHeadingSelector).text() shouldBe ratingListSectionHeading
      }

      "has heading above assigned properties list with correct text" in {
        document.select(assignedPropertiesHeadingSelector).text() shouldBe assignedPropertiesHeading
      }
      s"has correct text under the heading $ratingListSectionHeading" in {
        document.select(ratingListTextSelector).text shouldBe noOrMultipleRatingListText

      }
      s"has correct text under the heading $assignedPropertiesHeading in the first element of the list is capitalised" in {
        document.select(assignedPropertiesTextSelectorSomePropertiesFirst).text shouldBe testAddress
      }

      s"has correct text under the heading $assignedPropertiesHeading  in the second element of the list and the address is capitalised" in {
        document.select(assignedPropertiesTextSelectorSomePropertiesSecond).text shouldBe testAddress2
      }

    }

    "display 'Manage Agent Properties' screen with the correct text and the language is set to English and Agent got no properties assigned " which {

      lazy val document = getYourAgentsPropertiesPage(English, testOwnerAuthResultNoProperties, testAgentList)

      s"has correct text under the heading $ratingListSectionHeading" in {
        document.select(ratingListTextSelector).text shouldBe noOrMultipleRatingListText

      }
    }

    "display 'Manage Agent Properties' screen with the correct text and the language is set to English and Agent got properties assigned only for 2017 list" which {

      lazy val document = getYourAgentsPropertiesPage(English, testOwnerAuthResultNoProperties, testAgentListFor2017)

      s"has correct text under the heading $ratingListSectionHeading" in {
        document.select(ratingListTextSelector).text shouldBe singleRatingListText("2017")

      }
    }
    "display 'Manage Agent Properties' screen with the correct text and the language is set to English and Agent got properties assigned only for 2023 list" which {

      lazy val document = getYourAgentsPropertiesPage(English, testOwnerAuthResultNoProperties, testAgentListFor2023)

      s"has correct text under the heading $ratingListSectionHeading" in {
        document.select(ratingListTextSelector).text shouldBe singleRatingListText("2023")

      }
    }
  }
  "ManageAgentController manageAgentProperties method" should {
    "display 'Manage Agent Properties' screen with the correct text and the language is set to Welsh and Agent got multiple properties assigned " which {

      lazy val document = getYourAgentsPropertiesPage(Welsh, testOwnerAuthResultMultipleProperty, testAgentList)

      "has a title of $titleText" in {
        document.title() shouldBe titleTextWelsh
      }
      "displays a correct header with agent name included" in {
        document.select(headerSelector).text() shouldBe headerText(name = "Test Agent")
      }

      "displays a correct caption above the header" in {
        document.select(captionSelector).text shouldBe captionTextWelsh
      }

      "has a back link which takes you to 'Your business rates valuation account'- dashboard home page" in {
        document.select(backLinkSelector).text() shouldBe backLinkTextWelsh
        document.select(backLinkSelector).attr("href") shouldBe backLinkHref
      }

      "has heading above rating list info with correct text" in {
        document.select(ratingListHeadingSelector).text() shouldBe ratingListSectionHeadingWelsh
      }

      "has heading above assigned properties list with correct text" in {
        document.select(assignedPropertiesHeadingSelector).text() shouldBe assignedPropertiesHeadingWelsh
      }

      s"has correct text under the heading $ratingListSectionHeading" in {
        document.select(ratingListTextSelector).text shouldBe noOrMultipleRatingListTextWelsh

      }

    }
    "display 'Manage Agent Properties' screen with the correct text and the language is set to Welsh and Agent got no properties assigned " which {

      lazy val document = getYourAgentsPropertiesPage(Welsh, testOwnerAuthResultNoProperties, testAgentList)

      s"has correct text under the heading $ratingListSectionHeading" in {
        document.select(ratingListTextSelector).text shouldBe noOrMultipleRatingListTextWelsh

      }
    }

    "display 'Manage Agent Properties' screen with the correct text and the language is set to Welsh and Agent got properties assigned only for 2017 list" which {

      lazy val document = getYourAgentsPropertiesPage(Welsh, testOwnerAuthResultNoProperties, testAgentListFor2017)

      s"has correct text under the heading $ratingListSectionHeading" in {
        document.select(ratingListTextSelector).text shouldBe singleRatingListTextWelsh("2017")

      }
    }
    "display 'Manage Agent Properties' screen with the correct text and the language is set to Welsh and Agent got properties assigned only for 2023 list" which {

      lazy val document = getYourAgentsPropertiesPage(Welsh, testOwnerAuthResultNoProperties, testAgentListFor2023)

      s"has correct text under the heading $ratingListSectionHeading" in {
        document.select(ratingListTextSelector).text shouldBe singleRatingListTextWelsh("2023")

      }
    }
  }
  private def getYourAgentsPropertiesPage(language: Language, authDetails: OwnerAuthResult, list: AgentList): Document = {


    stubFor {
      get("/property-linking/my-organisation/agents/1001/property-links?sortField=ADDRESS&sortOrder=ASC&startPoint=1&pageSize=100&requestTotalRowCount=true")
        .willReturn {
          aResponse.withStatus(OK).withBody(Json.toJson(authDetails).toString())
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
          aResponse.withStatus(OK).withBody(Json.toJson(list).toString())
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