package plants

abstract class Interpreter {
  import Language._
  def run(ast: Language.Exp): Language.Exp
}

/**
 * This interpreter specifies the semantics of our
 * programming language.
 *
 * The evaluation of each node returns a simplified expression
 */
class ValueInterpreter extends Interpreter with BugReporter {
  import Language._

  /*
   * Expues for primitives operators. Already stored in the environment.
   */
  val primitives = Map[String, BoxedExp]()


  /**
   * Env of the interpreter. Keeps track of the value
   * of each variable defined.
   */
  class Env {
    def undef(name: String) =
      BUG(s"Undefined identifier $name (should have been found during the semantic analysis)")

    def updateVar(name: String, v: Exp): Exp = undef(name)
    def apply(name: String): Exp = undef(name)
  }

  case class BoxedExp(var v: Exp)
  case class ValueEnv(
                       vars: Map[String, BoxedExp] = primitives,
                       outer: Env = new Env) extends Env {

    /*
     * Return a copy of the current state plus an immutable
     * variable 'name' of value 'v'
     */
    def withVal(name: String, v: Exp): ValueEnv = {
      copy(vars = vars + (name -> BoxedExp(v)))
    }

    /*
     * Return a copy of the current state plus all the immutables
     * variable in list.
     */
    def withVals(list: List[(String,Exp)]): ValueEnv = {
      copy(vars = vars ++ (list.map {case (n, v) => n -> BoxedExp(v) }))
    }

    /*
     * Update the variable 'name' in this scope or in the
     * outer scope.
     * Return the new value of the variable
     */
    override def updateVar(name: String, v: Exp): Exp = {
      if (vars.contains(name))
        vars(name).v = v
      else
        outer.updateVar(name, v)
      v
    }

    /*
     * Return the value of the variable 'name'
     */
    override def apply(name: String): Exp = {
      if (vars.contains(name))
        vars(name).v
      else
        outer(name)
    }
  }

  /*
   * Compute and return the result of the unary
   * operation 'op' on the value 'v'
   */
  def evalUn(op: String)(v: Exp) = (op, v) match {
    case ("-", Lit(v:Double)) => Lit(-v)
    case ("+", Lit(_:Double)) => v
    case _ => BUG(s"unary operator $op undefined")
  }

  /*
   * Compute and return the result of the binary
   * operation 'op' on the value 'v' and 'w'
   * Note: v op w
   */
  // TODO fix boolean operators to work with doubles
  // TODO add boolean and, or, etc
  def evalBin(op: String)(v: Exp, w: Exp) = (op, v, w) match {
    case ("-", Lit(v: Double), Lit(w: Double)) => Lit(v-w)
    case ("+", Lit(v: Double), Lit(w: Double)) => Lit(v+w)
    case ("*", Lit(v:Double), Lit(w:Double)) => Lit(v*w)
    case ("/", Lit(v:Double), Lit(w:Double)) => Lit(v/w)
    case ("^", Lit(v:Double), Lit(w:Double)) => Lit(math.pow(v, w))
    case ("==", Lit(v:Double), Lit(w:Double)) => Lit(v == w)
    case ("!=", Lit(v:Double), Lit(w:Double)) => Lit(v != w)
    case ("<=", Lit(v:Double), Lit(w:Double)) => Lit(v <= w)
    case (">=", Lit(v:Double), Lit(w:Double)) => Lit(v >= w)
    case ("<" , Lit(v:Double), Lit(w:Double)) => Lit(v < w)
    case (">" , Lit(v:Double), Lit(w:Double)) => Lit(v > w)
    case _ => BUG(s"binary operator $op undefined")
  }

  def evalPrim(op: String)(eargs: List[Exp]) = eargs match {
    case List(v, w)    => evalBin(op)(v, w)
    case List(v)       => evalUn(op)(v)
    case _ => BUG(s"no prim with ${eargs.length} arguments")
  }

  /*
   * Evaluate the AST starting with an empty Env
   */
  def run(exp: Exp) = eval(exp)(ValueEnv())

  /*
   * Evaluate the AST within the environment 'env'
   */
  def eval(exp: Exp)(env: ValueEnv): Exp = exp match {
    case Lit(_) => exp
    case Prim(op, args) =>
      val eargs = args map { arg => eval(arg)(env) }
      evalPrim(op)(eargs)
    case Ref(x) =>
      env(x)
    case Module(name, args) =>
      val nargs = args map { arg => eval(arg)(env) }
      Module(name, nargs)
  }

}