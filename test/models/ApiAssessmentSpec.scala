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

package models

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import utils.FakeObjects

import java.time.LocalDate

class ApiAssessmentSpec extends AnyWordSpec with Matchers with FakeObjects {

  "sorting assessments" should {
    "sort assessments by effective date (desc) and secondarily by current From date (desc)" in {
      val prototype = apiAssessment(ownerAuthorisation)
      def makeAssessment(id: Long, ed: Option[LocalDate], fd: Option[LocalDate]) =
        prototype.copy(authorisationId = id, effectiveDate = ed, currentFromDate = fd)
      val today = LocalDate.now()
      val assessments = List(
        makeAssessment(1, Some(today.minusDays(1L)), Some(today.minusDays(1L))),
        makeAssessment(2, Some(today.minusDays(1L)), Some(today.minusDays(3L))),
        makeAssessment(3, Some(today.minusDays(1L)), Some(today.plusDays(1L))),
        makeAssessment(4, Some(today.minusDays(1L)), Some(today.plusDays(3L))),
        makeAssessment(5, Some(today.minusDays(5L)), Some(today.plusDays(3L))),
        makeAssessment(6, None, Some(today.plusDays(3L))),
        makeAssessment(7, Some(today.minusDays(1L)), None),
        makeAssessment(8, Some(today.plusDays(1L)), Some(today)),
        makeAssessment(9, Some(today.plusDays(1L)), Some(today.minusDays(3L))),
        makeAssessment(10, Some(today.plusDays(1L)), Some(today.plusDays(1L))),
        makeAssessment(11, Some(today.plusDays(1L)), Some(today.plusDays(3L))),
        makeAssessment(12, Some(today.plusDays(5L)), Some(today)),
        makeAssessment(13, None, None)
      )

      val sortedResults = assessments.sortBy(ApiAssessment.sortCriteria).map(_.authorisationId)
      sortedResults should contain theSameElementsInOrderAs List(13, 6, 12, 11, 10, 8, 9, 7, 4, 3, 1, 2, 5)
    }
  }

}
