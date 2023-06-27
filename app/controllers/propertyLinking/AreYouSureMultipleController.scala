/*
 * Copyright 2023 HM Revenue & Customs
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

import com.google.inject.Singleton
import config.ApplicationConfig
import controllers.PropertyLinkingController
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.http.FrontendErrorHandler

import javax.inject.Inject
import scala.concurrent.ExecutionContext

@Singleton
class AreYouSureMultipleController @Inject()(
                              areYouSureMultipleView: views.html.propertyLinking.areYouSureMultipleYears,
                            )(
                                           implicit executionContext: ExecutionContext,
                                           override val messagesApi: MessagesApi,
                                           override val controllerComponents: MessagesControllerComponents,
                                           val config: ApplicationConfig
                                         ) extends PropertyLinkingController {

   def show: Action[AnyContent] = Action { implicit request =>
     Ok(areYouSureMultipleView(agentName = getAgentName, backLink = getBackLink, agentCode = getAgentCode))
   }

   val getAgentName: String = "Joeys Agent"

   val getAgentCode: Long = 1000L

   val getBackLink: String = routes.ChooseRatingListController.show.url

   override def errorHandler: FrontendErrorHandler = ???
}