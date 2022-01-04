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

package actions

import actions.registration.SessionUserDetailsAction
import actions.registration.requests.{RequestWithSessionPersonDetails, RequestWithUserDetails}
import models.registration._
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.ScalaFutures
import play.api.test.FakeRequest
import repositories.PersonalDetailsSessionRepository
import tests.{AllMocks, BaseUnitSpec}
import uk.gov.hmrc.http.HeaderCarrier
import utils.{FakeObjects, GlobalExecutionContext, NoMetricsOneAppPerSuite}

import scala.concurrent.Future

class SessionUserDetailsActionSpec
    extends BaseUnitSpec with ScalaFutures with BeforeAndAfterEach with AllMocks with NoMetricsOneAppPerSuite {

  "SessionUserDetailsAction" should {
    "transform a RequestWithUserDetails into a RequestWithSessionPersonDetails - for Individual" in new Setup {

      val eventualSomeDetails = Future successful Some(individualUserAccountDetails)
      when(mockPersonalDetailsSessionRepository.get[User](any(), any())).thenReturn(eventualSomeDetails)

      implicit val fakeRequest = FakeRequest().withHeaders(("", "")).withSession(("", ""))
      implicit val request = new RequestWithUserDetails(individualUserDetails, fakeRequest)

      val action = new Harness(mockPersonalDetailsSessionRepository)

      val futureResult = action.callTransform(request)

      whenReady(futureResult) { result =>
        inside(result.sessionPersonDetails) {
          case Some(acc: IndividualUserAccountDetails) => acc shouldBe individualUserAccountDetails
        }
      }
    }

    "transform a RequestWithUserDetails into a RequestWithSessionPersonDetails - for AdminInExistingOrganisation" in new Setup {

      val eventualSomeDetails = Future successful Some(adminInExistingOrganisationAccountDetails)
      when(mockPersonalDetailsSessionRepository.get[User](any(), any())).thenReturn(eventualSomeDetails)

      implicit val fakeRequest = FakeRequest().withHeaders(("", "")).withSession(("", ""))
      implicit val request = new RequestWithUserDetails(orgUserDetails, fakeRequest)

      val action = new Harness(mockPersonalDetailsSessionRepository)

      val futureResult = action.callTransform(request)

      whenReady(futureResult) { result =>
        inside(result.sessionPersonDetails) {
          case Some(acc: AdminInExistingOrganisationAccountDetails) =>
            acc shouldBe adminInExistingOrganisationAccountDetails
        }
      }
    }

    "transform a RequestWithUserDetails into a RequestWithSessionPersonDetails - for AdminInNewOrganisation" in new Setup {

      val eventualSomeDetails = Future successful Some(adminOrganisationAccountDetails)
      when(mockPersonalDetailsSessionRepository.get[User](any(), any())).thenReturn(eventualSomeDetails)

      implicit val fakeRequest = FakeRequest().withHeaders(("", "")).withSession(("", ""))
      implicit val request = new RequestWithUserDetails(orgUserDetails, fakeRequest)

      val action = new Harness(mockPersonalDetailsSessionRepository)

      val futureResult = action.callTransform(request)

      whenReady(futureResult) { result =>
        inside(result.sessionPersonDetails) {
          case Some(acc: AdminOrganisationAccountDetails) => acc shouldBe adminOrganisationAccountDetails
        }
      }
    }
  }

  trait Setup extends FakeObjects with GlobalExecutionContext {

    implicit val hc: HeaderCarrier = HeaderCarrier()

    class Harness(personalDetailsSessionRepo: PersonalDetailsSessionRepository)
        extends SessionUserDetailsAction(personalDetailsSessionRepo) {
      def callTransform[A](request: RequestWithUserDetails[A]): Future[RequestWithSessionPersonDetails[A]] =
        transform(request)
    }

  }

}
