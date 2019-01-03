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

package utils

import config.ApplicationConfig
import org.scalatest.TestSuite
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.i18n.{Lang, Messages, MessagesApi}
import play.api.inject.guice.GuiceApplicationBuilder

trait NoMetricsOneAppPerSuite extends GuiceOneAppPerSuite {
  this: TestSuite =>

  val additionalAppConfig: Seq[(String, String)] = Nil

  implicit lazy val applicationConfig = app.injector.instanceOf[ApplicationConfig]

  override def fakeApplication() = new GuiceApplicationBuilder()
    .disable[com.kenshoo.play.metrics.PlayModule]
    .disable[modules.MongoStartup]
    .configure(additionalAppConfig:_*)
    .build()
}
