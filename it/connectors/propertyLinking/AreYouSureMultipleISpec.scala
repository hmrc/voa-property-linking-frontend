package connectors.propertyLinking

import base.{HtmlComponentHelpers, ISpecBase}
import models.propertyrepresentation.{AgentAppointmentChangesResponse, AgentSummary}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.http.Status.OK
import play.api.test.Helpers._
import repositories.ManageAgentSessionRepository
import uk.gov.hmrc.http.{HeaderCarrier, SessionId}
import com.github.tomakehurst.wiremock.client.WireMock.{aResponse, equalToJson, get, post, postRequestedFor, stubFor, urlEqualTo, verify}
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import play.api.http.HeaderNames
import play.api.libs.json.Json

import java.time.LocalDate
import java.util.UUID

class AreYouSureMultipleISpec extends ISpecBase with HtmlComponentHelpers {

  val testSessionId = s"stubbed-${UUID.randomUUID}"

  lazy val mockRepository: ManageAgentSessionRepository = app.injector.instanceOf[ManageAgentSessionRepository]
  implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId(testSessionId)))

  val titleText = "Are you sure you want Test Agent to act for you on the 2023 and 2017 rating lists? - Valuation Office Agency - GOV.UK"
  val backLinkText = "Back"
  val captionText = "Manage agent"
  val headerText = "Are you sure you want Test Agent to act for you on the 2023 and 2017 rating lists?"
  val forAllText = "For all your property valuations on the 2023 and 2017 rating lists, this agent will be able to:"
  val seeDetailedText = "see detailed property information"
  val seeCheckText = "see Check and Challenge case correspondence such as messages and emails"
  val sendCheckText = "send Check and Challenge cases"
  val thisAppliesText = "This applies to properties that you assign to them or they add to your account"
  val confirmText = "Confirm"
  val cancelText = "Cancel"

  val titleTextWelsh = "Welsh Are you sure you want Test Agent to act for you on the 2023 and 2017 rating lists? - Valuation Office Agency - GOV.UK"
  val backLinkTextWelsh = "Yn ôl"
  val captionTextWelsh = "Rheoli asiant"
  val headerTextWelsh = "Welsh Are you sure you want Test Agent to act for you on the 2023 and 2017 rating lists?"
  val forAllTextWelsh = "Welsh For all your property valuations on the 2023 and 2017 rating lists, this agent will be able to:"
  val seeDetailedTextWelsh = "Welsh see detailed property information"
  val seeCheckTextWelsh = "Welsh see Check and Challenge case correspondence such as messages and emails"
  val sendCheckTextWelsh = "Welsh send Check and Challenge cases"
  val thisAppliesTextWelsh = "Welsh This applies to properties that you assign to them or they add to your account"
  val confirmTextWelsh = "Cadarnhau"
  val cancelTextWelsh = "Canslo"

  val backLinkSelector = "#back-link"
  val captionSelector = "span.govuk-caption-l"
  val headerSelector = "h1.govuk-heading-l"
  val forAllSelector = "#main-content > div > div > p:nth-child(3)"
  val bulletPointSelector = "#main-content > div > div > ul.govuk-list--bullet > li"
  val thisAppliesSelector = "#main-content > div > div > p:nth-child(5)"
  val confirmSelector = "#submit-button"
  val cancelSelector = "#cancel-link"

  val cancelHref = "/business-rates-property-linking/my-organisation/manage-agent/property-links?agentCode=100"
  val backLinkHref = "/business-rates-property-linking/my-organisation/appoint/ratings-list/choose"

  "AreYouSureController show method" should {
    "Show an English are you sure multiple screen with the correct text when the language is set to English" which {

      lazy val document: Document = getAreYouSureMultiplePage(English)

      s"has a title of $titleText" in {
        document.title() shouldBe titleText
      }

      "has a back link which takes you to the which ratings list page" in {
        document.select(backLinkSelector).text() shouldBe backLinkText
        document.select(backLinkSelector).attr("href") shouldBe backLinkHref
      }

      s"has a header of '$headerText' with a caption above of '$captionText'" in {
        document.select(headerSelector).text shouldBe headerText
        document.select(captionSelector).text shouldBe captionText
      }

      s"has text on the screen of '$forAllText'" in {
        document.select(forAllSelector).text() shouldBe forAllText
      }

      s"has radio buttons on the screen with values of '$seeDetailedText', '$seeCheckText' and '$sendCheckText'" in {
        document.select(bulletPointSelector).get(0).text() shouldBe seeDetailedText
        document.select(bulletPointSelector).get(1).text() shouldBe seeCheckText
        document.select(bulletPointSelector).get(2).text() shouldBe sendCheckText
      }

      s"has text on the screen of '$thisAppliesText'" in {
        document.select(thisAppliesSelector).text() shouldBe thisAppliesText
      }

      s"has a '$confirmText' link on the screen" in {
        document.select(confirmSelector).text() shouldBe confirmText
      }

      s"has a '$cancelText' link on the screen, which takes you to the agent details screen" in {
        document.select(cancelSelector).text() shouldBe cancelText
        document.select(cancelSelector).attr("href") shouldBe cancelHref
      }
    }

    "Show a Welsh are you sure multiple screen with the correct text when the language is set to Welsh" which {

      lazy val document: Document = getAreYouSureMultiplePage(Welsh)
      
      s"has a title of $titleText in welsh" in {
        document.title() shouldBe titleTextWelsh
      }

      "has a back link which takes you to the which ratings list page" in {
        document.select(backLinkSelector).text() shouldBe backLinkTextWelsh
        document.select(backLinkSelector).attr("href") shouldBe backLinkHref
      }

      s"has a header of '$headerText' with a caption above of '$captionText' in welsh" in {
        document.select(headerSelector).text shouldBe headerTextWelsh
        document.select(captionSelector).text shouldBe captionTextWelsh
      }

      s"has text on the screen of '$forAllText' in welsh" in {
        document.select(forAllSelector).text() shouldBe forAllTextWelsh
      }

      s"has radio buttons on the screen with values of '$seeDetailedText', '$seeCheckText' and '$sendCheckText' in welsh" in {
        document.select(bulletPointSelector).get(0).text() shouldBe seeDetailedTextWelsh
        document.select(bulletPointSelector).get(1).text() shouldBe seeCheckTextWelsh
        document.select(bulletPointSelector).get(2).text() shouldBe sendCheckTextWelsh
      }

      s"has text on the screen of '$thisAppliesText' in welsh" in {
        document.select(thisAppliesSelector).text() shouldBe thisAppliesTextWelsh
      }

      s"has a '$confirmText' link on the screen in welsh" in {
        document.select(confirmSelector).text() shouldBe confirmTextWelsh
      }

      s"has a '$cancelText' link on the screen in welsh, which takes you to the agent details screen" in {
        document.select(cancelSelector).text() shouldBe cancelTextWelsh
        document.select(cancelSelector).attr("href") shouldBe cancelHref
      }
    }

  }

  "AreYouSureMultipleController post method" should {
    "Redirect to the confirmation page and APPOINT 2023+2017 when current is 2017" in {
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

      stubsSetup

      val res = await(
        ws.url(s"http://localhost:$port/business-rates-property-linking/my-organisation/appoint/ratings-list/are-you-sure-multiple")
          .withCookies(languageCookie(English), getSessionCookie(testSessionId))
          .withFollowRedirects(follow = false)
          .withHttpHeaders(HeaderNames.COOKIE -> "sessionId", "Csrf-Token" -> "nocheck")
          .post(body="")
      )

      res.status shouldBe SEE_OTHER
      res.headers("Location").head shouldBe "/business-rates-property-linking/my-organisation/appoint/ratings-list/confirmed"

      verify(1, postRequestedFor(urlEqualTo("/property-linking/my-organisation/agent/submit-appointment-changes"))
        .withRequestBody(equalToJson(
          """{
            |  "agentRepresentativeCode": 100,
            |  "action": "APPOINT",
            |  "scope": "LIST_YEAR",
            |  "listYears": ["2023", "2017"]
            |}""".stripMargin
        )))

    }

    "Redirect to the confirmation page and APPOINT 2023+2017 when current is 2023" in {
      await(
        mockRepository.saveOrUpdate(
          AgentSummary(
            listYears = Some(List("2023")),
            name = "Test Agent",
            organisationId = 100L,
            representativeCode = 100L,
            appointedDate = LocalDate.now(),
            propertyCount = 1
          )))

      stubsSetup

      val res = await(
        ws.url(s"http://localhost:$port/business-rates-property-linking/my-organisation/appoint/ratings-list/are-you-sure-multiple")
          .withCookies(languageCookie(English), getSessionCookie(testSessionId))
          .withFollowRedirects(follow = false)
          .withHttpHeaders(HeaderNames.COOKIE -> "sessionId", "Csrf-Token" -> "nocheck")
          .post(body = "")
      )

      res.status shouldBe SEE_OTHER
      res.headers("Location").head shouldBe "/business-rates-property-linking/my-organisation/appoint/ratings-list/confirmed"

      verify(1, postRequestedFor(urlEqualTo("/property-linking/my-organisation/agent/submit-appointment-changes"))
        .withRequestBody(equalToJson(
          """{
            |  "agentRepresentativeCode": 100,
            |  "action": "APPOINT",
            |  "scope": "LIST_YEAR",
            |  "listYears": ["2023", "2017"]
            |}""".stripMargin
        )))

    }

    "Redirect to the confirmation page and do not APPOINT 2023+2017 when current is 2023+2017" in {
      await(
        mockRepository.saveOrUpdate(
          AgentSummary(
            listYears = Some(List("2023", "2017")),
            name = "Test Agent",
            organisationId = 100L,
            representativeCode = 100L,
            appointedDate = LocalDate.now(),
            propertyCount = 1
          )))

      stubsSetup

      val res = await(
        ws.url(s"http://localhost:$port/business-rates-property-linking/my-organisation/appoint/ratings-list/are-you-sure-multiple")
          .withCookies(languageCookie(English), getSessionCookie(testSessionId))
          .withFollowRedirects(follow = false)
          .withHttpHeaders(HeaderNames.COOKIE -> "sessionId", "Csrf-Token" -> "nocheck")
          .post(body = "")
      )

      res.status shouldBe SEE_OTHER
      res.headers("Location").head shouldBe "/business-rates-property-linking/my-organisation/appoint/ratings-list/confirmed"

      verify(0, postRequestedFor(urlEqualTo("/property-linking/my-organisation/agent/submit-appointment-changes"))
      )

    }


  }
  private def getAreYouSureMultiplePage(language: Language): Document = {

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
      ws.url(s"http://localhost:$port/business-rates-property-linking/my-organisation/appoint/ratings-list/are-you-sure-multiple")
        .withCookies(languageCookie(language), getSessionCookie(testSessionId))
        .withFollowRedirects(follow = false)
        .get()
    )

    res.status shouldBe OK
    Jsoup.parse(res.body)
  }

  private def stubsSetup: StubMapping = {

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
      post("/property-linking/my-organisation/agent/submit-appointment-changes")
        .willReturn {
          aResponse.withStatus(ACCEPTED).withBody(Json.toJson(AgentAppointmentChangesResponse("success")).toString())
        }
    }
  }


}
