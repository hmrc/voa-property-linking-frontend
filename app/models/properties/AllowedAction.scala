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

package models.properties

import play.api.libs.json.Format
import utils.JsonUtils

object AllowedAction extends Enumeration {

  type AllowedAction = Value

  val PROPERTY_LINK: AllowedAction = Value("propertyLink")
  val CHECK: AllowedAction = Value("check")
  val CHALLENGE: AllowedAction = Value("challenge")
  val ENQUIRY: AllowedAction = Value("enquiry")
  val VIEW_DETAILED_VALUATION: AllowedAction = Value("viewDetailedValuation")
  val BUSINESS_RATES_ESTIMATOR: AllowedAction = Value("businessRatesEstimator")
  val SIMILAR_PROPERTIES: AllowedAction = Value("similarProperties")
  val VIEW_APPEALS: AllowedAction = Value("viewAppeals")
  val PROPOSAL: AllowedAction = Value("proposal")

  implicit val format: Format[AllowedAction] = JsonUtils.enumFormat(AllowedAction)
}
