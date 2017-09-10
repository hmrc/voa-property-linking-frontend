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

package controllers.agent

import config.ApplicationConfig
import connectors.Authenticated
import controllers.ControllerSpec
import models._
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.scalacheck.Arbitrary.arbitrary
import play.api.test.FakeRequest
import utils.{Formatters, StubAuthentication, StubPropertyLinkConnector, StubPropertyRepresentationConnector}
import resources._
import play.api.test.Helpers._

import scala.collection.JavaConverters._

class PendingRequestsSpec extends ControllerSpec {

  "The pending requests page" must "contain the organisation name for each of the agent's pending requests" in {
    val html = defaultHtml

    val organisationNames = StubPropertyRepresentationConnector.stubbedRepresentations(RepresentationPending) map { _.organisationName }
    checkTableColumn(html, 0, "Organisation name", organisationNames)
  }

  it must "contain the address for each pending request" in {
    val html = defaultHtml

    val addresses = StubPropertyRepresentationConnector.stubbedRepresentations(RepresentationPending) map { _.address }
    checkTableColumn(html, 1, "Property address", addresses)
  }

  it must "contain both permissions for each pending request" in {
    val html = defaultHtml

    val permissions = StubPropertyRepresentationConnector.stubbedRepresentations(RepresentationPending) map { r =>
      s"Check: ${displayPermission(r.checkPermission)} Challenge: ${displayPermission(r.challengePermission)}"
    }
    checkTableColumn(html, 2, "Permissions", permissions)
  }

  it must "contain the request date for each pending request" in {
    val html = defaultHtml

    val dates = StubPropertyRepresentationConnector.stubbedRepresentations(RepresentationPending) map { r => Formatters.formatDate(r.createDatetime) }
    checkTableColumn(html, 3, "Date requested", dates)
  }

  it must "display accept and reject links for each pending request" in {
    val html = defaultHtml

    val actions = StubPropertyRepresentationConnector.stubbedRepresentations(RepresentationPending) map { _ => "Accept Reject" }
    checkTableColumn(html, 4, "Actions", actions)
  }

  it must "not display the page if the user is not an agent" in {
    val groupAccount: GroupAccount = arbitrary[GroupAccount].copy(isAgent = false)
    val individualAccount: DetailedIndividualAccount = arbitrary[DetailedIndividualAccount]

    StubAuthentication.stubAuthenticationResult(Authenticated(Accounts(groupAccount, individualAccount)))

    val res = TestRepresentationController.pendingRepresentationRequest(1, 15)(FakeRequest())
    status(res) mustBe UNAUTHORIZED
  }

  it must "display the current page number" in {
    val html = defaultHtml

    html.select("ul.pagination li.active").text mustBe "1"
  }

  it must "include a 'next' link if there are more results" in {
    setup(pendingRequests = 16)

    val res = TestRepresentationController.pendingRepresentationRequest(1, 15)(FakeRequest())
    status(res) mustBe OK

    val html = Jsoup.parse(contentAsString(res))

    val nextLink = html.select("ul.pagination li.next")

    nextLink.hasClass("disabled") mustBe false withClue "'Next' link is incorrectly disabled"
    nextLink.select("a").attr("href") mustBe routes.RepresentationController.pendingRepresentationRequest(2).url
  }

  it must "include an inactive 'next' link if there are no further results" in {
    val html = defaultHtml

    val nextLink = html.select("ul.pagination li.next")

    nextLink.hasClass("disabled") mustBe true withClue "'Next' link is not disabled"
  }

  it must "include an inactive 'previous' link when on page 1" in {
    val html = defaultHtml

    val previousLink = html.select("ul.pagination li.previous")

    previousLink.hasClass("disabled") mustBe true withClue "'Previous' link is not disabled"
  }

  it must "include a 'previous' link when not on page 1" in {
    setup(pendingRequests = 16)

    val res = TestRepresentationController.pendingRepresentationRequest(2, 15)(FakeRequest())
    status(res) mustBe OK

    val html = Jsoup.parse(contentAsString(res))

    val previousLink = html.select("ul.pagination li.previous")

    previousLink.hasClass("disabled") mustBe false withClue "'Previous' link is incorrectly disabled"
    previousLink.select("a").attr("href") mustBe routes.RepresentationController.pendingRepresentationRequest(1).url
  }

  it must "include pagination controls" in {

    pending
    val html = defaultHtml

    val pageSizeControls = html.select("ul.pageLength li").asScala

    pageSizeControls must have size 4
    pageSizeControls.head.text mustBe "15"

    val pendingRequestsLink: Int => String = n => routes.RepresentationController.pendingRepresentationRequest(pageSize = n).url

    pageSizeControls.tail.map(_.select("a").attr("href")) must contain theSameElementsAs Seq(pendingRequestsLink(25), pendingRequestsLink(50), pendingRequestsLink(100))
  }

  private val displayPermission: AgentPermission => String = {
    case NotPermitted => "No"
    case _ => "Yes"
  }

  lazy val defaultHtml = {
    setup()

    val res = TestRepresentationController.pendingRepresentationRequest(1, 15)(FakeRequest())
    status(res) mustBe OK

    Jsoup.parse(contentAsString(res))
  }

  private def setup(pendingRequests: Int = 15) = {
    val groupAccount: GroupAccount = arbitrary[GroupAccount].copy(isAgent = true)
    val individualAccount: DetailedIndividualAccount = arbitrary[DetailedIndividualAccount]

    StubAuthentication.stubAuthenticationResult(Authenticated(Accounts(groupAccount, individualAccount)))

    val pending = (1 to pendingRequests) map { _ => arbitrary[PropertyRepresentation].copy(status = RepresentationPending) }
    StubPropertyRepresentationConnector.stubRepresentations(pending)
  }

  private def checkTableColumn(html: Document, index: Int, heading: String, values: Seq[String]) = {
    html.select("table#nojsPendingRequests").select("th").get(index).text mustBe heading

    val data = html.select("table#nojsPendingRequests").select("tr").asScala.drop(1).map(_.select("td").get(index).text.toUpperCase)

    values foreach { v => data must contain (v.toUpperCase) }
  }

  private object TestRepresentationController extends RepresentationController(app.injector.instanceOf[ApplicationConfig],
    StubPropertyRepresentationConnector, StubAuthentication, StubPropertyLinkConnector)
}
