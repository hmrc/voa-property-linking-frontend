package useCaseSpecs.utils

object UIFormats {
  def date(s: String): String = s.replaceAll("-", """/""")
}
