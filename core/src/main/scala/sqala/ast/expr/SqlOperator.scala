package sqala.ast.expr

enum SqlBinaryOperator(val operator: String, val priority: Int):
    case Times extends SqlBinaryOperator("*", 60)
    case Div extends SqlBinaryOperator("/", 60)
    case Mod extends SqlBinaryOperator("%", 60)
    case Plus extends SqlBinaryOperator("+", 50)
    case Minus extends SqlBinaryOperator("-", 50)
    case Json extends SqlBinaryOperator("->", 40)
    case JsonText extends SqlBinaryOperator("->>", 40)
    case Equal extends SqlBinaryOperator("=", 30)
    case NotEqual extends SqlBinaryOperator("<>", 30)
    case In extends SqlBinaryOperator("IN", 30)
    case NotIn extends SqlBinaryOperator("NOT IN", 30)
    case GreaterThan extends SqlBinaryOperator(">", 30)
    case GreaterThanEqual extends SqlBinaryOperator(">=", 30)
    case LessThan extends SqlBinaryOperator("<", 30)
    case LessThanEqual extends SqlBinaryOperator("<=", 30)
    case Like extends SqlBinaryOperator("LIKE", 30)
    case NotLike extends SqlBinaryOperator("NOT LIKE", 30)
    case And extends SqlBinaryOperator("AND", 20)
    case Or extends SqlBinaryOperator("OR", 10)
    case Custom(op: String) extends SqlBinaryOperator(op, 0)

enum SqlUnaryOperator(val operator: String):
    case Positive extends SqlUnaryOperator("+")
    case Negative extends SqlUnaryOperator("-")
    case Not extends SqlUnaryOperator("NOT")
    case Custom(op: String) extends SqlUnaryOperator(op)