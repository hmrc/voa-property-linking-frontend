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

import base.{HtmlComponentHelpers, ISpecBase}
import com.github.tomakehurst.wiremock.client.WireMock.{equalToJson, postRequestedFor, urlEqualTo, verify}
import models.propertyrepresentation.AgentSummary
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import repositories.ManageAgentSessionRepository
import uk.gov.hmrc.http.{HeaderCarrier, SessionId}

import java.time.LocalDate
import java.util.UUID

trait ListYearsHelpers extends ISpecBase with HtmlComponentHelpers {

  val testSessionId = s"stubbed-${UUID.randomUUID}"
  lazy val mockRepository: ManageAgentSessionRepository = app.injector.instanceOf[ManageAgentSessionRepository]
  implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId(testSessionId)))

  def verifyAppointedListYears(amount: Int, chosenListYear: String): Unit =
    verify(
      amount,
      postRequestedFor(urlEqualTo("/property-linking/my-organisation/agent/submit-appointment-changes"))
        .withRequestBody(
          equalToJson(
            s"""{
               |  "agentRepresentativeCode": 100,
               |  "action": "APPOINT",
               |  "scope": "LIST_YEAR",
               |  "listYears": ["$chosenListYear"]
               |}""".stripMargin
          ))
    )

  def verifyRevokedListYears(amount: Int, chosenListYear: String): Unit =
    verify(
      amount,
      postRequestedFor(urlEqualTo("/property-linking/my-organisation/agent/submit-appointment-changes"))
        .withRequestBody(
          equalToJson(
            s"""{
               |  "agentRepresentativeCode": 100,
               |  "action": "REVOKE",
               |  "scope": "LIST_YEAR",
               |  "listYears": ["$chosenListYear"]
               |}""".stripMargin
          ))
    )

  def setCurrentListYears(listYears: List[String]): Unit =
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
