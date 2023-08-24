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

class CheckYourAnswersControllerISpec extends ISpecBase with HtmlComponentHelpers {

  val testSessionId = s"stubbed-${UUID.randomUUID}"

  val titleText = "Check and confirm your details - Valuation Office Agency - GOV.UK"
  val backLinkText = "Back"
  val continueButtonText = "Confirm and appoint"
  val captionText = "Appoint an agent"
  val agentHeading = "Agent"

  val titleTextWelsh = "Gwirio a chadarnhau eich manylion - Valuation Office Agency - GOV.UK"
  val backLinkTextWelsh = "Yn ôl"
  val continueButtonTextWelsh = "Cadarnhau a phenodi"
  val captionTextWelsh = "Penodi asiant"
  val agentHeadingWelsh = "Asiant"
  val assignPropertiesHeading = "Which properties do you want to assign to this agent?"
  val assignPropertiesHeadingWelsh = "Pa eiddo ydych chi’n dymuno neilltuo ir asiant hwn?"
  val assignRatingListHeading = "Which rating list do you want this agent to act on for you?"
  val assignRatingListHeadingWelsh = "Pa restr ardrethu yr hoffech i’r asiant hwn ei gweithredu ar eich rhan?"
  val answerRatingList = "2023 and 2017 rating lists"
  val answerRatingListWelsh = "Rhestr ardrethu 2023 a rhestr ardrethu 2017"

  val agentHeadingId = "agent-heading"
  val agentValueId = "agent-value"
  val changeAgentLinkId = "change-agent"
  val propertiesHeadingId = "properties-heading"
  val propertiesValueId = "properties-value"
  val ratingsHeadingId = "ratings-heading"
  val ratingsValueId = "ratings-value"
  val changePropertiesLinkId = "change-properties"
  val continueButtonSelector = "button.govuk-button"
  val backLinkSelector = "#back-link"

