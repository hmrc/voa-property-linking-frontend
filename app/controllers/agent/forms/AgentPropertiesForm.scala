package controllers.agent.forms

import play.api.data.Form
import play.api.data.Forms._

object AgentPropertiesForm {

  val filterPropertiesForm: Form[FilterAgentProperties] = Form(
    mapping(
      "address"                 -> optional(text),
      "localAuthorityReference" -> optional(text)
    )(FilterAgentProperties.apply)(FilterAgentProperties.unapply)
      .verifying("propertyRepresentation.agentProperties.filter", fields => fields.searchCriteriaExists))

}
