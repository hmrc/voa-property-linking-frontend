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

import config.Wiring
import models.IndividualAccount

import scala.concurrent.Future

trait IdentityVerification extends PropertyLinkingController {
  val groups = Wiring().groupAccountConnector
  val individuals = Wiring().individualAccountConnector
  val userDetails = Wiring().userDetailsConnector
  val auth = Wiring().authConnector
  val ggAction = Wiring().ggAction

  def show = ggAction { _ => implicit request =>
    Ok(views.html.identityVerification.show())
  }

  def fail = ggAction { _ => implicit request =>
    Ok(views.html.identityVerification.failed())
  }

  def succeed = ggAction.async { ctx => implicit request =>
    for {
      groupId <- userDetails.getGroupId(ctx)
      userId <- auth.getInternalId(ctx)
      account <- groups.get(groupId)
      res <- account match {
        case Some(acc) => individuals.create(IndividualAccount(userId, groupId)) map { _ =>
          Redirect(routes.Dashboard.home)
        }
        case None => Future.successful(Redirect(routes.CreateGroupAccount.show))
      }
    } yield {
      res
    }
  }
}

object IdentityVerification extends IdentityVerification
