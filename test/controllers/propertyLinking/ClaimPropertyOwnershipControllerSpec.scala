/*
 * Copyright 2022 HM Revenue & Customs
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

package controllers.propertyLinking

import connectors.propertyLinking.PropertyLinkConnector
import controllers.VoaPropertyLinkingSpec
import models._
import org.jsoup.Jsoup
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.Gen
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks
import play.api.test.FakeRequest
import play.api.test.Helpers._
import repositories.SessionRepo
import uk.gov.hmrc.http.HeaderCarrier
import utils._

import java.time.LocalDate
import scala.concurrent.Future

class ClaimPropertyOwnershipControllerSpec extends VoaPropertyLinkingSpec with ScalaCheckDrivenPropertyChecks {

  implicit val hc = HeaderCarrier()
  lazy val withLinkingSession = new StubWithLinkingSession(mockSessionRepo)

  private def testClaimProperty(
        earliestStartDate: LocalDate = earliestEnglishStartDate,
        propertyOccupancy: Option[PropertyOccupancy] = Some(
          PropertyOccupancy(stillOccupied = false, lastOccupiedDate = None)),
        relationshipCapacity: CapacityType = Owner,
        fromCya: Boolean = false,
        userIsAgent: Boolean = true) =
    new ClaimPropertyOwnershipController(
      errorHandler = mockCustomErrorHandler,
      sessionRepository = mockSessionRepo,
      authenticatedAction = preAuthenticatedActionBuilders(),
      withLinkingSession = preEnrichedActionRefiner(
        UploadEvidenceData(NoEvidenceFlag, None),
        earliestStartDate = earliestStartDate,
        propertyOccupancy = propertyOccupancy,
        relationshipCapacity = relationshipCapacity,
        fromCya = Some(fromCya),
        userIsAgent = userIsAgent
      ),
      ownershipToPropertyView = ownershipToPropertyView
    )

  lazy val submissionId: String = shortString
  override val testAccounts: Accounts = arbitrary[Accounts]
  lazy val anEnvelopeId = java.util.UUID.randomUUID().toString

  lazy val mockSessionRepo = {
    val f = mock[SessionRepo]
    when(f.start(any())(any(), any())).thenReturn(Future.successful(()))
    f
  }

  lazy val propertyLinkingConnector = mock[PropertyLinkConnector]

  "The claim ownership page with earliest start date in the past" should "return valid page" in {
    StubSubmissionIdConnector.stubId(submissionId)

    val res = testClaimProperty().showOwnership()(FakeRequest())
    status(res) shouldBe OK

    val html = HtmlPage(res)
    html.titleShouldMatch(
      "Error: When your client became the owner or occupier of the property - Valuation Office Agency - GOV.UK")
    html.shouldContainText("There is a problem")

  }

  "The claim ownership page with earliest start date in the future" should "return redirect to choose evidence page" in {
    StubSubmissionIdConnector.stubId(submissionId)
    when(mockSessionRepo.saveOrUpdate(any())(any(), any()))
      .thenReturn(Future.successful(()))

    val res = testClaimProperty(earliestStartDate = LocalDate.now().plusYears(1)).showOwnership()(FakeRequest())
    status(res) shouldBe SEE_OTHER
  }

  "The claim ownership page" should "validate that the start date of the ownership is not after the end date" in {
    val dateGen: Gen[(LocalDate, LocalDate)] = for {
      startDate <- Gen.choose(earliestEnglishStartDate, LocalDate.now.minusDays(1))
      endDate   <- Gen.choose(earliestEnglishStartDate, LocalDate.now.minusDays(1))
      if endDate.isBefore(startDate)
    } yield (startDate, endDate)

    forAll(dateGen, arbitrary[Boolean], arbitrary[CapacityType]) { (d, isAgent, cap) =>
      {
        val result = testClaimProperty(
          propertyOccupancy = Some(PropertyOccupancy(stillOccupied = false, lastOccupiedDate = Some(d._2))),
          relationshipCapacity = cap,
          userIsAgent = isAgent
        ).submitOwnership()(FakeRequest().withFormUrlEncodedBody(
          "interestedOnOrBefore" -> "false",
          "fromDate.day"         -> d._1.getDayOfMonth.toString,
          "fromDate.month"       -> d._1.getMonthValue.toString,
          "fromDate.year"        -> d._1.getYear.toString
        ))
        status(result) shouldBe BAD_REQUEST
        val doc = Jsoup.parse(contentAsString(result))
        val error = doc.getElementsByAttributeValue("href", "#fromDate-day")
        error.text() shouldBe
          s"Date ${if (isAgent) "your client" else "you"} became the " +
            s"${if (cap == Owner) "owner" else if (cap == Occupier) "occupier" else "owner and occupier"} " +
            "of the property must be before " +
            s"${d._2.getDayOfMonth} ${d._2.getMonth.toString.toLowerCase.capitalize} ${d._2.getYear}"
      }
    }
  }

  "The claim ownership page on client behalf" should "return valid page" in {
    StubSubmissionIdConnector.stubId(submissionId)

    val res = testClaimProperty().showOwnership()(FakeRequest())
    status(res) shouldBe OK

    val html = HtmlPage(res)
    html.titleShouldMatch(
      "Error: When your client became the owner or occupier of the property - Valuation Office Agency - GOV.UK")
    html.shouldContainText("There is a problem")
  }

  it should "have a back link to the property relationship page" in {
    val result = testClaimProperty().showOwnership()(FakeRequest())
    val backLink = Jsoup.parse(contentAsString(result)).getElementById("back-link")
    backLink.attr("href") shouldBe routes.ClaimPropertyRelationshipController.back().url
  }

  it should "reject invalid form submissions" in {
    StubSubmissionIdConnector.stubId(submissionId)
    val res = testClaimProperty().submitOwnership()(FakeRequest())
    status(res) shouldBe BAD_REQUEST
  }

  it should "redirect to the choose evidence page on valid submissions" in {
    StubSubmissionIdConnector.stubId(submissionId)
    when(mockSessionRepo.saveOrUpdate(any())(any(), any())).thenReturn(Future.successful(()))

    val res =
      testClaimProperty().submitOwnership()(FakeRequest().withFormUrlEncodedBody("interestedOnOrBefore" -> "true"))

    status(res) shouldBe SEE_OTHER
    redirectLocation(res) shouldBe Some(routes.ClaimPropertyOccupancyController.showOccupancy().url)
  }

  it should "redirect to the CYA page on valid 'On or before X' submissions when coming from CYA" in {
    val result = testClaimProperty(fromCya = true)
      .submitOwnership()(FakeRequest().withFormUrlEncodedBody("interestedOnOrBefore" -> "true"))
    status(result) shouldBe SEE_OTHER
    redirectLocation(result) shouldBe Some(routes.DeclarationController.show().url)
  }

  it should "redirect to the CYA page on valid 'After X' submissions when coming from CYA" in {
    val result = testClaimProperty(fromCya = true)
      .submitOwnership()(
        FakeRequest().withFormUrlEncodedBody(
          "interestedOnOrBefore" -> "false",
          "fromDate.day"         -> "23",
          "fromDate.month"       -> "4",
          "fromDate.year"        -> "2017"
        ))
    status(result) shouldBe SEE_OTHER
    redirectLocation(result) shouldBe Some(routes.DeclarationController.show().url)
  }

  it should "validate that the start date of the ownership is not after the end date of the ownership, if already set" in {
    val result = testClaimProperty(
      propertyOccupancy =
        Some(PropertyOccupancy(stillOccupied = false, lastOccupiedDate = Some(LocalDate.of(2017, 4, 22)))),
    ).submitOwnership()(
      FakeRequest().withFormUrlEncodedBody(
        "interestedOnOrBefore" -> "false",
        "fromDate.day"         -> "23",
        "fromDate.month"       -> "4",
        "fromDate.year"        -> "2017"
      ))
    status(result) shouldBe BAD_REQUEST
    val doc = Jsoup.parse(contentAsString(result))
    val error = doc.getElementsByAttributeValue("href", "#fromDate-day")
    error.text() shouldBe "Date your client became the owner of the property must be before 22 April 2017"
  }

  it should "have a back link to the CYA page when coming from CYA" in {
    val result = testClaimProperty(fromCya = true).showOwnership()(FakeRequest())
    val document = Jsoup.parse(contentAsString(result))
    document.getElementById("back-link").attr("href") shouldBe routes.DeclarationController.show().url
  }

  it should "still have a back link to the CYA page when coming from CYA on failed form validation" in {
    val result = testClaimProperty(fromCya = true).submitOwnership()(FakeRequest())
    status(result) shouldBe BAD_REQUEST
    val document = Jsoup.parse(contentAsString(result))
    document.getElementById("back-link").attr("href") shouldBe routes.DeclarationController.show().url
  }
}
