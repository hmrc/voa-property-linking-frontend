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
import models.messages.{Message, MessagePagination, MessageSearchResults, MessageSortField}
import models.{DetailedIndividualAccount, GroupAccount, IndividualDetails, SortOrder}
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
      0, 0, Nil
    )
    val noMessagesPage = Jsoup.parse(messagesTab(noMessages, MessagePagination(), 1, 1).toString)

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

    val headings = messagesTable.select("thead tr").first.select("th").asScala.map(_.text)
    headings must contain theSameElementsAs Seq("Status", "Subject", "Address", "Reference", "Received at")

    val row = messagesTable.select("tbody tr td").asScala
    val message = oneMessage.messages.head

    row.head.select("i.icon-messageunread span.visuallyhidden").text mustBe "Unread"
    row(1).text mustBe message.subject
    row(2).text mustBe message.address
    row(3).text mustBe message.caseReference
    row(4).text mustBe message.effectiveDate.format(DateTimeFormatter.ofPattern("d MMMM yyyy hh:mm a"))
  }

  it must "include search fields for address and reference number" in {
    val messagesTable = pageWithOneUnreadMessage.select("#messagesTable")

    val inputs = messagesTable.select("thead tr th div.searchField input").asScala.map(_.attr("name"))
    inputs must contain ("address")
    inputs must contain ("caseReference")
  }

  it must "include hidden search fields for page number, page size, sort field, and sort order" in {
    val messagesTable = pageWithOneUnreadMessage.select("#messagesTable")

    val hiddenInputs = messagesTable.select("thead tr input[type=hidden]").asScala.map(_.attr("name"))
    hiddenInputs must contain ("pageNumber")
    hiddenInputs must contain ("pageSize")
    hiddenInputs must contain ("sortField")
    hiddenInputs must contain ("sortOrder")
  }

  it must "prepopulate the address search field if the results are filtered by address" in {
    val filteredByAddress = Jsoup.parse(messagesTab(oneMessage, MessagePagination(address = Some("where")), 1, 1).toString)

    val addressInput = filteredByAddress.select("input#address")
    addressInput.attr("value") mustBe "where"
  }

  it must "prepopulate the reference number search field if the results are filtered by reference" in {
    val filteredByReference = Jsoup.parse(messagesTab(oneMessage, MessagePagination(referenceNumber = Some("ref")), 1, 1).toString)

    val referenceInput = filteredByReference.select("input#caseReference")
    referenceInput.attr("value") mustBe "ref"
  }

  it must "show the client name for each message if the user is an agent" in {
    val messagesTable = clientMessagesPage.select("#messagesTable")
    val headings = messagesTable.select("thead tr").first.select("th").asScala.map(_.text)

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

  it must "include a search field for client name if the user is an agent" in {
    val messagesTable = clientMessagesPage.select("#messagesTable")

    val inputs = messagesTable.select("thead tr th div.searchField input").asScala.map(_.attr("name"))
    inputs must contain ("clientName")
  }

  it must "prepopulate the client name search field if the results are filtered by client name" in {
    val filteredByClient = Jsoup.parse(messagesTab(clientMessages, MessagePagination(clientName = Some("body")), 1, 1)(agentRequest, implicitly).toString)

    val clientInput = filteredByClient.select("input#clientName")
    clientInput.attr("value") mustBe "body"
  }

  it must """show a "next" button if there is another page of messages to show""" in {
    lazy val multiplePages = Jsoup.parse(messagesTab(oneMessage, MessagePagination(), 1, 2).toString)

    val nextPageLink = multiplePages.select("div#messagesTable_paginate a#messagesTable_next")
    nextPageLink.text mustBe "Next"
    nextPageLink.attr("href") mustBe routes.Dashboard.viewMessages(MessagePagination().copy(pageNumber = 2)).url
  }

  it must """show a "previous" button if the user is not on page 1""" in {
    lazy val messagesPg2 = Jsoup.parse(messagesTab(oneMessage, MessagePagination().copy(pageNumber = 2), 1, 2).toString)

    val previousPageLink = messagesPg2.select("div#messagesTable_paginate a#messagesTable_previous")
    previousPageLink.text mustBe "Previous"
    previousPageLink.attr("href") mustBe routes.Dashboard.viewMessages(MessagePagination()).url
  }

  it must "not show a next button on the last page of messages" in {
    lazy val messagesPg2 = Jsoup.parse(messagesTab(oneMessage, MessagePagination().copy(pageNumber = 2), 1, 1).toString)

    val nextPageLink = messagesPg2.select("div#messagesTable_paginate").select("li.next")
    nextPageLink.text mustBe "Next"
    nextPageLink.hasClass("disabled") mustBe true withClue "Next page link was not disabled"
  }

  it must "not show a previous button on page 1" in {
    lazy val multiplePages = Jsoup.parse(messagesTab(oneMessage, MessagePagination(), 1, 1).toString)

    val previousPageLink = multiplePages.select("div#messagesTable_paginate").select("li.previous")
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
          agentOrgId = None,
          agentName = None,
          caseReference = "a case ref",
          submissionId = "sub id",
          timestamp = LocalDateTime.now,
          address = "somewhere",
          effectiveDate = LocalDateTime.now,
          subject = "something",
          lastRead = Some(LocalDateTime.now),
          messageType = "message"
        )
      )
    )

    lazy val pageWithNoUnreadMessages = Jsoup.parse(messagesTab(noUnreadMessages, MessagePagination(), 1, 1).toString)
    val messagesTable = pageWithNoUnreadMessages.select("#messagesTable")

    val readStatus = messagesTable.select("tbody tr td").asScala.head
    readStatus.select("i.icon-messageread span.visuallyhidden").text mustBe "Read"
  }

  it must "have clickable links on each heading to sort by each field" in {
    val headings = pageWithOneUnreadMessage.select("#messagesTable thead tr th").asScala

    headings.head.select("div.sorting a").attr("href") mustBe routes.Dashboard.viewMessages(MessagePagination(sortField = MessageSortField.LastRead, sortOrder = SortOrder.Ascending)).url
    headings(1).select("div.sorting a").attr("href") mustBe routes.Dashboard.viewMessages(MessagePagination(sortField = MessageSortField.Subject, sortOrder = SortOrder.Ascending)).url
    headings(2).select("div.sorting a").attr("href") mustBe routes.Dashboard.viewMessages(MessagePagination(sortField = MessageSortField.Address, sortOrder = SortOrder.Ascending)).url
    headings(3).select("div.sorting a").attr("href") mustBe routes.Dashboard.viewMessages(MessagePagination(sortField = MessageSortField.CaseReference, sortOrder = SortOrder.Ascending)).url
    //sorted in descending date order by default
    headings(4).select("div.sorting_desc a").attr("href") mustBe routes.Dashboard.viewMessages(MessagePagination(sortField = MessageSortField.EffectiveDate, sortOrder = SortOrder.Ascending)).url
  }

  it must "have a clickable link to reverse the sort order on the current sorting field" in {
    val pageSortedByAscendingAddress = Jsoup.parse(messagesTab(oneMessage, MessagePagination(sortField = MessageSortField.Address, sortOrder = SortOrder.Ascending), 1, 1).toString)

    val addressHeading = pageSortedByAscendingAddress.select("#messagesTable thead").select("th").eq(2)
    addressHeading.select("a").attr("href") mustBe routes.Dashboard.viewMessages(MessagePagination(sortField = MessageSortField.Address, sortOrder = SortOrder.Descending)).url
  }

  it must "include controls to change the page size" in {
    val pageSizeControls = pageWithOneUnreadMessage.select("ul.showResults li")

    pageSizeControls.first().text mustBe "15"
    pageSizeControls.get(1).select("a").attr("href") mustBe routes.Dashboard.viewMessages(MessagePagination(pageSize = 25)).url
    pageSizeControls.get(2).select("a").attr("href") mustBe routes.Dashboard.viewMessages(MessagePagination(pageSize = 50)).url
    pageSizeControls.get(3).select("a").attr("href") mustBe routes.Dashboard.viewMessages(MessagePagination(pageSize = 100)).url
  }

  it must "have links on the read status and subject to view the message for each message" in {
    val rows = pageWithOneUnreadMessage.select("#messagesTable tbody tr td").asScala

    val message = oneMessage.messages.head

    rows.head.select("a").attr("href") mustBe routes.Dashboard.viewMessage(message.id).url
    rows(1).select("a").attr("href") mustBe routes.Dashboard.viewMessage(message.id).url
  }

  lazy val pageWithOneUnreadMessage: Document = Jsoup.parse(messagesTab(oneMessage, MessagePagination(), 1, 1).toString)

  lazy val clientMessagesPage = Jsoup.parse(messagesTab(clientMessages, MessagePagination(), 1, 1)(agentRequest, implicitly).toString)

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
        agentOrgId = None,
        agentName = None,
        caseReference = "a case ref",
        submissionId = "sub id",
        timestamp = LocalDateTime.now,
        address = "somewhere",
        effectiveDate = LocalDateTime.now,
        subject = "something",
        lastRead = None,
        messageType = "message"
      )
    )
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
      Some(213),
      Some("agent"),
      "aaa",
      "bbb",
      LocalDateTime.now,
      "address",
      LocalDateTime.now,
      "something",
      None,
      "a message"
    ))
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

  lazy val agentRequest: AgentRequest[AnyContentAsEmpty.type] = AgentRequest(
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

  implicit lazy val messages: Messages = Messages.Implicits.applicationMessages
}
