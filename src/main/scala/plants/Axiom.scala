package plants

import plants.Language._

import java.io.{BufferedWriter, File, FileWriter}
import scala.util.Random

class Axiom(instructions: Seq[(String, Seq[Double])]) {
  val random = new Random

  // Update axiom based on production rules
  def iterate(prodmap: ProductionMap): Axiom = {
    var newAxiom = Seq[(String, Seq[Double])]()
    var index = 0
    instructions foreach {
      module =>
        newAxiom = newAxiom.appendedAll(chooseSuccessor(module, index)(prodmap))
        index = index + 1
    }
    new Axiom(newAxiom)
  }

  def getInstructions(): Seq[(String, Seq[Double])] = instructions

  def writeToFile(filename: String): Unit = {
    val file = new File(filename)
    val bw = new BufferedWriter(new FileWriter(file))
    for (instruction <- instructions) {
      bw.write(instruction._1)
      bw.write(instruction._2.toString().substring(4))
      bw.write('\n')
    }
    bw.close()
  }

  // Find the successor of a given module to add to new axiom
  private def chooseSuccessor(module: (String, Seq[Double]), index: Int)(prodmap: ProductionMap): Seq[(String, Seq[Double])] = {
    // If no successors for this module are in productions add the module back
    val test = prodmap.get(module._1.charAt(0))
    if (test.isEmpty) {
      return Seq(module)
    }

    // Filter successors to just the ones with valid context
    var succs = prodmap(module._1.charAt(0))
    val contextMatched = succs map (s => (matchRContext(index, s._3)._1 && matchLContext(index, s._2)._1))
    succs = (succs zip contextMatched).filter(_._2) map (succ => succ._1)

    // If no successors are left match add the module back
    if (succs.isEmpty) {
      return Seq(module)
    }

    // Iterate through successors determing the float values of each parameter
    var matchedSuccsWithValues = Seq[(Seq[(String, Seq[Double])], Int)]()
    succs foreach {
      case _@(succ, lcont, rcont, weight, pred, cond) =>
        // Add parameter values to environment
        var params = List[(String, Exp)]()
        val addparamsFromModules = (names: Module, values: (String, Seq[Double])) =>
          names.args zip values._2 foreach {
            case _@(Ref(name: String), value: Double) =>
              params = params :+ (name, Lit(value))
            case _ =>
          }
        val addparamsFromPwords = (names: Seq[Module], values: Seq[(String, Seq[Double])]) =>
          (names zip values) foreach (l1 => addparamsFromModules(l1._1, l1._2))
        val axiomLeftContext = matchLContext(index, lcont)._2
        val axiomRightContext = matchRContext(index, rcont)._2
        addparamsFromPwords(lcont, axiomLeftContext)
        addparamsFromPwords(rcont, axiomRightContext)
        addparamsFromModules(pred, module)

        // Evaluate right hand side and condition using the new environment
        val interpreter = new ValueInterpreter
        val env = interpreter.ValueEnv().withVals(params)
        val succValuesFilledExp = succ map (s => interpreter.eval(s)(env))
        var nsucc: Seq[(String, Seq[Double])] = Seq[(String, Seq[Double])]()
        val ncond = interpreter.eval(cond)(env)

        // Reconstruct parametric word with parameter values filled in
        succValuesFilledExp foreach {
          case n@Module(name: String, args: Seq[Exp]) =>
            var nargs = Seq[Double]()
            args foreach {
              case arg@Lit(x: Double) =>
                nargs = nargs :+ x
            }
            nsucc = nsucc :+ (name, nargs)
        }

        // Add parametric word and weight to new list of matched successors if cond evaluates to true
        ncond match {
          case Lit(true) =>
            matchedSuccsWithValues = matchedSuccsWithValues :+ (nsucc, weight)
          case _ =>
        }
    }

    // Randomly choose final successor based on weights
    var weightedSuccessors = Seq[Seq[(String, Seq[Double])]]()
    matchedSuccsWithValues foreach {
      case _@(succ, weight) =>
        for (_ <- 0 until weight) {
          weightedSuccessors = weightedSuccessors :+ succ
        }
        // If lsystem is not stochastic return first matching production
        if (weight == 0) {
          return succ
        }
    }

    // If it is empty return base module
    if (weightedSuccessors.isEmpty) {
      return Seq(module)
    }

    weightedSuccessors(random.nextInt(weightedSuccessors.length))
  }

  // Determines whether the right context of the production matches the right context of the current module in axiom
  // Returns true/false and right context of current module
  // TODO: Change to return Some/None
  private def matchRContext(index: Int, context: Seq[Module]): (Boolean, Seq[(String, Seq[Double])]) = {
    if (context.head.name.isEmpty) {
      return (true, Seq[(String, Seq[Double])](("", Seq[Double]())))
    }
    var modules = Seq[(String, Seq[Double])]()
    var i = index + 1
    var j = 0
    var matched = true
    while (matched && i < instructions.length && j < context.length) {
      if (instructions(i)._1.charAt(0) == '[' && context(j).name.charAt(0) != '[') {
        var l = 1
        while (l != 0) {
          i = i + 1
          if (instructions(i)._1.charAt(0) == ']') {
            l = l - 1;
          } else if (instructions(i)._1.charAt(0) == '[') {
            l = l + 1;
          }
        }
        i = i + 1
      } else if (context(j).name.charAt(0) == ']') {
        while (instructions(i)._1.charAt(0) != ']') {
          i = i + 1
        }
        i = i + 1
        j = j + 1
        modules = modules :+ instructions(j - 1)
      } else if (i < instructions.length && instructions(i)._1.charAt(0) == context(j).name.charAt(0)
      && instructions(i)._2.length == context(j).args.length) {
        i = i + 1
        j = j + 1
        modules = modules :+ instructions(i - 1)
      } else {
        matched = false
      }
    }
    if (i >= instructions.length && j < context.length) {
      matched = false
    }
    (matched, modules)
  }

  // Determines whether the left context of the production matches the left context of the current module in axiom
  // Returns true/false and left context of current module
  // TODO change to Some/None
  private def matchLContext(index: Int, context: Seq[Module]): (Boolean, Seq[(String, Seq[Double])]) = {
    if (context.head.name.isEmpty) {
      return (true, Seq[(String, Seq[Double])](("", Seq[Double]())))
    }
    var modules = Seq[(String, Seq[Double])]()
    var i = index - 1
    var j = context.length - 1
    var matched = true
    while (matched && i >= 0 && j >= 0) {
      if (instructions(i)._1.charAt(0) == ']') {
        var l = 1
        while (l != 0) {
          i = i - 1
          if (instructions(i)._1.charAt(0) == '[') {
            l = l - 1;
          } else if (instructions(i)._1.charAt(0) == ']') {
            l = l + 1;
          }
        }
      } else if (instructions(i)._1.charAt(0) == '[') {
        i = i - 1
      } else if (instructions(i)._1.charAt(0) == context(j).name.charAt(0)
        && instructions(i)._2.length == context(j).args.length) {
        i = i - 1
        j = j - 1
        modules = instructions(i + 1) +: modules
      } else {
        matched = false
      }
    }
    if (i < 0 && j >= 0) {
      matched = false
    }
    (matched, modules)
  }
}