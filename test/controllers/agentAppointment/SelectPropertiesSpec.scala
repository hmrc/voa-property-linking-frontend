/*
 * Copyright 2019 HM Revenue & Customs
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

package controllers.agentAppointment

import auditing.AuditingService
import connectors.{AgentsConnector, Authenticated, GroupAccounts, _}
import controllers.VoaPropertyLinkingSpec
import models.searchApi._
import models._
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.scalacheck.Arbitrary.arbitrary
import play.api.test.FakeRequest
import play.api.test.Helpers.{contentAsString, status, _}
import repositories.SessionRepo
import resources._
import utils.{StubAuthentication, StubGroupAccountConnector, StubPropertyLinkConnector}

import scala.collection.JavaConverters._


class SelectPropertiesSpec extends VoaPropertyLinkingSpec {

  //Make the tests run significantly faster by only loading and parsing the default

  val agentGroup = GroupAccount(1L, "groupId", "company", 1, "email", "2341234", true, 1L)
  val pagination = AgentPropertiesParameters(
    agentCode = 1L,
    checkPermission = StartAndContinue,
    challengePermission = StartAndContinue)

  lazy val defaultHtml = {
    setup()

    val res = TestAppointAgentController.selectPropertiesSearchSort(pagination)(FakeRequest())
    status(res) mustBe OK

    Jsoup.parse(contentAsString(res))
  }

  "The appoint agent properties page" must "display the address for each of the user's first 15 properties" in {
    val html = defaultHtml
    val addresses = StubPropertyLinkConnector.getstubbedOwnerAuthResult().authorisations.map(_.address)
    checkTableColumn(html, 1, "Address", addresses)
  }


  it must "display the appointed agents for each of the user's first 15 properties" in {
    val html = defaultHtml
    val agents = StubPropertyLinkConnector.getstubbedOwnerAuthResult().authorisations.map {
      case authorisation if authorisation.agents.nonEmpty => authorisation.agents.get.map(_.organisationName) mkString " "
      case _ => "None"
    }

    checkTableColumn(html, 2, "Appointed agents", agents)
  }


  it must "display the current page number" in {
    val html = defaultHtml

    html.select("ul.pagination li.active").text mustBe "1"
  }

  it must "include a 'next' link if there are more results" in {
    setup(numberOfLinks = 16)

    val res = TestAppointAgentController.selectPropertiesSearchSort(pagination)(FakeRequest())
    status(res) mustBe OK

    val html = Jsoup.parse(contentAsString(res))

    val expectedPagination = pagination.copy(pageNumber = 2)

    val nextLink = html.select("ul.pagination li.next")

    nextLink.hasClass("disabled") mustBe false withClue "'Next' link is incorrectly disabled"
    nextLink.select("a").attr("href") mustBe routes.AppointAgentController.selectPropertiesSearchSort(expectedPagination).url
  }

  it must "include an inactive 'next' link if there are no further results" in {
    setup(numberOfLinks = 15)

    val res = TestAppointAgentController.selectPropertiesSearchSort(pagination)(FakeRequest())
    status(res) mustBe OK

    val html = Jsoup.parse(contentAsString(res))

    val nextLink = html.select("ul.pagination li.next")

    nextLink.hasClass("disabled") mustBe true withClue "'Next' link is not disabled"
  }

  it must "include an inactive 'previous' link when on page 1" in {
    setup(numberOfLinks = 16)

    val res = TestAppointAgentController.selectPropertiesSearchSort(pagination)(FakeRequest())
    status(res) mustBe OK

    val html = Jsoup.parse(contentAsString(res))

    val previousLink = html.select("ul.pagination li.previous")

    previousLink.hasClass("disabled") mustBe true withClue "'Previous' link is not disabled"
  }

  it must "include a 'previous' link when not on page 1" in {
    setup(numberOfLinks = 16)

    val res = TestAppointAgentController.selectPropertiesSearchSort(pagination.copy(pageNumber = 2))(FakeRequest())
    status(res) mustBe OK

    val html = Jsoup.parse(contentAsString(res))

    val previousLink = html.select("ul.pagination li.previous")

    previousLink.hasClass("disabled") mustBe false withClue "'Previous' link is incorrectly disabled"
    previousLink.select("a").attr("href") mustBe routes.AppointAgentController.selectPropertiesSearchSort(pagination).url
  }

  it must "include pagination controls" in {
    val html = defaultHtml

    val pageSizeControlsCurrent = html.select("span.page-size-option-current").asScala
    val pageSizeControls = html.select("a.page-size-option").asScala

    pageSizeControlsCurrent must have size 1
    pageSizeControlsCurrent.head.text mustBe "15"
    pageSizeControls must have size 4
    pageSizeControls.head.text mustBe "25"
  }

  private def setup(numberOfLinks: Int = 15) = {
    val groupAccount: GroupAccount = arbitrary[GroupAccount]
    val individualAccount: DetailedIndividualAccount = arbitrary[DetailedIndividualAccount]

    var arbitraryOwnerAuthorisation: Seq[OwnerAuthorisation] = Nil

    StubGroupAccountConnector.stubAccount(agentGroup)

    StubAuthentication.stubAuthenticationResult(Authenticated(Accounts(groupAccount, individualAccount)))
    (1 to numberOfLinks) foreach { _ =>
      arbitraryOwnerAuthorisation :+= arbitrary[OwnerAuthorisation].copy(authorisationId = groupAccount.id.toLong)
    }

    StubPropertyLinkConnector.stubOwnerAuthResult(OwnerAuthResult(start = 1,
      size = numberOfLinks,
      total = numberOfLinks,
      filterTotal = numberOfLinks,
      authorisations = arbitraryOwnerAuthorisation))
  }

  private def checkTableColumn(html: Document, index: Int, heading: String, values: Seq[String]) = {
    html.select("table#agentPropertiesTable").select("th").get(index).text mustBe heading

    val data = html.select("table#agentPropertiesTableBody").select("tr").asScala.map(_.select("td").get(index).text.toUpperCase)

    values foreach { v => data must contain(v.toUpperCase) }
  }

  private object TestAppointAgentController extends AppointAgentController(
    mock[PropertyRepresentationConnector],
    StubGroupAccountConnector,
    StubPropertyLinkConnector,
    mock[AgentsConnector],
    StubAuthentication,
    mock[AuditingService]
  )

}
 
