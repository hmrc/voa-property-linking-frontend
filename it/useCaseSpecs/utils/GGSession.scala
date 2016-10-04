package useCaseSpecs.utils

case class GGSession(userId: String, token: String) {
  def toSeq = Seq("userId" -> userId, "token" -> token)
}