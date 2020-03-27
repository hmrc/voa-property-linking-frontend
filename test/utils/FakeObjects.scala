/*
 * Copyright 2020 HM Revenue & Customs
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

import java.time.{Instant, LocalDate, Month}
import java.util.UUID

import auth.Principal
import models._
import models.attachment._
import models.domain.Nino
import models.propertyrepresentation.{AgentDetails, AgentList, AgentOrganisation, AgentSummary, AppointNewAgentSession, ManagingProperty, OrganisationLatestDetail, SearchedAgent, SelectedAgent}
import models.registration._
import models.searchApi.{OwnerAgent, OwnerAgents, OwnerAuthAgent, OwnerAuthResult, OwnerAuthorisation}
import models.upscan._
import uk.gov.hmrc.auth.core.{AffinityGroup, CredentialRole, User}

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
    DetailedIndividualAccount(ggExternalId, "", 1L, 2L, IndividualDetails("", "", "", "", None, 12))
  val individualUserDetails: UserDetails = userDetails(AffinityGroup.Individual)
  val orgUserDetails: UserDetails = userDetails(AffinityGroup.Organisation)

  def groupAccount(agent: Boolean): GroupAccount =
    GroupAccount(1L, ggGroupId, ggExternalId, 1, email, phone, isAgent = agent, Some(300L).filter(_ => agent))

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

  val ownerAuthorisation = OwnerAuthorisation(
    authorisationId = 4222211L,
    submissionId = "PLSubId",
    uarn = 999000111L,
    address = "123, Some address",
    localAuthorityRef = "BAREF",
    agents = Seq(ownerAuthAgent))
  val ownerAuthorisation2 = OwnerAuthorisation(
    authorisationId = 4222212L,
    submissionId = "PLSubId2",
    uarn = 999000112L,
    address = "124, Some address",
    localAuthorityRef = "BAREF2",
    agents = Seq(ownerAuthAgent2))
  val ownerAuthorisationWithNoAgent = OwnerAuthorisation(
    authorisationId = 4222212L,
    submissionId = "PLSubId2",
    uarn = 999000112L,
    address = "124, Some address",
    localAuthorityRef = "BAREF2",
    agents = Seq.empty)

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

}
