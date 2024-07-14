package sqala.printer

import sqala.ast.expr.SqlBinaryOperator.Is
import sqala.ast.expr.{SqlCase, SqlExpr}
import sqala.ast.expr.SqlExpr.*
import sqala.ast.limit.SqlLimit
import sqala.ast.statement.SqlStatement
import sqala.ast.order.SqlOrderBy
import sqala.ast.order.SqlOrderByNullsOption.*
import sqala.ast.order.SqlOrderByOption.*

class SqlitePrinter(override val prepare: Boolean) extends SqlPrinter(prepare):
    override def printLimit(limit: SqlLimit): Unit =
        sqlBuilder.append(" LIMIT ")
        printExpr(limit.offset)
        sqlBuilder.append(", ")
        printExpr(limit.limit)

    override def printUpsert(upsert: SqlStatement.Upsert): Unit =
        sqlBuilder.append("INSERT OR REPLACE INTO ")
        printTable(upsert.table)

        sqlBuilder.append(" (")
        printList(upsert.columns)(printExpr)
        sqlBuilder.append(")")

        sqlBuilder.append(" VALUES (")
        printList(upsert.values)(printExpr)
        sqlBuilder.append(")")

    override def printIntervalExpr(expr: SqlExpr.Interval): Unit = {}

    override def printOrderBy(orderBy: SqlOrderBy): Unit =
        val order = orderBy.order match
            case None | Some(Asc) => Asc
            case _ => Desc
        val orderExpr = Case(SqlCase(Binary(orderBy.expr, Is, Null), NumberLiteral(1)) :: Nil, NumberLiteral(0))
        (order, orderBy.nullsOrder) match
            case (_, None) | (Asc, Some(First)) | (Desc, Some(Last)) =>
                printExpr(orderBy.expr)
                sqlBuilder.append(s" ${order.order}")
            case (Asc, Some(Last)) | (Desc, Some(First)) =>
                printExpr(orderExpr)
                sqlBuilder.append(s" ${order.order}, ")
                printExpr(orderBy.expr)
                sqlBuilder.append(s" ${order.order}")