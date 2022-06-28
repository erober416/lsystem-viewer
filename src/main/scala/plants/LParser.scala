package plants

// Class used to carry position information within the source code
case class Position(gapLine: Int, gapCol: Int, startLine: Int, startCol: Int, endLine: Int, endCol: Int)

object Tokens {

  abstract class Token {
    var pos: Position = _
  }
  case object EOF extends Token
  case class Number(x: Double) extends Token
  case class Keyword(x: String) extends Token
  case class Delim(x: Char) extends Token
  case class Word(x : String) extends Token
}

// Scanner
class LScanner(in: Reader[Char]) extends Reader[Tokens.Token] with Reporter {
  import Tokens._

  // Position handling
  def pos = in.pos
  def input = in.input

  // Current line in the file
  var line = 0

  // lineStarts(i) contains the offset of the i th line within the file
  val lineStarts = scala.collection.mutable.ArrayBuffer(0)

  // Current column in the file
  def column = pos - lineStarts(line)

  // Extract the i th line of code.
  def getLine(i: Int) = {
    val start = lineStarts(i)
    val end = input.indexOf('\n', start)

    if (end < 0)
      input.substring(start)
    else
      input.substring(start, end)
  }

  // Information for the current Position
  var gapLine = 0;
  var gapCol = 0;
  var startLine = 0;
  var startCol = 0;
  var endLine = 0;
  var endCol = 0;

  override def abort(msg: String) = {
    abort(msg, showSource(getCurrentPos()))
  }

  /*
   * Show the line of code and highlight the token at position p
   */
  def showSource(p: Position) = {
    val width = if (p.endLine == p.startLine) (p.endCol - p.startCol) else 0

    val header = s"${p.startLine + 1}:${p.startCol + 1}: "
    val line1 = getLine(p.startLine)
    val line2 = " "*(p.startCol+header.length) + "^"*(width max 1)
    header + line1 + '\n' + line2
  }

  def isAlpha(c: Char) =
    ('a' <= c && c <= 'z') || ('A' <= c && c <= 'Z')

  def isDigit(c: Char) = ('0' <= c && c <= '9') || c == '.'

  def isAlphaNum(c: Char) = isAlpha(c) || isDigit(c)

  def isLetter(c: Char) = isAlphaNum(c) || '[' == c || ']' == c || '&' == c || '\\' == c  || '$' == c || '|' == c

  val isWhiteSpace = Set(' ','\t','\n','\r')

  //  Operators start with one of the following characters
  val isOperator   = Set('=', '>', '#', '+', '-', '/', '*', '<', '!', '^')

  // List of delimiters
  // TODO: Update this as delimiters are added to our language
  val isDelim      = Set(':', '<', '>', '(', ')', ',')

  // List of keywords
  // TODO: Update this as keywords are added to our language
  val isKeyword    = Set[String]("P", "W", "=>", "angle", "#", "define")

  def getWord() = {
    val buf = new StringBuilder
    while (in.hasNext(isLetter)) {
      buf += in.next()
    }
    val s = buf.toString
    if (isKeyword(s)) Keyword(s) else Word(s)
  }

  /*
   * Extract a number from the stream and return it.
   * Raise an error if there is overflow.
   *
   * NOTE: An integer can be between 0 and (2 to the power 31) minus 1
   */
  /*def getNum() = {
    var num = in.next().toInt - '0'
    while (in.hasNext(isDigit)) {
      num = Math.multiplyExact(num, 10)
      num = Math.addExact(num, in.next().toInt - '0')
    }
    Number(num)
  }*/

  def getNum() = {
    val num = new StringBuilder
    while (in.hasNext(isDigit)) {
      num += in.next()
    }
    val snum = num.toString()
    Number(snum.toDouble)
  }

