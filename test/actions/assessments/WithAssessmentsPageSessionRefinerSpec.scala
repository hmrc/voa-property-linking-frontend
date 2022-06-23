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

package actions.assessments

import actions.assessments.request.AssessmentsPageSessionRequest
import actions.requests.BasicAuthenticatedRequest
import models.assessments.AssessmentsPageSession
import models.{DetailedIndividualAccount, GroupAccount}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import org.scalatest.{BeforeAndAfterEach, Inside}
import org.scalatestplus.mockito.MockitoSugar
import play.api.libs.json._
import play.api.mvc.{AnyContent, Result, Results}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.twirl.api.Html
import tests.{AllMocks, BaseUnitSpec}
import utils.FakeObjects

import scala.concurrent.Future

class WithAssessmentsPageSessionRefinerSpec
    extends BaseUnitSpec with MockitoSugar with BeforeAndAfterEach with AllMocks with FakeObjects with Inside {

  trait Setup {
    val fakeRequest = FakeRequest()

    val refiner = new WithAssessmentsPageSessionRefiner(mockSessionRepository)

    val grpAccount: GroupAccount = groupAccount(true)
    val individualAccount = detailedIndividualAccount

    when(mockCustomErrorHandler.notFoundTemplate(any())).thenReturn(Html("not found"))

    val basicRequest: BasicAuthenticatedRequest[AnyContent] =
      BasicAuthenticatedRequest(grpAccount, individualAccount, fakeRequest)
  }

  case class AddedToRequest(
        sessionData: AssessmentsPageSession,
        individualAccount: DetailedIndividualAccount,
        groupAccount: GroupAccount
  )
  implicit val format: OFormat[AddedToRequest] = Json.format

  val actionThatReturnsEnrichedFieldsAsJson: AssessmentsPageSessionRequest[AnyContent] => Future[Result] =
    (req: AssessmentsPageSessionRequest[AnyContent]) =>
      Future.successful(
        Results.Ok(Json.toJson(AddedToRequest(req.sessionData, req.individualAccount, req.groupAccount))))

  "WithAssessmentsPageSessionRefiner" should {
    "add to request session data, individualAccount, and groupAccount to the request" when {
      "session is found" in new Setup {

        when(mockSessionRepository.get[AssessmentsPageSession](any(), any()))
          .thenReturn(Future.successful(Some(assessmentPageSession)))

        val res: Future[Result] = refiner.invokeBlock(basicRequest, actionThatReturnsEnrichedFieldsAsJson)
        status(res) shouldBe OK
        inside(contentAsJson(res).validate[AddedToRequest]) {
          case JsSuccess(AddedToRequest(sd, ia, ga), _) =>
            sd shouldBe assessmentPageSession
            ia shouldBe individualAccount
            ga shouldBe grpAccount
        }
      }
    }

    "return OK with Dashboard set as previousPage in the sessionData" when {
      "there's no active session found for the user" in new Setup {

        when(mockSessionRepository.get[AssessmentsPageSession](any(), any()))
          .thenReturn(Future.successful(None))

        val res: Future[Result] = refiner.invokeBlock(basicRequest, actionThatReturnsEnrichedFieldsAsJson)

        status(res) shouldBe OK
        (contentAsJson(res) \ "sessionData" \ "previousPage").get shouldBe JsString("Dashboard")
      }
    }
  }
}
