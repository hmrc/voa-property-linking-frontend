/*
 * Copyright 2021 HM Revenue & Customs
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

package models

import models.GroupAccount.{AgentCompanyNameAndCode, AgentGroupAccount}
import org.scalatest.Matchers.convertToAnyShouldWrapper
import org.scalatest.{FlatSpec, MustMatchers}

class GroupAccountSpec extends FlatSpec with MustMatchers {

  val testCompanyName = "testCompanyName"
  val agentCode = 1234567L

  val agentGroupAccount = GroupAccount(
    id = 12345L,
    groupId = "testGroupID",
    companyName = testCompanyName,
    addressId = 123456L,
    email = "testEmail@test.com",
    phone = "123456",
    isAgent = true,
    agentCode = Some(agentCode)
  )

  val ipGroupAccount = GroupAccount(
    id = 12345L,
    groupId = "testGroupID",
    companyName = testCompanyName,
    addressId = 123456L,
    email = "testEmail@test.com",
    phone = "123456",
    isAgent = false,
    agentCode = None
  )

  "AgentCompanyNameAndCode" must "extract company name and agent code if there is an agent" in {
    AgentCompanyNameAndCode.unapply(agentGroupAccount) shouldBe Some(testCompanyName, agentCode)
  }

  "AgentCompanyNameAndCode" must "return none if the account is a non-agent account" in {
    AgentCompanyNameAndCode.unapply(ipGroupAccount) shouldBe None
  }

  "AgentGroupAccount" must "extract agent code if there is an agent" in {
    AgentGroupAccount.unapply(agentGroupAccount) shouldBe Some(agentGroupAccount, agentCode)
  }

  "AgentGroupAccount" must "return none if the account is a non-agent account" in {
    AgentGroupAccount.unapply(ipGroupAccount) shouldBe None
  }

}