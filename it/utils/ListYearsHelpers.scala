package utils

import base.{HtmlComponentHelpers, ISpecBase}
import com.github.tomakehurst.wiremock.client.WireMock.{equalToJson, postRequestedFor, urlEqualTo, verify}
import models.propertyrepresentation.AgentSummary
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import repositories.ManageAgentSessionRepository
import uk.gov.hmrc.http.{HeaderCarrier, SessionId}

import java.time.LocalDate
import java.util.UUID

trait ListYearsHelpers extends ISpecBase with HtmlComponentHelpers{

  val testSessionId = s"stubbed-${UUID.randomUUID}"
  lazy val mockRepository: ManageAgentSessionRepository = app.injector.instanceOf[ManageAgentSessionRepository]
  implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId(testSessionId)))


  def verifyAppointedListYears(amount: Int, chosenListYear: String): Unit = {
    verify(amount, postRequestedFor(urlEqualTo("/property-linking/my-organisation/agent/submit-appointment-changes"))
      .withRequestBody(equalToJson(
        s"""{
           |  "agentRepresentativeCode": 100,
           |  "action": "APPOINT",
           |  "scope": "LIST_YEAR",
           |  "listYears": ["$chosenListYear"]
           |}""".stripMargin
      )))
  }

  def verifyRevokedListYears(amount: Int, chosenListYear: String): Unit = {
    verify(amount, postRequestedFor(urlEqualTo("/property-linking/my-organisation/agent/submit-appointment-changes"))
      .withRequestBody(equalToJson(
        s"""{
           |  "agentRepresentativeCode": 100,
           |  "action": "REVOKE",
           |  "scope": "LIST_YEAR",
           |  "listYears": ["$chosenListYear"]
           |}""".stripMargin
      )))
  }

  def verifyAllPropertiesAppointed(amount: Int, chosenListYear: String): Unit = {
    verify(amount, postRequestedFor(urlEqualTo("/property-linking/my-organisation/agent/submit-appointment-changes"))
      .withRequestBody(equalToJson(
        s"""{
           |  "agentRepresentativeCode": 100,
           |  "action": "APPOINT",
           |  "scope": "LIST_YEAR",
           |  "listYears": ["$chosenListYear"]
           |}""".stripMargin
      )))
  }

  def setCurrentListYears(listYears: List[String]): Unit = {
    await(
      mockRepository.saveOrUpdate(
        AgentSummary(
          listYears = Some(listYears),
          name = "Test Agent",
          organisationId = 100L,
          representativeCode = 100L,
          appointedDate = LocalDate.now(),
          propertyCount = 1
        )))
  }

}
