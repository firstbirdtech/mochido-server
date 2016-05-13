package mochido.server.bamboo

import spray.json.DefaultJsonProtocol


object BambooJsonProtocol extends DefaultJsonProtocol {

  case class BambooResults(results: BambooResultsPage)
  case class BambooResultsPage(result: List[BambooResult])
  case class BambooResult(plan: Plan, state: String, buildState: String)
  case class Plan(name: String)

  implicit val planFormat = jsonFormat1(Plan)
  implicit val bambooResultFormat = jsonFormat3(BambooResult)
  implicit val bamboooResultsPageFormat = jsonFormat1(BambooResultsPage)
  implicit val bambooResultsFormat = jsonFormat1(BambooResults)

}
