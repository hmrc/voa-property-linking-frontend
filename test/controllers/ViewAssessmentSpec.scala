/*
 * Copyright 2018 HM Revenue & Customs
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

import actions.BasicAuthenticatedRequest
import config.ApplicationConfig
import connectors.{CheckCaseConnector, IdentityVerification, _}
import models._
import org.jsoup.Jsoup
import org.mockito.ArgumentMatchers.{any, eq => matching}
import org.mockito.Mockito.when
import org.scalacheck.Arbitrary.arbitrary
import org.scalatest.OptionValues
import play.api.test.FakeRequest
import play.api.test.Helpers._
import resources._
import uk.gov.hmrc.domain.Nino
import utils.{StubBusinessRatesValuation, _}

import scala.collection.JavaConverters._
import scala.concurrent.Future
import uk.gov.hmrc.http.HeaderCarrier

class ViewAssessmentSpec extends VoaPropertyLinkingSpec with OptionValues with TestCheckCasesData{

  override val additionalAppConfig = Seq("featureFlags.checkCasesEnabled" -> "true")


  val mockCheckCaseConnector = mock[CheckCaseConnector]
  private object TestAssessmentController extends Assessments( StubPropertyLinkConnector,
    StubAuthentication,
    mockSubmissionIds,
    mockDvrCaseManagement,
    StubBusinessRatesValuation,
    mockCheckCaseConnector,
    StubBusinessRatesAuthorisation)

  lazy val mockDvrCaseManagement = {
    val m = mock[DVRCaseManagementConnector]
    when(m.requestDetailedValuation(any[DetailedValuationRequest])(any[HeaderCarrier])).thenReturn(Future.successful(()))
    m
  }

  lazy val mockSubmissionIds = {
    val m = mock[SubmissionIdConnector]
    when(m.get(matching("EMAIL"))(any[HeaderCarrier])).thenReturn(Future.successful("EMAIL123"))
    when(m.get(matching("POST"))(any[HeaderCarrier])).thenReturn(Future.successful("POST123"))
    m
  }

  "The assessments page for a property link" must "display the effective assessment date, the rateable value, capacity, and link dates for each assessment, agent check cases" in {
    val organisation = arbitrary[GroupAccount].sample.get
    val person = arbitrary[DetailedIndividualAccount].sample.get
    val link = arbitrary[PropertyLink].sample.get.copy().copy(pending = false)

    StubAuthentication.stubAuthenticationResult(Authenticated(Accounts(organisation, person)))
    StubPropertyLinkConnector.stubLink(link)

    when(mockCheckCaseConnector.getCheckCases(any[Option[PropertyLink]], any[Boolean])(any[BasicAuthenticatedRequest[_]], any[HeaderCarrier])).thenReturn(Future.successful(Some(agentCheckCasesResponse)))

    val res = TestAssessmentController.assessments(link.authorisationId)(FakeRequest())
    status(res) mustBe OK

    val html = Jsoup.parse(contentAsString(res))

    val assessmentTable = html.getElementById("viewAssessmentRadioGroup").select("tr").asScala.tail.map(_.select("td"))

    assessmentTable.map(_.first().text) must contain theSameElementsAs link.assessments.map(a => Formatters.formatDate(a.effectiveDate))
    assessmentTable.map(_.get(1).text) must contain theSameElementsAs link.assessments.map(a => "£" + a.rateableValue.getOrElse("N/A"))
    assessmentTable.map(_.get(2).text) must contain theSameElementsAs link.assessments.map(formatCapacity)
    assessmentTable.map(_.get(3).text) must contain theSameElementsAs link.assessments.map(a => Formatters.formatDate(a.capacity.fromDate))
    assessmentTable.map(_.get(4).text) must contain theSameElementsAs link.assessments.map(a => a.capacity.toDate.map(Formatters.formatDate).getOrElse("Present"))


    val checkCasesTable = html.getElementById("checkcases-table").select("tr").asScala.tail.map(_.select("td"))

    checkCasesTable.map(_.get(0).text.trim).head mustBe  Formatters.formatDateTimeToDate(agentCheckCase.createdDateTime)
    checkCasesTable.map(_.get(1).text.trim).head mustBe  agentCheckCase.checkCaseStatus
    checkCasesTable.map(_.get(3).text.trim).head mustBe  Formatters.formatDate(agentCheckCase.settledDate.get)
  }

  "The assessments page for a property link" must "display the effective assessment date, the rateable value, capacity, and link dates for each assessment, Owner Check cases" in {
    val organisation = arbitrary[GroupAccount].sample.get
    val person = arbitrary[DetailedIndividualAccount].sample.get
    val link = arbitrary[PropertyLink].sample.get.copy().copy(pending = false)

    StubAuthentication.stubAuthenticationResult(Authenticated(Accounts(organisation, person)))
    StubPropertyLinkConnector.stubLink(link)

    when(mockCheckCaseConnector.getCheckCases(any[Option[PropertyLink]], any[Boolean])(any[BasicAuthenticatedRequest[_]], any[HeaderCarrier])).thenReturn(Future.successful(Some(ownerCheckCasesResponse)))

    val res = TestAssessmentController.assessments(link.authorisationId)(FakeRequest())
    status(res) mustBe OK

    val html = Jsoup.parse(contentAsString(res))

    val assessmentTable = html.getElementById("viewAssessmentRadioGroup").select("tr").asScala.tail.map(_.select("td"))

    assessmentTable.map(_.first().text) must contain theSameElementsAs link.assessments.map(a => Formatters.formatDate(a.effectiveDate))
    assessmentTable.map(_.get(1).text) must contain theSameElementsAs link.assessments.map(a => "£" + a.rateableValue.getOrElse("N/A"))
    assessmentTable.map(_.get(2).text) must contain theSameElementsAs link.assessments.map(formatCapacity)
    assessmentTable.map(_.get(3).text) must contain theSameElementsAs link.assessments.map(a => Formatters.formatDate(a.capacity.fromDate))
    assessmentTable.map(_.get(4).text) must contain theSameElementsAs link.assessments.map(a => a.capacity.toDate.map(Formatters.formatDate).getOrElse("Present"))


    val ownerCheckCasesTable = html.getElementById("checkcases-table").select("tr").asScala.tail.map(_.select("td"))

    ownerCheckCasesTable.map(_.get(0).text.trim).head mustBe  Formatters.formatDateTimeToDate(ownerCheckCase.createdDateTime)
    ownerCheckCasesTable.map(_.get(1).text.trim).head mustBe  ownerCheckCase.checkCaseStatus
    ownerCheckCasesTable.map(_.get(3).text.trim).head mustBe  Formatters.formatDate(ownerCheckCase.settledDate.get)
  }

  private def formatCapacity(assessment: Assessment) = assessment.capacity.capacity match {
    case Owner => "Owner"
    case Occupier => "Occupier"
    case OwnerOccupier => "Owner and occupier"
  }

  it must "show N/A if the assessment does not have a rateable value" in {
    val organisation = arbitrary[GroupAccount].sample.get
    val person = arbitrary[DetailedIndividualAccount].sample.get
    val assessment = arbitrary[Assessment].copy(rateableValue = None)
    val link = arbitrary[PropertyLink].sample.get.copy(organisationId = organisation.id, assessments = Seq(assessment))

    StubAuthentication.stubAuthenticationResult(Authenticated(Accounts(organisation, person)))
    StubPropertyLinkConnector.stubLink(link)

    val res = TestAssessmentController.assessments(link.authorisationId)(FakeRequest())
    status(res) mustBe OK

    val html = Jsoup.parse(contentAsString(res))
    val assessmentTable = html.getElementById("viewAssessmentRadioGroup").select("tr").asScala.tail.map(_.select("td"))

    assessmentTable.map(_.get(1).text).head must startWith ("N/A")
  }

  "Viewing a detailed valuation" must "redirect to business rates valuation if the property is bulk" in {
    val organisation = arbitrary[GroupAccount].sample.get
    val person = arbitrary[DetailedIndividualAccount].sample.get
    val link = arbitrary[PropertyLink].sample.get.copy(organisationId = organisation.id, pending = true)

    StubAuthentication.stubAuthenticationResult(Authenticated(Accounts(organisation, person)))
    StubPropertyLinkConnector.stubLink(link)
    StubBusinessRatesValuation.stubValuation(link.assessments.head.assessmentRef, true)

    val res = TestAssessmentController.viewDetailedAssessment(
      link.assessments.head.authorisationId,
      link.assessments.head.assessmentRef,
      link.assessments.head.billingAuthorityReference)(FakeRequest())
    status(res) mustBe SEE_OTHER

    redirectLocation(res).value must endWith (s"/business-rates-valuation/property-link/${link.assessments.head.authorisationId}/assessment/${link.assessments.head.assessmentRef}")
  }

  it must "redirect to the request detailed valuation page if the property is non-bulk" in {
    val organisation = arbitrary[GroupAccount].sample.get
    val person = arbitrary[DetailedIndividualAccount].sample.get
    val link = arbitrary[PropertyLink].sample.get.copy(organisationId = organisation.id, pending = true)

    StubAuthentication.stubAuthenticationResult(Authenticated(Accounts(organisation, person)))
    StubPropertyLinkConnector.stubLink(link)
    StubBusinessRatesValuation.stubValuation(link.assessments.head.assessmentRef, false)

    val res = TestAssessmentController.viewDetailedAssessment(
      link.assessments.head.authorisationId,
      link.assessments.head.assessmentRef,
      link.assessments.head.billingAuthorityReference)(FakeRequest())
    status(res) mustBe SEE_OTHER

    redirectLocation(res).value mustBe routes.Assessments.requestDetailedValuation(
      link.assessments.head.authorisationId,
      link.assessments.head.assessmentRef,
      link.assessments.head.billingAuthorityReference).url
  }

  it must "go to the detailed valuation that is selected from the list of assessments" in {
    val organisation = arbitrary[GroupAccount].sample.get
    val person = arbitrary[DetailedIndividualAccount].sample.get
    val assessment = arbitrary[Assessment].sample.get
    val link = arbitrary[PropertyLink].sample.get.copy(organisationId = organisation.id, authorisationId = 12345, pending = false, assessments = Seq(assessment, assessment.copy(authorisationId = 12345, assessmentRef = 123456, billingAuthorityReference = "ABC123")))

    StubAuthentication.stubAuthenticationResult(Authenticated(Accounts(organisation, person)))
    StubPropertyLinkConnector.stubLink(link)

    val validFormData: Seq[(String, String)] = Seq(
      "viewAssessmentRadio" -> s"${""}-${link.assessments.tail.head.assessmentRef.toString}-${link.assessments.tail.head.billingAuthorityReference}"
    )

    val res = TestAssessmentController.submitViewAssessment(link.authorisationId)(FakeRequest().withFormUrlEncodedBody(validFormData:_*))

    status(res) mustBe SEE_OTHER

    redirectLocation(res).value mustBe routes.Assessments.viewDetailedAssessment(
      link.assessments.tail.head.authorisationId,
      link.assessments.tail.head.assessmentRef,
      link.assessments.tail.head.billingAuthorityReference).url
  }

  it must "go to the summary valuation that is selected from the list of assessments if the link is pending" in {
    val organisation = arbitrary[GroupAccount].sample.get
    val person = arbitrary[DetailedIndividualAccount].sample.get
    val assessment = arbitrary[Assessment].sample.get
    val link = arbitrary[PropertyLink].sample.get.copy(organisationId = organisation.id, authorisationId = 12345, pending = true, assessments = Seq(assessment, assessment.copy(authorisationId = 12345, assessmentRef = 123456, billingAuthorityReference = "ABC123")))

    StubAuthentication.stubAuthenticationResult(Authenticated(Accounts(organisation, person)))
    StubPropertyLinkConnector.stubLink(link)

    val validFormData: Seq[(String, String)] = Seq(
      "viewAssessmentRadio" -> s"${"123456"}-${link.assessments.tail.head.assessmentRef.toString}-${link.assessments.tail.head.billingAuthorityReference}"
    )

    val res = TestAssessmentController.submitViewAssessment(link.authorisationId)(FakeRequest().withFormUrlEncodedBody(validFormData:_*))

    status(res) mustBe SEE_OTHER

    redirectLocation(res).value mustBe routes.Assessments.viewSummary(123456).url
  }

}
