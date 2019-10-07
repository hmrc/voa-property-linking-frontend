package connectors

import uk.gov.hmrc.http.{JsonHttpReads, OptionHttpReads, RawReads}

trait BaseConnector extends JsonHttpReads with OptionHttpReads with RawReads
