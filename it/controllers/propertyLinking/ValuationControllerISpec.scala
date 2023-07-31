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


//class ValuationControllerISpec extends ISpecBase with HtmlComponentHelpers {
//
//  val testSessionId = s"stubbed-${UUID.randomUUID}"
//  val plSubId = "pl-submission-id"
//
//  val paragraphOneOwnerSelector = "#owner-section > p:nth-child(1)"
//  val paragraphLinkOwnerSelector = "#owner-section > p:nth-child(2)"
//  val linkSectionTextSelector = "#link-section"
//  val linkSectionLinkSelector = "#explanatory-link"
//  val listCaptionSelector = "#agent-section > p:nth-child(1)"
//  val paragraphOneClientSelector = "#owner-section > p:nth-child(1)"
//  val paragraphLinkClientSelector = "#owner-section > p:nth-child(2)"
//  val bulletPointOneSelector = "#reasons-list > li:nth-child(1)"
//  val bulletPointTwoSelector = "#reasons-list > li:nth-child(2)"
//  val contactTextSelector = "#contact-text"
//  val noValuationsTextSelector = "#no-valuation-text"
//
//  lazy val mockRepository: AssessmentsPageSession = app.injector.instanceOf[AssessmentsPageSession]
//  implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId(testSessionId)))
//
//  "Valuation controller" should {
//        "displays assessment page with the correct content for owner in English" in {
//
//          lazy val document: Document = getPage(language = English, listYears = Seq("2017", "2023"))
//
//          document.select(paragraphOneOwnerSelector).text shouldBe("We only show valuations for when you owned or occupied the property.")
//          document.select(paragraphLinkOwnerSelector).text shouldBe("Valuations for other periods may be available.")
//          document.select(linkSectionTextSelector).text shouldBe("Find public versions of all valuations for this property.")
//
//        }
//  }
//
//  "Valuation controller" should {
//    "displays assessment page with the correct content for owner in Welsh" in {
//
//      lazy val document: Document = getPage(language = Welsh, listYears = Seq("2017", "2023"))
//
//      document.select(paragraphOneOwnerSelector).text shouldBe ("We only show valuations for when you owned or occupied the property.")
//      document.select(paragraphLinkOwnerSelector).text shouldBe ("Valuations for other periods may be available.")
//      document.select(linkSectionTextSelector).text shouldBe ("Find public versions of all valuations for this property.")
//
//    }
//  }
//  "Valuation controller" should {
//    "displays assessment page with the correct content for client in English" in {
//
//      lazy val document: Document = getPage(language = English, listYears = Seq("2017", "2023"))
//
//      document.select(listCaptionSelector).text shouldBe ("We only show valuations:")
//      document.select(bulletPointOneSelector).text shouldBe ("for the rating lists your client wants you to act on")
//      document.select(bulletPointTwoSelector).text shouldBe ("for when your client owned or occupied the property")
//      document.select(linkSectionTextSelector).text shouldBe ("Valuations for other periods may be available.")
//      document.select(linkSectionLinkSelector).text shouldBe (" Find public versions of all valuations for this property.")
//      document.select(contactTextSelector).text shouldBe ("Contact your client if you need to change which lists you can act on.")
//
//    }
//  }
//  "Valuation controller" should {
//    "displays assessment page with the correct content for client in Welsh" in {
//
//      lazy val document: Document = getPage(language = Welsh, listYears = Seq("2017", "2023"))
//
//      document.select(listCaptionSelector).text shouldBe ("We only show valuations:")
//      document.select(bulletPointOneSelector).text shouldBe ("for the rating lists your client wants you to act on")
//      document.select(bulletPointTwoSelector).text shouldBe ("for when your client owned or occupied the property")
//      document.select(linkSectionTextSelector).text shouldBe ("Valuations for other periods may be available.")
//      document.select(linkSectionLinkSelector).text shouldBe (" Find public versions of all valuations for this property.")
//      document.select(contactTextSelector).text shouldBe ("Contact your client if you need to change which lists you can act on.")
//
//    }
//  }
//  "Valuation controller" should {
//        "displays assessment page with the correct content for client in English when there is no valuations to display" in {
//
//          lazy val document: Document = getPage(language = English, listYears = Seq("2017", "2023"))
//
//          document.select(noValuationsTextSelector).text shouldBe("There are no valuations available for this property.")
//
//        }
//  }
//  "Valuation controller" should {
//        "displays assessment page with the correct content for client in Welsh when there is no valuations to display" in {
//
//          lazy val document: Document = getPage(language = Welsh, listYears = Seq("2017", "2023"))
//
//          document.select(noValuationsTextSelector).text shouldBe("There are no valuations available for this property.")
//
//        }
//  }
//
//    private def getPage(language: Language, listYears: Seq[String]): Document = {
//
//      lazy val april2017 = LocalDate.of(2017, 4, 1)
//
//      def apiAssessments(): ApiAssessments = {
//        val april2017 = LocalDate.of(2017, 4, 1)
//        ApiAssessments(
//          authorisationId = 1111L,
//          submissionId = "submissionId",
//          uarn = 1111L,
//          address = "address",
//          pending = false,
//          clientOrgName = None,
//          capacity = Some("OWNER"),
//          assessments = Seq(
//            ApiAssessment(
//              authorisationId = 1111L,
//              assessmentRef = 1234L,
//              listYear = "2017",
//              uarn = 1111L,
//              effectiveDate = Some(april2017),
//              rateableValue = Some(123L),
//              address = PropertyAddress(Seq(""), "address.postcode"),
//              billingAuthorityReference = "localAuthorityRef",
//              billingAuthorityCode = Some("2715"),
//              listType = CURRENT,
//              allowedActions = List(VIEW_DETAILED_VALUATION, CHECK),
//              currentFromDate = Some(LocalDate.of(2019, 2, 21)),
//              currentToDate = Some(LocalDate.of(2023, 2, 21))
//            ),
//            ApiAssessment(
//              authorisationId = 1234L,
//              assessmentRef = 1236L,
//              listYear = "CURRENT",
//              uarn = 12345L,
//              effectiveDate = Some(april2017),
//              rateableValue = Some(1234L),
//              address = PropertyAddress(Seq(""), "address.postcode"),
//              billingAuthorityReference = "localAuthorityRef",
//              billingAuthorityCode = Some("2715"),
//              listType = ListType.CURRENT,
//              allowedActions = List(AllowedAction.ENQUIRY),
//              currentFromDate = Some(april2017.plusMonths(2L)),
//              currentToDate = None
//            )
//          ),
//          agents = Seq.empty[Party]
//        )
//      }
//
//
//      stubFor {
//        get(s"/property-linking/dashboard/agent/assessments/$plSubId")
//          .willReturn {
//            aResponse.withStatus(OK).withBody(Json.toJson(apiAssessments).toString())
//          }
//      }
//
//
//      stubFor {
//        get("/business-rates-authorisation/authenticate")
//          .willReturn {
//            aResponse.withStatus(OK).withBody(Json.toJson(testAccounts).toString())
//          }
//      }
//
//      stubFor {
//        post("/auth/authorise")
//          .willReturn {
//            aResponse.withStatus(OK).withBody("{}")
//          }
//      }
//
//      val res = await(
//        ws.url(s"http://localhost:$port/business-rates-property-linking/property-link/$plSubId/assessments?owner=false")
//          .withCookies(languageCookie(language), getSessionCookie(testSessionId))
//          .withFollowRedirects(follow = false)
//          .get()
//      )
//
//      res.status shouldBe OK
//      Jsoup.parse(res.body)
//    }
//
//  }
