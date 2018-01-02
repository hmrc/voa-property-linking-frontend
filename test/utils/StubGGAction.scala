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

import auth.GGAction
import play.api.mvc.{Action, AnyContent, Request, Result}
import uk.gov.hmrc.play.frontend.auth.{AuthContext, LoggedInUser, Principal}
import uk.gov.hmrc.play.frontend.auth.connectors.domain.{Accounts, Authority, ConfidenceLevel, CredentialStrength}

import scala.concurrent.Future

object StubGGAction extends GGAction(null, StubAuthConnector) {
  private val ctx = AuthContext(LoggedInUser("", None, None, None, CredentialStrength.Weak, ConfidenceLevel.L200, ""),
    Principal(None, Accounts()), None, None, None, None)

  override def apply(body: (AuthContext) => (Request[AnyContent]) => Result): Action[AnyContent] =
    Action { request => body(ctx)(request) }

  override def async(body: (AuthContext) => (Request[AnyContent]) => Future[Result]): Action[AnyContent] =
    Action.async { request => body(ctx)(request) }
}
