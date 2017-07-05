/*
 * Copyright 2017 HM Revenue & Customs
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

import actions.AuthenticatedAction
import connectors._
import connectors.propertyLinking.PropertyLinkConnector
import models._
import org.jsoup.Jsoup
import org.scalacheck.Arbitrary.arbitrary
import org.scalatest.OptionValues
import play.api.test.FakeRequest
import play.api.test.Helpers._
import resources._
import utils._

import scala.collection.JavaConverters._

class ViewAssessmentSpec extends ControllerSpec with OptionValues {

  private object TestAssessmentController extends Assessments {
    override val propertyLinks: PropertyLinkConnector = StubPropertyLinkConnector
    override val reprConnector: PropertyRepresentationConnector = StubPropertyRepresentationConnector
    override val individuals: IndividualAccounts = StubIndividualAccountConnector
    override val groups: GroupAccounts = StubGroupAccountConnector
    override val auth: VPLAuthConnector = StubAuthConnector
    override val authenticated: AuthenticatedAction = StubAuthentication
    override val businessRatesValuations = StubBusinessRatesValuation
  }

  "The assessments page for a property link" must "display the effective assessment date, the rateable value, capacity, and link dates for each assessment" in {
    val organisation = arbitrary[GroupAccount].sample.get
    val person = arbitrary[DetailedIndividualAccount].sample.get
    val link = arbitrary[PropertyLink].sample.get.copy(organisationId = organisation.id)

    StubAuthentication.stubAuthenticationResult(Authenticated(Accounts(organisation, person)))
    StubPropertyLinkConnector.stubLink(link)

    val res = TestAssessmentController.assessments(link.authorisationId)(FakeRequest())
    status(res) mustBe OK

    val html = Jsoup.parse(contentAsString(res))
    val assessmentTable = html.select("tr").asScala.tail.map(_.select("td"))

    assessmentTable.map(_.first().text) must contain theSameElementsAs link.assessments.map(a => Formatters.formatDate(a.effectiveDate))
    assessmentTable.map(_.get(1).text) must contain theSameElementsAs link.assessments.map(a => "£" + a.rateableValue.getOrElse("N/A"))
    assessmentTable.map(_.get(2).text) must contain theSameElementsAs link.assessments.map(formatCapacity)
    assessmentTable.map(_.get(3).text) must contain theSameElementsAs link.assessments.map(a => Formatters.formatDate(a.capacity.fromDate))
    assessmentTable.map(_.get(4).text) must contain theSameElementsAs link.assessments.map(a => a.capacity.toDate.map(Formatters.formatDate).getOrElse("Present"))
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
    val assessmentTable = html.select("tr").asScala.tail.map(_.select("td"))

    assessmentTable.map(_.get(1).text).head must startWith ("N/A")
  }

  it must "show a link to the detailed valuation for each assessment if the property link is approved" in {
    val organisation = arbitrary[GroupAccount].sample.get
    val person = arbitrary[DetailedIndividualAccount].sample.get
    val assessment = arbitrary[Assessment].sample.get
    val link = arbitrary[PropertyLink].sample.get.copy(organisationId = organisation.id, pending = false, assessments = Seq(assessment))

    StubAuthentication.stubAuthenticationResult(Authenticated(Accounts(organisation, person)))
    StubPropertyLinkConnector.stubLink(link)

    val res = TestAssessmentController.assessments(link.authorisationId)(FakeRequest())
    status(res) mustBe OK

    val html = Jsoup.parse(contentAsString(res))
    val assessmentLinks = html.select("td:eq(5)").asScala.map(_.select("a").attr("href"))

    assessmentLinks must contain theSameElementsAs link.assessments.map(a => controllers.routes.Assessments.viewDetailedAssessment(a.authorisationId, a.assessmentRef, a.billingAuthorityReference).url)
  }

  it must "show a link to the summary valuation for each assessment if the property link is pending" in {
    val organisation = arbitrary[GroupAccount].sample.get
    val person = arbitrary[DetailedIndividualAccount].sample.get
    val link = arbitrary[PropertyLink].sample.get.copy(organisationId = organisation.id, pending = true)

    StubAuthentication.stubAuthenticationResult(Authenticated(Accounts(organisation, person)))
    StubPropertyLinkConnector.stubLink(link)

    val res = TestAssessmentController.assessments(link.authorisationId)(FakeRequest())
    status(res) mustBe OK

    val html = Jsoup.parse(contentAsString(res))
    val assessmentLinks = html.select("td:eq(5)").asScala.map(_.select("a").attr("href"))

    assessmentLinks must contain theSameElementsAs link.assessments.map(a => controllers.routes.Assessments.viewSummary(a.uarn).url)
  }

  "Viewing a detailed valuation" must "redirect to business rates valuation if the property is bulk" in {
    val organisation = arbitrary[GroupAccount].sample.get
    val person = arbitrary[DetailedIndividualAccount].sample.get
    val link = arbitrary[PropertyLink].sample.get.copy(organisationId = organisation.id, pending = true)

    StubAuthentication.stubAuthenticationResult(Authenticated(Accounts(organisation, person)))
    StubPropertyLinkConnector.stubLink(link)
    StubBusinessRatesValuation.stubValuation(link.assessments.head.assessmentRef, true)

    val res = TestAssessmentController.viewDetailedAssessment(link.assessments.head.authorisationId, link.assessments.head.assessmentRef, link.assessments.head.billingAuthorityReference)(FakeRequest())
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

    val res = TestAssessmentController.viewDetailedAssessment(link.assessments.head.authorisationId, link.assessments.head.assessmentRef, link.assessments.head.billingAuthorityReference)(FakeRequest())
    status(res) mustBe SEE_OTHER

    redirectLocation(res).value mustBe routes.Assessments.requestDetailedValuation(link.assessments.head.authorisationId, link.assessments.head.assessmentRef, link.assessments.head.billingAuthorityReference).url
  }
}
