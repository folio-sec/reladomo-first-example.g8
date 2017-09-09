package example

import java.sql.DriverManager

import com.folio_sec.reladomo.scala_api.TransactionProvider.withTransaction
import com.folio_sec.reladomo.scala_api._
import com.folio_sec.reladomo.scala_api.configuration.DatabaseManager
import com.folio_sec.reladomo.scala_api.util.TimestampUtil.now
import com.folio_sec.reladomo.scala_api.util.LoanPattern
import kata.domain.scala_api._
import org.scalatest.{BeforeAndAfter, FunSpec, Matchers}

import scala.io.Source

class AggregationSpec extends FunSpec with Matchers with BeforeAndAfter {

  before {
    initializeDatabase()
  }

  describe("Aggregation") {
    it("should be supported properly") {
      withTransaction { implicit tx =>
        NewAccountBalanceList(Seq(
          NewAccountBalance(accountId = 100, balance = 1120.15),
          NewAccountBalance(accountId = 200, balance = 1910.99),
          NewAccountBalance(accountId = 300, balance = 830.65)
        )).insertAll()
      }

      {
        val aggregations = {
          AccountBalanceFinder.aggregateWith(_.businessDate.eq(now()))
            .count(_.accountId, "accounts")
            .sum(_.balance, "balance_sum")
            .max(_.balance, "balance_max")
        }
        val aggregation = aggregations.toSeq.head
        aggregation.attribute[Long]("accounts") should equal(3)
        aggregation.attribute[Double]("balance_sum") should equal(3861.0D)
        aggregation.attribute[Option[Double]]("balance_max") should equal(Some(1910.99D))
      }
    }
  }

  Class.forName("org.h2.Driver")
  val jdbcUrl = "jdbc:h2:mem:aggregation;MODE=MySQL;TRACE_LEVEL_FILE=2;TRACE_LEVEL_SYSTEM_OUT=2"
  val jdbcUser = "user"
  val jdbcPass = "pass"

  def initializeDatabase(): Unit = {
    LoanPattern.using(DriverManager.getConnection(jdbcUrl, jdbcUser, jdbcPass)) { conn =>
      val initialDdls = Source.fromResource("initial.sql").mkString
      conn.prepareStatement(initialDdls).execute()
      DatabaseManager.loadRuntimeConfig("ReladomoRuntimeConfig_aggregation.xml")
      withTransaction { implicit tx =>
        AccountBalanceFinder.findManyWith(_.businessDate.eq(now())).terminateAll()
      }
    }
  }

}
