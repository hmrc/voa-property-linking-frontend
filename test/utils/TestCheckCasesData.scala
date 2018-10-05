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

package utils

import java.time.{LocalDate, LocalDateTime}
import models._

trait TestCheckCasesData{

  lazy val ownerCheckCase = OwnerCheckCase(checkCaseSubmissionId ="123344",
    checkCaseReference = "CHK-1234",
    checkCaseStatus= "OPEN",
    address= "CHK-1234",
    uarn =1000L,
    createdDateTime = LocalDateTime.now(),
    settledDate = Some(LocalDate.now()),
    agent = Some(Agent(10000L, 10000L, "Agent -1")),
    organisationId =100000L)

  lazy val agentCheckCase = AgentCheckCase(checkCaseSubmissionId ="123344",
    checkCaseReference = "CHK-1234",
    checkCaseStatus= "OPEN",
    address= "CHK-1234",
    uarn =1000L,
    createdDateTime = LocalDateTime.now(),
    settledDate = Some(LocalDate.now()),
    client = Client(10000L, "Client -1"),
    organisationId =100000L)

  lazy val ownerCheckCasesResponse = OwnerCheckCasesResponse(start = 1,
                                     size = 15,
                                     filterTotal = 1,
                                     total = 1,
                                     checkCases = List(ownerCheckCase))


  lazy val agentCheckCasesResponse =  AgentCheckCasesResponse(start = 1,
                                      size = 15,
                                      filterTotal = 1,
                                      total = 1,
                                      checkCases = List(agentCheckCase))


}