  /*
  * Extract an operator from the stream
  */
  def getOperator() = {
    val buf = new StringBuilder
    do {
      buf += in.next()
    } while (in.hasNext(isOperator))
    val s = buf.toString
    // "=>" is a keyword
    if (isKeyword(s)) Keyword(s)
    else Word(s)
  }

  /*
   * Extract a raw token from the stream.
   * i.e. without position information.
   */
  def getRawToken(): Token = {
    if (in.hasNext(isDigit)) {
      getNum()
    } else if (in.hasNext(isOperator)) {
      getOperator()
    } else if (in.hasNext(isLetter)) {
      getWord()
    } else if (in.hasNext(isDelim)) {
      Delim(in.next())
    } else if (!in.hasNext) {
      EOF
    } else {
      abort(s"unexpected character")
    }
  }

  /*
   * Skip white space and comments. Stop at the next token.
   */
  def skipWhiteSpace() = {
    while (in.hasNext(isWhiteSpace)) {
      // Update file statistics if new line
      if (in.peek == '\n') {
        lineStarts += pos + 1
        line += 1
      }
      in.next()
    }
  }

  def getCurrentPos() = {
    endLine = line; endCol = column
    Position(gapLine,gapCol,startLine,startCol,endLine,endCol)
  }

  /*
   * Extract a token and set position information
   */
  def getToken(): Token = {
    gapLine = line; gapCol = column
    skipWhiteSpace()
    startLine = line; startCol = column
    val tok = getRawToken()
    tok.pos = getCurrentPos()

    tok
  }

  var peek  = getToken()
  var peek1 = getToken()
  def hasNext: Boolean = peek != EOF
  def hasNext(f: Token => Boolean) = f(peek)
  def hasNext2(f: (Token, Token) => Boolean) = f(peek, peek1)
  def next() = {
    val res = peek
    peek = peek1
    peek1 = getToken()
    res
  }
}

class Parser(in: LScanner) extends Reporter {
  import Tokens._

  /*
   * Overloaded methods that show the source code
   * and highlight the current token when reporting
   * an error.
   */
  override def expected(msg: String) = {
    expected(msg, in.showSource(in.peek.pos))
  }

  override def abort(msg: String) = {
    abort(msg, in.showSource(in.peek.pos))
  }

  def error(msg: String, pos: Position): Unit =
    error(msg, in.showSource(pos))

  def warn(msg: String, pos: Position): Unit =
    warn(msg, in.showSource(pos))

  def accept(c: Char) = {
    if (in.hasNext(_ == Delim(c))) in.next()
    else expected(s"'$c'")
  }

  def accept(s: String) = {
    if (in.hasNext(_ == Keyword(s))) in.next()
    else expected(s"'$s'")
  }

  /*
   * Auxilaries functions
   * Test and extract data
   */
  def isNum(x: Token) = x match {
    case Number(x) => true
    case _ => false
  }

  def getNum(): (Double, Position) = {
    if (!in.hasNext(isNum)) expected("Number")
    val pos = in.peek.pos
    val Number(x) = in.next()
    (x, pos)
  }

  /*
   * Test if the following token is an operator.
   */
  def isOperator(x: Token) = x match {
    case Word(x) => in.isOperator(x.charAt(0))
    case _ => false
  }

  /*
   * Test if the following token is a word.
   */
  def isWord(x: Token) = x match {
    case Word(x) => true
    case _ => false
  }

  def getWord(): (String, Position) = {
    if (!in.hasNext(isWord)) expected("Word")
    val pos = in.peek.pos
    val Word(x) = in.next()
    (x, pos)
  }

  def getOperator(): (String, Position) = {
    if (!in.hasNext(isOperator)) expected("Operator")
    val pos = in.peek.pos
    val Word(x) = in.next()
    (x, pos)
  }

  /*
   * Test if the following token is an infix
   * operator with highest precedence
   */
  def isInfixOp(min: Int)(x: Token) = isOperator(x) && (x match {
    case Word(x) => prec(x) >= min
    case _ => false
  })


