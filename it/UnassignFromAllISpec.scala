import base.{HtmlComponentHelpers, ISpecBase}
import com.github.tomakehurst.wiremock.client.WireMock.{aResponse, get, post, stubFor}
import models.propertyrepresentation.AgentSummary
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.http.HeaderNames
import play.api.http.Status.OK
import play.api.libs.json.Json
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import repositories.ManageAgentSessionRepository
import uk.gov.hmrc.http.{HeaderCarrier, SessionId}

import java.time.LocalDate
import java.util.UUID

class UnassignFromAllISpec extends ISpecBase with HtmlComponentHelpers {

  val testSessionId = s"stubbed-${UUID.randomUUID}"

  lazy val mockRepository: ManageAgentSessionRepository = app.injector.instanceOf[ManageAgentSessionRepository]
  implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId(testSessionId)))

  // Unassign from all question page
  val headingTag = "h1"
  val captionSelector = "#main-content > div > div > span"
  val paragraph1OnePropertyId = "unassignFromProperty-p1"
  val paragraph1MultiplePropertiesSelector = "#main-content > div > div > p"
  val bulletOneId = "unassigned-privilege-1"
  val bulletTwoId = "unassigned-privilege-2"
  val bulletThreeId = "unassigned-privilege-3"
  val warningTextSelector = "#warning-text > strong"
  val unassignButtonSelector = "#main-content > div > div > form > div > button"
  val cancelLinkId = "cancel-link"
  val backLinkId = "back-link"

  val titleOnePropertyText = "Are you sure you want to unassign Test Agent from your property? - Valuation Office Agency - GOV.UK"
  val titleMultiplePropertiesText = "Are you sure you want to unassign Test Agent from all your properties? - Valuation Office Agency - GOV.UK"
  val headingOnePropertyText = "Are you sure you want to unassign Test Agent from your property?"
  val headingMultiplePropertiesText = "Are you sure you want to unassign Test Agent from all your properties?"
  val captionText = "Manage agent"
  val paragraph1OnePropertyText = "For your property, the agent will not be able to:"
  val paragraph1MultiplePropertiesText = "For all your properties, the agent will not be able to:"
  val bulletOneText = "send or continue Check and Challenge cases"
  val bulletTwoText = "see new Check and Challenge case correspondence, such as messages and emails"
  val bulletThreeText = "see detailed property information"
  val warningText = "Warning Unassigning an agent that has Check and Challenge cases in progress means they will no longer be able to act on them for you."
  val unassignButtonText = "Confirm and unassign"
  val cancelLinkText = "Cancel"
  val backLinkText = "Back"

  val titleOnePropertyTextWelsh = "A hoffwch ddad-neilltuo Test Agent o’ch eiddo? - Valuation Office Agency - GOV.UK"
  val titleMultiplePropertiesTextWelsh = "Ydych chi’n siŵr eich bod am ddadneilltuo Test Agent o’ch holl eiddo? - Valuation Office Agency - GOV.UK"
  val headingOnePropertyTextWelsh = "A hoffwch ddad-neilltuo Test Agent o’ch eiddo?"
  val headingMultiplePropertiesTextWelsh = "Ydych chi’n siŵr eich bod am ddadneilltuo Test Agent o’ch holl eiddo?"
  val captionTextWelsh = "Rheoli asiant"
  val paragraph1OnePropertyTextWelsh = "Ar gyfer eich eiddo, ni fydd yr asiant yn gallu:"
  val paragraph1MultiplePropertiesTextWelsh = "Ar gyfer eich holl eiddo, ni fydd yr asiant yn gallu:"
  val bulletOneTextWelsh = "anfon neu barhau ag achosion Gwirio a Herio"
  val bulletTwoTextWelsh = "gweld gohebiaeth achos Gwirio a Herio newydd, er enghraifft negeseuon ac e-byst"
  val bulletThreeTextWelsh = "gweld gwybodaeth eiddo fanwl"
  val warningTextWelsh = "Rhybudd Mae dadneilltuo asiant sydd ag achosion Gwirio a Herio ar y gweill yn golygu na fydd yn gallu gweithredu arnynt ar eich rhan mwyach."
  val unassignButtonTextWelsh = "Cadarnhau a dadneilltuo"
  val cancelLinkTextWelsh = "Canslo"
  val cancelAndGoBackLinkTextWelsh = "Canslo a dychwelyd i’r hafan"
  val backLinkTextWelsh = "Yn ôl"

  val cancelLinkHref = "/business-rates-property-linking/my-organisation/manage-agent/property-links?agentCode=1"
  val backLinkOnePropertyHref = "/business-rates-property-linking/my-organisation/manage-agent/property-links?agentCode=1"
  val backLinkMultiplePropertiesHref = "/business-rates-property-linking/my-organisation/manage-agent"

  // Unassign from all confirmation page
  val confirmationParagraphOneLocator = "#main-content > div > div > p:nth-child(2)"
  val confirmationParagraphTwoLocator = "#main-content > div > div > p:nth-child(3)"
  val confirmationWhatHappensNextLocator = "#main-content > div > div > h2"
  val confirmationWhatHappensNextParaOneLocator = "#main-content > div > div > p:nth-child(5)"
  val confirmationWhatHappensNextParaTwoLocator = "#main-content > div > div > p:nth-child(7)"
  val confirmationRemoveAgentLinkLocator = "remove-agent-from-account"
  val confirmationGoHomeLinkLocator = "#main-content > div > div > p:nth-child(8) > a"

  val confirmationTitleMultiplePropertiesText = "Test Agent has been unassigned from all your properties - Valuation Office Agency - GOV.UK"
  val confirmationTitleOnePropertyText = "Test Agent has been unassigned from your property - Valuation Office Agency - GOV.UK"
  val confirmationHeadingMultiplePropertiesText = "Test Agent has been unassigned from all your properties"
  val confirmationHeadingOnePropertyText = "Test Agent has been unassigned from your property"
  val confirmationParagraphOneText = "The agent can no longer act for you on any of your properties."
  val confirmationParagraphTwoText = "The agent has not been removed from your account. They can still act for you if they add other properties to your account."
  val confirmationWhatHappensNextText = "What happens next"
  val confirmationWhatHappensNextParaOneText = "You can remove this agent from your account."
  val confirmationWhatHappensRemoveAgentLinkText = "remove this agent from your account"
  val confirmationWhatHappensNextParaTwoText = "You can reassign an agent to a property if you want them to act for you again."
  val confirmationGoHomeLinkText = "Go to your account home"

  val confirmationTitleMultiplePropertiesTextWelsh = "Mae Test Agent wedi’i ddadneilltuo o’ch holl eiddo - Valuation Office Agency - GOV.UK"
  val confirmationTitleOnePropertyTextWelsh = "Mae Test Agent wedi’i ddad-neilltuo o’ch eiddo - Valuation Office Agency - GOV.UK"
  val confirmationHeadingMultiplePropertiesTextWelsh = "Mae Test Agent wedi’i ddadneilltuo o’ch holl eiddo"
  val confirmationHeadingOnePropertyTextWelsh = "Mae Test Agent wedi’i ddad-neilltuo o’ch eiddo"
  val confirmationParagraphOneTextWelsh = "Ni all yr asiant weithredu ar eich rhan mwyach ar unrhyw un o’ch eiddo."
  val confirmationParagraphTwoTextWelsh = "Nid yw’r asiant wedi’i dynnu o’ch cyfrif. Gallant barhau i weithredu ar eich rhan os ydynt yn ychwanegu eiddo eraill at eich cyfrif."
  val confirmationWhatHappensNextTextWelsh = "Yr hyn sy’n digwydd nesaf"
  val confirmationWhatHappensNextParaOneTextWelsh = "Gallwch dynnu’r asiant hwn o’ch cyfrif."
  val confirmationWhatHappensRemoveAgentLinkTextWelsh = "dynnu’r asiant hwn o’ch cyfrif"
  val confirmationWhatHappensNextParaTwoTextWelsh = "Gallwch ailbennu asiant i eiddo os ydych am iddynt weithredu ar eich rhan eto."
  val confirmationGoHomeLinkTextWelsh = "Ewch i hafan eich cyfrif"

  val confirmationRemoveAgentLinkHref = "/business-rates-property-linking/my-organisation/manage-agent/remove/from-organisation"
  val confirmationGoHomeLinkHref = "/business-rates-dashboard/home"

  "showUnassignFromAll displays the correct content in English when a user has 1 property" which {
    lazy val document: Document = getUnassignFromAllQuestionPage(English, numOfProperties = 1)

    s"has a title of $titleOnePropertyText" in {
      document.title() shouldBe titleOnePropertyText
    }

    s"has a heading of $headingOnePropertyText" in {
      document.getElementsByTag(headingTag).text shouldBe headingOnePropertyText
    }

    s"has a caption of $captionText" in {
      document.select(captionSelector).text shouldBe captionText
    }

    s"has a paragraph of $paragraph1OnePropertyText" in {
      document.getElementById(paragraph1OnePropertyId).text shouldBe paragraph1OnePropertyText
    }

    s"has 3 bullet points of $bulletOneText, $bulletTwoText and $bulletThreeText" in {
      document.getElementById(bulletOneId).text shouldBe bulletOneText
      document.getElementById(bulletTwoId).text shouldBe bulletTwoText
      document.getElementById(bulletThreeId).text shouldBe bulletThreeText
    }

    s"has warning text of $warningText" in {
      document.select(warningTextSelector).text shouldBe warningText
    }

    s"has a $unassignButtonText button" in {
      document.select(unassignButtonSelector).text shouldBe unassignButtonText
    }

    s"has a cancel link with the correct href" in {
      document.getElementById(cancelLinkId).text shouldBe cancelLinkText
      document.getElementById(cancelLinkId).attr("href") shouldBe cancelLinkHref
    }

    s"has a back link with the correct href" in {
      document.getElementById(backLinkId).text shouldBe backLinkText
      document.getElementById(backLinkId).attr("href") shouldBe backLinkOnePropertyHref
    }
  }

  "showUnassignFromAll displays the correct content in Welsh when a user has 1 property" which {
    lazy val document: Document = getUnassignFromAllQuestionPage(Welsh, numOfProperties = 1)

    s"has a title of $titleOnePropertyText in Welsh" in {
      document.title() shouldBe titleOnePropertyTextWelsh
    }

    s"has a heading of $headingOnePropertyText in Welsh" in {
      document.getElementsByTag(headingTag).text shouldBe headingOnePropertyTextWelsh
    }

    s"has a caption of $captionText in Welsh" in {
      document.select(captionSelector).text shouldBe captionTextWelsh
    }

    s"has a paragraph of $paragraph1OnePropertyText in Welsh" in {
      document.getElementById(paragraph1OnePropertyId).text shouldBe paragraph1OnePropertyTextWelsh
    }

    s"has 3 bullet points of $bulletOneText, $bulletTwoText and $bulletThreeText in Welsh" in {
      document.getElementById(bulletOneId).text shouldBe bulletOneTextWelsh
      document.getElementById(bulletTwoId).text shouldBe bulletTwoTextWelsh
      document.getElementById(bulletThreeId).text shouldBe bulletThreeTextWelsh
    }

    s"has warning text of $warningText in Welsh" in {
      document.select(warningTextSelector).text shouldBe warningTextWelsh
    }

    s"has a $unassignButtonText button in Welsh" in {
      document.select(unassignButtonSelector).text shouldBe unassignButtonTextWelsh
    }

    s"has a cancel link with the correct href in Welsh" in {
      document.getElementById(cancelLinkId).text shouldBe cancelAndGoBackLinkTextWelsh
      document.getElementById(cancelLinkId).attr("href") shouldBe cancelLinkHref
    }

    s"has a back link with the correct href in Welsh" in {
      document.getElementById(backLinkId).text shouldBe backLinkTextWelsh
      document.getElementById(backLinkId).attr("href") shouldBe backLinkOnePropertyHref
    }
  }

  "showUnassignFromAll displays the correct content in English when a user has more than 1 property" which {
    lazy val document: Document = getUnassignFromAllQuestionPage(English, numOfProperties = 10)

    s"has a title of $titleMultiplePropertiesText" in {
      document.title() shouldBe titleMultiplePropertiesText
    }

    s"has a heading of $headingMultiplePropertiesText" in {
      document.getElementsByTag(headingTag).text shouldBe headingMultiplePropertiesText
    }

    s"has a caption of $captionText" in {
      document.select(captionSelector).text shouldBe captionText
    }

    s"has a paragraph of $paragraph1MultiplePropertiesText" in {
      document.select(paragraph1MultiplePropertiesSelector).text shouldBe paragraph1MultiplePropertiesText
    }

    s"has 3 bullet points of $bulletOneText, $bulletTwoText and $bulletThreeText" in {
      document.getElementById(bulletOneId).text shouldBe bulletOneText
      document.getElementById(bulletTwoId).text shouldBe bulletTwoText
      document.getElementById(bulletThreeId).text shouldBe bulletThreeText
    }

    s"has warning text of $warningText" in {
      document.select(warningTextSelector).text shouldBe warningText
    }

    s"has a $unassignButtonText button" in {
      document.select(unassignButtonSelector).text shouldBe unassignButtonText
    }

    s"has a cancel link with the correct href" in {
      document.getElementById(cancelLinkId).text shouldBe cancelLinkText
      document.getElementById(cancelLinkId).attr("href") shouldBe cancelLinkHref
    }

    s"has a back link with the correct href" in {
      document.getElementById(backLinkId).text shouldBe backLinkText
      document.getElementById(backLinkId).attr("href") shouldBe backLinkMultiplePropertiesHref
    }
  }

  "showUnassignFromAll displays the correct content in Welsh when a user has more than 1 property" which {
    lazy val document: Document = getUnassignFromAllQuestionPage(Welsh, numOfProperties = 10)

    s"has a title of $titleMultiplePropertiesText in Welsh" in {
      document.title() shouldBe titleMultiplePropertiesTextWelsh
    }

    s"has a heading of $headingMultiplePropertiesText in Welsh" in {
      document.getElementsByTag(headingTag).text shouldBe headingMultiplePropertiesTextWelsh
    }

    s"has a caption of $captionText in Welsh" in {
      document.select(captionSelector).text shouldBe captionTextWelsh
    }

    s"has a paragraph of $paragraph1MultiplePropertiesText in Welsh" in {
      document.select(paragraph1MultiplePropertiesSelector).text shouldBe paragraph1MultiplePropertiesTextWelsh
    }

    s"has 3 bullet points of $bulletOneText, $bulletTwoText and $bulletThreeText in Welsh" in {
      document.getElementById(bulletOneId).text shouldBe bulletOneTextWelsh
      document.getElementById(bulletTwoId).text shouldBe bulletTwoTextWelsh
      document.getElementById(bulletThreeId).text shouldBe bulletThreeTextWelsh
    }

    s"has warning text of $warningText in Welsh" in {
      document.select(warningTextSelector).text shouldBe warningTextWelsh
    }

    s"has a $unassignButtonText button in Welsh" in {
      document.select(unassignButtonSelector).text shouldBe unassignButtonTextWelsh
    }

    s"has a cancel link with the correct href in Welsh" in {
      document.getElementById(cancelLinkId).text shouldBe cancelLinkTextWelsh
      document.getElementById(cancelLinkId).attr("href") shouldBe cancelLinkHref
    }

    s"has a back link with the correct href in Welsh" in {
      document.getElementById(backLinkId).text shouldBe backLinkTextWelsh
      document.getElementById(backLinkId).attr("href") shouldBe backLinkMultiplePropertiesHref
    }
  }

  "confirmationUnassignAgentFromAll displays the correct content in English when a user has 1 property" which {
    lazy val document: Document = getUnassignFromAllConfirmationPage(English, numOfProperties = 1)

    s"has a title of $confirmationTitleOnePropertyText" in {
      document.title() shouldBe confirmationTitleOnePropertyText
    }

    s"has a heading of $confirmationHeadingOnePropertyText" in {
      document.getElementsByTag(headingTag).text shouldBe confirmationHeadingOnePropertyText
    }

    s"has the first paragraph of $confirmationParagraphOneText" in {
      document.select(confirmationParagraphOneLocator).text shouldBe confirmationParagraphOneText
    }

    s"has the second paragraph of $confirmationParagraphTwoText" in {
      document.select(confirmationParagraphTwoLocator).text shouldBe confirmationParagraphTwoText
    }

    s"has a subheading of $confirmationWhatHappensNextText" in {
      document.select(confirmationWhatHappensNextLocator).text shouldBe confirmationWhatHappensNextText
    }

    s"has the first paragraph in the $confirmationWhatHappensNextText section of $confirmationWhatHappensNextParaOneText" in {
      document.select(confirmationWhatHappensNextParaOneLocator).text shouldBe confirmationWhatHappensNextParaOneText
      document.getElementById(confirmationRemoveAgentLinkLocator).text shouldBe confirmationWhatHappensRemoveAgentLinkText
      document.getElementById(confirmationRemoveAgentLinkLocator).attr("href") shouldBe confirmationRemoveAgentLinkHref
    }

    s"has the second paragraph in the $confirmationWhatHappensNextText section of $confirmationWhatHappensNextParaTwoText" in {
      document.select(confirmationWhatHappensNextParaTwoLocator).text shouldBe confirmationWhatHappensNextParaTwoText
    }

    s"has a $confirmationGoHomeLinkText link" in {
      document.select(confirmationGoHomeLinkLocator).text shouldBe confirmationGoHomeLinkText
      document.select(confirmationGoHomeLinkLocator).attr("href") shouldBe confirmationGoHomeLinkHref
    }
  }

  "confirmationUnassignAgentFromAll displays the correct content in Welsh when a user has 1 property" which {
    lazy val document: Document = getUnassignFromAllConfirmationPage(Welsh, numOfProperties = 1)

    s"has a title of $confirmationTitleOnePropertyText in Welsh" in {
      document.title() shouldBe confirmationTitleOnePropertyTextWelsh
    }

    s"has a heading of $confirmationHeadingOnePropertyText in Welsh" in {
      document.getElementsByTag(headingTag).text shouldBe confirmationHeadingOnePropertyTextWelsh
    }

    s"has the first paragraph of $confirmationParagraphOneText in Welsh" in {
      document.select(confirmationParagraphOneLocator).text shouldBe confirmationParagraphOneTextWelsh
    }

    s"has the second paragraph of $confirmationParagraphTwoText in Welsh" in {
      document.select(confirmationParagraphTwoLocator).text shouldBe confirmationParagraphTwoTextWelsh
    }

    s"has a subheading of $confirmationWhatHappensNextText in Welsh" in {
      document.select(confirmationWhatHappensNextLocator).text shouldBe confirmationWhatHappensNextTextWelsh
    }

    s"has the first paragraph in the $confirmationWhatHappensNextText section of $confirmationWhatHappensNextParaOneText in Welsh" in {
      document.select(confirmationWhatHappensNextParaOneLocator).text shouldBe confirmationWhatHappensNextParaOneTextWelsh
      document.getElementById(confirmationRemoveAgentLinkLocator).text shouldBe confirmationWhatHappensRemoveAgentLinkTextWelsh
      document.getElementById(confirmationRemoveAgentLinkLocator).attr("href") shouldBe confirmationRemoveAgentLinkHref
    }

    s"has the second paragraph in the $confirmationWhatHappensNextText section of $confirmationWhatHappensNextParaTwoText in Welsh" in {
      document.select(confirmationWhatHappensNextParaTwoLocator).text shouldBe confirmationWhatHappensNextParaTwoTextWelsh
    }

    s"has a $confirmationGoHomeLinkText link in Welsh" in {
      document.select(confirmationGoHomeLinkLocator).text shouldBe confirmationGoHomeLinkTextWelsh
      document.select(confirmationGoHomeLinkLocator).attr("href") shouldBe confirmationGoHomeLinkHref
    }
  }

  "confirmationUnassignAgentFromAll displays the correct content in English when a user has more than 1 property" which {
    lazy val document: Document = getUnassignFromAllConfirmationPage(English, numOfProperties = 10)

    s"has a title of $confirmationTitleMultiplePropertiesText" in {
      document.title() shouldBe confirmationTitleMultiplePropertiesText
    }

    s"has a heading of $confirmationHeadingMultiplePropertiesText" in {
      document.getElementsByTag(headingTag).text shouldBe confirmationHeadingMultiplePropertiesText
    }

    s"has the first paragraph of $confirmationParagraphOneText" in {
      document.select(confirmationParagraphOneLocator).text shouldBe confirmationParagraphOneText
    }

    s"has the second paragraph of $confirmationParagraphTwoText" in {
      document.select(confirmationParagraphTwoLocator).text shouldBe confirmationParagraphTwoText
    }

    s"has a subheading of $confirmationWhatHappensNextText" in {
      document.select(confirmationWhatHappensNextLocator).text shouldBe confirmationWhatHappensNextText
    }

    s"has the first paragraph in the $confirmationWhatHappensNextText section of $confirmationWhatHappensNextParaOneText" in {
      document.select(confirmationWhatHappensNextParaOneLocator).text shouldBe confirmationWhatHappensNextParaOneText
      document.getElementById(confirmationRemoveAgentLinkLocator).text shouldBe confirmationWhatHappensRemoveAgentLinkText
      document.getElementById(confirmationRemoveAgentLinkLocator).attr("href") shouldBe confirmationRemoveAgentLinkHref
    }

    s"has the second paragraph in the $confirmationWhatHappensNextText section of $confirmationWhatHappensNextParaTwoText" in {
      document.select(confirmationWhatHappensNextParaTwoLocator).text shouldBe confirmationWhatHappensNextParaTwoText
    }

    s"has a $confirmationGoHomeLinkText link" in {
      document.select(confirmationGoHomeLinkLocator).text shouldBe confirmationGoHomeLinkText
      document.select(confirmationGoHomeLinkLocator).attr("href") shouldBe confirmationGoHomeLinkHref
    }
  }

  "confirmationUnassignAgentFromAll displays the correct content in Welsh when a user has more than 1 property" which {
    lazy val document: Document = getUnassignFromAllConfirmationPage(Welsh, numOfProperties = 10)

    s"has a title of $confirmationTitleMultiplePropertiesText in Welsh" in {
      document.title() shouldBe confirmationTitleMultiplePropertiesTextWelsh
    }

    s"has a heading of $confirmationHeadingMultiplePropertiesText in Welsh" in {
      document.getElementsByTag(headingTag).text shouldBe confirmationHeadingMultiplePropertiesTextWelsh
    }

    s"has the first paragraph of $confirmationParagraphOneText in Welsh" in {
      document.select(confirmationParagraphOneLocator).text shouldBe confirmationParagraphOneTextWelsh
    }

    s"has the second paragraph of $confirmationParagraphTwoText in Welsh" in {
      document.select(confirmationParagraphTwoLocator).text shouldBe confirmationParagraphTwoTextWelsh
    }

    s"has a subheading of $confirmationWhatHappensNextText in Welsh" in {
      document.select(confirmationWhatHappensNextLocator).text shouldBe confirmationWhatHappensNextTextWelsh
    }

    s"has the first paragraph in the $confirmationWhatHappensNextText section of $confirmationWhatHappensNextParaOneText in Welsh" in {
      document.select(confirmationWhatHappensNextParaOneLocator).text shouldBe confirmationWhatHappensNextParaOneTextWelsh
      document.getElementById(confirmationRemoveAgentLinkLocator).text shouldBe confirmationWhatHappensRemoveAgentLinkTextWelsh
      document.getElementById(confirmationRemoveAgentLinkLocator).attr("href") shouldBe confirmationRemoveAgentLinkHref
    }

    s"has the second paragraph in the $confirmationWhatHappensNextText section of $confirmationWhatHappensNextParaTwoText in Welsh" in {
      document.select(confirmationWhatHappensNextParaTwoLocator).text shouldBe confirmationWhatHappensNextParaTwoTextWelsh
    }

    s"has a $confirmationGoHomeLinkText link in Welsh" in {
      document.select(confirmationGoHomeLinkLocator).text shouldBe confirmationGoHomeLinkTextWelsh
      document.select(confirmationGoHomeLinkLocator).attr("href") shouldBe confirmationGoHomeLinkHref
    }
  }

  private def stubSetup(numOfProperties: Int): Unit = {
    await(
      mockRepository.saveOrUpdate(
        AgentSummary(
          listYears = Some(List("2017", "2023")),
          name = "Test Agent",
          organisationId = 1L,
          representativeCode = 1L,
          appointedDate = LocalDate.now(),
          propertyCount = numOfProperties
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

    stubFor {
      get("/property-linking/owner/agents")
        .willReturn {
          aResponse.withStatus(OK).withBody(Json.toJson(testAgentList).toString())
        }
    }

    stubFor {
      get("/property-linking/owner/property-links/count")
        .willReturn {
          aResponse.withStatus(OK).withBody(Json.toJson(numOfProperties).toString())
        }
    }
  }

  private def getUnassignFromAllConfirmationPage(language: Language, numOfProperties: Int) = {
    stubSetup(numOfProperties)

    val res = await(
      ws.url(s"http://localhost:$port/business-rates-property-linking/my-organisation/manage-agent/unassign/from-all-properties/confirmation")
        .withCookies(languageCookie(language), getSessionCookie(testSessionId))
        .withFollowRedirects(follow = false)
        .withHttpHeaders(HeaderNames.COOKIE -> "sessionId")
        .get()
    )

    res.status shouldBe OK
    Jsoup.parse(res.body)
  }

  private def getUnassignFromAllQuestionPage(language: Language, numOfProperties: Int) = {
    stubSetup(numOfProperties)

    val res = await(
      ws.url(s"http://localhost:$port/business-rates-property-linking/my-organisation/manage-agent/unassign/from-all-properties")
        .withCookies(languageCookie(language), getSessionCookie(testSessionId))
        .withFollowRedirects(follow = false)
        .withHttpHeaders(HeaderNames.COOKIE -> "sessionId")
        .get()
    )

    res.status shouldBe OK
    Jsoup.parse(res.body)
  }

}
