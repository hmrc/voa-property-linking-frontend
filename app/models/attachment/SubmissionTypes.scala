package models.attachment

sealed abstract class SubmissionTypes( val destination: String,
                               val submissionId: String)

case object SubmissionTypesValues {
  case object ChallengeCaseEvidence extends SubmissionTypes("CHALLENGE_CASE_EVIDENCE_DFE", "challengeSubmissionId")
}

sealed abstract class FileTypes( val value: String)
case object FileTypes {
  case object Evidence extends FileTypes("EVIDENCE")
  case object Statement extends FileTypes("STATEMENT")

}