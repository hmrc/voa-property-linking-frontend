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

import connectors.TrafficThrottleConnector
import play.api.mvc.{AnyContent, Request, Result}

import scala.concurrent.Future

import scala.concurrent.ExecutionContext.Implicits.global
import uk.gov.hmrc.http.HeaderCarrier

trait WithThrottling {

  val trafficThrottleConnector: TrafficThrottleConnector

  def withThrottledHoldingPage(route: String, throttledPage: => Result)(block: => Result)
                              (implicit request: Request[AnyContent], hc: HeaderCarrier): Future[Result] = {
    trafficThrottleConnector.isThrottled(route).map {
      case true => throttledPage
      case _ => block
    } recover {
      case _ => block
    }
  }
}