  "onPageLoad" should {
    "return 200 & display correct content (Some properties)" in new TestSetup {

      await(mockAppointAgentSessionRepository.saveOrUpdate(managingPropertyData.copy(propertySelectedSize = 1)))
      await(mockAppointAgentPropertiesSessionRepository
        .saveOrUpdate(propertiesSessionData.agentAppointAction.map(_.copy(propertyLinkIds = List("123")))))

      lazy val document = getDocument(English)
      document.getElementById(propertiesValueId).text() shouldBe "1 of 2 properties"
      document.select(backLinkSelector).text() shouldBe backLinkText
      document.select(backLinkSelector).attr("href")shouldBe
        "/business-rates-property-linking/my-organisation/appoint-new-agent/multiple-properties"
    }

    "return 200 & display correct content (Some properties) - Welsh" in new TestSetup {

      await(mockAppointAgentSessionRepository.saveOrUpdate(managingPropertyData.copy(propertySelectedSize = 1)))
      await(mockAppointAgentPropertiesSessionRepository
        .saveOrUpdate(propertiesSessionData.agentAppointAction.map(_.copy(propertyLinkIds = List("123")))))

      lazy val document = getDocument(Welsh)
      document.getElementById(propertiesValueId).text() shouldBe "1 o 2 eiddo"
      document.select(backLinkSelector).text() shouldBe backLinkTextWelsh
      document.select(backLinkSelector).attr("href") shouldBe
        "/business-rates-property-linking/my-organisation/appoint-new-agent/multiple-properties"

    }

    "return 200 & display correct content (All properties)" in new TestSetup {

      lazy val document = getDocument(English)
      document.getElementById(propertiesValueId).text() shouldBe "All properties"

    }

    "return 200 & display correct content (All properties) - Welsh" in new TestSetup {

      lazy val document = getDocument(Welsh)
      document.getElementById(propertiesValueId).text() shouldBe "Pob eiddo"
    }

    "return 200 & display correct content (Rate payer has no properties)" in new TestSetup {
      await(mockAppointAgentSessionRepository.saveOrUpdate(managingPropertyData
        .copy(totalPropertySelectionSize = 0).copy(propertySelectedSize = 0)))
      await(mockAppointAgentPropertiesSessionRepository
        .saveOrUpdate(propertiesSessionData.agentAppointAction.map(_.copy(propertyLinkIds = List().empty))))

      lazy val document = getDocument(English)
      document.select("#main-content > div > div > dl > div:nth-child(3)").hasClass("govuk-visually-hidden") shouldBe true
      document.select(backLinkSelector).text() shouldBe backLinkText
      document.select(backLinkSelector).attr("href") shouldBe
        "/business-rates-property-linking/my-organisation/appoint-new-agent/is-correct-agent"
    }

    "return 200 & display correct content (Rate payer has no properties) - Welsh" in new TestSetup {
      await(mockAppointAgentSessionRepository.saveOrUpdate(managingPropertyData
        .copy(totalPropertySelectionSize = 0).copy(propertySelectedSize = 0)))
      await(mockAppointAgentPropertiesSessionRepository
        .saveOrUpdate(propertiesSessionData.agentAppointAction.map(_.copy(propertyLinkIds = List().empty))))

      lazy val document = getDocument(Welsh)
      document.select("#main-content > div > div > dl > div:nth-child(3)").hasClass("govuk-visually-hidden") shouldBe true
      document.select(backLinkSelector).text() shouldBe backLinkTextWelsh
      document.select(backLinkSelector).attr("href") shouldBe
        "/business-rates-property-linking/my-organisation/appoint-new-agent/is-correct-agent"
    }

    "return 200 & display correct content (Assigned to only property)" in new TestSetup {
      await(mockAppointAgentSessionRepository.saveOrUpdate(managingPropertyData
        .copy(totalPropertySelectionSize = 1).copy(propertySelectedSize = 1).copy(singleProperty = true)))
      await(mockAppointAgentPropertiesSessionRepository
        .saveOrUpdate(propertiesSessionData.agentAppointAction.map(_.copy(propertyLinkIds = List("123")))))

      lazy val document = getDocument(English)
      document.getElementById(propertiesValueId).text() shouldBe "Your property"
      document.select(backLinkSelector).text() shouldBe backLinkText
      document.select(backLinkSelector).attr("href") shouldBe
        "/business-rates-property-linking/my-organisation/appoint-new-agent/one-property"
    }

    "return 200 & display correct content (Assigned to only property) - Welsh" in new TestSetup {
      await(mockAppointAgentSessionRepository.saveOrUpdate(managingPropertyData
        .copy(totalPropertySelectionSize = 1).copy(propertySelectedSize = 1).copy(singleProperty = true)))
      await(mockAppointAgentPropertiesSessionRepository
        .saveOrUpdate(propertiesSessionData.agentAppointAction.map(_.copy(propertyLinkIds = List("123")))))

      lazy val document = getDocument(Welsh)
      document.getElementById(propertiesValueId).text() shouldBe "Eich eiddo"
      document.select(backLinkSelector).text() shouldBe backLinkTextWelsh
      document.select(backLinkSelector).attr("href") shouldBe
        "/business-rates-property-linking/my-organisation/appoint-new-agent/one-property"
    }

    "return 200 & display correct content (Assigned to no properties)" in new TestSetup {
      await(mockAppointAgentSessionRepository.saveOrUpdate(managingPropertyData
        .copy(totalPropertySelectionSize = 1).copy(propertySelectedSize = 0)))
      await(mockAppointAgentPropertiesSessionRepository
        .saveOrUpdate(propertiesSessionData.agentAppointAction.map(_.copy(propertyLinkIds = List().empty))))

      lazy val document = getDocument(English)
      document.getElementById(propertiesValueId).text() shouldBe "No properties"
    }

    "return 200 & display correct content (Assigned to no properties) - Welsh" in new TestSetup {
      await(mockAppointAgentSessionRepository.saveOrUpdate(managingPropertyData
        .copy(totalPropertySelectionSize = 1).copy(propertySelectedSize = 0)))
      await(mockAppointAgentPropertiesSessionRepository
        .saveOrUpdate(propertiesSessionData.agentAppointAction.map(_.copy(propertyLinkIds = List().empty))))

      lazy val document = getDocument(Welsh)
      document.getElementById(propertiesValueId).text() shouldBe "Dim eiddo"
    }

    "return 200 & display correct content (both rating values)" in new TestSetup {

      await(mockAppointAgentSessionRepository.saveOrUpdate(managingPropertyData.copy(propertySelectedSize = 1)))
      await(mockAppointAgentPropertiesSessionRepository
        .saveOrUpdate(propertiesSessionData.agentAppointAction.map(_.copy(propertyLinkIds = List("123")))))

      lazy val document = getDocument(English)
      document.getElementById(ratingsValueId).text() shouldBe answerRatingList
      document.select(backLinkSelector).text() shouldBe backLinkText
      document.select(backLinkSelector).attr("href")shouldBe
        "/business-rates-property-linking/my-organisation/appoint-new-agent/multiple-properties"
    }

    "return 200 & display correct content (both rating values) - Welsh" in new TestSetup {

      await(mockAppointAgentSessionRepository.saveOrUpdate(managingPropertyData.copy(propertySelectedSize = 1)))
      await(mockAppointAgentPropertiesSessionRepository
        .saveOrUpdate(propertiesSessionData.agentAppointAction.map(_.copy(propertyLinkIds = List("123")))))

      lazy val document = getDocument(Welsh)
      document.getElementById(ratingsValueId).text() shouldBe answerRatingListWelsh
      document.select(backLinkSelector).text() shouldBe backLinkTextWelsh
      document.select(backLinkSelector).attr("href") shouldBe
        "/business-rates-property-linking/my-organisation/appoint-new-agent/multiple-properties"

    }

    "return 404 Not Found when no ManagingProperty data found in cache" in new TestSetup {
      await(mockAppointAgentSessionRepository.remove())

      val res = await(
        ws.url(s"http://localhost:$port/business-rates-property-linking/my-organisation/appoint-new-agent/check-your-answers")
          .withCookies(languageCookie(English), getSessionCookie(testSessionId))
          .withFollowRedirects(follow = false)
          .get()
      )

      res.status shouldBe NOT_FOUND

    }
  }

