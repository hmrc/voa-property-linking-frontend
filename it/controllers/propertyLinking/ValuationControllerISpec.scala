import base.{HtmlComponentHelpers, ISpecBase}
import com.github.tomakehurst.wiremock.client.WireMock.{aResponse, get, post, stubFor}
import models.ListType.CURRENT
import models.{ApiAssessment, ApiAssessments, ListType, Party, PropertyAddress}
import models.assessments.AssessmentsPageSession
import models.properties.AllowedAction
import models.properties.AllowedAction.{CHECK, VIEW_DETAILED_VALUATION}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.http.Status.OK
import play.api.libs.json.Json
import play.api.test.Helpers._
import uk.gov.hmrc.http.{HeaderCarrier, SessionId}

import java.time.LocalDate
import java.util.UUID


class ValuationControllerISpec extends ISpecBase with HtmlComponentHelpers {

  val testSessionId = s"stubbed-${UUID.randomUUID}"
  val plSubId = "pl-submission-id"

  val paragraphOneSelector = "#explanatory-section > p:nth-child(1)"
  val paragraphLinkSelector = "#explanatory-section > p:nth-child(2)"
  val linkSectionTextSelector = "#explanatory-link"
  val linkSelector = ""
  val bulletPointOneSelector = ".govuk-list--bullet > li:nth-child(1)"
  val bulletPointTwoSelector = ".govuk-list--bullet > li:nth-child(2)"

  lazy val mockRepository: AssessmentsPageSession = app.injector.instanceOf[AssessmentsPageSession]
  implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId(testSessionId)))

  "Valuation controller" should {
    "displays assessment page with the correct content" in {

      lazy val document: Document = getPage(language = English, chosenListYear = "2017")

      document.title() shouldBe "jhbkjbkjnkj"

    }
  }
  private def getPage(language: Language, chosenListYear: String): Document = {

    lazy val april2017 = LocalDate.of(2017, 4, 1)

    def apiAssessments(): ApiAssessments = {
      val april2017 = LocalDate.of(2017, 4, 1)
      ApiAssessments(
        authorisationId = 1111L,
        submissionId = "submissionId",
        uarn = 1111L,
        address = "address",
        pending = false,
        clientOrgName = None,
        capacity = Some("OWNER"),
        assessments = Seq(
          ApiAssessment(
            authorisationId = 1111L,
            assessmentRef = 1234L,
            listYear = "2017",
            uarn = 1111L,
            effectiveDate = Some(april2017),
            rateableValue = Some(123L),
            address = PropertyAddress(Seq(""), "address.postcode"),
            billingAuthorityReference = "localAuthorityRef",
            billingAuthorityCode = Some("2715"),
            listType = CURRENT,
            allowedActions = List(VIEW_DETAILED_VALUATION, CHECK),
            currentFromDate = Some(LocalDate.of(2019, 2, 21)),
            currentToDate = Some(LocalDate.of(2023, 2, 21))
          ),
          ApiAssessment(
            authorisationId = 1234L,
            assessmentRef = 1236L,
            listYear = "CURRENT",
            uarn = 12345L,
            effectiveDate = Some(april2017),
            rateableValue = Some(1234L),
            address = PropertyAddress(Seq(""), "address.postcode"),
            billingAuthorityReference = "localAuthorityRef",
            billingAuthorityCode = Some("2715"),
            listType = ListType.CURRENT,
            allowedActions = List(AllowedAction.ENQUIRY),
            currentFromDate = Some(april2017.plusMonths(2L)),
            currentToDate = None
          )
        ),
        agents = Seq.empty[Party]
      )
    }


    stubFor {
      get(s"/property-linking/dashboard/agent/assessments/$plSubId")
        .willReturn {
          aResponse.withStatus(OK).withBody(Json.toJson(apiAssessments).toString())
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
      ws.url(s"http://localhost:$port/business-rates-property-linking/property-link/$plSubId/assessments?owner=false")
        .withCookies(languageCookie(language), getSessionCookie(testSessionId))
        .withFollowRedirects(follow = false)
        .get()
    )

    res.status shouldBe OK
    Jsoup.parse(res.body)
  }

}
