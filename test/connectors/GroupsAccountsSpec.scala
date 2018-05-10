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

package connectors

import controllers.VoaPropertyLinkingSpec
import models.{Address, GroupAccount, GroupAccountSubmission, UpdatedOrganisationAccount}
import org.scalacheck.Arbitrary._
import play.api.http.Status._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.{JsValue, Json}
import resources._
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import utils.StubServicesConfig

class GroupsAccountsSpec extends VoaPropertyLinkingSpec {

  implicit val hc = HeaderCarrier()

  class Setup {
    val connector = new GroupAccounts(StubServicesConfig, mockWSHttp) {
      override lazy val url: String = "tst-url"
    }
  }

  "get" must "return a valid group account using the organisation ID" in new Setup {
    val validGroupAccount = arbitrary[GroupAccount].sample.get

    mockHttpGETOption[GroupAccount]("tst-url", validGroupAccount)
    whenReady(connector.get(1))(_ mustBe Some(validGroupAccount))
  }

  "withGroupId" must "return a valid group account using the group ID" in new Setup {
    val validGroupAccount = arbitrary[GroupAccount].sample.get

    mockHttpGETOption[GroupAccount]("tst-url", validGroupAccount)
    whenReady(connector.withGroupId("GROUP_ID"))(_ mustBe Some(validGroupAccount))
  }

  "withAgentCode" must "return a valid group account using the agent code" in new Setup {
    val validGroupAccount = arbitrary[GroupAccount].sample.get

    mockHttpGETOption[GroupAccount]("tst-url", validGroupAccount)
    whenReady(connector.withAgentCode("AGENT_CODE"))(_ mustBe Some(validGroupAccount))
  }

  "update" must "successfully update a group account" in new Setup {
    val updatedOrganisationAccount = arbitrary[UpdatedOrganisationAccount].sample.get
    mockHttpPUT[UpdatedOrganisationAccount, HttpResponse]("tst-url", HttpResponse(OK))
    whenReady(connector.update(1, updatedOrganisationAccount))(_ mustBe ())
  }

  "create" must "create a group account and return the ID" in new Setup {
    val groupAccountSubmission = mock[GroupAccountSubmission]
    val accountId = Json.obj("id" -> 1)

    mockHttpPOST[GroupAccountSubmission, JsValue]("tst-url", accountId)
    whenReady(connector.create(groupAccountSubmission))(_ mustBe 1L)
  }

}
