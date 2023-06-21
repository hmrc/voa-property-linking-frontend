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

package utils.connectors

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, Suite}

trait WireMockSpec extends BeforeAndAfterAll with BeforeAndAfterEach { self: Suite =>

  val wireMockPort = 19534
  private lazy val wireMockServer = new WireMockServer(wireMockPort)

  protected lazy val mockServerUrl = s"http://localhost:${wireMockServer.port}"

  override def beforeAll() {
    wireMockServer.start()
    WireMock.configureFor("localhost", wireMockServer.port)
  }

  override def beforeEach() =
    WireMock.reset()

  override def afterAll(): Unit =
    wireMockServer.stop()
}
