/*
 * Copyright 2019 HM Revenue & Customs
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

package actions

import auth.GovernmentGatewayProvider
import config.ApplicationConfig
import connectors.VPLAuthConnector
import javax.inject.Inject
import models.Accounts
import play.api.Logger
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.Results.Redirect
import play.api.mvc.{AnyContent, Request, Result}
import services.email.EmailService
import services.{EnrolmentResult, EnrolmentService, Failure, Success}
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.retrieve.Retrievals._
import uk.gov.hmrc.auth.core.retrieve.~
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

sealed trait Auth {
  def success(
               accounts: Accounts,
               body: BasicAuthenticatedRequest[AnyContent] => Future[Result]
             )(implicit request: Request[AnyContent], hc: HeaderCarrier, ec: ExecutionContext): Future[Result]

  def noVoaRecord: Future[Result]
}