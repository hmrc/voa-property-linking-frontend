/*
 * Copyright 2022 HM Revenue & Customs
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
import services.email.EmailService
import uk.gov.hmrc.http.HeaderCarrier
import utils._

import scala.concurrent.Future

class RegistrationServiceSpec extends ServiceSpec {

  val mockEmailService: EmailService = {
    val m = mock[EmailService]
    when(m.sendNewRegistrationSuccess(any(), any(), any(), any())(any(), any())).thenReturn(Future.successful(()))
    m
  }

  trait TestCase {
    protected val mockEnrolmentService: EnrolmentService = mock[EnrolmentService]

    protected val registrationService = new RegistrationService(
      groupAccounts = StubGroupAccountConnector,
      individualAccounts = StubIndividualAccountConnector,
      enrolmentService = mockEnrolmentService,
      addresses = StubAddresses,
      emailService = mockEmailService,
      config = app.injector.instanceOf[ApplicationConfig],
      personalDetailsSessionRepo = mockSessionRepository
    )
  }

  "create" should {
    "return EnrolmentSuccess when ivEnrolmentEnabled flag is true" in new TestCase {
      when(mockEnrolmentService.enrol(any(), any())(any(), any())).thenReturn(Future.successful(Success))
      StubIndividualAccountConnector.stubAccount(
        account = DetailedIndividualAccount(
          externalId = ggExternalId,
          trustId = None,
          organisationId = 1L,
          individualId = 2L,
          details =
            IndividualDetails(firstName = "", lastName = "", email = "", phone1 = "", phone2 = None, addressId = 12)
        ))
      implicit val headerCarrier: HeaderCarrier = HeaderCarrier()

      val res: Future[RegistrationResult] = registrationService.create(
        groupAccountDetails,
        userDetails()
      )(
        _ =>
          _ =>
            opt =>
              IndividualAccountSubmission(
                externalId = "",
                trustId = None,
                organisationId = opt,
                details = IndividualDetails(
                  firstName = "",
                  lastName = "",
                  email = "",
                  phone1 = "",
                  phone2 = None,
                  addressId = 12)))

      res.futureValue shouldBe RegistrationSuccess(2L)
      verify(mockEmailService).sendNewRegistrationSuccess(any(), any(), any(), any())(any(), any())
    }
  }

}
