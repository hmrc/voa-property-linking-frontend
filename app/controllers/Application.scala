/*
 * Copyright 2016 HM Revenue & Customs
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

package controllers

import auth.GGAction
import config.Wiring
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.mvc._
import uk.gov.hmrc.play.http.{HeaderCarrier, SessionKeys}

import scala.concurrent.Future

object Application extends Controller {
  val ggAction = Wiring().ggAction

 def typography = Action { implicit request =>
   Ok(views.html.typography())
 }

  def index() = Action.async { implicit request =>
    LoggedIn(request) map {
      case true => Redirect(routes.Dashboard.home())
      case false => Ok(views.html.start()).withSession(SessionKeys.sessionId -> java.util.UUID.randomUUID().toString)
    }
  }

  def logOut() = ggAction { _ => request =>
    Redirect(routes.Application.index()).withNewSession
  }
}

object LoggedIn {
  implicit def hc(implicit request: RequestHeader) = HeaderCarrier.fromHeadersAndSession(request.headers, Some(request.session))

  val accountRepo = Wiring().individualAccountConnector

  def apply(implicit r: RequestHeader) = {
    r.session.get("accountId").map { id =>
      accountRepo.get(id) map { _.isDefined }
    }.getOrElse(Future.successful(false))
  }
}
