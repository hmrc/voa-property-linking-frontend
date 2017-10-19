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

package views.dashboard

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

import actions.{AgentRequest, BasicAuthenticatedRequest}
import config.ApplicationConfig
import controllers.{ControllerSpec, routes}
import models.messages.{Message, MessageSearchResults}
import models.{DetailedIndividualAccount, GroupAccount, IndividualDetails}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.i18n.Messages
import play.api.mvc.{AnyContent, AnyContentAsEmpty}
import play.api.test.FakeRequest
import views.html.dashboard.messagesTab

import scala.collection.JavaConverters._

class MessagesPageSpec extends ControllerSpec {

  override val additionalAppConfig = Seq("featureFlags.searchSortEnabled" -> "true")

  "The messages page" must """show "There are no messages" when the user has no messages""" in {
    val noMessages = MessageSearchResults(
      0, 0, Nil, 0
    )
    val noMessagesPage = Jsoup.parse(messagesTab(noMessages, 0, 1, 1).toString)

    noMessagesPage.select("p#noMessages").text mustBe "There are no messages to display"
  }

  it must "show the dashboard tabs at the top of the page" in {
    //because travis is stupid
    val managePropertiesUrl = if(ApplicationConfig.config.searchSortEnabled) {
      routes.Dashboard.managePropertiesSearchSort().url
    } else {
      routes.Dashboard.manageProperties().url
    }
    
    val expectedTabLinks = Seq(
      managePropertiesUrl,
      routes.Dashboard.manageAgents().url,
      routes.Dashboard.viewDraftCases().url,
      controllers.manageDetails.routes.ViewDetails.show().url,
      routes.Dashboard.viewMessages().url
    )

    val tabs = pageWithOneUnreadMessage.select("div.section-tabs ul").select("a").asScala.map(_.attr("href"))
    tabs must have size 5
    tabs must contain theSameElementsAs expectedTabLinks
  }

  it must "show the number of unread messages in the messages tab text" in {
    val messagesTab = pageWithOneUnreadMessage.select("div.section-tabs ul").select(s"a[href=${routes.Dashboard.viewMessages().url}]")
    messagesTab.text mustBe "Messages (1)"
  }

  it must "show the read status, subject, address, reference number, and received date for each message" in {
    val messagesTable = pageWithOneUnreadMessage.select("#messagesTable")

    val headings = messagesTable.select("thead tr th").asScala.map(_.text)
    headings must contain theSameElementsAs Seq("Status", "Subject", "Address", "Reference", "Received at")

    val row = messagesTable.select("tbody tr td").asScala
    val message = oneMessage.messages.head

    row.head.select("i.icon-messageunread span.visuallyhidden").text mustBe "Unread"
    row(1).text mustBe message.subject
    row(2).text mustBe message.address
    row(3).text mustBe message.caseReference
    row(4).text mustBe message.effectiveDate.format(DateTimeFormatter.ofPattern("d MMMM yyyy hh:mm a"))
  }

  it must "show the client name for each message if the user is an agent" in {
    val agentRequest: AgentRequest[AnyContentAsEmpty.type] = AgentRequest(
      organisationAccount = GroupAccount(
        id = 456,
        groupId = "some-group",
        companyName = "a company",
        addressId = 999,
        email = "aa@bb.cc",
        phone = "123",
        isAgent = true,
        agentCode = 123
      ),
      individualAccount = DetailedIndividualAccount(
        externalId = "external-id",
        trustId = "trust-id",
        organisationId = 456,
        individualId = 789,
        details = IndividualDetails(
          firstName = "fname",
          lastName = "lname",
          email = "aa@bb.cc",
          phone1 = "1",
          phone2 = None,
          addressId = 111
        )
      ),
      agentCode = 123,
      request = FakeRequest()
    )

    lazy val clientMessages = MessageSearchResults(
      1,
      1,
      Seq(Message(
        "noderef",
        222,
        "a template",
        Some(333),
        Some("client"),
        "aaa",
        "bbb",
        LocalDateTime.now,
        "address",
        LocalDateTime.now,
        "something",
        None,
        "a message"
      )),
      1
    )

    val clientMessagesPage = Jsoup.parse(messagesTab(clientMessages, 1, 1, 1)(agentRequest, implicitly).toString)
    val messagesTable = clientMessagesPage.select("#messagesTable")
    val headings = messagesTable.select("thead tr th").asScala.map(_.text)

    headings must contain theSameElementsAs Seq("Status", "Subject", "Address", "Reference", "Client", "Received at")

    val message = clientMessages.messages.head
    val row = messagesTable.select("tbody tr td").asScala
    row.head.select("i.icon-messageunread span.visuallyhidden").text mustBe "Unread"
    row(1).text mustBe message.subject
    row(2).text mustBe message.address
    row(3).text mustBe message.caseReference
    row(4).text mustBe message.clientName.get
    row(5).text mustBe message.effectiveDate.format(DateTimeFormatter.ofPattern("d MMMM yyyy hh:mm a"))
  }

