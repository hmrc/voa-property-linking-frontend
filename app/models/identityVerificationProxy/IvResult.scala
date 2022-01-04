/*
 * Copyright 2022 HM Revenue & Customs
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

package models.identityVerificationProxy

sealed abstract class IvResult(protected val ivJourneyStatus: String) extends Product with Serializable

object IvResult {

  def all: Set[IvResult] = Set(IvSuccess) ++ IvFailure.all

  def fromString(str: String): Option[IvResult] =
    all.find(_.ivJourneyStatus == str)

  // The user completed the IV journey successfully.
  case object IvSuccess extends IvResult("Success")

  sealed abstract class IvFailure(
        override val ivJourneyStatus: String,
        val messageKey: String
  ) extends IvResult(ivJourneyStatus) with Product with Serializable

  object IvFailure {

    def all: Set[IvFailure] = Set(
      Incomplete,
      FailedMatching,
      FailedDirectorCheck,
      InsufficientEvidence,
      LockedOut,
      UserAborted,
      Timeout,
      TechnicalIssue,
      PreconditionFailed,
      Deceased,
      FailedIV
    )

    // The journey has not been completed yet. This result can only occur when a service asks for the result too early (before receiving the redirect from IV).
    case object Incomplete extends IvFailure("Incomplete", "incomplete")

    // The user entered details on the 'designatory details' page that could not be matched to an appropriate record in CID.
    case object FailedMatching extends IvFailure("FailedMatching", "failedMatching")

    // The check failed when attempting to match the user details against director details from Companies House.
    case object FailedDirectorCheck extends IvFailure("FailedDirectorCheck", "failedDirectorCheck")

    // The user was matched, but we do not have enough information about them to ask the number of questions needed to meet the requested Confidence Level
    case object InsufficientEvidence extends IvFailure("InsufficientEvidence", "insufficientEvidence")

    // The user failed to answer questions correctly and exceeded the allowable number of retries, so has been locked out.
    case object LockedOut extends IvFailure("LockedOut", "lockedOut")

    // The user specifically chose to end the journey.
    case object UserAborted extends IvFailure("UserAborted", "userAborted")

    // The user took to long to proceed with the journey and was timed out.
    case object Timeout extends IvFailure("Timeout", "timeout")

    // A technical issue on the platform caused the journey to end. This is usually a transient issue, so the user may be asked to try again later.
    case object TechnicalIssue extends IvFailure("TechnicalIssue", "technicalIssue")

    // we don't expect these, and don't have copy text to display for these journey outcomes
    // but we still model them and map all to "technicalIssue" key
    case object PreconditionFailed extends IvFailure("PreconditionFailed", "technicalIssue")

    case object Deceased extends IvFailure("Deceased", "technicalIssue")

    case object FailedIV extends IvFailure("FailedIV", "technicalIssue")

  }

}
