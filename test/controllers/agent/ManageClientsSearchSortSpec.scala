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

import connectors.Authenticated
import controllers.TestApp
import models._
import models.searchApi.{AgentAuthResult, AgentAuthorisation, Organisation}
import org.scalatest.mockito.MockitoSugar
import org.scalatest._
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.{DefaultAwaitTimeout, FutureAwaits}
import connectors.Authenticated
import controllers.TestApp
import models._
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.scalacheck.Arbitrary.arbitrary
import play.api.test.FakeRequest
import play.api.test.Helpers._
import resources._
import uk.gov.hmrc.play.test.WithFakeApplication
import utils._

import scala.collection.JavaConverters._

class ManageClientsSearchSortSpec extends FlatSpec with MustMatchers with FutureAwaits with DefaultAwaitTimeout
  with BeforeAndAfterEach with AppendedClues with MockitoSugar with BeforeAndAfterAll with WithFakeApplication{
  TestApp.stop()
  implicit override lazy val  fakeApplication = new GuiceApplicationBuilder()
    .configure("featureFlags.searchSortEnabled" -> "true")
    .configure("metrics.enabled" -> "false")
    .build()

  override def beforeAll() {
    TestApp.stop()
    play.api.Play.start(fakeApplication)
  }

  override def afterAll() {
    play.api.Play.stop(fakeApplication)
    TestApp.start()
  }

  val token = "Csrf-Token" -> "nocheck"
  override protected def beforeEach(): Unit = {
    StubIndividualAccountConnector.reset()
    StubGroupAccountConnector.reset()
    StubAuthConnector.reset()
    StubIdentityVerification.reset()
    StubPropertyLinkConnector.reset()
    StubAuthentication.reset()
    StubBusinessRatesValuation.reset()
    StubSubmissionIdConnector.reset()
    StubPropertyRepresentationConnector.reset()
  }

  //Make the tests run significantly faster by only loading and parsing the default case, of 15 property links, once
  private lazy val defaultHtml = {
    setup()
    val res = TestController.viewClientProperties(1, 15)(FakeRequest())
    status(res) mustBe OK

    Jsoup.parse(contentAsString(res))
  }

  "The manage clients page" must "display the organisation name for each of the agent's first 15 client properties" in {
    val html = defaultHtml

      val organisationNames = StubPropertyRepresentationConnector.getstubbedAgentAuthResult().authorisations.map(_.client.organisationName)
      checkTableColumn(html, 2, "Client", organisationNames)
  }

  it must "display the address for each of the agent's first 15 client properties" in {
    val html = defaultHtml
    val addresses = StubPropertyRepresentationConnector.getstubbedAgentAuthResult().authorisations.map(_.address)

    checkTableColumn(html, 0, "Address", addresses)
  }

  it must "display the BA ref for each of the agent's first 15 client properties" in {
    val html = defaultHtml
    val baRefs = StubPropertyRepresentationConnector.getstubbedAgentAuthResult().authorisations.map(_.localAuthorityRef)

    checkTableColumn(html, 1, "Local authority reference", baRefs)
  }


  it must "display the available actions for each of the user's first 15 client properties" in {
    val html = defaultHtml

    val actions = StubPropertyRepresentationConnector.getstubbedAgentAuthResult().authorisations.map { auth =>
      s"Revoke Client View Valuations"
    }
    checkTableColumn(html, 3, "Actions", actions)
  }

  it must "display the current page number" in {
    val html = defaultHtml

    html.select("ul.pagination li.active").text mustBe "1"
  }

  it must "include a 'next' link if there are more results" in {
    setup(numberOfLinks = 16)

    val res = TestController.viewClientProperties(1, 15)(FakeRequest())
    status(res) mustBe OK

    val html = Jsoup.parse(contentAsString(res))

    val nextLink = html.select("ul.pagination li.next")

    nextLink.hasClass("disabled") mustBe false withClue "'Next' link is incorrectly disabled"
    nextLink.select("a").attr("href") mustBe routes.RepresentationController.viewClientProperties(2).url
  }

  it must "include an inactive 'next' link if there are no further results" in {
    setup(numberOfLinks = 16)

    val res = TestController.viewClientProperties(2, 15)(FakeRequest())
    status(res) mustBe OK

    val html = Jsoup.parse(contentAsString(res))

    val nextLink = html.select("ul.pagination li.next")

    nextLink.hasClass("disabled") mustBe true withClue "'Next' link is not disabled"
  }

  it must "include an inactive 'previous' link when on page 1" in {
    setup(numberOfLinks = 16)

    val res = TestController.viewClientProperties(1, 15)(FakeRequest())
    status(res) mustBe OK

    val html = Jsoup.parse(contentAsString(res))

    val previousLink = html.select("ul.pagination li.previous")

    previousLink.hasClass("disabled") mustBe true withClue "'Previous' link is not disabled"
  }

  it must "include a 'previous' link when not on page 1" in {
    setup(numberOfLinks = 16)

    val res = TestController.viewClientProperties(2, 15)(FakeRequest())
    status(res) mustBe OK

    val html = Jsoup.parse(contentAsString(res))

    val previousLink = html.select("ul.pagination li.previous")

    previousLink.hasClass("disabled") mustBe false withClue "'Previous' link is incorrectly disabled"
    previousLink.select("a").attr("href") mustBe routes.RepresentationController.viewClientProperties(1).url
  }

  it must "include a link to pending properties view" in {
    val html = defaultHtml

    html.select("a#viewPending").attr("href") mustBe routes.RepresentationController.pendingRepresentationRequest().url
  }

  it must "not display the page if the user is not an agent" in {
    val groupAccount: GroupAccount = arbitrary[GroupAccount].copy(isAgent = false)
    val individualAccount: DetailedIndividualAccount = arbitrary[DetailedIndividualAccount]

    StubAuthentication.stubAuthenticationResult(Authenticated(Accounts(groupAccount, individualAccount)))

    val indirectLink = arbitrary[PropertyLink].retryUntil(_.organisationId != groupAccount.id).copy(agents = Seq(arbitrary[Party].copy(organisationId = groupAccount.id)))
    StubPropertyLinkConnector.stubLink(indirectLink)

    val res = TestController.viewClientProperties(1, 15)(FakeRequest())
    status(res) mustBe UNAUTHORIZED
  }

  it must "include pagination controls" in {
    pending
    val html = defaultHtml

    val pageSizeControls = html.select("ul.pageLength li").asScala

    pageSizeControls must have size 4
    pageSizeControls.head.text mustBe "15"

    val manageClientsLink: Int => String = n => routes.RepresentationController.viewClientProperties(pageSize = n).url

    pageSizeControls.tail.map(_.select("a").attr("href")) must contain theSameElementsAs Seq(manageClientsLink(25), manageClientsLink(50), manageClientsLink(100))
  }

  private def setup(numberOfLinks: Int = 15) = {

    val groupAccount: GroupAccount = arbitrary[GroupAccount].copy(isAgent = true)
    val individualAccount: DetailedIndividualAccount = arbitrary[DetailedIndividualAccount]
    var arbitraryAgentAuthorisation: Seq[AgentAuthorisation] = Nil

    StubAuthentication.stubAuthenticationResult(Authenticated(Accounts(groupAccount, individualAccount)))
    (1 to numberOfLinks) foreach { _ =>
      arbitraryAgentAuthorisation :+= arbitrary[AgentAuthorisation].copy(id = groupAccount.id.toLong, client = arbitrary[Organisation].copy(organisationId = groupAccount.id.toLong))
    }

    StubPropertyRepresentationConnector.stubAgentAuthResult(AgentAuthResult(start =1,
      size = numberOfLinks,
      total = numberOfLinks,
      filterTotal = numberOfLinks,
      authorisations = arbitraryAgentAuthorisation))

  }

  private def checkTableColumn(html: Document, index: Int, heading: String, values: Seq[String]) = {
    html.select("table#nojsManageClients").select("th").get(index).text mustBe heading

    val data = html.select("table#nojsManageClients").select("tr").asScala.drop(1).map(_.select("td").get(index).text.toUpperCase)

    values foreach { v => data must contain (v.toUpperCase) }
  }

  private object TestController extends RepresentationController {
    override val reprConnector = StubPropertyRepresentationConnector
    override val authenticated = StubAuthentication
  }

}