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

package controllers.agent

import config.ApplicationConfig
import connectors.Authenticated
import models._
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.scalacheck.Arbitrary.arbitrary
import org.scalatest._
import org.scalatest.mockito.MockitoSugar
import play.api.test.Helpers._
import play.api.test.{DefaultAwaitTimeout, FakeRequest, FutureAwaits}
import resources._
import utils._

import scala.collection.JavaConverters._

class ManageClientsSpec extends FlatSpec with MustMatchers with FutureAwaits with DefaultAwaitTimeout
  with BeforeAndAfterEach with AppendedClues with MockitoSugar with BeforeAndAfterAll with NoMetricsOneAppPerSuite {

  override val additionalAppConfig = Seq("featureFlags.searchSortEnabled" -> "false")

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

  lazy val defaultHtml = {
    setup()

    val res = TestController.viewClientProperties(1, 15)(FakeRequest())
    status(res) mustBe OK

    Jsoup.parse(contentAsString(res))
  }

  "The manage clients page" must "display the organisation name for each of the agent's first 15 client properties" in {
    val html = defaultHtml
      val organisationNames: Seq[String] = StubPropertyRepresentationConnector.stubbedRepresentations().map(_.organisationName)
      checkTableColumn(html, 0, "Organisation name", organisationNames)
  }

  it must "display the address for each of the agent's first 15 client properties" in {
    val html = defaultHtml
    val addresses = StubPropertyRepresentationConnector.stubbedRepresentations().map(_.address)

    checkTableColumn(html, 1, "Address", addresses)
  }

  it must "display the BA ref for each of the agent's first 15 client properties" in {
    val html = defaultHtml
    val baRefs = StubPropertyRepresentationConnector.stubbedRepresentations().map(_.billingAuthorityReference)

    checkTableColumn(html, 2, "Local authority reference", baRefs)
  }

  val permToString: AgentPermission => String = {
    case StartAndContinue => "Yes"
    case _ => "No"
  }
  val toPermissions: PropertyRepresentation => (AgentPermission, AgentPermission) = rep => (rep.checkPermission, rep.challengePermission)
  val toStringCheck: Tuple2[AgentPermission, AgentPermission] => String = {
    case (check, chal) => s"Check: ${permToString(check)} Challenge: ${permToString(chal)}"
  }

  it must "display the permissions for each of the agent's first 15 client properties" in {
    val html = defaultHtml
    val permissions = StubPropertyRepresentationConnector.stubbedRepresentations()
      .map(toPermissions.andThen(toStringCheck))

    checkTableColumn(html, 3, "Permissions", permissions)
  }

  it must "display the available actions for each of the user's first 15 client properties" in {
    val html = defaultHtml
    val actions = StubPropertyRepresentationConnector.stubbedRepresentations().map { l =>
      s"Revoke Client View Valuations"
    }

    checkTableColumn(html, 4, "Actions", actions)
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

  it must "tell the user they have no clients, if they have no clients to display" in {
    StubAuthentication.stubAuthenticationResult(Authenticated(Accounts(arbitrary[GroupAccount].copy(isAgent = true),
      arbitrary[DetailedIndividualAccount])))
    StubPropertyRepresentationConnector.reset()

    val res = TestController.viewClientProperties(1, 15)(FakeRequest())
    status(res) mustBe OK

    val html = Jsoup.parse(contentAsString(res))

    html.select("p#noProperties").text mustBe "There are no existing clients"

    setup()
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

    StubAuthentication.stubAuthenticationResult(Authenticated(Accounts(groupAccount, individualAccount)))
    (1 to numberOfLinks) foreach { _ =>
      StubPropertyLinkConnector.stubLink(arbitrary[PropertyLink].copy(organisationId = groupAccount.id))
      StubPropertyRepresentationConnector.stubRepresentation(arbitrary[PropertyRepresentation].copy(organisationId = groupAccount.id, status = RepresentationApproved))
      StubPropertyRepresentationConnector.stubRepresentation(arbitrary[PropertyRepresentation].copy(organisationId = groupAccount.id, status = RepresentationPending))
    }

  }

  private def checkTableColumn(html: Document, index: Int, heading: String, values: Seq[String]) = {
    html.select("table#nojsManageClients").select("th").get(index).text mustBe heading

    val data = html.select("table#nojsManageClients").select("tr").asScala.drop(1).map(_.select("td").get(index).text.toUpperCase)

    values foreach { v => data must contain (v.toUpperCase) }
  }

  private object TestController extends RepresentationController(
    app.injector.instanceOf[ApplicationConfig],
    StubPropertyRepresentationConnector,
    StubAuthentication,
    StubPropertyLinkConnector,
    new StubMessagesConnector(app.injector.instanceOf[ApplicationConfig])
  )
}