  /*
   * Define precedence of operator.
   * Negative precedence means that the operator can
   * not be used as an infix operator within a simple expression.
   *
   * CHANGED: boolean operators have precedence of 0
   */
  def prec(a: String) = a match { // higher bind tighter
    case "!=" | "==" => 1
    case "<" | ">" | "<=" | ">=" => 2
    case "+" | "-" => 3
    case "*" | "/" => 4
    case "^" => 5
    case _ => 0
  }

  def assoc(a: String) = a match {
    case "+" | "-" | "*" | "/"  => 1
    case _    => 1
  }

}

class InstructionParser(in: LScanner) extends Parser(in) {
  import Tokens._

  def parseCode(): (Axiom, ProductionMap) = {
    var axiom = Seq[(String, Seq[Double])]()
    while(in.hasNext(isWord)) {
      val name = getWord()._1
      accept('(')
      var args = Seq[Double]()
      while(in.hasNext(isNum)) {
        args = args :+ getNum()._1
        if (in.peek == Delim(',')) {
          accept(',')
        }
      }
      accept(')')
      axiom = axiom :+ (name, args)
    }
    (new Axiom(axiom), null)
  }
}

/**
 * Definition of our target language.
 *
 * The different nodes of the AST also keep Position information
 * for error handling during the semantic analysis.
 */
object Language {
  abstract class Exp {
    var pos: Position = _
    def withPos(p: Position) = {
      pos = p
      this
    }
  }
  case class Lit(value: Any) extends Exp
  case class Prim(op: String, args: List[Exp]) extends Exp
  case class Production(pred: Module, succ: Seq[Module], lcontext: Seq[Module], rcontext: Seq[Module], weight: Int, cond: Exp) extends Exp
  case class Module(name: String, args: Seq[Exp]) extends Exp
  case class Ref(x: String) extends Exp
}

/*
 * <axiom>      ::= "W:" <pword>
 * <production> ::= "P:" [<pword> "<"] <module> [">" <pword>] "=>" <pword> [: <Int>]
 *
 * <lsystem>    ::= <axiom> [<production>]*
 */
class ILParser(in: LScanner) extends InstructionParser(in) {
  import Tokens._
  import Language._

  var map = Map[String, Double]()
  var angle = 90.0

  def preprocess: Unit = {
    val interpreter = new ValueInterpreter
    while (in.peek == Keyword("#")) {
      accept("#")
      if (in.peek == Keyword("define")) {
        accept("define")
        val a = getWord()._1
        val b = parseSimpleExpression(0)
        var l = List[(String, Exp)]()
        map foreach { m => l = l :+ (m._1, Lit(m._2))}
        val nb = interpreter.eval(b)(interpreter.ValueEnv().withVals(l))
        nb match {
          case Lit(x: Double) => map = map + (a -> x)
        }
      }
    }
  }

  override def parseCode: (Axiom, ProductionMap) = {
    preprocess
    if (in.peek == Keyword("angle")) {
      accept("angle")
      accept(':')
      angle = getNum()._1
    }
    accept("W")
    accept(':')
    val interpreter = new ValueInterpreter
    val mods = parsePWord map (mod => interpreter.run(mod))
    var axiom = Seq[(String, Seq[Double])]()
    mods foreach {
      case mod@Module(name, args) =>
        var nargs = Seq[Double]()
        args foreach {
          case _@Lit(x: Double) =>
            nargs = nargs :+ x
        }
        axiom = axiom :+ (name, nargs)
    }
    var productions = Seq[Production]()
    while (in.peek == Keyword("P")) {
      val p = parseProduction
      productions = productions :+ p
    }
    val prodmap = ProductionMap().withProductions(productions)
    (new Axiom(axiom), prodmap)
  }