  "onSubmit" should {
    "return 303 & redirect confirm appointment page on successful appointment(some properties - 2017 list years)" in new TestSetup {

      val requestBody = Json.obj(
        "agentCode" -> agentCode,
        "scope" -> s"${AppointmentScope.PROPERTY_LIST}"
      )

      val res = await(
        ws.url(s"http://localhost:$port/business-rates-property-linking/my-organisation/appoint-new-agent/check-your-answers")
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
          |       "123", "321"
          |   ],
          |   "listYears":[
          |      "2017"
          |   ]
          |}""".stripMargin)

      //Check that the listYears returned from agent summary call are included in request body to backend
      verify(1, postRequestedFor(urlEqualTo("/property-linking/my-organisation/agent/submit-appointment-changes"))
        .withRequestBody(equalToJson(jsonRequest.toString())))

      res.status shouldBe SEE_OTHER
      res.headers("Location").head shouldBe "/business-rates-property-linking/my-organisation/confirm-appoint-agent"
    }

    "return 303 & redirect confirm appointment page on successful appointment(some properties - 2023 list years)" in new TestSetup {
      stubFor {
        get("/property-linking/owner/agents")
          .willReturn {
            aResponse.withStatus(OK).withBody(Json.toJson(testAgentListFor2023).toString())
          }
      }

      val requestBody = Json.obj(
        "agentCode" -> agentCode,
        "scope" -> s"${AppointmentScope.PROPERTY_LIST}"
      )

      val res = await(
        ws.url(s"http://localhost:$port/business-rates-property-linking/my-organisation/appoint-new-agent/check-your-answers")
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
          |      "123", "321"
          |   ],
          |   "listYears":[
          |      "2023"
          |   ]
          |}""".stripMargin)

      //Check that the listYears returned from agent summary call are included in request body to backend
      verify(1, postRequestedFor(urlEqualTo("/property-linking/my-organisation/agent/submit-appointment-changes"))
        .withRequestBody(equalToJson(jsonRequest.toString())))


