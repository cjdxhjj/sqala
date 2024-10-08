package sqala.jdbc

import sqala.dsl.Result
import sqala.dsl.statement.dml.{Delete, Insert, Save, Update}
import sqala.dsl.statement.query.*
import sqala.printer.Dialect
import sqala.util.{queryToString, statementToString}

import java.sql.Connection
import javax.sql.DataSource

class JdbcContext(val dataSource: DataSource, val dialect: Dialect)(using val logger: Logger):
    private[sqala] inline def execute[T](inline handler: Connection => T): T =
        val conn = dataSource.getConnection()
        val result = handler(conn)
        conn.close()
        result

    private[sqala] def executeDml(sql: String, args: Array[Any]): Int =
        logger(sql, args)
        execute(c => jdbcExec(c, sql, args))

    def execute(insert: Insert[?, ?]): Int =
        val (sql, args) = statementToString(insert.ast, dialect, true)
        executeDml(sql, args)

    def executeReturnKey(insert: Insert[?, ?]): List[Long] =
        val (sql, args) = statementToString(insert.ast, dialect, true)
        logger(sql, args)
        execute(c => jdbcExecReturnKey(c, sql, args))

    def execute(update: Update[?, ?]): Int =
        val (sql, args) = statementToString(update.ast, dialect, true)
        executeDml(sql, args)

    def execute(delete: Delete[?]): Int =
        val (sql, args) = statementToString(delete.ast, dialect, true)
        executeDml(sql, args)

    def execute(save: Save): Int =
        val (sql, args) = statementToString(save.ast, dialect, true)
        executeDml(sql, args)

    def execute(nativeSql: NativeSql): Int =
        val NativeSql(sql, args) = nativeSql
        executeDml(sql, args)

    def fetchTo[T](query: Query[?, ?])(using JdbcDecoder[T]): List[T] =
        val (sql, args) = queryToString(query.ast, dialect, true)
        logger(sql, args)
        execute(c => jdbcQuery(c, sql, args))

    def fetch[T](query: Query[T, ?])(using r: Result[T], d: JdbcDecoder[r.R]): List[r.R] =
        fetchTo[r.R](query)

    def fetchTo[T](query: WithRecursive[?])(using JdbcDecoder[T]): List[T] =
        val (sql, args) = queryToString(query.ast, dialect, true)
        logger(sql, args)
        execute(c => jdbcQuery(c, sql, args))

    def fetch[T](query: WithRecursive[T])(using r: Result[T], d: JdbcDecoder[r.R]): List[r.R] =
        fetchTo[r.R](query)

    def fetch[T <: Record](nativeSql: StaticNativeSql[T]): List[T] =
        logger(nativeSql.sql, nativeSql.args)
        execute(c => jdbcQueryToMap(c, nativeSql.sql, nativeSql.args).map(Record(_).asInstanceOf[T]))

    def fetchTo[T](nativeSql: NativeSql)(using JdbcDecoder[T]): List[T] =
        val NativeSql(sql, args) = nativeSql
        logger(sql, args)
        execute(c => jdbcQuery(c, sql, args))

    def fetchToMap[T](nativeSql: NativeSql)(using JdbcDecoder[T]): List[Map[String, Any]] =
        val NativeSql(sql, args) = nativeSql
        logger(sql, args)
        execute(c => jdbcQueryToMap(c, sql, args))

    def fetchTo[T](nativeSql: (String, Array[Any]))(using JdbcDecoder[T]): List[T] =
        val (sql, args) = nativeSql
        logger(sql, args)
        execute(c => jdbcQuery(c, sql, args))

    def fetchToMap[T](nativeSql: (String, Array[Any]))(using JdbcDecoder[T]): List[Map[String, Any]] =
        val (sql, args) = nativeSql
        logger(sql, args)
        execute(c => jdbcQueryToMap(c, sql, args))

    def pageTo[T](query: Query[?, ?], pageSize: Int, pageNo: Int, returnCount: Boolean = true)(using JdbcDecoder[T]): Page[T] =
        val data = if pageSize == 0 then Nil
            else fetchTo[T](query.drop(if pageNo <= 1 then 0 else pageSize * (pageNo - 1)).take(pageSize))
        val count = if returnCount then fetch(query.size).head else 0L
        val total = if count == 0 || pageSize == 0 then 0
            else if count % pageSize == 0 then count / pageSize
            else count / pageSize + 1
        Page(total, count, pageNo, pageSize, data)

    def page[T](query: Query[T, ?], pageSize: Int, pageNo: Int, returnCount: Boolean = true)(using r: Result[T], d: JdbcDecoder[r.R]): Page[r.R] =
        pageTo[r.R](query, pageSize, pageNo, returnCount)

    def findTo[T](query: Query[?, ?])(using JdbcDecoder[T]): Option[T] =
        fetchTo[T](query.take(1)).headOption

    def find[T](query: Query[T, ?])(using r: Result[T], d: JdbcDecoder[r.R]): Option[r.R] =
        findTo[r.R](query)

    def fetchSize[T](query: Query[T, ?]): Long =
        val sizeQuery = query.size
        fetch(sizeQuery).head

    def fetchExists[T](query: Query[T, ?]): Boolean =
        val existsQuery = query.exists
        fetch(existsQuery).head

    def showSql[T](query: Query[T, ?]): String =
        queryToString(query.ast, dialect, true)._1

    def transaction[T](block: JdbcTransactionContext ?=> T): T =
        val conn = dataSource.getConnection()
        conn.setAutoCommit(false)
        try
            given JdbcTransactionContext = new JdbcTransactionContext(conn, dialect)
            val result = block
            conn.commit()
            result
        catch case e: Exception =>
            conn.rollback()
            throw e
        finally
            conn.setAutoCommit(true)
            conn.close()

    def transactionWithIsolation[T](isolation: Int)(block: JdbcTransactionContext ?=> T): T =
        val conn = dataSource.getConnection()
        conn.setAutoCommit(false)
        conn.setTransactionIsolation(isolation)
        try
            given JdbcTransactionContext = new JdbcTransactionContext(conn, dialect)
            val result = block
            conn.commit()
            result
        catch case e: Exception =>
            conn.rollback()
            throw e
        finally
            conn.setAutoCommit(true)
            conn.close()