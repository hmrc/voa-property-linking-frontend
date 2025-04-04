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

package utils

import auth.Principal
import models.{CanChallengeResponse, _}
import models.assessments.{AssessmentsPageSession, PreviousPage}
import models.attachment._
import models.challenge.ChallengeCaseStatus
import models.challenge.myclients.{ChallengeCaseWithClient, ChallengeCasesWithClient}
import models.challenge.myorganisations.{ChallengeCaseWithAgent, ChallengeCasesWithAgent}
import models.domain.Nino
import models.dvr.cases.check.CheckCaseStatus
import models.dvr.cases.check.common.{Agent, Client}
import models.dvr.cases.check.myclients.{CheckCaseWithClient, CheckCasesWithClient}
import models.dvr.cases.check.myorganisation.{CheckCaseWithAgent, CheckCasesWithAgent}
import models.dvr.cases.check.projection.CaseDetails
import models.properties.{AllowedAction, PropertyHistory, PropertyValuation, ValuationStatus}
import models.propertyrepresentation._
import models.referencedata.ReferenceData
import models.registration._
import models.searchApi._
import models.upscan._
import uk.gov.hmrc.auth.core.{AffinityGroup, ConfidenceLevel, CredentialRole, User}

import java.time.{Instant, LocalDate, LocalDateTime, Month}
import java.util.UUID
import binders.propertylinks.ClaimPropertyReturnToPage
import models.ListType.ListType
import models.dvr.DvrRecord
import models.properties.AllowedAction.{CHECK, VIEW_DETAILED_VALUATION}

trait FakeObjects {

  lazy val clientPropertyLink = ClientPropertyLink(
    authorisationId = 4222211L,
    authorisedPartyId = 1222L,
    status = PropertyLinkingApproved,
    startDate = LocalDate.parse("2019-07-22"),
    endDate = Some(LocalDate.parse("2020-07-12")),
    submissionId = "PLSubId",
    capacity = "OWNER",
    uarn = 999000111L,
    address = "123, SOME ADDRESS, SOME TOWN, EF3 GH4",
    localAuthorityRef = "BAREF",
    client = ClientDetails(10000L, "Some organisation 2")
  )
  lazy val ownerAuthorisation = OwnerAuthorisation(
    authorisationId = 4222211L,
    status = "APPROVED",
    submissionId = "PLSubId",
    uarn = 999000111L,
    address = "123, SOME ADDRESS, SOME TOWN, AB1 CD2",
    localAuthorityRef = "BAREF",
    agents = Seq(ownerAuthAgent)
  )

