package useCaseSpecs

import org.joda.time.DateTime
import useCaseSpecs.utils.{Property, _}

class SelfCertification extends FrontendTest {
  import TestData._

  "Given an interested person has declared their capacity to a self-certifiable property" - {
    implicit val sid: SessionID = java.util.UUID.randomUUID.toString
    HTTP.stubKeystoreSession(SessionDocument(selfCertifiableProperty, Some(capacityDeclaration)))

    "When they arrive at the the self certification page" - {
      val page = Page.get(s"/property-linking/self-certify")

      "They can see the self certification terms and conditions" in {
        page.mustContain1("#self-certification-statement")
      }

      "And they are asked to agree to the terms and conditions" in {
        page.mustContainCheckbox("iAgree")
      }
    }
  }

  object TestData {
    lazy val address = Address(Seq.empty, "AA11 1AA", true)
    lazy val selfCertifiableProperty = Property("xyzbaref332", address, "shop", false, true)
    lazy val capacityDeclaration = CapacityDeclaration("occupier", new DateTime(2001, 1, 1, 0, 0, 0), None)
  }
}
