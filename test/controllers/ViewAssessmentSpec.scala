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
import play.api.test.FakeRequest
import play.api.test.Helpers._
import resources._
import utils._

import scala.collection.JavaConverters._

class ViewAssessmentSpec extends ControllerSpec {

  private object TestDashboardController extends Dashboard {
    override val propertyLinks: PropertyLinkConnector = StubPropertyLinkConnector
    override val reprConnector: PropertyRepresentationConnector = StubPropertyRepresentationConnector
    override val individuals: IndividualAccounts = StubIndividualAccountConnector
    override val groups: GroupAccounts = StubGroupAccountConnector
    override val auth: VPLAuthConnector = StubAuthConnector
    override val authenticated: AuthenticatedAction = StubAuthentication
  }

  "The assessments page for a property link" must "display the effective assessment date, the rateable value, capacity, and link dates for each assessment" in {
    val organisationId = arbitrary[Int].sample.get
    val personId = arbitrary[Int].sample.get
    val link = arbitrary[PropertyLink].sample.get.copy(organisationId = organisationId)

    StubAuthentication.stubAuthenticationResult(Authenticated(AccountIds(organisationId, personId)))
    StubPropertyLinkConnector.stubLink(link)

    val res = TestDashboardController.assessments(link.authorisationId, link.pending)(FakeRequest())
    status(res) mustBe OK

    val html = Jsoup.parse(contentAsString(res))
    val assessmentTable = html.select("tr").asScala.tail.map(_.select("td"))

    assessmentTable.map(_.first().text) must contain theSameElementsAs link.assessment.map(a => Formatters.formatDate(a.effectiveDate))
    assessmentTable.map(_.get(1).text) must contain theSameElementsAs link.assessment.map(a => "£" + a.rateableValue)
    assessmentTable.map(_.get(2).text) must contain theSameElementsAs link.assessment.map(formatCapacity)
    assessmentTable.map(_.get(3).text) must contain theSameElementsAs link.assessment.map(a => Formatters.formatDate(a.capacity.fromDate))
    assessmentTable.map(_.get(4).text) must contain theSameElementsAs link.assessment.map(a => a.capacity.toDate.map(Formatters.formatDate).getOrElse("Present"))
  }

  private def formatCapacity(assessment: Assessment) = assessment.capacity.capacity match {
    case Owner => "Owner"
    case Occupier => "Occupier"
    case OwnerOccupier => "Owner and occupier"
  }

  it must "show a link to the detailed valuation for each assessment if the property link is approved" in {
    val organisationId = arbitrary[Int].sample.get
    val personId = arbitrary[Int].sample.get
    val link = arbitrary[PropertyLink].sample.get.copy(organisationId = organisationId, pending = false)

    StubAuthentication.stubAuthenticationResult(Authenticated(AccountIds(organisationId, personId)))
    StubPropertyLinkConnector.stubLink(link)

    val res = TestDashboardController.assessments(link.authorisationId, link.pending)(FakeRequest())
    status(res) mustBe OK

    val html = Jsoup.parse(contentAsString(res))
    val assessmentLinks = html.select("td.last").asScala.map(_.select("a").attr("href"))

    assessmentLinks must contain theSameElementsAs link.assessment.map(a => controllers.routes.Dashboard.viewDetailedAssessment(a.authorisationId, a.assessmentRef).url)
  }

  it must "show a link to the summary valuation for each assessment if the property link is pending" in {
    val organisationId = arbitrary[Int].sample.get
    val personId = arbitrary[Int].sample.get
    val link = arbitrary[PropertyLink].sample.get.copy(organisationId = organisationId, pending = true)

    StubAuthentication.stubAuthenticationResult(Authenticated(AccountIds(organisationId, personId)))
    StubPropertyLinkConnector.stubLink(link)

    val res = TestDashboardController.assessments(link.authorisationId, link.pending)(FakeRequest())
    status(res) mustBe OK

    val html = Jsoup.parse(contentAsString(res))
    val assessmentLinks = html.select("td.last").asScala.map(_.select("a").attr("href"))

    assessmentLinks must contain theSameElementsAs link.assessment.map(a => controllers.routes.Dashboard.viewSummary(a.uarn).url)
  }
}
