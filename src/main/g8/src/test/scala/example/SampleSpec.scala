package example

import java.sql.DriverManager

import com.folio_sec.reladomo.scala_api._
import com.folio_sec.reladomo.scala_api.configuration.DatabaseManager
import com.folio_sec.reladomo.scala_api.TransactionProvider.withTransaction
import com.folio_sec.reladomo.scala_api.util.{LoanPattern, TimestampUtil}
import com.folio_sec.reladomo.scala_api.util.TimestampUtil.now
import kata.domain.scala_api._
import org.scalatest.{BeforeAndAfter, FunSpec, Matchers}

import scala.io.Source

class SampleSpec extends FunSpec with Matchers with BeforeAndAfter {

  before {
    initializeDatabase()
  }

  describe("Customer and CustomerAccount") {

    it("shows examples about non-temporal data model") {
      {
        // ---
        // Fetch all rows from still empty table
        val customers = CustomerFinder.findManyWith(_.all)
        customers.size should equal(0)
        // select count(*) from CUSTOMER t0;

        // Query with multiple conditions
        val finder = { // CustomerFinder#ListFinder[Customer, CustomerList, kata.domain.Customer]
          CustomerFinder.findManyWith { q =>
            q.name.eq("Folio") && q.country.endsWith("N") || q.customerId.lessThan(100)
          }.limit(10).orderByWith(_.customerId.descendingOrderBy)
        }

        finder.size should equal(0)
        // select count(*) from CUSTOMER t0
        //   where  t0.NAME_C = ? and t0.COUNTRY_I like ? or t0.CUSTOMER_I < ?
        //   {1: 'Folio', -- NAME_C
        //    2: '%N',    -- COUNTRY_I
        //    3: 100};    -- CUSTOMER_I

        finder.foreach(row => println(row))
        // select t0.CUSTOMER_I,t0.NAME_C,t0.COUNTRY_I from CUSTOMER t0
        //   where  t0.NAME_C = ? and t0.COUNTRY_I like ? or t0.CUSTOMER_I < ?
        //   order by t0.CUSTOMER_I desc
        //   {1: 'Folio', -- NAME_C
        //    2: '%N',    -- COUNTRY_I
        //    3: 100};    -- CUSTOMER_I

        finder.size should equal(0)
        // select max(t0.CUSTOMER_I) from CUSTOMER t0;
      }

      // ---
      // Insert three rows
      withTransaction { implicit tx =>
        NewCustomer(name = "itohiro73", country = "Japan").insert()
        NewCustomer(name = "matsu-chara", country = "Japan").insert()
        NewCustomer(name = "komsit37", country = "Japan").insert()

        // select t0.SEQUENCE_NAME,t0.NEXT_VALUE from OBJECT_SEQUENCE t0 where  t0.SEQUENCE_NAME = ?
        //   {1: 'Customer'}; -- SEQUENCE_NAME
        // insert into OBJECT_SEQUENCE(SEQUENCE_NAME,NEXT_VALUE) values (?,?)
        //   {1: 'Customer',  -- SEQUENCE_NAME
        //    2: 2};          -- NEXT_VALUE
        // select t0.NEXT_VALUE from OBJECT_SEQUENCE t0 where t0.SEQUENCE_NAME = 
        //   {1: 'Customer'}; -- SEQUENCE_NAME
        // update OBJECT_SEQUENCE set NEXT_VALUE = ?  where SEQUENCE_NAME = ?
        //   {1: 3,           -- NEXT_VALUE
        //    2: 'Customer'}; -- SEQUENCE_NAME
        // select t0.NEXT_VALUE from OBJECT_SEQUENCE t0 where t0.SEQUENCE_NAME = ?
        //   {1: 'Customer'}; -- SEQUENCE_NAME
        // update OBJECT_SEQUENCE set NEXT_VALUE = ?  where SEQUENCE_NAME = ?
        //   {1: 4,           -- NEXT_VALUE
        //    2: 'Customer'}; -- SEQUENCE_NAME

        // insert into CUSTOMER (CUSTOMER_I,NAME_C,COUNTRY_I) values (?,?,?) 
        //   {1: 1,             -- CUSTOMER_I
        //    2: 'itohiro73',   -- NAME_C
        //    3: 'Japan'};      -- COUNTRY_I

        // insert into CUSTOMER (CUSTOMER_I,NAME_C,COUNTRY_I) values (?,?,?)
        //   {1: 2,             -- CUSTOMER_I
        //    2: 'matsu-chara', -- NAME_C
        //    3: 'Japan'};      -- COUNTRY_I

        // insert into CUSTOMER (CUSTOMER_I,NAME_C,COUNTRY_I) values (?,?,?)
        //   {1: 3,             -- CUSTOMER_I
        //    2: 'komsit37',    -- NAME_C
        //    3: 'Japan'};      -- COUNTRY_I
      }

      // Find three saved rows
      {
        val customers = CustomerFinder.findManyWith(_.all)
        customers.size should equal(3)
        // select count(*) from CUSTOMER t0;
      }

      // ---
      // Access relationships (A Customer entity has n CustomerAccount entities)
      val maybeItohiro73 = CustomerFinder.findOneWith(_.name.eq("itohiro73"))
      maybeItohiro73.map(_.accounts.size) should equal(Some(0))
      // select count(*) from CUSTOMER_ACCOUNT t0 where  t0.CUSTOMER_I = ? 
      //   {1: 1}; -- CUSTOMER_I

      withTransaction { implicit tx =>
        maybeItohiro73 match {
          case Some(itohiro73) =>
            NewCustomerAccount(
              accountId = 1,
              customerId = itohiro73.customerId,
              accountName = "Hiroshi Ito",
              accountType = "NORMAL"
            ).insert()
            // insert into CUSTOMER_ACCOUNT(ACCOUNT_I,CUSTOMER_I,ACCOUNT_NAME_C,ACCOUNT_TYPE_C) values (?,?,?,?)
            //   {1: 1,             -- ACCOUNT_I
            //    2: 1,             -- CUSTOMER_I
            //    3: 'Hiroshi Ito', -- ACCOUNT_NAME_C
            //    4: 'NORMAL'};     -- ACCOUNT_TYPE_C
          case _ =>
        }
      }
      maybeItohiro73.map(_.accounts.size) should equal(Some(1))
      // select count(*) from CUSTOMER_ACCOUNT t0 where  t0.CUSTOMER_I = ?
      //   {1: 1}; -- CUSTOMER_I

      // ---
      // Update multiple rows at a time
      withTransaction { implicit tx =>
        val listFinder = CustomerFinder.findManyWith(_.all)
        listFinder.currentList.withCountry("JP").updateAll()
        // select t0.CUSTOMER_I,t0.NAME_C,t0.COUNTRY_I from CUSTOMER t0;
        // update CUSTOMER set COUNTRY_I = ?  where CUSTOMER_I in (?,?,?)
        //   {1: 'JP', -- COUNTRY_I
        //    2: 1,    -- CUSTOMER_I
        //    3: 2,    -- CUSTOMER_I
        //    4: 3};   -- CUSTOMER_I
      }

      // Confirm updated rows
      {
        CustomerFinder.findManyWith(_.country.eq("Japan")).size should equal(0)
        // select count(*) from CUSTOMER t0 where  t0.COUNTRY_I = ?
        //   {1: 'Japan'}; -- COUNTRY_I
        CustomerFinder.findManyWith(_.country.eq("JP")).size should equal(3)
        // select count(*) from CUSTOMER t0 where  t0.COUNTRY_I = ?
        //   {1: 'JP'}; -- COUNTRY_I

        val maybeIto = CustomerFinder.findOneWith { q =>
          q.name.eq("itohiro73") && q.country.eq("JP")
        }
        // No query issued here
        maybeIto.isDefined should equal(true)

        withTransaction { implicit tx =>
          val maybeIto = CustomerFinder.findOneWith { q =>
            q.name.eq("itohiro73") && q.country.eq("JP")
          }
          // select t0.CUSTOMER_I,t0.NAME_C,t0.COUNTRY_I from CUSTOMER t0 where  t0.COUNTRY_I = ? and t0.NAME_C = ?
          //   {1: 'JP',         -- COUNTRY_I
          //    2: 'itohiro73'}; -- NAME_C
          maybeIto.isDefined should equal(true)
        }
      }

      // Deep Fetch
      {
        val customers = CustomerFinder.findManyWith(_.all)
        // select t0.CUSTOMER_I,t0.NAME_C,t0.COUNTRY_I from CUSTOMER t0;
        customers.deepFetch(CustomerFinder.accounts)
        // select t0.ACCOUNT_I,t0.CUSTOMER_I,t0.ACCOUNT_NAME_C,t0.ACCOUNT_TYPE_C from CUSTOMER_ACCOUNT t0
        //   where  t0.CUSTOMER_I in ( ?,?,?)
        //   {1: 1,  -- CUSTOMER_I
        //    2: 2,  -- CUSTOMER_I
        //    3: 3}; -- CUSTOMER_I

        customers.map(_.accounts.size).sum should equal(1)
      }
    }
  }

