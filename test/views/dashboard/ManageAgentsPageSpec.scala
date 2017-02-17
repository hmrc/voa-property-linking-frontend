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

package views.dashboard

import controllers.{AgentInfo, ControllerSpec, ManageAgentsVM}
import play.api.test.FakeRequest
import utils.HtmlPage
import play.api.i18n.Messages.Implicits._

class ManageAgentsPageSpec extends ControllerSpec {
  implicit val request = FakeRequest()
  val noAgents = ManageAgentsVM(Nil)
  val twoAgents = ManageAgentsVM(List(AgentInfo("name1", 111), AgentInfo("name2", 222)))

  "ManageClientPage" must "show a message stating that no agents have been appointed" in  {
    val html = views.html.dashboard.manageAgents(noAgents)
    val page = HtmlPage(html)
    page.mustContain1("#noAgents")
  }

  it must "not show this message when agent have been appointed" in {
    val html = views.html.dashboard.manageAgents(twoAgents)
    val page = HtmlPage(html)
    page.mustContain("#noAgents", 0)
  }

  it must "display the right table header" in {
    val html = views.html.dashboard.manageAgents(twoAgents)
    val page = HtmlPage(html)
    page.mustContain1("#agentsTable")
    page.mustContainTableHeader("Agent name", "Agent code")
  }

  it must "display the right table content" in {
    val html = views.html.dashboard.manageAgents(twoAgents)
    val page = HtmlPage(html)
    page.mustContain1("#agentsTable")
    page.mustContainTableHeader("Agent name", "Agent code")
  }

}
