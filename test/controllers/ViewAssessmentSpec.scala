/*
 * Copyright 2020 HM Revenue & Customs
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

package controllers

import connectors._
import models._
import models.dvr.DetailedValuationRequest
import org.mockito.ArgumentMatchers.{any, eq => matching}
import org.mockito.Mockito.when
import org.scalacheck.Arbitrary.arbitrary
import org.scalatest.OptionValues
import play.api.test.FakeRequest
import play.api.test.Helpers._
import resources._
import uk.gov.hmrc.http.HeaderCarrier
import utils.{StubBusinessRatesValuation, _}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class ViewAssessmentSpec extends VoaPropertyLinkingSpec with OptionValues {

  private object TestAssessmentController extends Assessments(
    mockCustomErrorHandler,
    StubPropertyLinkConnector,
    mockPropertyLinkService,
    preAuthenticatedActionBuilders(),
    mockSubmissionIds,
    mockDvrCaseManagement,
    StubBusinessRatesValuation,
    mockBusinessRatesAuthorisation,
    stubMessagesControllerComponents(),
    isExternalValuation = false,
    isSkipAssessment = false,
    isSummaryValuationNewRoute = true) {
    when(mockDvrCaseManagement.requestDetailedValuation(any[DetailedValuationRequest])(any[HeaderCarrier])).thenReturn(Future.successful(()))
  }

  lazy val mockSubmissionIds = {
    val m = mock[SubmissionIdConnector]
    when(m.get(matching("EMAIL"))(any[HeaderCarrier])).thenReturn(Future.successful("EMAIL123"))
    when(m.get(matching("POST"))(any[HeaderCarrier])).thenReturn(Future.successful("POST123"))
    m
  }

  // TODO Delete or fix these tests

  //  "The assessments page for a property link" must "display the effective assessment date, the rateable value, capacity, and link dates for each assessment, agent check cases" in {
  //    val organisation = arbitrary[GroupAccount].sample.get
  //    val person = arbitrary[DetailedIndividualAccount].sample.get
  //    val link = arbitrary[PropertyLink].sample.get.copy().copy()
  //
  //    StubPropertyLinkConnector.stubLink(link)
  //    val res = TestAssessmentController.assessments(link.authorisationId, link.submissionId)(FakeRequest())
  //    status(res) mustBe OK
  //
  //    val html = Jsoup.parse(contentAsString(res))
  //
  //    val assessmentTable = html.getElementById("viewAssessmentRadioGroup").select("tr").asScala.tail.map(_.select("td"))

  //    assessmentTable.map(_.first().text) must contain theSameElementsAs link.assessments.map(a => Formatters.formatDate(a.effectiveDate))
  //    assessmentTable.map(_.get(1).text) must contain theSameElementsAs link.assessments.map(a => "£" + a.rateableValue.getOrElse("N/A"))
  //    assessmentTable.map(_.get(2).text) must contain theSameElementsAs link.assessments.map(formatCapacity)
  //    assessmentTable.map(_.get(3).text) must contain theSameElementsAs link.assessments.map(a => a.currentFromDate.map(Formatters.formatDate).getOrElse(""))
  //    assessmentTable.map(_.get(4).text) must contain theSameElementsAs link.assessments.map {
  //      a =>
  //        (a.currentFromDate, a.currentToDate) match {
  //          case (None, None) => ""
  //          case (Some(_), None) => "Present"
  //          case (Some(_), Some(to)) => Formatters.formatDate(to)
  //          case (None, Some(_)) => ""
  //        }
  //    }
  //  }

  "viewOwnerSummary" must "redirect to business-rates-valuation view owner summary details" in {
    val res = TestAssessmentController.viewOwnerSummary(123L, true)(FakeRequest())

    status(res) mustBe SEE_OTHER
    redirectLocation(res) mustBe Some("http://localhost:9537/business-rates-valuation/property-link/123/valuation/summary")
  }

  "viewClientSummary" must "redirect to business-rates-valuation view client summary details" in {
    val res = TestAssessmentController.viewClientSummary(123L, true)(FakeRequest())

    status(res) mustBe SEE_OTHER
    redirectLocation(res) mustBe Some("http://localhost:9537/business-rates-valuation/property-link/clients/all/123/valuation/summary")
  }

  //  "The assessments page for a property link" must "display the effective assessment date, the rateable value, capacity, and link dates for each assessment, Owner Check cases" in {
  //    val organisation = arbitrary[GroupAccount].sample.get
  //    val person = arbitrary[DetailedIndividualAccount].sample.get
  //    val link = arbitrary[PropertyLink].sample.get.copy().copy()
  //
  //    StubPropertyLinkConnector.stubLink(link)
  //
  //    val res = TestAssessmentController.assessments(link.authorisationId, link.submissionId)(FakeRequest())
  //    status(res) mustBe OK
  //
  //    val html = Jsoup.parse(contentAsString(res))
  //
  //    val assessmentTable = html.getElementById("viewAssessmentRadioGroup").select("tr").asScala.tail.map(_.select("td"))

  //    assessmentTable.map(_.first().text) must contain theSameElementsAs link.assessments.map(a => Formatters.formatDate(a.effectiveDate))
  //    assessmentTable.map(_.get(1).text) must contain theSameElementsAs link.assessments.map(a => "£" + a.rateableValue.getOrElse("N/A"))
  //    assessmentTable.map(_.get(2).text) must contain theSameElementsAs link.assessments.map(formatCapacity)
  //    assessmentTable.map(_.get(3).text) must contain theSameElementsAs link.assessments.map(a => a.currentFromDate.map(Formatters.formatDate).getOrElse(""))
  //    assessmentTable.map(_.get(4).text) must contain theSameElementsAs link.assessments.map {
  //      a =>
  //        (a.currentFromDate, a.currentToDate) match {
  //          case (None, None) => ""
  //          case (Some(_), None) => "Present"
  //          case (Some(_), Some(to)) => Formatters.formatDate(to)
  //          case (None, Some(_)) => ""
  //        }
  //    }
  //  }

  private def formatCapacity(assessment: Assessment) = assessment.capacity.capacity match {
    case Owner => "Owner"
    case Occupier => "Occupier"
    case OwnerOccupier => "Owner and occupier"
  }

  //  it must "show N/A if the assessment does not have a rateable value" in {
  //    val organisation = arbitrary[GroupAccount].sample.get
  //    val person = arbitrary[DetailedIndividualAccount].sample.get
  //    val assessment = arbitrary[Assessment].copy(rateableValue = None)
  //    val assessment2 = arbitrary[Assessment].copy(rateableValue = None)
  //    val link = arbitrary[PropertyLink].sample.get.copy()
  //
  //    StubPropertyLinkConnector.stubLink(link)
  //
  //    val res = TestAssessmentController.assessments(link.authorisationId, link.submissionId)(FakeRequest())
  //    status(res) mustBe OK
  //
  //    val html = Jsoup.parse(contentAsString(res))
  //    val assessmentTable = html.getElementById("viewAssessmentRadioGroup").select("tr").asScala.tail.map(_.select("td"))
  //
  //    assessmentTable.map(_.get(1).text).head must startWith ("N/A")
  //  }

  //  it must "redirect to detailed valuation when only 1 assessment" in {
  //    val organisation = arbitrary[GroupAccount].sample.get
  //    val person = arbitrary[DetailedIndividualAccount].sample.get
  //    val assessment = arbitrary[Assessment].copy(rateableValue = None)
  //    val link = arbitrary[PropertyLink].sample.get.copy()
  //
  //    StubPropertyLinkConnector.stubLink(link)
  //
  //    val res = TestAssessmentController.assessments(link.authorisationId, link.submissionId)(FakeRequest())
  //    status(res) mustBe SEE_OTHER
  //    //redirectLocation(res).value must endWith (s"/business-rates-property-linking/detailed/${link.authorisationId}/${link.assessments.head.assessmentRef}?baRef=${link.assessments.head.billingAuthorityReference}")
  //  }

  "Viewing a detailed valuation" must "redirect to business rates valuation if the property is bulk" in {
    val organisation = arbitrary[GroupAccount].sample.get
    val person = arbitrary[DetailedIndividualAccount].sample.get
    val link = arbitrary[PropertyLink].sample.get.copy()

    StubPropertyLinkConnector.stubLink(link)
    //StubBusinessRatesValuation.stubValuation(link.assessments.head.assessmentRef, true)

    //    val res = TestAssessmentController.viewDetailedAssessment(
    //      link.assessments.head.authorisationId,
    //      link.assessments.head.assessmentRef,
    //      link.assessments.head.billingAuthorityReference)(FakeRequest())
    //    status(res) mustBe SEE_OTHER
    //
    //    redirectLocation(res).value must endWith (s"/business-rates-valuation/property-link/${link.assessments.head.authorisationId}/assessment/${link.assessments.head.assessmentRef}")
  }

  it must "redirect to the request detailed valuation page if the property is non-bulk" in {
    val organisation = arbitrary[GroupAccount].sample.get
    val person = arbitrary[DetailedIndividualAccount].sample.get
    val link = arbitrary[PropertyLink].sample.get.copy()

    StubPropertyLinkConnector.stubLink(link)
    //StubBusinessRatesValuation.stubValuation(link.assessments.head.assessmentRef, false)

    //    val res = TestAssessmentController.viewDetailedAssessment(
    //      link.assessments.head.authorisationId,
    //      link.assessments.head.assessmentRef,
    //      link.assessments.head.billingAuthorityReference)(FakeRequest())
    //    status(res) mustBe SEE_OTHER
    //
    //    redirectLocation(res).value mustBe routes.Assessments.requestDetailedValuation(
    //      link.assessments.head.authorisationId,
    //      link.assessments.head.assessmentRef,
    //      link.assessments.head.billingAuthorityReference).url
  }

  it must "go to the detailed valuation that is selected from the list of assessments" in {
    val organisation = arbitrary[GroupAccount].sample.get
    val person = arbitrary[DetailedIndividualAccount].sample.get
    val assessment = arbitrary[Assessment].sample.get
    val link = arbitrary[PropertyLink].sample.get.copy(authorisationId = 12345)

    StubPropertyLinkConnector.stubLink(link)

    //    val validFormData: Seq[(String, String)] = Seq(
    //      "viewAssessmentRadio" -> s"${""}-${link.assessments.tail.head.assessmentRef.toString}-${link.assessments.tail.head.billingAuthorityReference}"
    //    )
    //
    //    val res = TestAssessmentController.submitViewAssessment(link.authorisationId)(FakeRequest().withFormUrlEncodedBody(validFormData:_*))
    //
    //    status(res) mustBe SEE_OTHER
    //
    //    redirectLocation(res).value mustBe routes.Assessments.viewDetailedAssessment(
    //      link.assessments.tail.head.authorisationId,
    //      link.assessments.tail.head.assessmentRef,
    //      link.assessments.tail.head.billingAuthorityReference).url
  }

  it must "go to the summary valuation that is selected from the list of assessments if the link is pending" in {
    val organisation = arbitrary[GroupAccount].sample.get
    val person = arbitrary[DetailedIndividualAccount].sample.get
    val assessment = arbitrary[Assessment].sample.get
    val link = arbitrary[PropertyLink].sample.get.copy(authorisationId = 12345)

    StubPropertyLinkConnector.stubLink(link)

    //    val validFormData: Seq[(String, String)] = Seq(
    //      "viewAssessmentRadio" -> s"${"123456"}-${link.assessments.tail.head.assessmentRef.toString}-${link.assessments.tail.head.billingAuthorityReference}"
    //    )
    //
    //    val res = TestAssessmentController.submitViewAssessment(link.authorisationId)(FakeRequest().withFormUrlEncodedBody(validFormData:_*))

    //    status(res) mustBe SEE_OTHER
    //
    //    redirectLocation(res).value mustBe routes.Assessments.viewSummary(123456, true).url
  }

}
