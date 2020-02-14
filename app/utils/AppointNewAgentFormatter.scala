/*
 * Copyright 2020 HM Revenue & Customs
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

import models.propertyrepresentation.{All, AsWellAsCurrent, ChooseFromList, InsteadOfCurrent, No, None, Yes}

object AppointNewAgentFormatter {

  def format(option: String): String = option match {
    case All.name              => "propertyRepresentation.checkYourAnswers.options.all"
    case None.name             => "propertyRepresentation.checkYourAnswers.options.none"
    case AsWellAsCurrent.name  => "propertyRepresentation.checkYourAnswers.options.asWellAsCurrent"
    case InsteadOfCurrent.name => "propertyRepresentation.checkYourAnswers.options.insteadOfCurrent"
    case No.name               => "propertyRepresentation.checkYourAnswers.options.no"
    case Yes.name              => "propertyRepresentation.checkYourAnswers.options.yes"
    case ChooseFromList.name   => "" //should not be possible
  }

}
