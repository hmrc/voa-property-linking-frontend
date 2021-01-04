/*
 * Copyright 2021 HM Revenue & Customs
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
import models._
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
import models.propertyrepresentation._
import models.registration._
import models.searchApi._
import models.upscan._
import uk.gov.hmrc.auth.core.{AffinityGroup, CredentialRole, User}

import java.time.{Instant, LocalDate, LocalDateTime, Month}
import java.util.UUID

trait FakeObjects {

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
  val dateOfBirth: LocalDate = LocalDate.of(1979, Month.OCTOBER, 12)
  val agentCode = 12345L

  val principal = Principal(ggExternalId, ggGroupId)
  val FILE_REFERENCE: String = "1862956069192540"
  val preparedUpload = PreparedUpload(Reference(FILE_REFERENCE), UploadFormTemplate("http://localhost/upscan", Map()))
  val fileMetadata = FileMetadata(FILE_REFERENCE, "application/pdf")
  val uploadedFileDetails = UploadedFileDetails(fileMetadata, preparedUpload)
  val fileUpscanMetaData: Map[String, UploadedFileDetails] = Map(FILE_REFERENCE -> uploadedFileDetails)
  val attachment = Attachment(
    UUID.randomUUID(),
    Instant.now(),
    "fileName",
    "image/jpeg",
    "DESTINATION",
    Map(),
    Initiated,
    List(),
    None,
    None,
    principal)
  val noEvidencelinkBasis: NoEvidenceFlag.type = NoEvidenceFlag
  val fileInfo = FileInfo("test.pdf", Some(RatesBillType))
  val uploadEvidenceData =
    UploadEvidenceData(RatesBillFlag, Some(fileInfo), Some(Map(FILE_REFERENCE -> uploadedFileDetails)))
  val detailedIndividualAccount =
    DetailedIndividualAccount(
      ggExternalId,
      "",
      1L,
      2L,
      IndividualDetails(firstName = "", lastName = "", email = "", phone1 = "", phone2 = None, addressId = 12))
  val individualUserDetails: UserDetails = userDetails(AffinityGroup.Individual)
  val orgUserDetails: UserDetails = userDetails(AffinityGroup.Organisation)

  def groupAccount(agent: Boolean): GroupAccount =
    GroupAccount(
      id = 1L,
      groupId = ggGroupId,
      companyName = ggExternalId,
      addressId = 1,
      email = email,
      phone = phone,
      isAgent = agent,
      agentCode = Some(300L).filter(_ => agent))

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

  val adminInExistingOrganisationAccountDetails = AdminInExistingOrganisationAccountDetails(
    firstName = "Billy-Bob",
    lastName = "AdminInExistingOrganisation",
    dob = dateOfBirth,
    nino = nino)

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
    isAgent = Some(false)
  )

  def userDetails(
        affinityGroup: AffinityGroup = AffinityGroup.Individual,
        credentialRole: CredentialRole = User): UserDetails =
    UserDetails(
      firstName = Some(firstName),
      lastName = Some(lastName),
      email = email,
      postcode = Some(postCode),
      groupIdentifier = ggGroupId,
      affinityGroup = affinityGroup,
      externalId = ggExternalId,
      credentialRole = credentialRole
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

  val startJourney = Start()

  val searchedAgent = SearchedAgent(
    agentCode = agentCode,
    agentOrganisationName = "Some Org",
    agentAddress = "An Address"
  )

  val selectedAgent = SelectedAgent(
    agentCode = agentCode,
    agentOrganisationName = "Some Org",
    isCorrectAgent = true,
    agentAddress = "An Address"
  )

  val managingProperty = ManagingProperty(
    agentCode = agentCode,
    agentOrganisationName = "Some Org",
    isCorrectAgent = true,
    managingPropertyChoice = "All properties",
    agentAddress = "An Address"
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

  lazy val ownerAuthorisation = OwnerAuthorisation(
    authorisationId = 4222211L,
    status = "APPROVED",
    submissionId = "PLSubId",
    uarn = 999000111L,
    address = "123, Some address",
    localAuthorityRef = "BAREF",
    agents = Seq(ownerAuthAgent)
  )
  val ownerAuthorisation2 = OwnerAuthorisation(
    authorisationId = 4222212L,
    status = "APPROVED",
    submissionId = "PLSubId2",
    uarn = 999000112L,
    address = "124, Some address",
    localAuthorityRef = "BAREF2",
    agents = Seq(ownerAuthAgent2)
  )
  val ownerAuthorisationWithNoAgent = OwnerAuthorisation(
    authorisationId = 4222212L,
    status = "APPROVED",
    submissionId = "PLSubId2",
    uarn = 999000112L,
    address = "124, Some address",
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

  val agentSummary = AgentSummary(
    organisationId = 1L,
    representativeCode = 987L,
    name = "Some Agent Org",
    appointedDate = LocalDate.now().minusDays(1),
    propertyCount = 2
  )

  val organisationsAgentsList = AgentList(resultCount = 1, agents = List(agentSummary))
  val emptyOrganisationsAgentsList = AgentList(resultCount = 0, agents = List.empty)

  val agentDetails = AgentDetails(name = "Awesome Agent", address = "1 Awesome Street, AB1 1BA")

  val ownerCheckCaseDetails =
    CaseDetails(
      submittedDate = LocalDateTime.parse("2020-07-11T17:19:33"),
      status = "CLOSED",
      caseReference = "CHK10000732",
      closedDate = Some(LocalDate.parse("2020-07-12")),
      clientOrAgent = "Some organisation",
      submittedBy = "Some other person"
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

  val ownerChallengeCaseDetails =
    CaseDetails(
      submittedDate = LocalDateTime.parse("2020-07-11T17:19:33"),
      status = "CLOSED",
      caseReference = "CHG10000732",
      closedDate = Some(LocalDate.parse("2020-07-12")),
      clientOrAgent = "Some organisation",
      submittedBy = "Some other person"
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

  lazy val ownerCheckCase = CheckCaseWithAgent(
    checkCaseSubmissionId = "123344",
    checkCaseReference = "CHK10000732",
    checkCaseStatus = CheckCaseStatus.CLOSED,
    address = "CHK-1234",
    uarn = 1000L,
    createdDateTime = LocalDateTime.parse("2020-07-11T17:19:33"),
    settledDate = Some(LocalDate.parse("2020-07-12")),
    agent = Some(Agent(10000L, 10000L, "Some organisation")),
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
    agent = Some(Agent(10000L, 10000L, "Some organisation")),
    submittedBy = "Some other person",
    originatingAssessmentReference = 1L
  )

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
      challengeCases = List(agentChallengeCase))

  lazy val assessmentPageSession: AssessmentsPageSession = AssessmentsPageSession(PreviousPage.SelectedClient)
  lazy val appointNewAgentSession: AppointNewAgentSession = SelectedAgent(
    agentCode = agentCode,
    agentOrganisationName = agentDetails.name,
    agentAddress = agentDetails.address,
    isCorrectAgent = true)

  lazy val april2017 = LocalDate.of(2017, 4, 1)

  def apiAssessments(a: OwnerAuthorisation) =
    ApiAssessments(
      authorisationId = a.authorisationId,
      submissionId = a.submissionId,
      uarn = a.uarn,
      address = a.address,
      pending = a.status == "PENDING",
      capacity = Some("OWNER"),
      assessments = List(
        ApiAssessment(
          authorisationId = a.authorisationId,
          assessmentRef = 1234L,
          listYear = "2017",
          uarn = a.uarn,
          effectiveDate = Some(april2017),
          rateableValue = Some(123L),
          address = PropertyAddress(Seq(address.line1, address.line2, address.line3, address.line4), address.postcode),
          billingAuthorityReference = a.localAuthorityRef,
          listType = ListType.CURRENT,
          currentFromDate = Some(april2017),
          currentToDate = Some(april2017.plusMonths(2L))
        ),
        ApiAssessment(
          authorisationId = a.authorisationId,
          assessmentRef = 1235L,
          listYear = "2017",
          uarn = a.uarn,
          effectiveDate = Some(april2017),
          rateableValue = Some(1234L),
          address = PropertyAddress(Seq(address.line1, address.line2, address.line3, address.line4), address.postcode),
          billingAuthorityReference = a.localAuthorityRef,
          listType = ListType.CURRENT,
          currentFromDate = Some(april2017.plusMonths(2L)),
          currentToDate = None
        )
      ),
      agents = a.agents.map(
        agent =>
          Party(
            authorisedPartyId = agent.authorisedPartyId,
            agentCode = agent.agentCode,
            organisationName = agent.organisationName,
            organisationId = agent.organisationId
        ))
    )

}
