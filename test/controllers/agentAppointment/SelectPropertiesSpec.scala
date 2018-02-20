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

//
//package controllers.agentAppointment
//
//import connectors.{AgentsConnector, Authenticated, GroupAccounts, _}
//import controllers.ControllerSpec
//import models.searchApi._
//import models.{Accounts, DetailedIndividualAccount, GroupAccount}
//import org.jsoup.Jsoup
//import org.jsoup.nodes.Document
//import org.scalacheck.Arbitrary.arbitrary
//import play.api.test.FakeRequest
//import play.api.test.Helpers.{contentAsString, status, _}
//import repositories.SessionRepo
//import resources._
//import utils.{StubAuthentication, StubPropertyLinkConnector}
//
//import scala.collection.JavaConverters._
//
//
//class SelectPropertiesSpec extends ControllerSpec {
//
//  override val additionalAppConfig = Seq("featureFlags.manageAgentsEnabled" -> "true", "featureFlags.agentMultipleAppointEnabled" -> "true")
//
//  //Make the tests run significantly faster by only loading and parsing the default case, of 15 property links, once
//  lazy val defaultHtml = {
//    setup()
//
//    val res = TestAppointAgentController.selectProperties()(FakeRequest())
//    status(res) mustBe OK
//
//    Jsoup.parse(contentAsString(res))
//  }
//
//  "The appoint agent properties page" must "display the address for each of the user's first 15 properties" in {
//    val html = defaultHtml
//    val addresses = StubPropertyLinkConnector.getstubbedOwnerAuthResult().authorisations.map(_.address)
//    checkTableColumn(html, 1, "Address", addresses)
//  }
//
//
//  it must "display the appointed agents for each of the user's first 15 properties" in {
//    val html = defaultHtml
//    val agents = StubPropertyLinkConnector.getstubbedOwnerAuthResult().authorisations.map {
//      case authorisation if authorisation.agents.nonEmpty => authorisation.agents.get.map(_.organisationName) mkString " "
//      case _ => "None"
//    }
//
//    checkTableColumn(html, 2, "Appointed Agents", agents)
//  }
//
//
//  it must "display the current page number" in {
//    val html = defaultHtml
//
//    html.select("ul.pagination li.active").text mustBe "1"
//  }

//  it must "include a 'next' link if there are more results" in {
//    setup(numberOfLinks = 16)
//
//    val res = TestAppointAgentController.selectProperties(AgentPropertiesPagination())(FakeRequest())
//    status(res) mustBe OK
//
//    val html = Jsoup.parse(contentAsString(res))
//
//    val expectedPagination = AgentPropertiesPagination(pageNumber = 2)
//
//    val nextLink = html.select("ul.pagination li.next")
//
//    nextLink.hasClass("disabled") mustBe false withClue "'Next' link is incorrectly disabled"
//    nextLink.select("a").attr("href") mustBe routes.AppointAgentController.selectProperties(expectedPagination).url
//  }
//Need to fix in view file
//  it must "include an inactive 'next' link if there are no further results" in {
//    setup(numberOfLinks = 16)
//
//    val res = TestAppointAgentController.selectProperties(AgentPropertiesPagination())(FakeRequest())
//    status(res) mustBe OK
//
//    val html = Jsoup.parse(contentAsString(res))
//
//    val nextLink = html.select("ul.pagination li.next")
//
//    nextLink.hasClass("disabled") mustBe true withClue "'Next' link is not disabled"
//  }

  //this shouldn't be passing
//  it must "include an inactive 'previous' link when on page 1" in {
//    setup(numberOfLinks = 16)
//
//    val res = TestAppointAgentController.selectProperties(AgentPropertiesPagination())(FakeRequest())
//    status(res) mustBe OK
//
//    val html = Jsoup.parse(contentAsString(res))
//
//    val previousLink = html.select("ul.pagination li.previous")
//
//    previousLink.hasClass("disabled") mustBe true withClue "'Previous' link is not disabled"
//  }
//
//  it must "include a 'previous' link when not on page 1" in {
//    setup(numberOfLinks = 16)
//
//    val res = TestAppointAgentController.selectProperties(AgentPropertiesPagination())(FakeRequest())
//    status(res) mustBe OK
//
//    val html = Jsoup.parse(contentAsString(res))
//
//    val previousLink = html.select("ul.pagination li.previous")
//
//    previousLink.hasClass("disabled") mustBe false withClue "'Previous' link is incorrectly disabled"
//    previousLink.select("a").attr("href") mustBe routes.AppointAgentController.selectProperties().url
//  }

//  it must "include pagination controls" in {
//    val html = defaultHtml
//
//    val pageSizeControls = html.select("ul.pageLength li").asScala
//
//    pageSizeControls must have size 4
//    pageSizeControls.head.text mustBe "15"
//
//    val paginationControls = AgentPropertiesPagination(pageSize = 1.toString(n))
//
//    val paginationControls: Int => String = n => routes.AppointAgentController.selectProperties
//
//    val managePropertiesLink: Int => String = n => routes.Dashboard.manageProperties(pageSize = n).url
//
//    pageSizeControls.tail.map(_.select("a").attr("href")) must contain theSameElementsAs Seq(paginationControls(25), paginationControls(50), paginationControls(100))
//  }
//
//  private def setup(numberOfLinks: Int = 15) = {
//    val groupAccount: GroupAccount = arbitrary[GroupAccount]
//    val individualAccount: DetailedIndividualAccount = arbitrary[DetailedIndividualAccount]
//
//    var arbitraryOwnerAuthorisation: Seq[OwnerAuthorisation] = Nil
//
//    StubAuthentication.stubAuthenticationResult(Authenticated(Accounts(groupAccount, individualAccount)))
//    (1 to numberOfLinks) foreach { _ =>
//      arbitraryOwnerAuthorisation :+= arbitrary[OwnerAuthorisation].copy(authorisationId = groupAccount.id.toLong)
//    }
//
//    StubPropertyLinkConnector.stubOwnerAuthResult(OwnerAuthResult(start =1,
//      size = numberOfLinks,
//      total = numberOfLinks,
//      filterTotal = numberOfLinks,
//      authorisations = arbitraryOwnerAuthorisation))
//  }
//
//  private def checkTableColumn(html: Document, index: Int, heading: String, values: Seq[String]) = {
//    html.select("table#agentPropertiesTable").select("th").get(index).text mustBe heading
//
//    val data = html.select("table#agentPropertiesTable").select("tr").asScala.drop(2).map(_.select("td").get(index).text.toUpperCase)
//
//    values foreach { v => data must contain (v.toUpperCase) }
//  }
//
//  private object TestAppointAgentController extends AppointAgentController(
//    mock[PropertyRepresentationConnector],
//    mock[GroupAccounts],
//    StubPropertyLinkConnector,
//    mock[AgentsConnector],
//    StubAuthentication,
//    mock[SessionRepo]
//  )
//}
//
