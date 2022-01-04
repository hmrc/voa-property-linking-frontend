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

package connectors

import controllers.VoaPropertyLinkingSpec
import models.{GroupAccount, GroupAccountSubmission, UpdatedOrganisationAccount}
import org.scalacheck.Arbitrary._
import play.api.http.Status._
import play.api.libs.json.{JsValue, Json}
import utils._
import uk.gov.hmrc.http.HttpResponse

class GroupsAccountsSpec extends VoaPropertyLinkingSpec {

  class Setup {
    val connector = new GroupAccounts(servicesConfig, mockHttpClient) {
      override val url: String = "tst-url"
    }
  }

  "get" should "return a valid group account using the organisation ID" in new Setup {
    val validGroupAccount = arbitrary[GroupAccount].sample.get

    mockHttpGETOption[GroupAccount]("tst-url", validGroupAccount)
    whenReady(connector.get(1))(_ shouldBe Some(validGroupAccount))
  }

  "withGroupId" should "return a valid group account using the group ID" in new Setup {
    val validGroupAccount = arbitrary[GroupAccount].sample.get

    mockHttpGETOption[GroupAccount]("tst-url", validGroupAccount)
    whenReady(connector.withGroupId("GROUP_ID"))(_ shouldBe Some(validGroupAccount))
  }

  "withAgentCode" should "return a valid group account using the agent code" in new Setup {
    val validGroupAccount = arbitrary[GroupAccount].sample.get

    mockHttpGETOption[GroupAccount]("tst-url", validGroupAccount)
    whenReady(connector.withAgentCode("AGENT_CODE"))(_ shouldBe Some(validGroupAccount))
  }

  "update" should "successfully update a group account" in new Setup {
    val updatedOrganisationAccount = arbitrary[UpdatedOrganisationAccount].sample.get
    mockHttpPUT[UpdatedOrganisationAccount, HttpResponse]("tst-url", emptyJsonHttpResponse(OK))
    whenReady(connector.update(1, updatedOrganisationAccount))(_ shouldBe ((): Unit))
  }

  "create" should "create a group account and return the ID" in new Setup {
    val groupAccountSubmission = mock[GroupAccountSubmission]
    val accountId = Json.obj("id" -> 1)

    mockHttpPOST[GroupAccountSubmission, JsValue]("tst-url", accountId)
    whenReady(connector.create(groupAccountSubmission))(_ shouldBe 1L)
  }

}
