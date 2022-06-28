package plants

import plants.Language.{Exp, Module, Production}

case class ProductionMap(var prods:  Map[Char, Seq[(Seq[Module], Seq[Module], Seq[Module], Int, Module, Exp)]] = Map()) {

  def withProduction(p: Production) = {
    p match {
      case _@Production(pred, succ, lcontext, rcontext, weight, cond) =>
        val t = prods.get(pred.name.charAt(0))
        if (t.isEmpty) {
          val sec = Seq[(Seq[Module], Seq[Module], Seq[Module], Int, Module, Exp)]((succ, lcontext, rcontext, weight, pred, cond))
          prods = prods + (pred.name.charAt(0) -> sec)
        } else {
          var l = prods(pred.name.charAt(0))
          l = l :+ (succ, lcontext, rcontext, weight, pred, cond)
          prods = prods.updated(pred.name.charAt(0), l)
        }
    }
    copy(prods)
  }

  def withProductions(p: Seq[Production]) = {
    p foreach { prod => withProduction(prod) }
    copy(prods)
  }

  def get(c: Char) = prods.get(c)

  def apply(c: Char) = prods(c)
}
