package sqala.dsl

import sqala.dsl.statement.query.ResultSize

import scala.compiletime.ops.boolean.&&

type Wrap[T, F[_]] = T match
    case F[t] => T
    case _ => F[T]

type Unwrap[T, F[_]] = T match
    case F[t] => t
    case _ => T

type ToTuple[T] <: Tuple = T match
    case h *: t => h *: t
    case EmptyTuple => EmptyTuple
    case _ => Tuple1[T]

type InverseMap[T, F[_]] = T match
    case x *: xs => Tuple.InverseMap[x *: xs, F]
    case F[x] => x

type CheckOverPartition[T] <: Boolean = T match
    case Expr[_, k] *: xs => k match
        case SimpleKind => CheckOverPartition[xs]
        case _ => false
    case EmptyTuple => true

type CheckOverOrder[T] <: Boolean = T match
    case OrderBy[_, k] *: xs => k match
        case ColumnKind | CommonKind => CheckOverOrder[xs]
        case _ => false
    case EmptyTuple => true

type CheckGrouping[T] <: Boolean = T match
    case Expr[_, k] *: xs => k match
        case GroupKind => CheckGrouping[xs]
        case _ => false
    case EmptyTuple => true

type QuerySize[N <: Int] <: ResultSize = N match
    case 1 => ResultSize.OneRow
    case _ => ResultSize.ManyRows

type ProjectionSize[IsAgg <: Boolean] <: ResultSize = IsAgg match
    case true => ResultSize.OneRow
    case _ => ResultSize.ManyRows
