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

package controllers.propertyLinking

import javax.inject.{Inject, Singleton}

import config.ApplicationConfig
import connectors.fileUpload.FileUploadConnector
import uk.gov.hmrc.circuitbreaker.{CircuitBreakerConfig, UsingCircuitBreaker}
import uk.gov.hmrc.play.http.{HeaderCarrier, Upstream4xxResponse}

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class FileUploadCircuitBreaker @Inject()(override val circuitBreakerConfig: CircuitBreakerConfig,
                                         fileUploadConnector: FileUploadConnector)
                                        (implicit ec: ExecutionContext) extends UsingCircuitBreaker {

  override protected def breakOnException(t: Throwable): Boolean = t match {
    case _: Upstream4xxResponse => false
    case _ => true
  }

  def apply[T](f: => Future[T])(implicit hc: HeaderCarrier): Future[T] = {
    withCircuitBreaker(fileUploadConnector.healthCheck flatMap { _ => f })
  }
}
