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

package controllers

import connectors.Addresses
import play.api.Logging

import javax.inject.Inject
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, RequestHeader}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import uk.gov.hmrc.propertylinking.errorhandler.CustomErrorHandler

import scala.concurrent.ExecutionContext
import scala.util.Try

class AddressLookup @Inject()(
      val errorHandler: CustomErrorHandler,
      addresses: Addresses,
      override val controllerComponents: MessagesControllerComponents
)(
      implicit executionContext: ExecutionContext
) extends PropertyLinkingController with Logging {

  private def useBst(request: RequestHeader): Boolean =
    request.cookies.get("bst").flatMap(c => Try(c.value.toBoolean).toOption).getOrElse(false)

  implicit override def hc(implicit request: RequestHeader): HeaderCarrier =
    HeaderCarrierConverter
      .fromRequestAndSession(request, request.session)
      .withExtraHeaders("USE-BST" -> useBst(request).toString)

  def findByPostcode(postcode: String): Action[AnyContent] = Action.async { implicit request =>
    addresses
      .findByPostcode(postcode.trim)(hc)
      .recover {
        case t =>
          logger.warn("Failed to find address by post code", t)
          Seq.empty
      }
      .map {
        case Seq()         => NotFound
        case seq @ Seq(_*) => Ok(Json.toJson(seq))
      }
  }

}
