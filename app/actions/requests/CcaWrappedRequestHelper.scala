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

package actions.requests

import models.analytics.GoogleAnalyticsUserData
import play.api.mvc.RequestHeader

object CcaWrappedRequestHelper {
  implicit class CcaWrappedRequestOps(ccaRequest: CcaWrappedRequest) {
    val googleAnalyticsUserData: GoogleAnalyticsUserData = GoogleAnalyticsUserData(ccaRequest)
  }

  implicit class RequestOps(val request: RequestHeader) extends AnyVal {
    def isLoggedIn: Boolean = request match {
      case r: CcaWrappedRequest => r.isLoggedIn
      case _                    => false
    }

    def yourDetailsName: Option[String] = request match {
      case r: CcaWrappedRequest =>
        r.optAccounts.map { acc =>
          if (s"${acc.person.details.firstName} ${acc.person.details.lastName}" == acc.organisation.companyName) {
            s"${acc.person.details.firstName} ${acc.person.details.lastName}"
          } else {
            s"${acc.person.details.firstName} ${acc.person.details.lastName} - ${acc.organisation.companyName}"
          }
        }
      case _ => None
    }
  }

}