  lazy val ownerAuthorisationNoAgents = OwnerAuthorisation(
    authorisationId = 2222211L,
    status = "APPROVED",
    submissionId = "PLSubId",
    uarn = 999000111L,
    address = "321, SOME ADDRESS, SOME TOWN, AB1 CD2",
    localAuthorityRef = "BAREF",
    agents = Seq.empty
  )
  lazy val ownerCheckCase = CheckCaseWithAgent(
    checkCaseSubmissionId = "123344",
    checkCaseReference = "CHK10000732",
    checkCaseStatus = CheckCaseStatus.CLOSED,
    address = "CHK-1234",
    uarn = 1000L,
    createdDateTime = LocalDateTime.parse("2020-07-11T17:19:33"),
    settledDate = Some(LocalDate.parse("2020-07-12")),
    agent = Some(agent1),
    submittedBy = "Some other person",
    originatingAssessmentReference = 1L
  )
  lazy val agentCheckCase = new CheckCaseWithClient(
    checkCaseSubmissionId = "123344",
    checkCaseReference = "CHK10000742",
    checkCaseStatus = CheckCaseStatus.OPEN,
    address = "CHK-1234",
    uarn = 1000L,
    createdDateTime = LocalDateTime.parse("2020-07-21T17:19:33"),
    settledDate = Some(LocalDate.parse("2020-07-22")),
    client = Client(10000L, "Some organisation 2"),
    submittedBy = "Some other person 2",
    originatingAssessmentReference = 1L
  )
  lazy val agentChallengeCase = new ChallengeCaseWithClient(
    challengeCaseSubmissionId = "123344",
    challengeCaseReference = "CHG10000742",
    challengeCaseStatus = ChallengeCaseStatus.OPEN,
    address = "CHG-1234",
    uarn = 1000L,
    createdDateTime = LocalDateTime.parse("2020-07-21T17:19:33"),
    settledDate = Some(LocalDate.parse("2020-07-22")),
    client = Client(10000L, "Some organisation 2"),
    submittedBy = "Some other person 2",
    originatingAssessmentReference = 1L
  )
  lazy val ownerChallengeCase = ChallengeCaseWithAgent(
    challengeCaseSubmissionId = "123344",
    challengeCaseReference = "CHG10000732",
    challengeCaseStatus = ChallengeCaseStatus.CLOSED,
    address = "CHG-1234",
    uarn = 1000L,
    createdDateTime = LocalDateTime.parse("2020-07-11T17:19:33"),
    settledDate = Some(LocalDate.parse("2020-07-12")),
    agent = Some(agent1),
    submittedBy = "Some other person",
    originatingAssessmentReference = 1L
  )
  lazy val canChallengeResponse = CanChallengeResponse(result = true, reasonCode = Some("41a"), reason = Some("Ok"))
  lazy val ownerCheckCasesResponse =
    CheckCasesWithAgent(start = 1, size = 15, filterTotal = 1, total = 1, checkCases = List(ownerCheckCase))
  lazy val agentCheckCasesResponse =
    CheckCasesWithClient(start = 1, size = 15, filterTotal = 1, total = 1, checkCases = List(agentCheckCase))
  lazy val ownerAuthResultResponse =
    OwnerAuthResult(start = 1, size = 15, filterTotal = 1, total = 1, authorisations = List(ownerAuthorisation))
  lazy val ownerChallengeCasesResponse =
    ChallengeCasesWithAgent(start = 1, size = 15, filterTotal = 1, total = 1, challengeCases = List(ownerChallengeCase))
  lazy val agentChallengeCasesResponse =
    ChallengeCasesWithClient(
      start = 1,
      size = 15,
      filterTotal = 1,
      total = 1,
      challengeCases = List(agentChallengeCase)
    )
  lazy val assessmentPageSession: AssessmentsPageSession = AssessmentsPageSession(PreviousPage.SelectedClient)
  lazy val appointNewAgentSession: AppointNewAgentSession = SelectedAgent(
    agentCode = agentCode,
    agentOrganisationName = agentDetails.name,
    agentAddress = agentDetails.address,
    isCorrectAgent = true,
    backLink = None,
    ratingLists = Seq.empty
  )
  lazy val april2017 = LocalDate.of(2017, 4, 1)
  lazy val april2023 = LocalDate.of(2023, 4, 1)
  lazy val dvrRecord: DvrRecord = DvrRecord(
    organisationId = orgIdIp,
    assessmentRef = assessmentRef,
    agents = Some(List(orgIdAgent)),
    dvrSubmissionId = Some("DVR-123A45B")
  )
  lazy val ownerChallengeCaseDetails =
    CaseDetails(
      submittedDate = LocalDateTime.parse("2020-07-11T17:19:33"),
      status = "CLOSED",
      caseReference = "CHG10000732",
      closedDate = Some(LocalDate.parse("2020-07-12")),
      clientOrAgent = "Some organisation",
      submittedBy = "Some other person",
      agent = Some(agent1)
    )
  private lazy val agent1: Agent = Agent(10000L, 10000L, "Some organisation")
  val ggExternalId = "gg-ext-id"
  val ggGroupId = "gg-group-id"
  val firstName = "Bob"
  val lastName = "Smith"
  val companyName = "ACME Ltd."
  val postCode = "BN12 6DL"
  val addressLine = "7 The Strand, Worthing, BN12 6DL"
  val address: Address = Address(Some(7L), "The Strand", "Worthing", "", "", postCode)
  val email = "some@email.com"
  val phone = "01293666666"
  val nino: Nino = Nino("AA111111A")
  val ninoString: String = "AA111111A"
  val dateOfBirth: LocalDate = LocalDate.of(1979, Month.OCTOBER, 12)
  val agentCode = 12345L
  val orgIdIp = 321L
  val orgIdAgent = 654L
  val assessmentRef = 987L
  val earliestEnglishStartDate = LocalDate.of(2017, 4, 1)
  val earliestWelshStartDate = LocalDate.of(2023, 4, 1)
  val principal = Principal(ggExternalId, ggGroupId)
  val FILE_REFERENCE: String = "1862956069192540"
  val preparedUpload = PreparedUpload(Reference(FILE_REFERENCE), UploadFormTemplate("http://localhost/upscan", Map()))
  val fileMetadata = FileMetadata(FILE_REFERENCE, "application/pdf")
  val uploadedFileDetails = UploadedFileDetails(fileMetadata, preparedUpload)
  val fileUpscanMetaData: Map[String, UploadedFileDetails] = Map(FILE_REFERENCE -> uploadedFileDetails)
  val attachment = Attachment(
    _id = UUID.randomUUID(),
    initiatedAt = Instant.now(),
    fileName = "fileName",
    mimeType = "image/jpeg",
    destination = "DESTINATION",
    data = Map(),
    state = Initiated,
    history = List(),
    scanResult = None,
    initiateResult = None,
    principal = principal
  )
  val noEvidencelinkBasis: NoEvidenceFlag.type = NoEvidenceFlag
  val uploadRatesBillData: UploadEvidenceData = uploadEvidenceData(fileInfo(RatesBillType))
  val uploadLeaseData: UploadEvidenceData = uploadEvidenceData(fileInfo(Lease))
  val uploadLicenseData: UploadEvidenceData = uploadEvidenceData(fileInfo(License))
  val uploadServiceChargeData: UploadEvidenceData = uploadEvidenceData(fileInfo(ServiceCharge), OtherEvidenceFlag)
  val detailedIndividualAccount =
    DetailedIndividualAccount(
      externalId = ggExternalId,
      trustId = None,
      organisationId = 1L,
      individualId = 2L,
      details = IndividualDetails(firstName = "", lastName = "", email = "", phone1 = "", phone2 = None, addressId = 12)
    )
  val individualUserDetails: UserDetails = userDetails(AffinityGroup.Individual)
  val orgUserDetails: UserDetails = userDetails(AffinityGroup.Organisation)
  val groupAccountDetails = GroupAccountDetails(companyName, address, email, email, phone, isAgent = false)
  val testAccounts = Accounts(groupAccount(agent = true), detailedIndividualAccount)
  val individualUserAccountDetails = IndividualUserAccountDetails(
    firstName = firstName,
    lastName = lastName,
    address = address,
    dob = dateOfBirth,
    nino = nino,
    phone = "03245262782",
    mobilePhone = "04357282921",
    email = email,
    confirmedEmail = email,
    tradingName = Some("Trading name"),
    selectedAddress = None
  )
  val individualUserAccountDetailsUplift: IndividualUserAccountDetailsUplift = IndividualUserAccountDetailsUplift(
    address = address,
    phone = "03245262782",
    mobilePhone = "04357282921",
    email = email,
    confirmedEmail = email,
    tradingName = Some("Trading name"),
    selectedAddress = None
  )
  val adminInExistingOrganisationAccountDetails = AdminInExistingOrganisationAccountDetails(
    firstName = "Billy-Bob",
    lastName = "AdminInExistingOrganisation",
    dob = dateOfBirth,
    nino = nino
  )
  val adminOrganisationAccountDetails = AdminOrganisationAccountDetails(
    firstName = firstName,
    lastName = lastName,
    address = Address(Some(123L), "1 Some street", "", "", "", "BN12 6DL"),
    dob = dateOfBirth,
    nino = nino,
    phone = "03245262782",
    email = email,
    confirmedEmail = email,
    companyName = "Trading name",
    selectedAddress = None,
    isAgent = false
  )
  val agentOrganisation = AgentOrganisation(
    id = 12L,
    representativeCode = Some(agentCode),
    organisationLatestDetail = OrganisationLatestDetail(
      id = 1L,
      addressUnitId = 1L,
      organisationName = "An Org",
      organisationEmailAddress = "some@email.com",
      organisationTelephoneNumber = "0456273893232",
      representativeFlag = true
    ),
    persons = List()
  )
  val startJourney = Start(backLink = None)
  val searchedAgent = SearchedAgent(
    agentCode = agentCode,
    agentOrganisationName = "Some Org",
    agentAddress = "An Address",
    backLink = None
  )
  val selectedAgent = SelectedAgent(
    agentCode = agentCode,
    agentOrganisationName = "Some Org",
    isCorrectAgent = true,
    agentAddress = "An Address",
    backLink = None,
    ratingLists = Seq.empty
  )
  val managingProperty = ManagingProperty(
    agentCode = agentCode,
    agentOrganisationName = "Some Org",
    isCorrectAgent = true,
    managingPropertyChoice = All.name,
    agentAddress = "An Address",
    backLink = None,
    totalPropertySelectionSize = 1,
    propertySelectedSize = 1
  )
  val managingPropertyNoProperties = ManagingProperty(
    agentCode = agentCode,
    agentOrganisationName = "Some Org",
    isCorrectAgent = true,
    managingPropertyChoice = NoProperties.name,
    agentAddress = "An Address",
    backLink = None
  )
  val managingPropertyChooseProperties = ManagingProperty(
    agentCode = agentCode,
    agentOrganisationName = "Some Org",
    isCorrectAgent = true,
    managingPropertyChoice = ChooseFromList.name,
    agentAddress = "An Address",
    backLink = None,
    totalPropertySelectionSize = 1,
    propertySelectedSize = 1
  )
  val ownerAuthAgent = OwnerAuthAgent(
    authorisedPartyId = 12L,
    organisationId = 1L,
    organisationName = "Some Org name",
    agentCode = agentCode
  )
  val ownerAuthAgent2 = OwnerAuthAgent(
    authorisedPartyId = 1222L,
    organisationId = 122L,
    organisationName = "Some Org name 2",
    agentCode = 6754L
  )
  val ownerAuthorisation2 = OwnerAuthorisation(
    authorisationId = 4222212L,
    status = "APPROVED",
    submissionId = "PLSubId2",
    uarn = 999000112L,
    address = "124, SOME ADDRESS, SOME TOWN, AB1 CD2",
    localAuthorityRef = "BAREF2",
    agents = Seq(ownerAuthAgent2)
  )
  val ownerAuthorisationWithNoAgent = OwnerAuthorisation(
    authorisationId = 4222212L,
    status = "APPROVED",
    submissionId = "PLSubId2",
    uarn = 999000112L,
    address = "124, SOME ADDRESS, SOME TOWN, AB1 CD2",
    localAuthorityRef = "BAREF2",
    agents = Seq.empty
  )
  val ownerAuthResultWithOneAuthorisation = OwnerAuthResult(
    start = 1,
    size = 15,
    filterTotal = 1,
    total = 1,
    authorisations = Seq(ownerAuthorisation)
  )
  val ownerAuthResultWithTwoAuthorisation = OwnerAuthResult(
    start = 1,
    size = 15,
    filterTotal = 2,
    total = 2,
    authorisations = Seq(ownerAuthorisation, ownerAuthorisation2)
  )
  val ownerAuthResultWithTwoAuthsAgentAssignedToOne = OwnerAuthResult(
    start = 1,
    size = 15,
    filterTotal = 2,
    total = 2,
    authorisations = Seq(ownerAuthorisation, ownerAuthorisationWithNoAgent)
  )
  val ownerAuthResultWithNoAuthorisations = OwnerAuthResult(
    start = 1,
    size = 15,
    filterTotal = 0,
    total = 0,
    authorisations = Seq.empty
  )
  val ownerAgentsNoAgents = OwnerAgents(agents = Seq.empty)
  val ownerAgent = OwnerAgent(name = "Some Agent Org", ref = agentCode)
  val ownerAgentsWithOneAgent = OwnerAgents(agents = Seq(ownerAgent))
  val agentSummary: AgentSummary = AgentSummary(
    organisationId = 1L,
    representativeCode = 987L,
    name = "Some Agent Org",
    appointedDate = LocalDate.now().minusDays(1),
    propertyCount = 2
  )
  val agentSummary2: AgentSummary = AgentSummary(
    organisationId = 2L,
    representativeCode = 988L,
    name = "Another Agent Org",
    appointedDate = LocalDate.now().minusDays(2),
    propertyCount = 1
  )
  val agentSummary3: AgentSummary = AgentSummary(
    organisationId = 3L,
    representativeCode = 989L,
    name = "A third Agent Org",
    appointedDate = LocalDate.now().minusDays(3),
    propertyCount = 1
  )
  val organisationsAgentsListWithOneAgent: AgentList = AgentList(resultCount = 1, agents = List(agentSummary))
  val organisationsAgentsListWithTwoAgents: AgentList =
    AgentList(resultCount = 2, agents = List(agentSummary, agentSummary2))
  val organisationsAgentsListWithThreeAgents: AgentList =
    AgentList(resultCount = 3, agents = List(agentSummary, agentSummary2, agentSummary3))
  val emptyOrganisationsAgentsList: AgentList = AgentList(resultCount = 0, agents = List.empty)
  val agentDetails: AgentDetails = AgentDetails(name = "Awesome Agent", address = "1 Awesome Street, AB1 1BA")
  val ownerCheckCaseDetails =
    CaseDetails(
      submittedDate = LocalDateTime.parse("2020-07-11T17:19:33"),
      status = "CLOSED",
      caseReference = "CHK10000732",
      closedDate = Some(LocalDate.parse("2020-07-12")),
      clientOrAgent = "Some organisation",
      submittedBy = "Some other person",
      agent = Some(agent1)
    )
  val agentCheckCaseDetails =
    CaseDetails(
      submittedDate = LocalDateTime.parse("2020-07-21T17:19:33"),
      status = "OPEN",
      caseReference = "CHK10000742",
      closedDate = Some(LocalDate.parse("2020-07-22")),
      clientOrAgent = "Some organisation 2",
      submittedBy = "Some other person 2"
    )
  val agentChallengeCaseDetails =
    CaseDetails(
      submittedDate = LocalDateTime.parse("2020-07-21T17:19:33"),
      status = "OPEN",
      caseReference = "CHG10000742",
      closedDate = Some(LocalDate.parse("2020-07-22")),
      clientOrAgent = "Some organisation 2",
      submittedBy = "Some other person 2"
    )
  val propertyValuation1: PropertyValuation = PropertyValuation(
    valuationId = 123,
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
  val propertyValuation2: PropertyValuation = PropertyValuation(
    valuationId = 123,
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
    propertyLinkEarliestStartDate = Some(LocalDate.now().plusYears(1))
  )
  val propertyValuationNoRateable: PropertyValuation = PropertyValuation(
    valuationId = 123,
    valuationStatus = ValuationStatus.CURRENT,
    rateableValue = None,
    scatCode = Some("scatCode"),
    effectiveDate = LocalDate.of(2019, 2, 21),
    currentFromDate = LocalDate.of(2019, 2, 21),
    currentToDate = None,
    listYear = "current",
    primaryDescription = ReferenceData("code", "description"),
    allowedActions = AllowedAction.values.toList,
    listType = ListType.CURRENT,
    propertyLinkEarliestStartDate = Some(LocalDate.now().plusYears(1))
  )
  val testPropertyValuations = Seq(
    propertyValuation1,
    propertyValuation2
  )
  val testPropertyValuationsNoRateableInfo = Seq(
    propertyValuation1,
    propertyValuationNoRateable
  )
  val propertyHistory = new PropertyHistory(
    uarn = 4500,
    addressFull = address.toString,
    localAuthorityCode = "4500",
    localAuthorityReference = "BACODE",
    history = testPropertyValuations,
    allowedActions = List(AllowedAction.PROPERTY_LINK)
  )
  val propertyHistoryNoRatableInfo = new PropertyHistory(
    uarn = 4500,
    addressFull = address.toString,
    localAuthorityCode = "4500",
    localAuthorityReference = "BACODE",
    history = testPropertyValuations,
    allowedActions = List(AllowedAction.PROPERTY_LINK)
  )
  val propertyLinkingSession = LinkingSession(
    address = "LS",
    uarn = 1L,
    submissionId = "PL-123456",
    personId = 1L,
    earliestStartDate = earliestEnglishStartDate,
    propertyRelationship = Some(PropertyRelationship(Owner, 1L)),
    propertyOwnership = Some(PropertyOwnership(fromDate = LocalDate.of(2017, 1, 1))),
    propertyOccupancy = Some(PropertyOccupancy(stillOccupied = false, lastOccupiedDate = None)),
    hasRatesBill = Some(true),
    uploadEvidenceData = UploadEvidenceData.empty,
    evidenceType = Some(RatesBillType),
    clientDetails = Some(ClientDetails(100, "ABC")),
    localAuthorityReference = "12341531531",
    rtp = ClaimPropertyReturnToPage.FMBR,
    isSubmitted = None
  )

  def fileInfo(evType: EvidenceType): CompleteFileInfo = CompleteFileInfo("test.pdf", evType)

  def uploadEvidenceData(fileInfo: FileInfo, flag: LinkBasis = RatesBillFlag): UploadEvidenceData =
    UploadEvidenceData(flag, Some(fileInfo), Some(Map(FILE_REFERENCE -> uploadedFileDetails)))

  def groupAccount(agent: Boolean): GroupAccount =
    GroupAccount(
      id = 1L,
      groupId = ggGroupId,
      companyName = ggExternalId,
      addressId = 1,
      email = email,
      phone = phone,
      isAgent = agent,
      agentCode = Some(300L).filter(_ => agent)
    )

  def userDetails(
        affinityGroup: AffinityGroup = AffinityGroup.Individual,
        credentialRole: CredentialRole = User,
        confidenceLevel: ConfidenceLevel = ConfidenceLevel.L200
  ): UserDetails =
    UserDetails(
      firstName = Some(firstName),
      lastName = Some(lastName),
      email = email,
      postcode = Some(postCode),
      groupIdentifier = ggGroupId,
      affinityGroup = affinityGroup,
      externalId = ggExternalId,
      credentialRole = credentialRole,
      confidenceLevel = confidenceLevel,
      dob = Some(LocalDate.now().minusYears(28)),
      nino = Some(ninoString)
    )

  def clientProperties(a: ClientPropertyLink) =
    ClientPropertyLink(
      authorisationId = a.authorisationId,
      authorisedPartyId = a.authorisedPartyId,
      status = a.status,
      startDate = a.startDate,
      endDate = a.endDate,
      submissionId = a.submissionId,
      capacity = a.capacity,
      uarn = a.uarn,
      address = a.address,
      localAuthorityRef = a.localAuthorityRef,
      client = a.client
    )

  def apiAssessments(
        a: OwnerAuthorisation,
        toDate: Option[LocalDate] = Some(april2017.plusMonths(2L)),
        listType: ListType = ListType.CURRENT,
        listYear: Int = 2017,
        isWelsh: Boolean = false
  ) =
    ApiAssessments(
      authorisationId = a.authorisationId,
      submissionId = a.submissionId,
      uarn = a.uarn,
      address = a.address,
      pending = a.status == "PENDING",
      clientOrgName = None,
      capacity = Some("OWNER"),
      assessments = List(
        ApiAssessment(
          authorisationId = a.authorisationId,
          assessmentRef = 1234L,
          listYear = listYear.toString,
          uarn = a.uarn,
          effectiveDate = Some(april2017),
          rateableValue = Some(123L),
          address = PropertyAddress(Seq(address.line1, address.line2, address.line3, address.line4), address.postcode),
          billingAuthorityReference = a.localAuthorityRef,
          billingAuthorityCode = Some(if (isWelsh) "62715" else "2715"),
          listType = listType,
          allowedActions = List(VIEW_DETAILED_VALUATION, CHECK),
          currentFromDate = Some(april2017),
          currentToDate = toDate
        ),
        ApiAssessment(
          authorisationId = a.authorisationId,
          assessmentRef = 1235L,
          listYear = listYear.toString,
          uarn = a.uarn,
          effectiveDate = Some(april2017),
          rateableValue = Some(1234L),
          address = PropertyAddress(Seq(address.line1, address.line2, address.line3, address.line4), address.postcode),
          billingAuthorityReference = a.localAuthorityRef,
          billingAuthorityCode = Some(if (isWelsh) "62715" else "2715"),
          listType = ListType.CURRENT,
          allowedActions = List(AllowedAction.VIEW_DETAILED_VALUATION),
          currentFromDate = Some(april2017.plusMonths(2L)),
          currentToDate = None
        ),
        ApiAssessment(
          authorisationId = a.authorisationId,
          assessmentRef = 1236L,
          listYear = listYear.toString,
          uarn = a.uarn,
          effectiveDate = Some(april2017),
          rateableValue = Some(1234L),
          address = PropertyAddress(Seq(address.line1, address.line2, address.line3, address.line4), address.postcode),
          billingAuthorityReference = a.localAuthorityRef,
          billingAuthorityCode = Some(if (isWelsh) "62715" else "2715"),
          listType = ListType.CURRENT,
          allowedActions = List(AllowedAction.ENQUIRY),
          currentFromDate = Some(april2017.plusMonths(2L)),
          currentToDate = None
        )
      ),
      agents = a.agents.map(agent =>
        Party(
          authorisedPartyId = agent.authorisedPartyId,
          agentCode = agent.agentCode,
          organisationName = agent.organisationName,
          organisationId = agent.organisationId
        )
      )
    )

  def apiAssessmentsNoRateableInfo(
        a: OwnerAuthorisation,
        toDate: Option[LocalDate] = Some(april2017.plusMonths(2L)),
        listType: ListType = ListType.CURRENT,
        listYear: Int = 2017,
        isWelsh: Boolean = false
  ) =
    ApiAssessments(
      authorisationId = a.authorisationId,
      submissionId = a.submissionId,
      uarn = a.uarn,
      address = a.address,
      pending = a.status == "PENDING",
      clientOrgName = None,
      capacity = Some("OWNER"),
      assessments = List(
        ApiAssessment(
          authorisationId = a.authorisationId,
          assessmentRef = 1234L,
          listYear = listYear.toString,
          uarn = a.uarn,
          effectiveDate = Some(april2017),
          rateableValue = None,
          address = PropertyAddress(Seq(address.line1, address.line2, address.line3, address.line4), address.postcode),
          billingAuthorityReference = a.localAuthorityRef,
          billingAuthorityCode = Some(if (isWelsh) "62715" else "2715"),
          listType = listType,
          allowedActions = List(VIEW_DETAILED_VALUATION, CHECK),
          currentFromDate = Some(april2017),
          currentToDate = toDate
        ),
        ApiAssessment(
          authorisationId = a.authorisationId,
          assessmentRef = 1235L,
          listYear = listYear.toString,
          uarn = a.uarn,
          effectiveDate = Some(april2017),
          rateableValue = Some(1234L),
          address = PropertyAddress(Seq(address.line1, address.line2, address.line3, address.line4), address.postcode),
          billingAuthorityReference = a.localAuthorityRef,
          billingAuthorityCode = Some(if (isWelsh) "62715" else "2715"),
          listType = ListType.CURRENT,
          allowedActions = List(AllowedAction.VIEW_DETAILED_VALUATION),
          currentFromDate = Some(april2017.plusMonths(2L)),
          currentToDate = None
        ),
        ApiAssessment(
          authorisationId = a.authorisationId,
          assessmentRef = 1236L,
          listYear = listYear.toString,
          uarn = a.uarn,
          effectiveDate = Some(april2017),
          rateableValue = Some(1234L),
          address = PropertyAddress(Seq(address.line1, address.line2, address.line3, address.line4), address.postcode),
          billingAuthorityReference = a.localAuthorityRef,
          billingAuthorityCode = Some(if (isWelsh) "62715" else "2715"),
          listType = ListType.CURRENT,
          allowedActions = List(AllowedAction.ENQUIRY),
          currentFromDate = Some(april2017.plusMonths(2L)),
          currentToDate = None
        )
      ),
      agents = a.agents.map(agent =>
        Party(
          authorisedPartyId = agent.authorisedPartyId,
          agentCode = agent.agentCode,
          organisationName = agent.organisationName,
          organisationId = agent.organisationId
        )
      )
    )

  def clientApiAssessments(
        a: ClientPropertyLink,
        toDate: Option[LocalDate] = Some(april2017.plusMonths(2L)),
        listType: ListType = ListType.CURRENT,
        listYear: Int = 2017,
        isWelsh: Boolean = false
  ) =
    ApiAssessments(
      authorisationId = a.authorisationId,
      submissionId = a.submissionId,
      uarn = a.uarn,
      address = a.address,
      pending = a.status == PropertyLinkingPending,
      clientOrgName = Some(a.client.organisationName),
      capacity = Some("OWNER"),
      assessments = List(
        ApiAssessment(
          authorisationId = a.authorisationId,
          assessmentRef = 1234L,
          listYear = listYear.toString,
          uarn = a.uarn,
          effectiveDate = Some(april2017),
          rateableValue = Some(123L),
          address = PropertyAddress(Seq(address.line1, address.line2, address.line3, address.line4), address.postcode),
          billingAuthorityReference = a.localAuthorityRef,
          billingAuthorityCode = Some(if (isWelsh) "62715" else "2715"),
          listType = listType,
          allowedActions = List(VIEW_DETAILED_VALUATION, CHECK),
          currentFromDate = Some(april2017),
          currentToDate = toDate
        ),
        ApiAssessment(
          authorisationId = a.authorisationId,
          assessmentRef = 1235L,
          listYear = listYear.toString,
          uarn = a.uarn,
          effectiveDate = Some(april2017),
          rateableValue = Some(1234L),
          address = PropertyAddress(Seq(address.line1, address.line2, address.line3, address.line4), address.postcode),
          billingAuthorityReference = a.localAuthorityRef,
          billingAuthorityCode = Some(if (isWelsh) "62715" else "2715"),
          listType = ListType.CURRENT,
          allowedActions = List(AllowedAction.VIEW_DETAILED_VALUATION),
          currentFromDate = Some(april2017.plusMonths(2L)),
          currentToDate = None
        ),
        ApiAssessment(
          authorisationId = a.authorisationId,
          assessmentRef = 1236L,
          listYear = listYear.toString,
          uarn = a.uarn,
          effectiveDate = Some(april2017),
          rateableValue = Some(1234L),
          address = PropertyAddress(Seq(address.line1, address.line2, address.line3, address.line4), address.postcode),
          billingAuthorityReference = a.localAuthorityRef,
          billingAuthorityCode = Some(if (isWelsh) "62715" else "2715"),
          listType = ListType.CURRENT,
          allowedActions = List(AllowedAction.ENQUIRY),
          currentFromDate = Some(april2017.plusMonths(2L)),
          currentToDate = None
        )
      ),
      agents = Seq.empty[Party]
    )

  def apiAssessment(a: OwnerAuthorisation) =
    ApiAssessment(
      authorisationId = a.authorisationId,
      assessmentRef = 1L,
      listYear = "2017",
      uarn = a.uarn,
      effectiveDate = Some(april2017),
      rateableValue = Some(65433L),
      address = PropertyAddress(Seq(address.line1, address.line2, address.line3, address.line4), address.postcode),
      billingAuthorityReference = a.localAuthorityRef,
      billingAuthorityCode = Some("2715"),
      listType = ListType.CURRENT,
      allowedActions = List(AllowedAction.VIEW_DETAILED_VALUATION),
      currentFromDate = Some(april2017.plusMonths(2L)),
      currentToDate = None
    )

}
