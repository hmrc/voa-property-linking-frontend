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

package models.analytics

import actions.requests.CcaWrappedRequest

case class GoogleAnalyticsUserData(personId: String, loggedIn: String, ccaAgent: String)

object GoogleAnalyticsUserData {
  def apply(personId: Option[Long], loggedIn: Boolean, ccaAgent: Option[Boolean]): GoogleAnalyticsUserData =
    GoogleAnalyticsUserData(
      personId = personId.fold("N/A")(_.toString),
      loggedIn = if (loggedIn) "Yes" else "No",
      ccaAgent = ccaAgent.fold("N/A")(if (_) "Yes" else "No"))

  def apply(req: CcaWrappedRequest): GoogleAnalyticsUserData =
    GoogleAnalyticsUserData(
      req.optAccounts.map(_.person.individualId),
      req.isLoggedIn,
      req.optAccounts.map(_.organisation.isAgent))
}
