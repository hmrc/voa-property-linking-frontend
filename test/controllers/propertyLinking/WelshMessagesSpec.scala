/*
 * Copyright 2024 HM Revenue & Customs
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

package controllers.propertyLinking

import controllers.VoaPropertyLinkingSpec
import play.api.i18n.MessagesApi

class WelshMessagesSpec extends VoaPropertyLinkingSpec {
  lazy val realMessagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]

  "all english messages" should "have a welsh translation" in {
    val realMessages = realMessagesApi.messages
    val englishMessages = realMessages("default")
    val welshMessages = realMessages("cy")

    val missingWelshKeys = englishMessages.keySet.filterNot(welshMessages.keySet)

    if (missingWelshKeys.nonEmpty) {
      val failureText = missingWelshKeys.foldLeft(s"There are ${missingWelshKeys.size} missing Welsh translations:") {
        case (failureString, key) =>
          failureString + s"\n$key:${englishMessages(key)}"
      }

      fail(failureText)
    }
  }
}
