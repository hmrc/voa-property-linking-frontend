/*
 * Copyright 2017 HM Revenue & Customs
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

import config.{ApplicationConfig, Wiring}
import models.{IVDetails, IndividualAccount, IndividualDetails}
import play.api.mvc.{Action, Request}
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.http.{HeaderCarrier, SessionKeys}

import scala.concurrent.Future

trait IdentityVerification extends PropertyLinkingController {
  val groups = Wiring().groupAccountConnector
  val individuals = Wiring().individualAccountConnector
  val auth = Wiring().authConnector
  val ggAction = Wiring().ggAction
  val keystore = Wiring().sessionCache
  val identityVerification = Wiring().identityVerification
  val addresses = Wiring().addresses
  val identityVerificationProxyConnector = Wiring().identityVerificationProxyConnector

  def startIv = ggAction.async { _ => implicit request =>
    if (ApplicationConfig.ivEnabled) {
      keystore.fetchAndGetEntry[IVDetails]("ivDetails") flatMap {
        case Some(d) => identityVerificationProxyConnector.start(ApplicationConfig.baseUrl + routes.IdentityVerification.restoreSession().url,
          ApplicationConfig.baseUrl + routes.IdentityVerification.fail().url, d, None).map(l => Redirect(l.link))
        case None => NotFound
      }
    } else {
      Future.successful(Redirect(routes.IdentityVerification.success()).addingToSession("journeyId" -> java.util.UUID.randomUUID().toString))
    }
  }

  def fail = Action { implicit request =>
    Ok(views.html.identityVerification.failed())
  }

  def restoreSession = Action.async { implicit request =>
    Redirect(routes.IdentityVerification.success).addingToSession(
      SessionKeys.authToken -> request.session.get("bearerToken").getOrElse(""),
      SessionKeys.sessionId -> request.session.get("oldSessionId").getOrElse("")
    )
  }

  def success = ggAction.async { implicit ctx => implicit request =>
    request.session.get("journeyId").fold(Future.successful(Unauthorized("Unauthorised"))) { journeyId =>
      identityVerification.verifySuccess(journeyId) flatMap {
        case true => continue
        case false => Unauthorized("Unauthorised")
      }
    }
  }

  private def continue(implicit ctx: AuthContext, request: Request[_]) = {
    request.session.get("journeyId").fold(Future.successful(Unauthorized("Unauthorised"))) { journeyId =>
      for {
        groupId <- auth.getGroupId(ctx)
        userId <- auth.getExternalId(ctx)
        account <- groups.withGroupId(groupId)
        details <- keystore.getIndividualDetails
        res <- account match {
          case Some(acc) => individuals.create(IndividualAccount(userId, journeyId, acc.id, details)) map { _ =>
            Ok(views.html.createAccount.groupAlreadyExists(acc.companyName))
          }
          case _ => registerAddress(details) map { _ => Ok(views.html.identityVerification.success()) }
        }
      } yield res
    }
  }

  private def registerAddress(details: IndividualDetails)(implicit hc: HeaderCarrier): Future[Unit] = details.address.addressUnitId match {
    case Some(_) => ()
    case None => for {
      id <- addresses.create(details.address)
      _ <- keystore.cacheIndividualDetails(details.copy(address = details.address.copy(addressUnitId = Some(id))))
    } yield ()
  }
}

object IdentityVerification extends IdentityVerification

case class StartIVVM(data: IVDetails, url: String)
