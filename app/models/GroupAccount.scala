/*
 * Copyright 2019 HM Revenue & Customs
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

import play.api.libs.json.Json

case class GroupAccount(
                         id: Long,
                         groupId: String,
                         companyName: String,
                         addressId: Long,
                         email: String,
                         phone: String,
                         isAgent: Boolean,
                         agentCode: Option[Long]
                       )

object GroupAccount {
  implicit val format = Json.format[GroupAccount]

  object AgentGroupAccount {
    def unapply(account: GroupAccount): Option[(GroupAccount, Long)] =
      account
        .agentCode
        .map(code => account -> code)
        .filter(_ => account.isAgent) //TODO this logic might not be required here.
  }
}