  it must """show a "next" button if there is another page of messages to show""" in {
    lazy val multiplePages = Jsoup.parse(messagesTab(oneMessage, 1, 1, 2).toString)

    val nextPageLink = multiplePages.select("div#messagesTable_paginate a#messagesTable_next")
    nextPageLink.text mustBe "Next"
    nextPageLink.attr("href") mustBe routes.Dashboard.viewMessages(pageNumber = 2).url
  }

  it must """show a "previous" button if the user is not on page 1""" in {
    lazy val messagesPg2 = Jsoup.parse(messagesTab(oneMessage, 1, 2, 2).toString)

    val previousPageLink = messagesPg2.select("div#messagesTable_paginate a#messagesTable_previous")
    previousPageLink.text mustBe "Previous"
    previousPageLink.attr("href") mustBe routes.Dashboard.viewMessages(pageNumber = 1).url
  }

  it must "not show a next button on the last page of messages" in {
    lazy val messagesPg2 = Jsoup.parse(messagesTab(oneMessage, 1, 2, 2).toString)

    val nextPageLink = messagesPg2.select("div#messagesTable_paginate a#messagesTable_next")
    nextPageLink.text mustBe "Next"
    nextPageLink.hasClass("disabled") mustBe true withClue "Next page link was not disabled"
  }

  it must "not show a previous button on page 1" in {
    lazy val multiplePages = Jsoup.parse(messagesTab(oneMessage, 1, 1, 2).toString)

    val previousPageLink = multiplePages.select("div#messagesTable_paginate a#messagesTable_previous")
    previousPageLink.text mustBe "Previous"
    previousPageLink.hasClass("disabled") mustBe true withClue "Previous page link was not disabled"
  }

  it must "mark messages as unread where lastRead is None" in {
    val messagesTable = pageWithOneUnreadMessage.select("#messagesTable")

    val readStatus = messagesTable.select("tbody tr td").asScala.head
    readStatus.select("i.icon-messageunread span.visuallyhidden").text mustBe "Unread"
  }

  it must "mark messages as read when lastRead is not None" in {
    lazy val noUnreadMessages = MessageSearchResults(
      start = 1,
      size = 1,
      messages = Seq(
        Message(
          id = "somenoderef",
          recipientOrgId = 123,
          templateName = "aTemplate",
          clientOrgId = None,
          clientName = None,
          caseReference = "a case ref",
          submissionId = "sub id",
          timestamp = LocalDateTime.now,
          address = "somewhere",
          effectiveDate = LocalDateTime.now,
          subject = "something",
          lastRead = Some(LocalDateTime.now),
          messageType = "message"
        )
      ),
      total = 1
    )

    lazy val pageWithNoUnreadMessages = Jsoup.parse(messagesTab(noUnreadMessages, 0, 1, 1).toString)
    val messagesTable = pageWithNoUnreadMessages.select("#messagesTable")

    val readStatus = messagesTable.select("tbody tr td").asScala.head
    readStatus.select("i.icon-messageread span.visuallyhidden").text mustBe "Read"
  }

  lazy val pageWithOneUnreadMessage: Document = Jsoup.parse(messagesTab(oneMessage, 1, 1, 1).toString)

  lazy val oneMessage = MessageSearchResults(
    start = 1,
    size = 1,
    messages = Seq(
      Message(
        id = "somenoderef",
        recipientOrgId = 123,
        templateName = "aTemplate",
        clientOrgId = None,
        clientName = None,
        caseReference = "a case ref",
        submissionId = "sub id",
        timestamp = LocalDateTime.now,
        address = "somewhere",
        effectiveDate = LocalDateTime.now,
        subject = "something",
        lastRead = None,
        messageType = "message"
      )
    ),
    total = 1
  )

  implicit lazy val nonAgentRequest: BasicAuthenticatedRequest[AnyContent] = {
    BasicAuthenticatedRequest(
      GroupAccount(
        id = 123,
        groupId = "321",
        companyName = "company",
        addressId = 456,
        email = "aa@bb.cc",
        phone = "0",
        isAgent = false,
        agentCode = 0
      ),
      DetailedIndividualAccount(
        externalId = "external-id",
        trustId = "trust-id",
        organisationId = 123,
        individualId = 456,
        details = IndividualDetails(
          firstName = "Mr",
          lastName = "Man",
          email = "aa@bb.cc",
          phone1 = "123",
          phone2 = None,
          addressId = 789
        )
      ),
      FakeRequest()
    )
  }

  implicit lazy val messages: Messages = Messages.Implicits.applicationMessages
}
