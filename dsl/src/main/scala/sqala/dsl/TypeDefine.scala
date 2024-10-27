package sqala.dsl

import sqala.dsl.statement.query.ResultSize

import scala.compiletime.ops.int.S

type Wrap[T, F[_]] = T match
    case F[t] => T
    case _ => F[T]

type Unwrap[T, F[_]] = T match
    case F[t] => t
    case _ => T

type InverseMap[T, F[_]] = T match
    case x *: xs => Tuple.InverseMap[x *: xs, F]
    case F[x] => x

type MapField[X, T] = T match
    case Option[_] => Expr[Wrap[X, Option], ColumnKind]
    case _ => Expr[X, ColumnKind]

type Index[T <: Tuple, X, N <: Int] <: Int = T match
    case X *: xs => N
    case x *: xs => Index[xs, X, S[N]]

type SimpleKind = ColumnKind | CommonKind | ValueKind

type CompositeKind = CommonKind | AggOperationKind | WindowKind

type SortKind = ColumnKind | CommonKind | WindowKind

type FuncKind = CommonKind | AggKind | AggOperationKind | WindowKind

type ResultKind[L <: ExprKind, R <: ExprKind] <: CompositeKind = (L, R) match
    case (WindowKind, r) => WindowKind
    case (l, WindowKind) => WindowKind
    case (AggKind | AggOperationKind | GroupKind, r) => AggOperationKind
    case (l, AggKind | AggOperationKind | GroupKind) => AggOperationKind
    case (l, r) => CommonKind

type CheckOverPartition[T] <: Boolean = T match
    case Expr[_, k] => k match
        case SimpleKind => true
        case _ => false
    case Expr[_, k] *: xs => k match
        case SimpleKind => CheckOverPartition[xs]
        case _ => false
    case EmptyTuple => true

type CheckOverOrder[T] <: Boolean = T match
    case OrderBy[_, k] => k match
        case ColumnKind | CommonKind => true
        case _ => false
    case OrderBy[_, k] *: xs => k match
        case ColumnKind | CommonKind => CheckOverOrder[xs]
        case _ => false
    case EmptyTuple => true

type QuerySize[N <: Int] <: ResultSize = N match
    case 1 => ResultSize.OneRow
    case _ => ResultSize.ManyRows

type ProjectionSize[IsAgg <: Boolean] <: ResultSize = IsAgg match
    case true => ResultSize.OneRow
    case _ => ResultSize.ManyRows