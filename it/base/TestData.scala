package base

import models.propertyrepresentation.{AgentDetails, AgentList, AgentSummary}
import models.searchApi.{OwnerAuthAgent, OwnerAuthResult, OwnerAuthorisation}
import models.{Accounts, DetailedIndividualAccount, GroupAccount, IndividualDetails}
import java.time.LocalDate

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
      details = IndividualDetails(firstName = "", lastName = "", email = "individual@test.com", phone1 = "", phone2 = None, addressId = 12)
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
      )))
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
      )))
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
  val  testAgentDetails = AgentDetails(
    name = "Test Agent",
    address = ""
  )

}
