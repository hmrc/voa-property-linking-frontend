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

class ClaimPropertyOccupancyControllerSpec extends VoaPropertyLinkingSpec with ScalaCheckDrivenPropertyChecks {

  implicit val hc = HeaderCarrier()
  lazy val withLinkingSession = new StubWithLinkingSession(mockSessionRepo)

  private def testClaimPropertyOccupancyController(
        earliestStartDate: LocalDate = earliestEnglishStartDate,
        propertyOwnership: PropertyOwnership =
          PropertyOwnership(interestedOnOrBefore = true, fromDate = Some(LocalDate.of(2017, 1, 1))),
        relationshipCapacity: CapacityType = Owner,
        userIsAgent: Boolean = true,
        fromCya: Boolean = false) =
    new ClaimPropertyOccupancyController(
      errorHandler = mockCustomErrorHandler,
      sessionRepository = mockSessionRepo,
      authenticatedAction = preAuthenticatedActionBuilders(),
      withLinkingSession = preEnrichedActionRefiner(
        evidenceData = UploadEvidenceData(NoEvidenceFlag, None),
        earliestStartDate = earliestStartDate,
        propertyOwnership = Some(propertyOwnership),
        relationshipCapacity = relationshipCapacity,
        fromCya = Some(fromCya),
        userIsAgent = userIsAgent
      ),
      occupancyOfPropertyView = occupancyOfPropertyPage
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

  "The claim occupancy page with earliest start date in the past" should "return valid page" in {
    StubSubmissionIdConnector.stubId(submissionId)

    val res = testClaimPropertyOccupancyController().showOccupancy()(FakeRequest())
    status(res) shouldBe OK

    val html = HtmlPage(res)
    html.verifyElementText("page-header", "Does your client still own or occupy the property?")
    html.verifyElementText("caption", "Add a property")
    html.mustContainRadioSelect("stillOccupied", Seq("true", "false"))
    html.shouldContainDateSelect("lastOccupiedDate")
  }

  "The claim occupancy page with earliest start date in the future" should "return redirect to choose evidence page" in {
    StubSubmissionIdConnector.stubId(submissionId)
    when(mockSessionRepo.saveOrUpdate(any())(any(), any()))
      .thenReturn(Future.successful(()))

    val res = testClaimPropertyOccupancyController(LocalDate.now.plusYears(1)).showOccupancy()(FakeRequest())
    status(res) shouldBe SEE_OTHER
  }

  "The claim occupancy page" should "validate that the end date of ownership is not before the start date" in {
    val dateGen: Gen[(LocalDate, LocalDate)] = for {
      startDate <- Gen.choose(earliestEnglishStartDate, LocalDate.now.minusDays(1))
      endDate   <- Gen.choose(earliestEnglishStartDate, LocalDate.now.minusDays(1))
      if endDate.isBefore(startDate)
    } yield (startDate, endDate)

    forAll(dateGen, arbitrary[Boolean], arbitrary[CapacityType]) { (d, isAgent, cap) =>
      {
        val result = testClaimPropertyOccupancyController(
          propertyOwnership = PropertyOwnership(interestedOnOrBefore = false, fromDate = Some(d._1)),
          relationshipCapacity = cap,
          userIsAgent = isAgent
        ).submitOccupancy()(FakeRequest().withFormUrlEncodedBody(
          "stillOccupied"          -> "false",
          "lastOccupiedDate.day"   -> d._2.getDayOfMonth.toString,
          "lastOccupiedDate.month" -> d._2.getMonthValue.toString,
          "lastOccupiedDate.year"  -> d._2.getYear.toString
        ))
        status(result) shouldBe BAD_REQUEST
        val doc = Jsoup.parse(contentAsString(result))
        val error = doc.getElementsByAttributeValue("href", "#lastOccupiedDate-day")
        error.text() shouldBe
          s"Date of ${if (isAgent) "your client's " else ""}last day as " +
            s"${if (cap == Owner) "owner" else if (cap == Occupier) "occupier" else "owner and occupier"} " +
            "of the property must be after " +
            s"${d._1.getDayOfMonth} ${d._1.getMonth.toString.toLowerCase.capitalize} ${d._1.getYear}"
      }
    }
  }

  "The claim occupancy page on client behalf" should "return valid page" in {
    StubSubmissionIdConnector.stubId(submissionId)

    val res = testClaimPropertyOccupancyController().showOccupancy()(FakeRequest())
    status(res) shouldBe OK

    val html = HtmlPage(res)
    html.verifyElementText("page-header", "Does your client still own or occupy the property?")
    html.verifyElementText("caption", "Add a property")
    html.mustContainRadioSelect("stillOccupied", Seq("true", "false"))
    html.shouldContainDateSelect("lastOccupiedDate")
  }

  it should "reject invalid form submissions" in {
    StubSubmissionIdConnector.stubId(submissionId)

    val res = testClaimPropertyOccupancyController().submitOccupancy()(FakeRequest())
    status(res) shouldBe BAD_REQUEST
  }

  it should "redirect to the choose evidence page on valid submissions" in {
    StubSubmissionIdConnector.stubId(submissionId)
    when(mockSessionRepo.saveOrUpdate(any())(any(), any()))
      .thenReturn(Future.successful(()))

    val res = testClaimPropertyOccupancyController().submitOccupancy()(
      FakeRequest().withFormUrlEncodedBody(
        "stillOccupied"          -> "false",
        "lastOccupiedDate.day"   -> "23",
        "lastOccupiedDate.month" -> "4",
        "lastOccupiedDate.year"  -> "2017"
      ))

    status(res) shouldBe SEE_OTHER
    redirectLocation(res) shouldBe Some(routes.ChooseEvidenceController.show().url)
  }

  it should "have a back link to the property ownership page" in {
    val result = testClaimPropertyOccupancyController().showOccupancy()(FakeRequest())
    val document = Jsoup.parse(contentAsString(result))
    val backLink = document.getElementById("back-link")
    backLink.attr("href") shouldBe routes.ClaimPropertyOwnershipController.showOwnership().url
  }

  it should "redirect to the CYA page on 'yes' submissions when coming from CYA" in {
    val result = testClaimPropertyOccupancyController(fromCya = true)
      .submitOccupancy()(FakeRequest().withFormUrlEncodedBody("stillOccupied" -> "true"))
    status(result) shouldBe SEE_OTHER
    redirectLocation(result) shouldBe Some(routes.DeclarationController.show().url)
  }

  it should "redirect to the CYA page on 'no' submissions when coming from CYA" in {
    val result = testClaimPropertyOccupancyController(fromCya = true)
      .submitOccupancy()(
        FakeRequest().withFormUrlEncodedBody(
          "stillOccupied"          -> "false",
          "lastOccupiedDate.day"   -> "23",
          "lastOccupiedDate.month" -> "4",
          "lastOccupiedDate.year"  -> "2017"
        ))
    status(result) shouldBe SEE_OTHER
    redirectLocation(result) shouldBe Some(routes.DeclarationController.show().url)
  }

  it should "have a back link to the CYA page when coming from CYA" in {
    val result = testClaimPropertyOccupancyController(fromCya = true).showOccupancy()(FakeRequest())
    val document = Jsoup.parse(contentAsString(result))
    document.getElementById("back-link").attr("href") shouldBe routes.DeclarationController.show().url
  }

  it should "still have a back link to the CYA page when coming from CYA on failed form validation" in {
    val result = testClaimPropertyOccupancyController(fromCya = true).submitOccupancy()(FakeRequest())
    status(result) shouldBe BAD_REQUEST
    val document = Jsoup.parse(contentAsString(result))
    document.getElementById("back-link").attr("href") shouldBe routes.DeclarationController.show().url
  }
}
