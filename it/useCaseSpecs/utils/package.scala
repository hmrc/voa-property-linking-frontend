package useCaseSpecs

package object utils {
  type SessionID = String

  // TODO - delete when authentication finalised
  implicit def toAccountID(id: String): AccountID = AccountID(id)
  implicit def aidToString(aid: AccountID): String = aid.id
  case class AccountID(id: String)
}
