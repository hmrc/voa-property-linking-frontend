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

import akka.stream.Materializer
import org.scalatest._
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.{Action, EssentialAction, Results}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.api.{Application => App}

class WhitelistFilterSpec extends FlatSpec with MustMatchers {

  val filterEnabled: App = GuiceApplicationBuilder()
    .configure("metrics.enabled" -> false)
    .configure("feature.whitelist" -> true)
    .configure("whitelist.ips" -> "127.0.0.1,9.9.9.9")
    .configure("whitelist.destination" -> "http://somewhere.com")
    .configure("whitelist.exclusions" -> "/ping/ping,/ping/pong")
    .build()

  implicit lazy val materializer: Materializer = filterEnabled.materializer

  val filterOn = new config.WhitelistFilter(filterEnabled.configuration)
  val dummyAction: EssentialAction = Action(req => Results.Ok("done"))

  "With the whitelist filter enabled" must "redirect to the configured destination" in {
    val req = FakeRequest("GET", "/").withHeaders("True-Client-Ip" -> "127.0.0.2")
    val result = filterOn(dummyAction)(req).run()

    status(result) must be(303)
    header("Location", result) must be(Some("http://somewhere.com"))
  }

  it must "allow access if the ip given is whitelisted" in {
    val req = FakeRequest("GET", "/").withHeaders("True-Client-Ip" -> "127.0.0.1")
    val result = filterOn(dummyAction)(req).run()

    status(result) must be(200)

    val req2 = FakeRequest("GET", "/").withHeaders("True-Client-Ip" -> "9.9.9.9")
    val result2 = filterOn(dummyAction)(req2).run()

    status(result2) must be(200)
  }

  it must "allow access to configured exclusions irrespective of IP" in {
    val req = FakeRequest("GET", "/ping/ping").withHeaders("True-Client-Ip" -> "9.9.9.9")
    val result = filterOn(dummyAction)(req).run()

    status(result) must be(200)

    val req2 = FakeRequest("GET", "/ping/ping").withHeaders("True-Client-Ip" -> "1.1.1.1")
    val result2 = filterOn(dummyAction)(req2).run()

    status(result2) must be(200)

    val req3 = FakeRequest("GET", "/ping/pong").withHeaders("True-Client-Ip" -> "1.1.1.1")
    val result3 = filterOn(dummyAction)(req3).run()

    status(result3) must be(200)
  }

  "With the whitelist filter disabled" must "allow access to all urls" in {
    val filterDisabled: App = GuiceApplicationBuilder()
      .configure("metrics.enabled" -> false)
      .configure("feature.whitelist" -> false)
      .build()

    implicit val materializer: Materializer = filterDisabled.materializer

    val filterOff = new config.WhitelistFilter(filterDisabled.configuration)

    val req = FakeRequest("GET", "/").withHeaders("True-Client-Ip" -> "127.0.0.2")
    val result = filterOff(dummyAction)(req).run()

    status(result) must be(200)
  }
}