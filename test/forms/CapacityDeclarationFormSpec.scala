package forms

import controllers.{CapacityDeclaration, Search}
import models.Occupier
import org.joda.time.DateTime
import org.scalatest.{FlatSpec, MustMatchers}
import utils.FormBindingVerification._

class CapacityDeclarationFormSpec extends FlatSpec with MustMatchers {

  behavior of "Capacity declaration form"

  val form = Search.declareCapacityForm

  it should "bind when the inputs are all valid" in {
    val data = Map(
      "capacity" -> Occupier.name, "fromDate.day" -> "24", "fromDate.month" -> "11", "fromDate.year" -> "2001",
      "toDate.day" -> "13", "toDate.month" -> "1", "toDate.year" -> "2011"
    )
    mustBindTo(form.bind(data), CapacityDeclaration(Occupier, new DateTime(2001, 11, 24, 0, 0, 0), new DateTime(2011, 1, 13, 0, 0, 0)))
  }

}


