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

package forms

import controllers.VoaPropertyLinkingSpec
import models.propertyrepresentation.{AgentAppointmentChangesRequest, AppointmentScope}
import utils.FormBindingVerification._

class AgentAppointmentChangesRequestSpec extends VoaPropertyLinkingSpec {

  import TestData._

  behavior of "Appoint new agent form"

  it should "bind when the inputs are all valid - PROPERTY_LIST" in {
    shouldBindTo(
      form,
      validData,
      AgentAppointmentChangesRequest(agentRepresentativeCode = 12345L, scope = "PROPERTY_LIST"))
  }

  it should "bind when the inputs are all valid - ALL_PROPERTIES" in {
    val data = validData.updated("scope", s"${AppointmentScope.ALL_PROPERTIES}")
    shouldBindTo(form, data, AgentAppointmentChangesRequest(agentRepresentativeCode = 12345L, scope = "ALL_PROPERTIES"))
  }

  it should "bind when the inputs are all valid - RELATIONSHIP" in {
    val data = validData.updated("scope", s"${AppointmentScope.RELATIONSHIP}")
    shouldBindTo(form, data, AgentAppointmentChangesRequest(agentRepresentativeCode = 12345L, scope = "RELATIONSHIP"))
  }

  it should "require an agentCode" in {
    val data = validData.updated("agentCode", "")
    verifyError(form, data, "agentCode", "error.number")
  }

  it should "require a scope" in {
    val data = validData.updated("scope", "BLAH")
    verifyError(form, data, "scope", "error.unknown")
  }

  object TestData {
    val form = AgentAppointmentChangesRequest.submitAgentAppointmentRequest
    val validData = Map(
      "agentCode" -> "12345",
      "scope"     -> s"${AppointmentScope.PROPERTY_LIST}"
    )
  }

}
