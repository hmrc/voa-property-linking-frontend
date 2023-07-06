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
      details = IndividualDetails(firstName = "", lastName = "", email = "", phone1 = "", phone2 = None, addressId = 12)
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
    resultCount = 2,
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
        address = "",
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
  val  testAgentDetails = AgentDetails(
    name = "Test Agent",
    address = ""
  )

}
