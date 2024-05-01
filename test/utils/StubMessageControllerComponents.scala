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

package utils

import org.apache.pekko.stream.testkit.NoMaterializer
import play.api.Environment
import play.api.http.{DefaultFileMimeTypes, FileMimeTypes, FileMimeTypesConfiguration}
import play.api.i18n.Messages.UrlMessageSource
import play.api.i18n._
import play.api.mvc.{MessagesControllerComponents, _}
import play.api.test.Helpers._

import scala.concurrent.ExecutionContext

trait StubMessageControllerComponents extends Configs {

  val langs: Langs = new DefaultLangs(Seq(Lang.defaultLang, Lang("cy")))
  val environment: Environment = Environment.simple()

  val messages: Map[String, String] =
    Messages.parse(UrlMessageSource(this.getClass.getClassLoader.getResource("messages")), "").right.get

  val messages_cy: Map[String, String] =
    Messages.parse(UrlMessageSource(this.getClass.getClassLoader.getResource("messages.cy")), "").right.get

  lazy val messagesApi: MessagesApi =
    new DefaultMessagesApi(messages = Map("default" -> messages, "cy" -> messages_cy), langs = langs)

  def stubMessagesControllerComponents(
        bodyParser: BodyParser[AnyContent] = stubBodyParser(AnyContentAsEmpty),
        playBodyParsers: PlayBodyParsers = stubPlayBodyParsers(NoMaterializer),
        fileMimeTypes: FileMimeTypes = new DefaultFileMimeTypes(FileMimeTypesConfiguration()),
        messagesApi: MessagesApi = messagesApi
  )(
        implicit executionContext: ExecutionContext
  ): MessagesControllerComponents =
    DefaultMessagesControllerComponents(
      new DefaultMessagesActionBuilderImpl(bodyParser, messagesApi),
      DefaultActionBuilder(bodyParser),
      playBodyParsers,
      messagesApi,
      langs,
      fileMimeTypes,
      executionContext
    )

}

object StubMessageControllerComponents extends StubMessageControllerComponents
