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

import config.Wiring
import play.api.i18n.Messages
import play.api.libs.json.Json
import play.api.mvc.Action

trait AddressLookup extends PropertyLinkingController {

  val addresses = Wiring().addresses

  def findByPostcode(postcode: String) = Action.async { implicit request =>
    if (postcode.trim.contains(" ")) {
      addresses.findByPostcode(postcode) map { res =>
        if (res.isEmpty) {
          NotFound
        } else {
          Ok(Json.toJson(res))
        }
      }
    } else {
      BadRequest(Messages("error.postcode.nospace"))
    }
  }
}

object AddressLookup extends AddressLookup
