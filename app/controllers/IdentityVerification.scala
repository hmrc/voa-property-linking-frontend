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

package controllers

import javax.inject.{Inject, Named}

import auth.{GGAction, VoaAction}
import config.ApplicationConfig
import connectors._
import connectors.identityVerificationProxy.IdentityVerificationProxyConnector
import models.{IVDetails, IndividualAccountSubmission, PersonalDetails}
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, Request}
import repositories.SessionRepo
import uk.gov.hmrc.play.frontend.auth.AuthContext

import scala.concurrent.Future
import uk.gov.hmrc.http.{HeaderCarrier, SessionKeys}

class IdentityVerification @Inject() (ggAction: VoaAction,
                                      identityVerification: connectors.IdentityVerification,
                                      addresses: Addresses,
                                      individuals: IndividualAccounts,
                                      identityVerificationProxyConnector: IdentityVerificationProxyConnector,
                                      groups: GroupAccounts,
                                      auth: VPLAuthConnector,
                                      @Named ("personSession") val personalDetailsSessionRepo: SessionRepo)
                                     (implicit val messagesApi: MessagesApi, val config: ApplicationConfig)
  extends PropertyLinkingController {

  def startIv = ggAction.async(true) { _ => implicit request =>
    if (config.ivEnabled) {
      personalDetailsSessionRepo.get[PersonalDetails] flatMap { details  => {
        val d = details.getOrElse(throw new Exception("details not found"))
        identityVerificationProxyConnector.start(config.baseUrl + routes.IdentityVerification.restoreSession().url,
          config.baseUrl + routes.IdentityVerification.fail().url, d.ivDetails).map(l => Redirect(l.link))
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
    Redirect(routes.IdentityVerification.success()).addingToSession(
      SessionKeys.authToken -> request.session.get("bearerToken").getOrElse(""),
      SessionKeys.sessionId -> request.session.get("oldSessionId").getOrElse("")
    )
  }

  def success = ggAction.async(false) { implicit ctx => implicit request =>
    request.session.get("journeyId").fold(Future.successful(Unauthorized("Unauthorised"))) { journeyId =>
      identityVerification.verifySuccess(journeyId) flatMap {
        case true => continue(ctx, request)
        case false => Unauthorized("Unauthorised")
      }
    }
  }

  private def continue[A](implicit ctx: A, request: Request[_]) = {
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
