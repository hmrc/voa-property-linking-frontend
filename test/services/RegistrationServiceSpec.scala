/*
 * Copyright 2018 HM Revenue & Customs
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

package services

import config.ApplicationConfig
import models._
import models.registration._
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import resources.{shortString, _}
import uk.gov.hmrc.auth.core.AffinityGroup.Individual
import uk.gov.hmrc.auth.core.{AffinityGroup, User}
import uk.gov.hmrc.http.HeaderCarrier
import utils._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class RegistrationServiceSpec extends ServiceSpec {

  "create" should "return EnrolmentSuccess when ivEnrolmentEnabled flag is true" in new TestCase {

    when(mockEnrolmentService.enrol(any(), any())(any(), any())).thenReturn(Future.successful(Success))
    StubIndividualAccountConnector.stubAccount(DetailedIndividualAccount(externalId, "", 1l, 2l, IndividualDetails("", "", "", "", None, 12)))

    val res: Future[RegistrationResult] = registrationService
      .create(
        GroupAccountDetails("", Address(None, "", "", "", "", ""), "", "", "", false),
        UserDetails("", UserInfo(None, None, "", None, "", "", Individual, User))
      )(userDetails => int => opt => IndividualAccountSubmission("", "", opt, IndividualDetails("", "", "", "", None, 12)))

    res.futureValue must be(EnrolmentSuccess(2L))
  }

  trait TestCase {
    val (groupId, externalId): (String, String) = (shortString, shortString)
    StubVplAuthConnector.stubGroupId(groupId)
    StubVplAuthConnector.stubExternalId(externalId)
    StubVplAuthConnector.stubUserDetails(externalId, testOrganisationInfo)

    implicit val hc = HeaderCarrier()

    def testOrganisationInfo = UserInfo(firstName = Some("Bob"),
      lastName = Some("Smith"),
      email = "bob@smith.com",
      postcode = Some("AB12 3CD"),
      groupIdentifier = "GroupIdenfifier",
      affinityGroup = AffinityGroup.Organisation,
      gatewayId = "",
      credentialRole = User)

    protected val mockEnrolmentService = mock[EnrolmentService]

    protected val registrationService = new RegistrationService(
      groupAccounts = StubGroupAccountConnector,
      individualAccounts = StubIndividualAccountConnector,
      enrolmentService = mockEnrolmentService,
      auth = StubVplAuthConnector,
      addresses = StubAddresses,
      StubEmailService,
      app.injector.instanceOf[ApplicationConfig]
    )
  }
}
