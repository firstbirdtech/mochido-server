package mochido.server

import spray.json.DefaultJsonProtocol

object Bamboo {

  case class BambooResults(results: BambooResultsPage)

  case class BambooResultsPage(result: List[BambooResult])

  case class BambooResult(plan: Plan, state: String, buildState: String)

  case class Plan(name: String)

  object JsonProtocol extends DefaultJsonProtocol {
    implicit val planFormat = jsonFormat1(Plan)
    implicit val bambooResultFormat = jsonFormat3(BambooResult)
    implicit val bamboooResultsPageFormat = jsonFormat1(BambooResultsPage)
    implicit val bambooResultsFormat = jsonFormat1(BambooResults)
  }

}