      res.status shouldBe SEE_OTHER
      res.headers("Location").head shouldBe "/business-rates-property-linking/my-organisation/confirm-appoint-agent"
    }

    "return 400 BadRequest when agent code isn't submitted" in new TestSetup {

      val requestBody = Json.obj("scope" -> s"${AppointmentScope.PROPERTY_LIST}")

      val res = await(
        ws.url(s"http://localhost:$port/business-rates-property-linking/my-organisation/appoint-new-agent/check-your-answers")
          .withCookies(languageCookie(English), getSessionCookie(testSessionId))
          .withFollowRedirects(follow = false)
          .withHttpHeaders(HeaderNames.COOKIE -> "sessionId", "Csrf-Token" -> "nocheck")
          .post(body = requestBody)
      )

      res.status shouldBe BAD_REQUEST
    }
    "return 400 BadRequest when scope isn't submitted" in new TestSetup {

      val requestBody = Json.obj("agentCode" -> agentCode)

      val res = await(
        ws.url(s"http://localhost:$port/business-rates-property-linking/my-organisation/appoint-new-agent/check-your-answers")
          .withCookies(languageCookie(English), getSessionCookie(testSessionId))
          .withFollowRedirects(follow = false)
          .withHttpHeaders(HeaderNames.COOKIE -> "sessionId", "Csrf-Token" -> "nocheck")
          .post(body = requestBody)
      )

      res.status shouldBe BAD_REQUEST
    }
  }

  class TestSetup {

    lazy val mockAppointAgentSessionRepository: AppointAgentSessionRepository = app.injector.instanceOf[AppointAgentSessionRepository]
    lazy val mockAppointAgentPropertiesSessionRepository: AppointAgentPropertiesSessionRepository = app.injector.instanceOf[AppointAgentPropertiesSessionRepository]
    implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId(testSessionId)))

    val account = groupAccount(true)
    val agentCode = 1001
    val agentName = "Test Agent"
    val backLinkUrl = "some/url"

    val managingPropertyData: ManagingProperty = ManagingProperty(
      agentCode = agentCode,
      agentOrganisationName = "Some Org",
      isCorrectAgent = true,
      managingPropertyChoice = All.name,
      agentAddress = "An Address",
      backLink = None,
      totalPropertySelectionSize = 2,
      propertySelectedSize = 2,
      bothRatingLists = Some(true),
      specificRatingList = None
    )

    val propertiesSessionData: AppointAgentToSomePropertiesSession = AppointAgentToSomePropertiesSession(agentAppointAction =
      Some(AgentAppointBulkAction(agentCode = agentCode, name = agentName, propertyLinkIds = List("123", "321"), backLinkUrl = "some-back-link")),
      filters = FilterAppointProperties(None, None))

    await(mockAppointAgentSessionRepository.saveOrUpdate(managingPropertyData))
    await(mockAppointAgentPropertiesSessionRepository.saveOrUpdate(propertiesSessionData))

    stubFor {
      post("/property-linking/my-organisation/agent/submit-appointment-changes")
        .willReturn {
          aResponse.withStatus(OK).withBody(Json.toJson(AgentAppointmentChangesResponse("some-id")).toString())
        }
    }

    stubFor {
      get("/property-linking/owner/agents")
        .willReturn {
          aResponse.withStatus(OK).withBody(Json.toJson(testAgentListFor2017).toString())
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

  def getDocument(language: Language): Document = {
    val res = await(
      ws.url(s"http://localhost:$port/business-rates-property-linking/my-organisation/appoint-new-agent/check-your-answers")
        .withCookies(languageCookie(language), getSessionCookie(testSessionId))
        .withFollowRedirects(follow = false)
        .get()
    )
    res.status shouldBe OK
    val document = Jsoup.parse(res.body)

    if (language == English) {
      document.title() shouldBe titleText
      document.getElementsByClass("govuk-caption-l").text() shouldBe captionText
      document.getElementById(agentHeadingId).text() shouldBe agentHeading
      document.getElementById(agentValueId).text() shouldBe "Some Org"
      document.getElementById(propertiesHeadingId).text() shouldBe assignPropertiesHeading
      document.getElementById(ratingsHeadingId).text() shouldBe assignRatingListHeading
      document.select(continueButtonSelector).text() shouldBe continueButtonText
    } else {
      document.title() shouldBe titleTextWelsh
      document.getElementsByClass("govuk-caption-l").text() shouldBe captionTextWelsh
      document.getElementById(agentHeadingId).text() shouldBe agentHeadingWelsh
      document.getElementById(agentValueId).text() shouldBe "Some Org"
      document.getElementById(propertiesHeadingId).text() shouldBe assignPropertiesHeadingWelsh
      document.getElementById(ratingsHeadingId).text() shouldBe assignRatingListHeadingWelsh
      document.select(continueButtonSelector).text() shouldBe continueButtonTextWelsh
    }

    document
  }
}