  def parseProduction: Production = {
    accept("P")
    accept(':')
    var a = Seq(Module("", Seq()))
    var b = Seq(Module("", Seq()))
    var c = Seq(Module("", Seq()))
    a = parsePWord
    if (in.peek == Word("<")) {
      getWord
      b = parsePWord
      if (in.peek == Word(">")) {
        getWord
        c = parsePWord
      } else {
        c = Seq(Module("", Seq()))
      }
    } else if (in.peek == Word(">")) {
      getWord
      b = a
      a = Seq(Module("", Seq()))
      c = parsePWord
    } else {
      b = a
      a = Seq(Module("", Seq()))
      c = Seq(Module("", Seq()))
    }
    var cond: Exp = Lit(true)
    if (in.peek == Delim(':')) {
      accept(':')
      cond = parseSimpleExpression(0)
    }
    accept("=>")
    val s = parsePWord
    var n = 0.0
    if (in.peek == Delim(':')) {
      accept(':')
      n = getNum()._1
    }
    val p = Production(b.head, s, a, c, n.toInt, cond)
    p
  }

  def parseAtom: Exp = (in.peek, in.peek1) match {
    case (Number(x), _) =>
      val (_, pos) = getNum()
      Lit(x).withPos(pos)
    case (Delim('('), _) =>
      in.next()
      val res = parseSimpleExpression(0)
      accept(')')
      res
    case (Word(x), _) =>
      val (_, pos) = getWord()
      val y = map.get(x)
      if (y.isEmpty) {
        Ref(x).withPos(pos)
      } else {
        Lit(map(x))
      }
    case _ =>
      abort(s"Illegal start of simple expression")
  }

  def parseUAtom: Exp = if (in.hasNext(isOperator)) {
    val (op, pos) = getWord()
    Prim(op, List(parseAtom)).withPos(pos)
  } else {
    parseAtom
  }

  def parseSimpleExpression(min: Int): Exp = {
    var res = parseUAtom
    while (in.hasNext(isInfixOp(min))) {
      val (op, pos) = getOperator()
      val nMin = prec(op) + assoc(op)
      val rhs = parseSimpleExpression(nMin)
      res = Prim(op, List(res, rhs)).withPos(pos)
    }
    res
  }

  def parseModule: Module = {
    var name = ""
    if (isWord(in.peek)) {
      name = getWord()._1
    } else {
      name = getOperator()._1
    }
    var args = Seq[Exp]()
    if (in.peek == Delim('(')) {
      accept('(')
      args = args :+ parseSimpleExpression(0)
      while(in.peek == Delim(',')) {
        accept(',')
        args = args :+ parseSimpleExpression(0)
      }
      accept(')')
    }
    Module(name, args)
  }

  // Parse parametric word
  def parsePWord: Seq[Module] = {
    var pword = Seq[Module]()
    while (isWord(in.peek) && in.peek != Word("<") && in.peek != Word(">")) {
      var m = parseModule
      if (m.name.length > 1) {
        var npword = Seq[Module]()
        while (m.name.length > 1) {
          if (m.args.nonEmpty) {
            npword = Module(m.name.last.toString, m.args) +: npword
          } else {
            npword = Module(m.name.last.toString, Seq()) +: npword
          }
          m = Module(m.name.substring(0, m.name.length - 1), Seq())
        }
        npword = m +: npword
        pword = pword.appendedAll(npword)
      } else {
        m = Module(m.name.substring(0, 1), m.args)
        pword = pword :+ m
      }
    }
    var npword = Seq[Module]()

    // Fill in blank modules with default values
    pword foreach {
      case _@Module(name, args) =>
        val c = name.charAt(0)
        var nargs = args
        if (args.isEmpty) {
          c match {
            case '+' | '-' | '&' | '^' | '\\' | '/' =>
              nargs = args :+ Lit(angle)
            case 'F' | 'G' | 'f' | 'g' =>
              nargs = args :+ Lit(10.0)
            case _ =>
              nargs = args :+ Lit(0.0)
          }
        }
        npword = npword :+ Module(name, nargs)
    }
    npword
  }
}