  describe("Task") {
    it("shows a uni-temporal (=only processingDate supported) data model example") {

      // Insert a data
      withTransaction { implicit tx =>
        NewTask(name = "Learning Reladomo", status = "Open").insert()
        // select max(t0.TASK_ID) from TASK t0;
        // select t0.SEQUENCE_NAME,t0.NEXT_VALUE from OBJECT_SEQUENCE t0 where  t0.SEQUENCE_NAME = ?
        //   {1: 'Task'}; -- SEQUENCE_NAME
        // insert into OBJECT_SEQUENCE(SEQUENCE_NAME,NEXT_VALUE) values (?,?)
        //   {1: 'Task', -- SEQUENCE_NAME
        //    2: 2};     -- NEXT_VALUE

        // insert into TASK(TASK_ID,NAME,STATUS,IN_Z,OUT_Z) values (?,?,?,?,?)
        //   {1: 1, 
        //    2: 'Learning Reladomo',
        //    3: 'Open', 
        //    4: TIMESTAMP '2017-08-26 13:48:23.61', 
        //    5: TIMESTAMP '9999-12-01 23:59:00.0'};
      }

      TaskFinder.findManyWith(_.all).size should equal(1)
      // select count(*) from TASK t0 where  t0.OUT_Z = ?
      //   {1: TIMESTAMP '9999-12-01 23:59:00.0'}; -- OUT_Z

      // Modify the data
      withTransaction { implicit tx =>
        val maybeTask = TaskFinder.findOneWith(_.name.eq("Learning Reladomo"))
        // select t0.TASK_ID,t0.NAME,t0.STATUS,t0.IN_Z,t0.OUT_Z from TASK t0
        //   where  t0.NAME = ? and t0.OUT_Z = ?
        //   {1: 'Learning Reladomo',                -- NAME
        //    2: TIMESTAMP '9999-12-01 23:59:00.0'}; -- OUT_Z

        maybeTask match {
          case Some(task) => task.copy(status = "In Progress").update()
          // update TASK set OUT_Z = ?  where TASK_ID = ? AND OUT_Z = ?
          //   {1: TIMESTAMP '2017-08-26 13:48:23.74', -- OUT_Z
          //    2: 1,                                  -- TASK_ID
          //    3: TIMESTAMP '9999-12-01 23:59:00.0'}; -- OUT_Z

          // insert into TASK(TASK_ID,NAME,STATUS,IN_Z,OUT_Z) values (?,?,?,?,?)
          //   {1: 1,                                  -- TASK_ID
          //    2: 'Learning Reladomo',                -- NAME
          //    3: 'In Progress',                      -- STATUS
          //    4: TIMESTAMP '2017-08-26 13:48:23.74', -- IN_Z
          //    5: TIMESTAMP '9999-12-01 23:59:00.0'}; -- OUT_Z
          case _ =>
        }
      }

      TaskFinder.findManyWith(_.all).size should equal(1)
      // select count(*) from TASK t0 where  t0.OUT_Z = ?
      //   {1: TIMESTAMP '9999-12-01 23:59:00.0'}; -- OUT_Z

      // Terminate the data
      withTransaction { implicit tx =>
        TaskFinder.findOneWith(_.name.eq("Learning Reladomo")).foreach { task =>
          // select t0.TASK_ID,t0.NAME,t0.STATUS,t0.IN_Z,t0.OUT_Z from TASK t0
          //   where  t0.NAME = ? and t0.OUT_Z = ?
          //   {1: 'Learning Reladomo',                -- NAME
          //    2: TIMESTAMP '9999-12-01 23:59:00.0'}; -- OUT_Z

          task.terminate()
          // update TASK set OUT_Z = ?  where TASK_ID = ? AND OUT_Z = ?
          //   {1: TIMESTAMP '2017-08-26 13:48:23.75', -- OUT_Z
          //    2: 1,                                  -- TASK_ID
          //    3: TIMESTAMP '9999-12-01 23:59:00.0'}; -- OUT_Z
        }
      }

      TaskFinder.findManyWith(_.all).size should equal(0)
      // select count(*) from TASK t0 where  t0.OUT_Z = ?
      //   {1: TIMESTAMP '9999-12-01 23:59:00.0'}; -- OUT_Z

    }
  }

