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

import javax.inject.Inject
import connectors.Addresses
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent}

class AddressLookup @Inject()(addresses: Addresses) extends PropertyLinkingController {
  def findByPostcode(postcode: String): Action[AnyContent] = Action.async { implicit request =>
      addresses.findByPostcode(postcode.trim) map { res =>
        if (res.isEmpty) {
          NotFound
        } else {
          Ok(Json.toJson(res))
        }
    } recover {
        case _ => NotFound
    }
  }
}
