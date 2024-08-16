/*
 * Copyright 2024 HM Revenue & Customs
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

package base

import models.challenge.myclients.ChallengeCasesWithClient
import models.dvr.cases.check.myclients.CheckCasesWithClient
import models.dvr.documents.{Document, DocumentSummary, DvrDocumentFiles}
import models.properties.{AllowedAction, PropertyHistory, PropertyValuation, ValuationStatus}
import models.propertyrepresentation.{AgentDetails, AgentList, AgentSummary}
import models.referencedata.ReferenceData
import models.searchApi.{OwnerAuthAgent, OwnerAuthResult, OwnerAuthorisation}
import models.{Accounts, ApiAssessment, ApiAssessments, DetailedIndividualAccount, GroupAccount, IndividualDetails, ListType, Party, PropertyAddress}

import java.time.LocalDate
import java.time.LocalDateTime.now

trait TestData {

  val ggGroupId = "gg-group-id"
  val ggExternalId = "gg-ext-id"
  val email = "some@email.com"
  val phone = "01293666666"

  val detailedIndividualAccount: DetailedIndividualAccount =
    DetailedIndividualAccount(
      externalId = ggExternalId,
      trustId = None,
      organisationId = 1L,
      individualId = 2L,
      details = IndividualDetails(
        firstName = "",
        lastName = "",
        email = "individual@test.com",
        phone1 = "",
        phone2 = None,
        addressId = 12)
    )

  def submissionId: String = ""
  def uarn: Long = 1L
  def valuationId: Long = 1L
  def propertyLinkId: Long = 1L

  lazy val april2017: LocalDate = LocalDate.of(2017, 4, 1)
  lazy val april2023: LocalDate = LocalDate.of(2023, 4, 1)
  val localAuthorityRef = "AMBER-VALLEY-REF"

  val ownerAuthAgent = Party(
    authorisedPartyId = 12L,
    organisationId = 1L,
    organisationName = "Some Org name",
    agentCode = 6754L
  )

  val ownerAuthAgent2 = Party(
    authorisedPartyId = 1222L,
    organisationId = 122L,
    organisationName = "Some Org name 2",
    agentCode = 6754L
  )

  val apiAssessment: ApiAssessment = ApiAssessment(
    authorisationId = propertyLinkId,
    assessmentRef = valuationId,
    listYear = "2017",
    uarn = uarn,
    effectiveDate = Some(april2017),
    rateableValue = Some(123L),
    address = PropertyAddress(Seq("7", "The Strand", "Worthing", "", ""), "BN12 6DL"),
    billingAuthorityReference = localAuthorityRef,
    billingAuthorityCode = Some("2715"),
    listType = ListType.CURRENT,
    allowedActions = List(AllowedAction.VIEW_DETAILED_VALUATION, AllowedAction.CHECK),
    currentFromDate = Some(april2017),
    currentToDate = Some(april2017.plusMonths(2L))
  )

  lazy val currentApiAssessment: ApiAssessment = apiAssessment.copy(
    listYear = "2023",
    effectiveDate = Some(april2023),
    currentToDate = None
  )

  lazy val previousApiAssessment: ApiAssessment = apiAssessment.copy(
    listType = ListType.PREVIOUS,
    assessmentRef = valuationId
  )

  val testApiAssessments = ApiAssessments(
    authorisationId = propertyLinkId,
    submissionId = submissionId,
    uarn = uarn,
    address = "ADDRESS",
    pending = false,
    clientOrgName = None,
    capacity = Some("OWNER"),
    assessments = List(currentApiAssessment, previousApiAssessment),
    Seq(ownerAuthAgent, ownerAuthAgent2)
  )

  val testApiAssessmentsNoCheck = ApiAssessments(
    authorisationId = propertyLinkId,
    submissionId = submissionId,
    uarn = uarn,
    address = "ADDRESS",
    pending = false,
    clientOrgName = None,
    capacity = Some("OWNER"),
    assessments = List(currentApiAssessmentNoCheck, previousApiAssessment),
    Seq(ownerAuthAgent, ownerAuthAgent2)
  )

  lazy val currentApiAssessmentNoCheck: ApiAssessment = apiAssessment.copy(
    listYear = "2023",
    effectiveDate = Some(april2023),
    currentToDate = None,
    allowedActions = List(AllowedAction.VIEW_DETAILED_VALUATION)
  )

  def groupAccount(agent: Boolean): GroupAccount =
    GroupAccount(
      id = 1L,
      groupId = ggGroupId,
      companyName = ggExternalId,
      addressId = 1,
      email = email,
      phone = phone,
      isAgent = agent,
      agentCode = Some(100L).filter(_ => agent))

  val testAccounts: Accounts = Accounts(groupAccount(agent = true), detailedIndividualAccount)

  val testIpAccounts: Accounts = Accounts(groupAccount(agent = false), detailedIndividualAccount)

  val placeForData = None

  val testAgentList = AgentList(
    resultCount = 3,
    agents = List(
      AgentSummary(
        organisationId = 1,
        representativeCode = 1001,
        name = "Test Agent",
        appointedDate = LocalDate.parse("2023-01-01"),
        propertyCount = 10,
        listYears = Some(Seq("2017", "2023"))
      ),
      AgentSummary(
        organisationId = 2,
        representativeCode = 1002,
        name = "Test Agent2",
        appointedDate = LocalDate.parse("2023-02-01"),
        propertyCount = 5,
        listYears = Some(Seq("2023"))
      ),
      AgentSummary(
        organisationId = 3,
        representativeCode = 1003,
        name = "Test Agent3",
        appointedDate = LocalDate.parse("2023-03-01"),
        propertyCount = 5,
        listYears = Some(Seq("2017"))
      )
    )
  )

  val testAgentNoListYears = AgentList(
    resultCount = 1,
    agents = List(
      AgentSummary(
        organisationId = 1,
        representativeCode = 1001,
        name = "Test Agent",
        appointedDate = LocalDate.parse("2023-01-01"),
        propertyCount = 0,
        listYears = None)
    )
  )

  val testAgentListFor2023 = AgentList(
    resultCount = 1,
    agents = List(
      AgentSummary(
        organisationId = 1,
        representativeCode = 1001,
        name = "Test Agent",
        appointedDate = LocalDate.parse("2023-01-01"),
        propertyCount = 0,
        listYears = Some(Seq("2023"))
      ))
  )
  val testAgentListFor2017 = AgentList(
    resultCount = 1,
    agents = List(
      AgentSummary(
        organisationId = 1,
        representativeCode = 1001,
        name = "Test Agent",
        appointedDate = LocalDate.parse("2023-01-01"),
        propertyCount = 1,
        listYears = Some(Seq("2017"))
      ))
  )

  val testAgentListForBoth = AgentList(
    resultCount = 1,
    agents = List(
      AgentSummary(
        organisationId = 1,
        representativeCode = 1001,
        name = "Test Agent",
        appointedDate = LocalDate.parse("2023-01-01"),
        propertyCount = 1,
        listYears = Some(Seq("2017", "2023"))
      ))
  )

  val noAgentsList = AgentList(resultCount = 0, agents = List())

  val testResultCount = 2
  val testOwnerAuthResult = OwnerAuthResult(
    start = 0,
    size = 1,
    filterTotal = 2,
    total = 10,
    authorisations = Seq(
      OwnerAuthorisation(
        authorisationId = 1,
        status = "",
        submissionId = "",
        uarn = 1L,
        address = "Test Address",
        localAuthorityRef = "",
        agents = Seq(
          OwnerAuthAgent(
            authorisedPartyId = 1L,
            organisationId = 1L,
            organisationName = "Test Agent",
            agentCode = 1001
          )
        )
      )
    )
  )
  val testOwnerAuthResult1 = OwnerAuthResult(
    start = 0,
    size = 1,
    filterTotal = 1,
    total = 1,
    authorisations = Seq(
      OwnerAuthorisation(
        authorisationId = 1,
        status = "",
        submissionId = "",
        uarn = 1L,
        address = "Test Address",
        localAuthorityRef = "",
        agents = Seq(
          OwnerAuthAgent(
            authorisedPartyId = 1L,
            organisationId = 1L,
            organisationName = "Test Agent",
            agentCode = 1001
          )
        )
      )
    )
  )
  val testOwnerAuthResultMultipleProperty = OwnerAuthResult(
    start = 0,
    size = 1,
    filterTotal = 2,
    total = 10,
    authorisations = Seq(
      OwnerAuthorisation(
        authorisationId = 1,
        status = "",
        submissionId = "",
        uarn = 1L,
        address = "Test Address",
        localAuthorityRef = "",
        agents = Seq(
          OwnerAuthAgent(
            authorisedPartyId = 1L,
            organisationId = 1L,
            organisationName = "Test Agent",
            agentCode = 1001
          )
        )
      ),
      OwnerAuthorisation(
        authorisationId = 2,
        status = "",
        submissionId = "",
        uarn = 2L,
        address = "AddressTest 2",
        localAuthorityRef = "",
        agents = Seq(
          OwnerAuthAgent(
            authorisedPartyId = 2L,
            organisationId = 2L,
            organisationName = "Test Agent 2",
            agentCode = 1002
          )
        )
      )
    )
  )
  val testOwnerAuthResultNoProperties = OwnerAuthResult(
    start = 0,
    size = 0,
    filterTotal = 0,
    total = 0,
    authorisations = Seq.empty
  )
  val testAgentDetails = AgentDetails(
    name = "Test Agent",
    address = ""
  )

  val testPropertyValuation: PropertyValuation = PropertyValuation(
    valuationId = 10007980,
    valuationStatus = ValuationStatus.CURRENT,
    rateableValue = Some(BigDecimal(1000)),
    scatCode = Some("scatCode"),
    effectiveDate = LocalDate.of(2019, 2, 21),
    currentFromDate = LocalDate.of(2019, 2, 21),
    currentToDate = None,
    listYear = "current",
    primaryDescription = ReferenceData("code", "description"),
    allowedActions = AllowedAction.values.toList,
    listType = ListType.CURRENT,
    propertyLinkEarliestStartDate = None
  )

  val testPropertyHistory = new PropertyHistory(
    uarn = 2198480000L,
    addressFull = "Test Address, Test Lane, T35 T3R",
    localAuthorityCode = "4500",
    localAuthorityReference = "2050466366770",
    history = Seq(testPropertyValuation),
    allowedActions = List(AllowedAction.PROPERTY_LINK)
  )

  val ownerAuthResultWithOneAuthorisation = OwnerAuthResult(
    start = 1,
    size = 15,
    filterTotal = 1,
    total = 1,
    authorisations = Seq(
      OwnerAuthorisation(
        authorisationId = 4222211L,
        status = "APPROVED",
        submissionId = "PLSubId",
        uarn = 999000111L,
        address = "123, SOME ADDRESS, SOME TOWN, AB1 CD2",
        localAuthorityRef = "BAREF",
        agents = Seq(
          OwnerAuthAgent(
            authorisedPartyId = 12L,
            organisationId = 1L,
            organisationName = "Some Org name",
            agentCode = 1001
          ))
      ))
  )

  val someDvrDocumentFiles: Some[DvrDocumentFiles] = Some(
    DvrDocumentFiles(
      checkForm = Document(DocumentSummary("1L", "Check Document", now)),
      detailedValuation = Document(DocumentSummary("2L", "Detailed Valuation Document", now))
    ))

  val testCheckCasesWithClient = CheckCasesWithClient(1, 100, 0, 0, List())

  val testChallengeCasesWithClient = ChallengeCasesWithClient(1, 100, 0, 0, List())

}
