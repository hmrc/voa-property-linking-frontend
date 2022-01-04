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

package actions.agentrelationship

import actions.agentrelationship.request.AppointAgentSessionRequest
import actions.requests.BasicAuthenticatedRequest
import models.propertyrepresentation.AppointNewAgentSession
import models.{DetailedIndividualAccount, GroupAccount}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import play.api.libs.json.{JsSuccess, Json, OFormat}
import play.api.mvc.{AnyContent, Result, Results}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.twirl.api.Html
import tests.{AllMocks, BaseUnitSpec}

import scala.concurrent.Future

class WithAppointAgentSessionRefinerSpec extends BaseUnitSpec with MockitoSugar with BeforeAndAfterEach with AllMocks {

  trait Setup {
    val fakeRequest = FakeRequest()

    val refiner = new WithAppointAgentSessionRefiner(mockCustomErrorHandler, mockSessionRepository)

    val grpAccount: GroupAccount = groupAccount(true)
    val individualAccount = detailedIndividualAccount

    when(mockCustomErrorHandler.notFoundTemplate(any())).thenReturn(Html("not found"))

    val basicRequest: BasicAuthenticatedRequest[AnyContent] =
      BasicAuthenticatedRequest(grpAccount, individualAccount, fakeRequest)
  }

  implicit val appointNewAgentSessionFormat: OFormat[AppointNewAgentSession] = Json.format

  case class AddedToRequest(
        sessionData: AppointNewAgentSession,
        individualAccount: DetailedIndividualAccount,
        groupAccount: GroupAccount
  )
  implicit val format: OFormat[AddedToRequest] = Json.format

  val actionThatReturnsEnrichedFieldsAsJson: AppointAgentSessionRequest[AnyContent] => Future[Result] =
    (req: AppointAgentSessionRequest[AnyContent]) =>
      Future.successful(
        Results.Ok(Json.toJson(AddedToRequest(req.sessionData, req.individualAccount, req.groupAccount))))

  "WithAssessmentsPageSessionRefiner" should {
    "add to request session data, individualAccount, and groupAccount to the request" when {
      "session is found" in new Setup {

        when(mockSessionRepository.get[AppointNewAgentSession](any(), any()))
          .thenReturn(Future.successful(Some(appointNewAgentSession)))

        val res: Future[Result] = refiner.invokeBlock(basicRequest, actionThatReturnsEnrichedFieldsAsJson)
        status(res) shouldBe OK
        inside(contentAsJson(res).validate[AddedToRequest]) {
          case JsSuccess(AddedToRequest(sd, ia, ga), _) =>
            sd shouldBe appointNewAgentSession
            ia shouldBe individualAccount
            ga shouldBe grpAccount
        }
      }
    }

    "return 404" when {
      "there's no active session found for the user" in new Setup {

        when(mockSessionRepository.get[AppointNewAgentSession](any(), any()))
          .thenReturn(Future.successful(None))

        status(refiner.invokeBlock(basicRequest, actionThatReturnsEnrichedFieldsAsJson)) shouldBe NOT_FOUND
      }
    }
  }
}
