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

package services.email

import connectors.email.EmailConnector
import controllers.PayLoad
import models.{DetailedIndividualAccount, GroupAccount, IndividualDetails}
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.scalatestplus.mockito.MockitoSugar
import play.api.http.Status.OK
import play.api.libs.json.Writes
import play.api.test.{DefaultAwaitTimeout, Helpers}
import tests.BaseUnitSpec
import uk.gov.hmrc.auth.core.AffinityGroup.Organisation
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpReads, HttpResponse}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import utils.HttpResponseUtils

import scala.concurrent.Future

class EmailServiceSpec extends BaseUnitSpec with MockitoSugar with HttpResponseUtils with DefaultAwaitTimeout {

  private implicit val headerCarrier = HeaderCarrier()
  val config = mock[ServicesConfig]

  "EmailService" should {

    "send new enrolment success email" in {
      val mockWSHttp = mock[HttpClient]
      val emailConnector = new EmailConnector(config, mockWSHttp)
      val emailService = new EmailService(emailConnector)

      val groupAccount =
        GroupAccount(
          id = 123L,
          groupId = "groupId",
          companyName = "companyName",
          addressId = 221L,
          email = "email@email.com",
          phone = "01234567889",
          isAgent = true,
          agentCode = Some(12345678L))

      when(config.baseUrl("email")).thenReturn("http://blah:2909/")

      when(
        mockWSHttp.POST[PayLoad, HttpResponse](anyString(), any[PayLoad](), any())(
          any[Writes[PayLoad]](),
          any[HttpReads[HttpResponse]](),
          any[HeaderCarrier](),
          any()))
        .thenReturn(Future.successful(emptyJsonHttpResponse(OK)))

      val individualDetails =
        IndividualDetails(
          firstName = "firstName",
          lastName = "lastName",
          email = "email@email.com",
          phone1 = "012345567788",
          phone2 = None,
          addressId = 12345L)

      Helpers.await(
        emailService.sendNewRegistrationSuccess(
          "toAddress@email.com",
          DetailedIndividualAccount(
            externalId = "externalId",
            trustId = Some("trustId"),
            organisationId = 123L,
            individualId = 234L,
            details = individualDetails),
          Some(groupAccount),
          Some(Organisation)
        ))

      verify(mockWSHttp)
        .POST(any, any, any)(any[Writes[PayLoad]](), any[HttpReads[HttpResponse]](), any[HeaderCarrier](), any())
    }
  }

}
