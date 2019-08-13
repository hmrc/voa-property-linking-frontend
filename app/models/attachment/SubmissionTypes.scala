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

package models.attachment

sealed abstract class SubmissionTypes( val destination: String,
                               val submissionId: String)

case object SubmissionTypesValues {
  case object PropertyLinkEvidence extends SubmissionTypes("PROPERTY_LINK_EVIDENCE_DFE", "propertyLinkSubmissionId")
}

sealed abstract class FileTypes( val value: String)
case object FileTypes {
  case object Evidence extends FileTypes("EVIDENCE")
  case object Statement extends FileTypes("STATEMENT")

}