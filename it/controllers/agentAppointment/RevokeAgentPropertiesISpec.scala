package controllers.agentAppointment

import base.{HtmlComponentHelpers, ISpecBase}
import com.github.tomakehurst.wiremock.client.WireMock.{aResponse, get, post, stubFor}
import models.{AgentRevokeBulkAction, GroupAccount}
import models.propertyrepresentation.RevokeAgentFromSomePropertiesSession
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.http.HeaderNames
import play.api.http.Status.{NOT_FOUND, OK}
import play.api.libs.json.Json
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import repositories.RevokeAgentPropertiesSessionRepository
import services.AgentRelationshipService
import uk.gov.hmrc.http.{HeaderCarrier, SessionId}

import java.util.UUID

class RevokeAgentPropertiesISpec extends ISpecBase with HtmlComponentHelpers {

  val testSessionId = s"stubbed-${UUID.randomUUID}"

  val agentName = "gg-ext-id"

  lazy val mockAppointRevokeService: AgentRelationshipService = app.injector.instanceOf[AgentRelationshipService]
  lazy val mockRevokeAgentPropertiesSessionRepository: RevokeAgentPropertiesSessionRepository = app.injector.instanceOf[RevokeAgentPropertiesSessionRepository]
  implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId(testSessionId)))

  val backLinkSelector = "#back-link"
  val headingSelector = "h1"
  val forThePropertiesSelector = "#main-content > div > div > div:nth-child(1) > div > p"
  val bulletPointSelector = "#main-content > div > div > div:nth-child(1) > div > ul > li"
  val unassigningAnAgentSelector = "#warning-text > strong"
  val searchYourSelector = "#main-content > div > div > form > h2"
  val addressSelector = "#main-content > div > div > form > div > div.govuk-form-group > label"
  val addressInputSelector = "#address"
  val searchSelector = "#search-button"
  val clearSearchSelector = "#clear-search"
  val selectAllSelector = "#par-select-all-top"
  val addressHeaderSelector = "#sort-by-address > a"
  val appointedAgentsSelector = "#agentPropertiesTableBody > thead > tr > th:nth-child(2)"
  val checkboxSelector = "#checkbox-1"
  val addressValueSelector = "#agentPropertiesTableBody > tbody > tr:nth-child(1) > td:nth-child(1) > div > label"
  val appointedAgentsValueSelector = "#agentPropertiesTableBody > tbody > tr:nth-Child(1) > td:nth-child(2)"
  val secondCheckboxSelector = "#checkbox-2"
  val secondAddressValueSelector = "#agentPropertiesTableBody > tbody > tr:nth-child(2) > td:nth-child(1) > div > label"
  val secondAppointedAgentsValueSelector = "#agentPropertiesTableBody > tbody > tr:nth-Child(2) > td:nth-child(2)"
  val confirmAndUnassignSelector = "#submit-button"
  val cancelSelector = "#cancel-link"

  val titleText = s"Which of your properties do you want to unassign $agentName from? - Valuation Office Agency - GOV.UK"
  val backLinkText = "Back"
  val headingText = s"Which of your properties do you want to unassign $agentName from?"
  val forThePropertiesText = "For the properties you select, the agent will not be able to:"
  val sendOrContinueText = "send or continue Check and Challenge cases"
  val seeNewText = "see new Check and Challenge case correspondence, such as messages and emails"
  val seeDetailedText = "see detailed property information"
  val unassigningAnAgentText = "Warning Unassigning an agent that has Check and Challenge cases in progress means they will no longer be able to act on them for you."
  val searchYourText = "Search your properties"
  val addressText = "Address"
  val searchText = "Search"
  val clearSearchText = "Clear search"
  val selectAllText = "Select all"
  val appointedAgentsText = "Appointed agents"
  val confirmAndUnassignText = "Confirm and unassign"
  val cancelText = "Cancel"

  val titleTextWelsh = s"O ba eiddo ydych chi am ddadneilltuo $agentName? - Valuation Office Agency - GOV.UK"
  val backLinkTextWelsh = "Yn ôl"
  val headingTextWelsh = s"O ba eiddo ydych chi am ddadneilltuo $agentName?"
  val forThePropertiesTextWelsh = "Ar gyfer yr eiddo a ddewiswch, ni fydd yr asiant yn gallu:"
  val sendOrContinueTextWelsh = "anfon neu barhau ag achosion Gwirio a Herio"
  val seeNewTextWelsh = "gweld gohebiaeth achos Gwirio a Herio newydd, er enghraifft negeseuon ac e-byst"
  val seeDetailedTextWelsh = "gweld gwybodaeth eiddo fanwl"
  val unassigningAnAgentTextWelsh = "Rhybudd Mae dadneilltuo asiant sydd ag achosion Gwirio a Herio ar y gweill yn golygu na fydd yn gallu gweithredu arnynt ar eich rhan mwyach."
  val searchYourTextWelsh = "Chwiliwch eich eiddo"
  val addressTextWelsh = "Cyfeiriad"
  val searchTextWelsh = "Chwilio"
  val clearSearchTextWelsh = "Clirio’r chwiliad"
  val selectAllTextWelsh = "Dewiswch popeth"
  val appointedAgentsTextWelsh = "Asiantiaid penodedig"
  val confirmAndUnassignTextWelsh = "Cadarnhau a dadneilltuo"
  val cancelTextWelsh = "Canslo"

  val backLinkHref = "/business-rates-property-linking/my-organisation/manage-agent"
  val clearSearchHref = "/business-rates-property-linking/my-organisation/revoke/properties?page=1&pageSize=15&agentCode=1383"
  val selectAllHref = "#"
  val cancelHref = "/business-rates-property-linking/my-organisation/manage-agent/property-links?agentCode=1383"

  "revokeAgentProperties method displays the correct content in English when theres multiple properties" which {
    lazy val document: Document = getRevokeAgentPropertiesPage(English, multipleProps = true)

    s"has a title of $titleText" in {
      document.title() shouldBe titleText
    }

    s"has a $backLinkText link" in {
      document.select(backLinkSelector).text shouldBe backLinkText
      document.select(backLinkSelector).attr("href") shouldBe backLinkHref
    }

    s"has a heading of $headingText" in {
      document.getElementsByTag(headingSelector).text shouldBe headingText
    }

    s"has text on the screen of $forThePropertiesText" in {
      document.select(forThePropertiesSelector).text shouldBe forThePropertiesText
    }

    s"has bullet point list on the screen with text $sendOrContinueText, $seeNewText and $seeDetailedText" in {
      document.select(bulletPointSelector).get(0).text shouldBe sendOrContinueText
      document.select(bulletPointSelector).get(1).text shouldBe seeNewText
      document.select(bulletPointSelector).get(2).text shouldBe seeDetailedText
    }

    s"has a warning on the screen of $unassigningAnAgentText" in {
      document.select(unassigningAnAgentSelector).text shouldBe unassigningAnAgentText
    }

    s"has a subheading of $searchYourText" in {
      document.select(searchYourSelector).text shouldBe searchYourText
    }

    s"has a text input field for an $addressText" in {
      document.select(addressSelector).text shouldBe addressText
      document.select(addressInputSelector).attr("type") shouldBe "text"
    }

    s"has a $searchText button" in {
      document.select(searchSelector).text shouldBe searchText
    }

    s"has a $clearSearchText link" in {
      document.select(clearSearchSelector).text shouldBe clearSearchText
      document.select(clearSearchSelector).attr("href") shouldBe clearSearchHref
    }

    s"has a $selectAllText link" in {
      document.select(selectAllSelector).text shouldBe selectAllText
      document.select(selectAllSelector).attr("href") shouldBe selectAllHref
    }

    s"has an $addressText column header" in {
      document.select(addressHeaderSelector).text shouldBe addressText
    }

    s"has an $appointedAgentsText column header" in {
      document.select(appointedAgentsSelector).text shouldBe appointedAgentsText + " Appointed agents"
    }

    s"has a row that contains a checkbox, $addressText and $appointedAgentsText" in {
      document.select(checkboxSelector).attr("type") shouldBe "checkbox"
      document.select(addressValueSelector).text shouldBe "Appoint TEST ADDRESS"
      document.select(appointedAgentsValueSelector).text shouldBe ""
    }

    s"have a second row that contains a checkbox, $addressText and $appointedAgentsText" in {
      document.select(secondCheckboxSelector).attr("type") shouldBe "checkbox"
      document.select(secondAddressValueSelector).text shouldBe "Appoint ADDRESSTEST 2"
      document.select(secondAppointedAgentsValueSelector).text shouldBe "Test Agent 2"
    }

    s"has a $confirmAndUnassignText button" in {
      document.select(confirmAndUnassignSelector).text shouldBe confirmAndUnassignText
    }

    s"has a $cancelText link" in {
      document.select(cancelSelector).text shouldBe cancelText
      document.select(cancelSelector).attr("href") shouldBe cancelHref
    }
  }

  "revokeAgentProperties method displays the correct content in English when theres one property" which {
    lazy val document: Document = getRevokeAgentPropertiesPage(English, multipleProps = false)

    s"has a title of $titleText" in {
      document.title() shouldBe titleText
    }

    s"has a $backLinkText link" in {
      document.select(backLinkSelector).text shouldBe backLinkText
      document.select(backLinkSelector).attr("href") shouldBe backLinkHref
    }

    s"has a heading of $headingText" in {
      document.getElementsByTag(headingSelector).text shouldBe headingText
    }

    s"has text on the screen of $forThePropertiesText" in {
      document.select(forThePropertiesSelector).text shouldBe forThePropertiesText
    }

    s"has bullet point list on the screen with text $sendOrContinueText, $seeNewText and $seeDetailedText" in {
      document.select(bulletPointSelector).get(0).text shouldBe sendOrContinueText
      document.select(bulletPointSelector).get(1).text shouldBe seeNewText
      document.select(bulletPointSelector).get(2).text shouldBe seeDetailedText
    }

    s"has a warning on the screen of $unassigningAnAgentText" in {
      document.select(unassigningAnAgentSelector).text shouldBe unassigningAnAgentText
    }

    s"has a subheading of $searchYourText" in {
      document.select(searchYourSelector).text shouldBe searchYourText
    }

    s"has a text input field for an $addressText" in {
      document.select(addressSelector).text shouldBe addressText
      document.select(addressInputSelector).attr("type") shouldBe "text"
    }

    s"has a $searchText button" in {
      document.select(searchSelector).text shouldBe searchText
    }

    s"has a $clearSearchText link" in {
      document.select(clearSearchSelector).text shouldBe clearSearchText
      document.select(clearSearchSelector).attr("href") shouldBe clearSearchHref
    }

    s"has a $selectAllText link" in {
      document.select(selectAllSelector).text shouldBe selectAllText
      document.select(selectAllSelector).attr("href") shouldBe selectAllHref
    }

    s"has an $addressText column header" in {
      document.select(addressHeaderSelector).text shouldBe addressText
    }

    s"has an $appointedAgentsText column header" in {
      document.select(appointedAgentsSelector).text shouldBe appointedAgentsText + " Appointed agents"
    }

    s"has a row that contains a checkbox, $addressText and $appointedAgentsText" in {
      document.select(checkboxSelector).attr("type") shouldBe "checkbox"
      document.select(addressValueSelector).text shouldBe "Appoint TEST ADDRESS"
      document.select(appointedAgentsValueSelector).text shouldBe ""
    }

    s"does not have a second row that contains a checkbox, $addressText and $appointedAgentsText" in {
      document.select(secondCheckboxSelector).size() shouldBe 0
      document.select(secondAddressValueSelector).size() shouldBe 0
      document.select(secondAppointedAgentsValueSelector).size() shouldBe 0
    }

    s"has a $confirmAndUnassignText button" in {
      document.select(confirmAndUnassignSelector).text shouldBe confirmAndUnassignText
    }

    s"has a $cancelText link" in {
      document.select(cancelSelector).text shouldBe cancelText
      document.select(cancelSelector).attr("href") shouldBe cancelHref
    }
  }

  "revokeAgentProperties method displays the correct content in Welsh when theres multiple properties" which {
    lazy val document: Document = getRevokeAgentPropertiesPage(Welsh, multipleProps = true)

    s"has a title of $titleText in welsh" in {
      document.title() shouldBe titleTextWelsh
    }

    s"has a $backLinkText link in welsh" in {
      document.select(backLinkSelector).text shouldBe backLinkTextWelsh
      document.select(backLinkSelector).attr("href") shouldBe backLinkHref
    }

    s"has a heading of $headingText in welsh" in {
      document.getElementsByTag(headingSelector).text shouldBe headingTextWelsh
    }

    s"has text on the screen of $forThePropertiesText in welsh" in {
      document.select(forThePropertiesSelector).text shouldBe forThePropertiesTextWelsh
    }

    s"has bullet point list on the screen with text $sendOrContinueText, $seeNewText and $seeDetailedText in welsh" in {
      document.select(bulletPointSelector).get(0).text shouldBe sendOrContinueTextWelsh
      document.select(bulletPointSelector).get(1).text shouldBe seeNewTextWelsh
      document.select(bulletPointSelector).get(2).text shouldBe seeDetailedTextWelsh
    }

    s"has a warning on the screen of $unassigningAnAgentText in welsh" in {
      document.select(unassigningAnAgentSelector).text shouldBe unassigningAnAgentTextWelsh
    }

    s"has a subheading of $searchYourText in welsh" in {
      document.select(searchYourSelector).text shouldBe searchYourTextWelsh
    }

    s"has a text input field for an $addressText in welsh" in {
      document.select(addressSelector).text shouldBe addressTextWelsh
      document.select(addressInputSelector).attr("type") shouldBe "text"
    }

    s"has a $searchText button in welsh" in {
      document.select(searchSelector).text shouldBe searchTextWelsh
    }

    s"has a $clearSearchText link in welsh" in {
      document.select(clearSearchSelector).text shouldBe clearSearchTextWelsh
      document.select(clearSearchSelector).attr("href") shouldBe clearSearchHref
    }

    s"has a $selectAllText link in welsh" in {
      document.select(selectAllSelector).text shouldBe selectAllTextWelsh
      document.select(selectAllSelector).attr("href") shouldBe selectAllHref
    }

    s"has an $addressText column header in welsh" in {
      document.select(addressHeaderSelector).text shouldBe addressTextWelsh
    }

    s"has an $appointedAgentsText column header in welsh" in {
      document.select(appointedAgentsSelector).text shouldBe appointedAgentsTextWelsh + " Asiantiaid penodedig"
    }

    s"has a row that contains a checkbox, $addressText and $appointedAgentsText in welsh" in {
      document.select(checkboxSelector).attr("type") shouldBe "checkbox"
      document.select(addressValueSelector).text shouldBe "Penodi TEST ADDRESS"
      document.select(appointedAgentsValueSelector).text shouldBe ""
    }

    s"have a second row that contains a checkbox, $addressText and $appointedAgentsText in welsh" in {
      document.select(secondCheckboxSelector).attr("type") shouldBe "checkbox"
      document.select(secondAddressValueSelector).text shouldBe "Penodi ADDRESSTEST 2"
      document.select(secondAppointedAgentsValueSelector).text shouldBe "Test Agent 2"
    }

    s"has a $confirmAndUnassignText button in welsh" in {
      document.select(confirmAndUnassignSelector).text shouldBe confirmAndUnassignTextWelsh
    }

    s"has a $cancelText link in welsh" in {
      document.select(cancelSelector).text shouldBe cancelTextWelsh
      document.select(cancelSelector).attr("href") shouldBe cancelHref
    }
  }

  "revokeAgentProperties method displays the correct content in Welsh when theres one property" which {
    lazy val document: Document = getRevokeAgentPropertiesPage(Welsh, multipleProps = false)

    s"has a title of $titleText in welsh" in {
      document.title() shouldBe titleTextWelsh
    }

    s"has a $backLinkText link in welsh" in {
      document.select(backLinkSelector).text shouldBe backLinkTextWelsh
      document.select(backLinkSelector).attr("href") shouldBe backLinkHref
    }

    s"has a heading of $headingText in welsh" in {
      document.getElementsByTag(headingSelector).text shouldBe headingTextWelsh
    }

    s"has text on the screen of $forThePropertiesText in welsh" in {
      document.select(forThePropertiesSelector).text shouldBe forThePropertiesTextWelsh
    }

    s"has bullet point list on the screen with text $sendOrContinueText, $seeNewText and $seeDetailedText in welsh" in {
      document.select(bulletPointSelector).get(0).text shouldBe sendOrContinueTextWelsh
      document.select(bulletPointSelector).get(1).text shouldBe seeNewTextWelsh
      document.select(bulletPointSelector).get(2).text shouldBe seeDetailedTextWelsh
    }

    s"has a warning on the screen of $unassigningAnAgentText in welsh" in {
      document.select(unassigningAnAgentSelector).text shouldBe unassigningAnAgentTextWelsh
    }

    s"has a subheading of $searchYourText in welsh" in {
      document.select(searchYourSelector).text shouldBe searchYourTextWelsh
    }

    s"has a text input field for an $addressText in welsh" in {
      document.select(addressSelector).text shouldBe addressTextWelsh
      document.select(addressInputSelector).attr("type") shouldBe "text"
    }

    s"has a $searchText button in welsh" in {
      document.select(searchSelector).text shouldBe searchTextWelsh
    }

    s"has a $clearSearchText link in welsh" in {
      document.select(clearSearchSelector).text shouldBe clearSearchTextWelsh
      document.select(clearSearchSelector).attr("href") shouldBe clearSearchHref
    }

    s"has a $selectAllText link in welsh" in {
      document.select(selectAllSelector).text shouldBe selectAllTextWelsh
      document.select(selectAllSelector).attr("href") shouldBe selectAllHref
    }

    s"has an $addressText column header in welsh" in {
      document.select(addressHeaderSelector).text shouldBe addressTextWelsh
    }

    s"has an $appointedAgentsText column header in welsh" in {
      document.select(appointedAgentsSelector).text shouldBe appointedAgentsTextWelsh + " Asiantiaid penodedig"
    }

    s"has a row that contains a checkbox, $addressText and $appointedAgentsText in welsh" in {
      document.select(checkboxSelector).attr("type") shouldBe "checkbox"
      document.select(addressValueSelector).text shouldBe "Penodi TEST ADDRESS"
      document.select(appointedAgentsValueSelector).text shouldBe ""
    }

    s"does not have a second row that contains a checkbox, $addressText and $appointedAgentsText in welsh" in {
      document.select(secondCheckboxSelector).size() shouldBe 0
      document.select(secondAddressValueSelector).size() shouldBe 0
      document.select(secondAppointedAgentsValueSelector).size() shouldBe 0
    }

    s"has a $confirmAndUnassignText button in welsh" in {
      document.select(confirmAndUnassignSelector).text shouldBe confirmAndUnassignTextWelsh
    }

    s"has a $cancelText link in welsh" in {
      document.select(cancelSelector).text shouldBe cancelTextWelsh
      document.select(cancelSelector).attr("href") shouldBe cancelHref
    }
  }


  //TODO: Remember to do tests for the error scenarios, no input, wrong input etc
  //TODO: Can you do some scenarios around the filtering aspect? i.e. has 4 props, but searched for 1 so only shows one (seems to be a POST to the ?filter endpoint
  //TODO: Select all can also be deselect all, how to test this

  private def getRevokeAgentPropertiesPage(language: Language, multipleProps: Boolean): Document = {

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

    val testGroup = GroupAccount(id = 1L, groupId = "1L", companyName = "Test name", addressId = 1L, email = "test@email.com", phone = "1234567890", isAgent = false, agentCode = None)

    stubFor {
      get("/property-linking/groups?groupId=1383")
        .willReturn {
          aResponse.withStatus(OK).withBody(Json.toJson(testGroup).toString())
        }
    }

    val account = groupAccount(true)

    stubFor {
      get(s"/property-linking/groups/agentCode/1383")
        .willReturn {
          aResponse.withStatus(OK).withBody(Json.toJson(account).toString())
        }
    }

    stubFor {
      get("/property-linking/owner/property-links?sortField=ADDRESS&sortOrder=ASC&startPoint=1&pageSize=15&requestTotalRowCount=false")
        .willReturn {
          aResponse.withStatus(OK).withBody(Json.toJson(if(multipleProps)testOwnerAuthResultMultipleProperty else testOwnerAuthResult).toString())
        }
    }

    stubFor {
      get("/property-linking/my-organisation/agents/1383/property-links?sortField=ADDRESS&sortOrder=ASC&startPoint=1&pageSize=15&requestTotalRowCount=false")
        .willReturn {
          aResponse.withStatus(OK).withBody(Json.toJson(if(multipleProps)testOwnerAuthResultMultipleProperty else testOwnerAuthResult).toString())
        }
    }

    val res = await(
      ws.url(s"http://localhost:$port/business-rates-property-linking/my-organisation/revoke/properties?page=1&pageSize=15&agentCode=1383")
        .withCookies(languageCookie(language), getSessionCookie(testSessionId))
        .withFollowRedirects(follow = false)
        .withHttpHeaders(HeaderNames.COOKIE -> "sessionId")
        .get()
    )

    res.status shouldBe OK
    Jsoup.parse(res.body)
  }

}
