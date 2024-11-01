/*
 * Copyright 2024 HM Revenue & Customs
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

package base

import com.github.tomakehurst.wiremock.client.WireMock.{aResponse, get, post, stubFor}
import models.ApiAssessment
import play.api.http.Status.OK
import play.api.libs.json.Json

trait CommonStubs extends TestData {

  def stubAuth(userIsAgent: Boolean): Unit = {
    stubFor {
      get("/business-rates-authorisation/authenticate")
        .willReturn {
          aResponse.withStatus(OK).withBody(Json.toJson(if (userIsAgent) testAccounts else testIpAccounts).toString())
        }
    }

    stubFor {
      post("/auth/authorise")
        .willReturn {
          aResponse.withStatus(OK).withBody("{}")
        }
    }
  }

  def stubOwnerProperties(assessments: List[ApiAssessment], connectionToProperty: String): Unit = {
    stubFor {
      get(s"/property-linking/dashboard/owner/assessments/$plSubmissionId")
        .willReturn {
          aResponse
            .withStatus(OK)
            .withBody(Json.toJson(testApiAssessments(assessments, connectionToProperty)).toString())
        }
    }

    stubFor {
      get(s"/property-linking/owner/property-links/$plSubmissionId")
        .willReturn {
          aResponse.withStatus(OK).withBody(Json.toJson(propertyLink).toString())
        }
    }
  }

  def stubAgentProperties(assessments: List[ApiAssessment], connectionToProperty: String): Unit = {
    stubFor {
      get(s"/property-linking/dashboard/agent/assessments/$plSubmissionId")
        .willReturn {
          aResponse
            .withStatus(OK)
            .withBody(Json.toJson(testApiAssessments(assessments, connectionToProperty, isAgent = true)).toString())
        }
    }

    stubFor {
      get(s"/property-linking/agent/property-links/$plSubmissionId?projection=clientsPropertyLink")
        .willReturn {
          aResponse.withStatus(OK).withBody(Json.toJson(clientPropertyLink).toString())
        }
    }
  }

}
