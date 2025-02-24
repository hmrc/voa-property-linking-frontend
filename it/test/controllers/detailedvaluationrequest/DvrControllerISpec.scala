/*
 * Copyright 2024 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package controllers.detailedvaluationrequest

import base.{HtmlComponentHelpers, ISpecBase}
import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.http.HeaderNames
import play.api.http.Status.{NO_CONTENT, OK}
import play.api.libs.json.Json
import play.api.libs.json.Json.toJson
import play.api.libs.ws.WSResponse
import play.api.test.Helpers._
import uk.gov.hmrc.http.{HeaderCarrier, SessionId}

import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.UUID

class DvrControllerISpec extends ISpecBase with HtmlComponentHelpers {

  override lazy val extraConfig: Map[String, String] = Map("feature-switch.comparablePropertiesEnabled" -> "false")

  val introSelector = "#intro"
  val mainContendSelector = "#main-content"
  val currentReadableValueSelector = "#rateable-value-caption"
  val errorAtRadioSelector = "#checkType_-error"
  val errorSummarySelector = "#error-link"

  val errorTitleText = "ADDRESS - Valuation Office Agency - GOV.UK" //TODO: This is a bug, should error
  val noRadioSelectedErrorText = "Select what you want to tell us"
  val errorTitleTextWelsh = "ADDRESS - Valuation Office Agency - GOV.UK" //TODO: This is a bug, should error
  val noRadioSelectedErrorTextWelsh = "Dewisiwch beth rydych am ei ddweud wrthym"
  val errorText = "Error: "
  val errorTextWelsh = "Gwall: "

  override def submissionId = "PL1ZRPBP7"
  override def uarn: Long = 7651789000L
  override def valuationId: Long = 10028428L
  override def propertyLinkId: Long = 128L

  val checkId = "1774b2a8-4ad1-4351-88fa-f9dc4868fa1c"

  val testSessionId = s"stubbed-${UUID.randomUUID}"

  implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId(testSessionId)))

  "DvrController startCheck method" should {
    "Redirect to the 'The property details need changing' page and create a draft check when they choose the 'The property details need changing' radio button" in {

      val checkType = "internal"
      val dvrCheck = true
      val rateableValueTooHigh = false

      lazy val res = postStartCheckPage(language = English, checkType, dvrCheck, rateableValueTooHigh)

      res.status shouldBe SEE_OTHER
      res
        .headers("Location")
        .head shouldBe s"http://localhost:9534/business-rates-check/property-link/$propertyLinkId/assessment/$valuationId/internal/$checkId?propertyLinkSubmissionId=$submissionId&uarn=$uarn&dvrCheck=true&rvth=$rateableValueTooHigh"

      val resumeCheckJsonBody = Json.parse(s"""{
                                              |   "resumeUrl": "/business-rates-check/property-link/$propertyLinkId/assessment/$valuationId/internal/$checkId?propertyLinkSubmissionId=$submissionId&uarn=$uarn&dvrCheck=true&rvth=$rateableValueTooHigh"
                                              |}""".stripMargin)

      verify(
        1,
        putRequestedFor(urlEqualTo(s"/partial-check/$checkId/resume"))
          .withRequestBody(equalToJson(resumeCheckJsonBody.toString()))
      )
    }

    "Redirect to the 'rateable value is too high' page and create a draft check when they choose the 'The rateable value is too high' radio button" in {

      val checkType = "rateableValueTooHigh"
      val dvrCheck = true
      val rateableValueTooHigh = true

      lazy val res = postStartCheckPage(language = English, checkType, dvrCheck, rateableValueTooHigh)

      res.status shouldBe SEE_OTHER
      res
        .headers("Location")
        .head shouldBe s"http://localhost:9534/business-rates-check/property-link/$propertyLinkId/assessment/$valuationId/internal/$checkId?propertyLinkSubmissionId=$submissionId&uarn=$uarn&dvrCheck=true&rvth=$rateableValueTooHigh"

      val resumeCheckJsonBody = Json.parse(s"""{
                                              |   "resumeUrl": "/business-rates-check/property-link/$propertyLinkId/assessment/$valuationId/internal/$checkId?propertyLinkSubmissionId=$submissionId&uarn=$uarn&dvrCheck=true&rvth=$rateableValueTooHigh"
                                              |}""".stripMargin)

      verify(
        1,
        putRequestedFor(urlEqualTo(s"/partial-check/$checkId/resume"))
          .withRequestBody(equalToJson(resumeCheckJsonBody.toString()))
      )
    }

    "Redirect to the 'remove' page and create a draft check when they choose the 'remove' radio button" in {

      val checkType = "remove"
      val dvrCheck = true
      val rateableValueTooHigh = false

      lazy val res = postStartCheckPage(language = English, checkType, dvrCheck, rateableValueTooHigh)

      res.status shouldBe SEE_OTHER
      res
        .headers("Location")
        .head shouldBe s"http://localhost:9534/business-rates-check/property-link/$propertyLinkId/assessment/$valuationId/remove/$checkId?propertyLinkSubmissionId=$submissionId&uarn=$uarn&dvrCheck=true&rvth=$rateableValueTooHigh"

      val resumeCheckJsonBody = Json.parse(s"""{
                                              |   "resumeUrl": "/business-rates-check/property-link/$propertyLinkId/assessment/$valuationId/remove/$checkId?propertyLinkSubmissionId=$submissionId&uarn=$uarn&dvrCheck=true&rvth=$rateableValueTooHigh"
                                              |}""".stripMargin)

      verify(
        1,
        putRequestedFor(urlEqualTo(s"/partial-check/$checkId/resume"))
          .withRequestBody(equalToJson(resumeCheckJsonBody.toString()))
      )
    }

    "Redirect to the 'split' page and create a draft check when they choose the 'split' radio button" in {

      val checkType = "split"
      val dvrCheck = true
      val rateableValueTooHigh = false

      lazy val res = postStartCheckPage(language = English, checkType, dvrCheck, rateableValueTooHigh)

      res.status shouldBe SEE_OTHER
      res
        .headers("Location")
        .head shouldBe s"http://localhost:9534/business-rates-check/property-link/$propertyLinkId/assessment/$valuationId/split/$checkId?propertyLinkSubmissionId=$submissionId&uarn=$uarn&dvrCheck=true&rvth=$rateableValueTooHigh"

      val resumeCheckJsonBody = Json.parse(s"""{
                                              |   "resumeUrl": "/business-rates-check/property-link/$propertyLinkId/assessment/$valuationId/split/$checkId?propertyLinkSubmissionId=$submissionId&uarn=$uarn&dvrCheck=true&rvth=$rateableValueTooHigh"
                                              |}""".stripMargin)

      verify(
        1,
        putRequestedFor(urlEqualTo(s"/partial-check/$checkId/resume"))
          .withRequestBody(equalToJson(resumeCheckJsonBody.toString()))
      )
    }

    "Redirect to the 'merged' page and create a draft check when they choose the 'merged' radio button" in {

      val checkType = "merge"
      val dvrCheck = true
      val rateableValueTooHigh = false

      lazy val res = postStartCheckPage(language = English, checkType, dvrCheck, rateableValueTooHigh)

      res.status shouldBe SEE_OTHER
      res
        .headers("Location")
        .head shouldBe s"http://localhost:9534/business-rates-check/property-link/$propertyLinkId/assessment/$valuationId/merge/$checkId?propertyLinkSubmissionId=$submissionId&uarn=$uarn&dvrCheck=true&rvth=$rateableValueTooHigh"

      val resumeCheckJsonBody = Json.parse(s"""{
                                              |   "resumeUrl": "/business-rates-check/property-link/$propertyLinkId/assessment/$valuationId/merge/$checkId?propertyLinkSubmissionId=$submissionId&uarn=$uarn&dvrCheck=true&rvth=$rateableValueTooHigh"
                                              |}""".stripMargin)

      verify(
        1,
        putRequestedFor(urlEqualTo(s"/partial-check/$checkId/resume"))
          .withRequestBody(equalToJson(resumeCheckJsonBody.toString()))
      )
    }

    "Redirect to the 'external local area' page and create a draft check when they choose the 'external local area' radio button" in {

      val checkType = "external"
      val dvrCheck = true
      val rateableValueTooHigh = false

      lazy val res = postStartCheckPage(language = English, checkType, dvrCheck, rateableValueTooHigh)

      res.status shouldBe SEE_OTHER
      res
        .headers("Location")
        .head shouldBe s"http://localhost:9534/business-rates-check/property-link/$propertyLinkId/assessment/$valuationId/external/$checkId?propertyLinkSubmissionId=$submissionId&uarn=$uarn&dvrCheck=true&rvth=$rateableValueTooHigh"

      val resumeCheckJsonBody = Json.parse(s"""{
                                              |   "resumeUrl": "/business-rates-check/property-link/$propertyLinkId/assessment/$valuationId/external/$checkId?propertyLinkSubmissionId=$submissionId&uarn=$uarn&dvrCheck=true&rvth=$rateableValueTooHigh"
                                              |}""".stripMargin)

      verify(
        1,
        putRequestedFor(urlEqualTo(s"/partial-check/$checkId/resume"))
          .withRequestBody(equalToJson(resumeCheckJsonBody.toString()))
      )
    }

    "Redirect to the 'court decision' page and create a draft check when they choose the 'court decision' radio button" in {

      val checkType = "legal-decision"
      val dvrCheck = true
      val rateableValueTooHigh = false

      lazy val res = postStartCheckPage(language = English, checkType, dvrCheck, rateableValueTooHigh)

      res.status shouldBe SEE_OTHER
      res
        .headers("Location")
        .head shouldBe s"http://localhost:9534/business-rates-check/property-link/$propertyLinkId/assessment/$valuationId/legal-decision/$checkId?propertyLinkSubmissionId=$submissionId&uarn=$uarn&dvrCheck=true&rvth=$rateableValueTooHigh"

      val resumeCheckJsonBody = Json.parse(s"""{
                                              |   "resumeUrl": "/business-rates-check/property-link/$propertyLinkId/assessment/$valuationId/legal-decision/$checkId?propertyLinkSubmissionId=$submissionId&uarn=$uarn&dvrCheck=true&rvth=$rateableValueTooHigh"
                                              |}""".stripMargin)

      verify(
        1,
        putRequestedFor(urlEqualTo(s"/partial-check/$checkId/resume"))
          .withRequestBody(equalToJson(resumeCheckJsonBody.toString()))
      )
    }

    "Return a BAD_REQUEST and show an error when no radio button is chosen in English" in {

      val checkType = "Internal" // Just for the stubs setup
      val dvrCheck = true
      val rateableValueTooHigh = false

      lazy val res = invalidPostStartCheckPage(language = English, checkType, dvrCheck, rateableValueTooHigh)

      res.status shouldBe BAD_REQUEST

      lazy val doc: Document = Jsoup.parse(res.body)

      doc.title shouldBe errorTitleText
      doc.select(errorSummarySelector).text shouldBe noRadioSelectedErrorText
      doc.select(errorAtRadioSelector).text shouldBe errorText + noRadioSelectedErrorText

    }

    "Return a BAD_REQUEST and show an error when no radio button is chosen in Welsh" in {

      val checkType = "Internal" // Just for the stubs setup
      val dvrCheck = true
      val rateableValueTooHigh = false

      lazy val res = invalidPostStartCheckPage(language = Welsh, checkType, dvrCheck, rateableValueTooHigh)

      res.status shouldBe BAD_REQUEST

      lazy val doc: Document = Jsoup.parse(res.body)

      doc.title shouldBe errorTitleTextWelsh
      doc.select(errorSummarySelector).text shouldBe noRadioSelectedErrorTextWelsh
      doc.select(errorAtRadioSelector).text shouldBe errorTextWelsh + noRadioSelectedErrorTextWelsh

    }

  }
  private def postStartCheckPage(
        language: Language,
        checkType: String,
        dvrCheck: Boolean,
        rateableValueTooHigh: Boolean
  ): WSResponse = {

    postRequestStubs(checkType, dvrCheck, rateableValueTooHigh)

    val formUrlEncodedBody: String = Seq(
      s"checkType=${URLEncoder.encode(checkType, StandardCharsets.UTF_8.toString)}",
      s"propertyLinkSubmissionId=${URLEncoder.encode(submissionId, StandardCharsets.UTF_8.toString)}",
      s"authorisationId=${URLEncoder.encode(propertyLinkId.toString, StandardCharsets.UTF_8.toString)}",
      s"uarn=${URLEncoder.encode(uarn.toString, StandardCharsets.UTF_8.toString)}",
      s"dvrCheck=${URLEncoder.encode(dvrCheck.toString, StandardCharsets.UTF_8.toString)}",
      s"isOwner=${URLEncoder.encode(true.toString, StandardCharsets.UTF_8.toString)}"
    ).mkString("&")
    await(
      ws.url(
          s"http://localhost:$port/business-rates-property-linking/my-organisation/property-link/$submissionId/valuations/$valuationId/startCheck"
        )
        .withCookies(languageCookie(language), getSessionCookie(testSessionId))
        .withFollowRedirects(follow = false)
        .withHttpHeaders(
          HeaderNames.COOKIE -> "sessionId",
          "Csrf-Token"       -> "nocheck",
          "Content-Type"     -> "application/x-www-form-urlencoded"
        )
        .post(formUrlEncodedBody)
    )
  }

  private def invalidPostStartCheckPage(
        language: Language,
        checkType: String,
        dvrCheck: Boolean,
        rateableValueTooHigh: Boolean
  ): WSResponse = {

    postRequestStubs(checkType, dvrCheck, rateableValueTooHigh)

    val formUrlEncodedBody: String = Seq(
      s"propertyLinkSubmissionId=${URLEncoder.encode(submissionId, StandardCharsets.UTF_8.toString)}",
      s"authorisationId=${URLEncoder.encode(propertyLinkId.toString, StandardCharsets.UTF_8.toString)}",
      s"uarn=${URLEncoder.encode(uarn.toString, StandardCharsets.UTF_8.toString)}",
      s"dvrCheck=${URLEncoder.encode(dvrCheck.toString, StandardCharsets.UTF_8.toString)}",
      s"isOwner=${URLEncoder.encode(true.toString, StandardCharsets.UTF_8.toString)}"
    ).mkString("&")

    await(
      ws.url(
          s"http://localhost:$port/business-rates-property-linking/my-organisation/property-link/$submissionId/valuations/$valuationId/startCheck"
        )
        .withCookies(languageCookie(language), getSessionCookie(testSessionId))
        .withFollowRedirects(follow = false)
        .withHttpHeaders(
          HeaderNames.COOKIE -> "sessionId",
          "Csrf-Token"       -> "nocheck",
          "Content-Type"     -> "application/x-www-form-urlencoded"
        )
        .post(formUrlEncodedBody)
    )
  }

  def authStubs: StubMapping = {
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
  def postRequestStubs(checkType: String, dvrCheck: Boolean, rateableValueTooHigh: Boolean): StubMapping = {

    authStubs

    stubFor {
      get(s"/business-rates-challenge/my-organisations/challenge-cases?submissionId=$submissionId")
        .willReturn {
          aResponse.withStatus(OK).withBody(Json.toJson(testChallengeCasesWithClient).toString())
        }
    }

    stubFor {
      get(s"/property-linking/dashboard/owner/assessments/$submissionId")
        .willReturn {
          aResponse
            .withStatus(OK)
            .withBody(Json.toJson(testApiAssessments(List(currentApiAssessment, previousApiAssessment))).toString())
        }
    }

    stubFor {
      get(s"/property-linking/properties/$uarn/valuation/$valuationId/files?propertyLinkId=$submissionId")
        .willReturn {
          aResponse.withStatus(OK).withBody(Json.toJson(someDvrDocumentFiles).toString())
        }
    }

    stubFor {
      get(s"/property-linking/check-cases/$submissionId/client")
        .willReturn {
          aResponse.withStatus(OK).withBody(Json.toJson(testCheckCasesWithClient).toString())
        }
    }

    stubFor {
      get(s"/business-rates-challenge/my-organisations/challenge-cases?submissionId=$submissionId")
        .willReturn {
          aResponse.withStatus(OK).withBody(Json.toJson(testChallengeCasesWithClient).toString())
        }
    }

    val checkStartBody = Json.obj("id" -> toJson(checkId))

    val updatedCheckType = if (checkType == "rateableValueTooHigh") "internal" else checkType

    stubFor {
      post(
        s"/property-link/$propertyLinkId/assessment/$valuationId/start-check/$updatedCheckType?propertyLinkSubmissionId=$submissionId&uarn=$uarn&dvrCheck=$dvrCheck&rateableValueTooHigh=$rateableValueTooHigh"
      ).willReturn {
        aResponse.withStatus(CREATED).withBody(checkStartBody.toString())
      }
    }

    stubFor {
      put(s"/partial-check/$checkId/resume")
        .willReturn {
          aResponse.withStatus(NO_CONTENT).withBody(checkStartBody.toString())
        }
    }
  }

  "DvrController myOrganisationRequestDetailValuationCheck method" should {
    "Load to the 'Dvr files' page & display the correct content on the valuationTab - English" in {

      val res = getDvrFilesPage(English)
      val doc = Jsoup.parse(res.body)
      doc.title() shouldBe "ADDRESS - Valuation Office Agency - GOV.UK"
      doc
        .getElementById("valuation-tab-li4")
        .text() shouldBe "we have altered this valuation in the last 6 months and you send a Check case from the current valuation."
      doc
        .getElementById("valuation-tab-li5")
        .text() shouldBe "a court decision affected this property’s rateable value and before 1 October 2023 you send a Check case from the current valuation."
      doc
        .getElementById("valuation-tab-p4")
        .text() shouldBe "If the assessment has been deleted for either example, you can send a check case from the most recent live valuation."
    }

    "Load to the 'Dvr files' page & display the correct content on the valuationTab - Welsh" in {
      val res = getDvrFilesPage(Welsh)
      val doc = Jsoup.parse(res.body)
      doc.title() shouldBe "ADDRESS - Valuation Office Agency - GOV.UK"
      doc
        .getElementById("valuation-tab-li4")
        .text() shouldBe "ydym wedi newid y prisiad hwn yn ystod y 6 mis diwethaf ac eich bod yn anfon achos Gwirio o’r prisiad cyfredol."
      doc
        .getElementById("valuation-tab-li5")
        .text() shouldBe "effeithiodd benderfyniad llys ar werth ardrethol yr eiddo hwn a chyn 1 Hydref 2023, rydych yn anfon achos Gwirio o’r prisiad cyfredol."
      doc
        .getElementById("valuation-tab-p4")
        .text() shouldBe "Os yw’r asesiad wedi’i ddileu ar gyfer y naill enghraifft neu’r llall, gallwch anfon achos gwirio o’r prisiad byw mwyaf diweddar."
    }
  }

  private def getDvrFilesPage(language: Language): WSResponse = {

    getRequestStubs

    await(
      ws.url(
          s"http://localhost:$port/business-rates-property-linking/my-organisation/property-link/$submissionId/valuations/$valuationId"
        )
        .withCookies(languageCookie(language), getSessionCookie(testSessionId))
        .withFollowRedirects(follow = false)
        .withHttpHeaders(
          HeaderNames.COOKIE -> "sessionId",
          "Csrf-Token"       -> "nocheck",
          "Content-Type"     -> "application/x-www-form-urlencoded"
        )
        .get()
    )
  }

  def getRequestStubs: StubMapping = {

    authStubs

    stubFor {
      get(s"/business-rates-challenge/my-organisations/challenge-cases?submissionId=$submissionId")
        .willReturn {
          aResponse.withStatus(OK).withBody(Json.toJson(testChallengeCasesWithClient).toString())
        }
    }

    stubFor {
      get(s"/property-linking/dashboard/owner/assessments/$submissionId")
        .willReturn {
          aResponse.withStatus(OK).withBody(Json.toJson(testApiAssessmentsNoCheck).toString())
        }
    }

    stubFor {
      get(s"/property-linking/properties/$uarn/valuation/$valuationId/files?propertyLinkId=$submissionId")
        .willReturn {
          aResponse.withStatus(OK).withBody(Json.toJson(someDvrDocumentFiles).toString())
        }
    }

    stubFor {
      get(s"/property-linking/check-cases/$submissionId/client")
        .willReturn {
          aResponse.withStatus(OK).withBody(Json.toJson(testCheckCasesWithClient).toString())
        }
    }

    stubFor {
      get(s"/business-rates-challenge/my-organisations/challenge-cases?submissionId=$submissionId")
        .willReturn {
          aResponse.withStatus(OK).withBody(Json.toJson(testChallengeCasesWithClient).toString())
        }
    }
  }
}
