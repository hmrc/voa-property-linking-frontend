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

import javax.inject.{Inject, Named}

import config.{ApplicationConfig, Wiring}
import models.{IVDetails, IndividualAccount, IndividualAccountSubmission, PersonalDetails}
import play.api.mvc.{Action, Request}
import repositories.SessionRepo
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.http.{HeaderCarrier, SessionKeys}

import scala.concurrent.Future

class IdentityVerification @Inject() (
                                       @Named ("personSession") val personalDetailsSessionRepo: SessionRepo)
  extends PropertyLinkingController {
  val groups = Wiring().groupAccountConnector
  val individuals = Wiring().individualAccountConnector
  val auth = Wiring().authConnector
  val ggAction = Wiring().ggAction
  val identityVerification = Wiring().identityVerification
  val addresses = Wiring().addresses
  val identityVerificationProxyConnector = Wiring().identityVerificationProxyConnector

  def startIv = ggAction.async { _ => implicit request =>
    if (ApplicationConfig.ivEnabled) {
      personalDetailsSessionRepo.get[PersonalDetails] flatMap { details  => {
        val d = details.getOrElse(throw new Exception("details not found"))
        identityVerificationProxyConnector.start(ApplicationConfig.baseUrl + routes.IdentityVerification.restoreSession().url,
          ApplicationConfig.baseUrl + routes.IdentityVerification.fail().url, d.ivDetails, None).map(l => Redirect(l.link))
      }
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
      val eventualGroupId = auth.getGroupId(ctx)
      val eventualExternalId = auth.getExternalId(ctx)
      val eventualIndividualDetails = personalDetailsSessionRepo.get[PersonalDetails]

      for {
        groupId <- eventualGroupId
        userId <- eventualExternalId
        account <- groups.withGroupId(groupId)
        details <- eventualIndividualDetails.map(_.getOrElse(throw new Exception("no details found")))
        id <- registerAddress(details)
        d = details.withAddressId(id)
        res <- account match {
          case Some(acc) => individuals.create(IndividualAccountSubmission(userId, journeyId, Some(acc.id), d.individualDetails)) map { _ =>
            Ok(views.html.createAccount.groupAlreadyExists(acc.companyName))
          }
          case _ => personalDetailsSessionRepo.saveOrUpdate(d) map { _ => Ok(views.html.identityVerification.success()) }
        }
      } yield res
    }
  }

  private def registerAddress(details: PersonalDetails)(implicit hc: HeaderCarrier): Future[Int] = details.address.addressUnitId match {
    case Some(id) => id
    case None => addresses.create(details.address)
  }
}

case class StartIVVM(data: IVDetails, url: String)