  describe("AccountBalance") {
    it("shows a bi-temporal data model (=processingDate and businessDate supported) example") {

      // insert rows
      withTransaction { implicit tx =>
        NewAccountBalance(accountId = 123, balance = 345.67).insert()
        // insert into ACCOUNT_BALANCE (ACCOUNT_I,BALANCE_F,FROM_Z,THRU_Z,IN_Z,OUT_Z) values (?,?,?,?,?,?)
        //   {1: 123,                                 -- ACCOUNT_I
        //    2: 345.67,                              -- BALANCE_F
        //    3: TIMESTAMP '2017-08-27 09:15:51.972', -- FROM_Z
        //    4: TIMESTAMP '9999-12-01 23:59:00.0',   -- THRU_Z
        //    5: TIMESTAMP '2017-08-27 09:15:51.98',  -- IN_Z
        //    6: TIMESTAMP '9999-12-01 23:59:00.0'};  -- OUT_Z

        NewAccountBalance(accountId = 234, balance = 456.11).insert()
        // insert into ACCOUNT_BALANCE (ACCOUNT_I,BALANCE_F,FROM_Z,THRU_Z,IN_Z,OUT_Z) values (?,?,?,?,?,?)
        //   {1: 234,                                 -- ACCOUNT_I
        //    2: 456.11,                              -- BALANCE_F
        //    3: TIMESTAMP '2017-08-27 09:15:51.989', -- FROM_Z
        //    4: TIMESTAMP '9999-12-01 23:59:00.0',   -- THRU_Z
        //    5: TIMESTAMP '2017-08-27 09:15:51.98',  -- IN_Z
        //    6: TIMESTAMP '9999-12-01 23:59:00.0'};  -- OUT_Z
      }

      // Fetch currently active rows
      {
        val finder = AccountBalanceFinder.findManyWith(_.businessDate.eq(now()))
        finder.size should equal(2)
        // select count(*) from ACCOUNT_BALANCE t0 where  t0.FROM_Z <= ? and t0.THRU_Z > ? and t0.OUT_Z = ?
        //   {1: TIMESTAMP '2017-08-27 09:15:51.998', -- FROM_Z
        //    2: TIMESTAMP '2017-08-27 09:15:51.998', -- THRU_Z
        //    3: TIMESTAMP '9999-12-01 23:59:00.0'};  -- OUT_Z
      }

      // Modify the first one
      withTransaction { implicit tx =>
        val account1 = AccountBalanceFinder.findOneWith { q =>
          q.accountId.eq(123) && q.businessDate.eq(now())
        }
        // select t0.ACCOUNT_I,t0.BALANCE_F,t0.FROM_Z,t0.THRU_Z,t0.IN_Z,t0.OUT_Z from ACCOUNT_BALANCE t0
        //   where  t0.ACCOUNT_I = ? and t0.FROM_Z <= ? and t0.THRU_Z > ? and t0.OUT_Z = ?
        //   {1: 123,                                 -- ACCOUNT_I
        //    2: TIMESTAMP '2017-08-27 09:15:52.013', -- FROM_Z
        //    3: TIMESTAMP '2017-08-27 09:15:52.013', -- THRU_Z
        //    4: TIMESTAMP '9999-12-01 23:59:00.0'};  -- OUT_Z

        account1.isDefined should equal(true)

        account1.foreach(_.copy(balance = 10000.12).update())
        // update ACCOUNT_BALANCE set THRU_Z = ? , OUT_Z = ?  where ACCOUNT_I = ? AND THRU_Z = ? AND OUT_Z = ?
        //   {1: TIMESTAMP '2017-08-28 09:15:52.02', -- THRU_Z (toIsInclusive="false")
        //    2: TIMESTAMP '2017-08-27 09:15:52.02', -- OUT_Z
        //    3: 123,                                -- ACCOUNT_I
        //    4: TIMESTAMP '9999-12-01 23:59:00.0',  -- THRU_Z
        //    5: TIMESTAMP '9999-12-01 23:59:00.0'}; -- OUT_Z

        // insert into ACCOUNT_BALANCE (ACCOUNT_I,BALANCE_F,FROM_Z,THRU_Z,IN_Z,OUT_Z) values (?,?,?,?,?,?)
        //   {1: 123,                                 -- ACCOUNT_I
        //    2: 345.67,                              -- BALANCE_F
        //    3: TIMESTAMP '2017-08-27 09:15:51.972', -- FROM_Z
        //    4: TIMESTAMP '2017-08-27 09:15:52.013', -- THRU_Z
        //    5: TIMESTAMP '2017-08-27 09:15:52.02',  -- IN_Z
        //    6: TIMESTAMP '9999-12-01 23:59:00.0'};  -- OUT_Z

        // insert into ACCOUNT_BALANCE (ACCOUNT_I,BALANCE_F,FROM_Z,THRU_Z,IN_Z,OUT_Z) values (?,?,?,?,?,?)
        //   {1: 123,                                 -- ACCOUNT_I
        //    2: 10000.12,                            -- BALANCE_F
        //    3: TIMESTAMP '2017-08-27 09:15:52.013', -- FROM_Z
        //    4: TIMESTAMP '9999-12-01 23:59:00.0',   -- THRU_Z
        //    5: TIMESTAMP '2017-08-27 09:15:52.02',  -- IN_Z
        //    6: TIMESTAMP '9999-12-01 23:59:00.0'};  -- OUT_Z
      }

      // Fetch currently active rows
      {
        val finder = AccountBalanceFinder.findManyWith(_.businessDate.eq(now()))
        finder.size should equal(2)
        // select count(*) from ACCOUNT_BALANCE t0 where  t0.FROM_Z <= ? and t0.THRU_Z > ? and t0.OUT_Z = ?
        //   {1: TIMESTAMP '2017-08-27 09:15:52.033', -- FROM_Z
        //    2: TIMESTAMP '2017-08-27 09:15:52.033', -- THRU_Z
        //    3: TIMESTAMP '9999-12-01 23:59:00.0'};  -- OUT_Z
      }

      // Terminate the second one
      withTransaction { implicit tx =>
        val account2 = AccountBalanceFinder.findOneWith { q =>
          q.accountId.eq(234) && q.businessDate.eq(TimestampUtil.now())
        }
        // select t0.ACCOUNT_I,t0.BALANCE_F,t0.FROM_Z,t0.THRU_Z,t0.IN_Z,t0.OUT_Z from ACCOUNT_BALANCE t0
        //   where  t0.ACCOUNT_I = ? and t0.FROM_Z <= ? and t0.THRU_Z > ? and t0.OUT_Z = ?
        //   {1: 234,                                 -- ACCOUNT_I
        //    2: TIMESTAMP '2017-08-27 09:15:52.035', -- FROM_Z
        //    3: TIMESTAMP '2017-08-27 09:15:52.035', -- THRU_Z
        //    4: TIMESTAMP '9999-12-01 23:59:00.0'};  -- OUT_Z

        account2.foreach(_.terminate())
        // update ACCOUNT_BALANCE set THRU_Z = ? , OUT_Z = ?
        //   where ACCOUNT_I = ? AND THRU_Z = ? AND OUT_Z = ?
        //   {1: TIMESTAMP '2017-08-28 09:15:52.04', -- THRU_Z (toIsInclusive="false")
        //    2: TIMESTAMP '2017-08-27 09:15:52.04', -- OUT_Z
        //    3: 234,                                -- ACCOUNT_I
        //    4: TIMESTAMP '9999-12-01 23:59:00.0',  -- THRU_Z
        //    5: TIMESTAMP '9999-12-01 23:59:00.0'}; -- OUT_Z

        // insert into ACCOUNT_BALANCE(ACCOUNT_I,BALANCE_F,FROM_Z,THRU_Z,IN_Z,OUT_Z) values (?,?,?,?,?,?)
        //   {1: 234,                                 -- ACCOUNT_I
        //    2: 456.11,                              -- BALANCE_F
        //    3: TIMESTAMP '2017-08-27 09:15:51.989', -- FROM_Z
        //    4: TIMESTAMP '2017-08-27 09:15:52.035', -- THRU_Z
        //    5: TIMESTAMP '2017-08-27 09:15:52.04',  -- IN_Z
        //    6: TIMESTAMP '9999-12-01 23:59:00.0'};  -- OUT_Z
      }

      // Fetch currently active rows
      {
        val finder = AccountBalanceFinder.findManyWith(_.businessDate.eq(now()))
        finder.size should equal(1)
        // select count(*) from ACCOUNT_BALANCE t0 where  t0.FROM_Z <= ? and t0.THRU_Z > ? and t0.OUT_Z = ?
        //   {1: TIMESTAMP '2017-08-27 09:15:52.047', -- FROM_Z
        //    2: TIMESTAMP '2017-08-27 09:15:52.047', -- THRU_Z
        //    3: TIMESTAMP '9999-12-01 23:59:00.0'};  -- OUT_Z
      }
    }

  }

  Class.forName("org.h2.Driver")
  val jdbcUrl = "jdbc:h2:mem:sample;MODE=MySQL;TRACE_LEVEL_FILE=2;TRACE_LEVEL_SYSTEM_OUT=2"
  val jdbcUser = "user"
  val jdbcPass = "pass"

  def initializeDatabase(): Unit = {
    val initialDdls = Source.fromResource("initial.sql").mkString
    LoanPattern.using(DriverManager.getConnection(jdbcUrl, jdbcUser, jdbcPass)) { conn =>
      conn.prepareStatement(initialDdls).execute()
      DatabaseManager.loadRuntimeConfig("ReladomoRuntimeConfig.xml")
      withTransaction { implicit tx =>
        AccountBalanceFinder.findManyWith(_.businessDate.eq(now())).terminateAll()
      }
    }
  }

}
