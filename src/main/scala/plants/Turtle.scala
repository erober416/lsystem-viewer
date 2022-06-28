package plants

import scala.collection.mutable.Stack
import org.fxyz3d.geometry.Point3D

class Turtle(axiom: Axiom) {

  // Orientation matrix starts out as I3
  var currentdir: Point3D = new Point3D(1, 0,  0)
  var leftdir: Point3D = new Point3D(0, 1, 0)
  var updir: Point3D = new Point3D(0, 0, 1)

  // Current line information
  var lastpos: Point3D = new Point3D(0, 0, 0)
  var currentpos: Point3D = new Point3D(0, 0, 0)
  var width: Float = 1.0f

  var lines = Seq[(Point3D, Point3D, Float)]()
  var stack = Stack[(Point3D, Point3D, Point3D, Point3D, Point3D, Float)]()

  // Iterates through instructions and executes commands based on name and parameters of each module
  def process: Seq[(Point3D, Point3D, Float)] = {
    val instructions = axiom.getInstructions()
    instructions foreach {
      case _@(name: String, args: Seq[Double]) =>
        name foreach {
          c => {
            lastpos = currentpos
            (c, args.head) match {
              // Position modules
              case ('F', q) =>
                currentpos = currentpos + (currentdir * q)
                lines = lines :+ (lastpos, currentpos, width)
              case ('G', q) =>
                currentpos = currentpos + (currentdir * q)
                lines = lines :+ (lastpos, currentpos, width)
              case ('f', q) =>
                currentpos = currentpos + (currentdir * q)
              case ('g', q) =>
                currentpos = currentpos + (currentdir * q)

              // Orientation modules
              case ('+', q) => turn(q)
              case ('-', q) => turn(-q)
              case ('&', q) => pitch(q)
              case ('^', q) => pitch(-q)
              case ('\\', q) => roll(q)
              case ('/', q) => roll(-q)
              case ('$', _) =>
                val cross = new Point3D(1, 0, 0).crossProduct(currentdir)
                leftdir = cross.normalize()
                updir = currentdir.crossProduct(leftdir)
              case ('|', _) => turn(180)

              // Stack modules
              case ('[', _) => stack.push((lastpos, currentpos, currentdir, leftdir, updir, width))
              case (']', _) =>
                val x = stack.pop()
                lastpos = x._1
                currentpos = x._2
                currentdir = x._3
                leftdir = x._4
                updir = x._5
                width = x._6

              // Drawing attribute modules
              case ('!', q) =>
                width = q.toFloat
              case _ =>
            }
          }
        }
    }
    lines
  }

  // Turns orientation by specific angle
  def turn (angle: Double) = {
    val cos = math.cos(math.toRadians(angle))
    val sin = math.sin(math.toRadians(angle))
    val ru1 = new Point3D(cos, -sin, 0)
    val ru2 = new Point3D(sin, cos, 0)
    val ru3 = new Point3D(0, 0, 1)
    setOrientation((currentdir, leftdir, updir) * (ru1, ru2, ru3))
  }

  // Pitches orientation by specific angle
  def pitch (angle: Double) = {
    val cos = math.cos(math.toRadians(angle))
    val sin = math.sin(math.toRadians(angle))
    val rl1 = new Point3D(cos, 0, sin)
    val rl2 = new Point3D(0, 1, 0)
    val rl3 = new Point3D(-sin, 0, cos)
    setOrientation((currentdir, leftdir, updir) * (rl1, rl2, rl3))
  }

  // Rolls orientation by specific angle
  def roll (angle: Double) = {
    val cos = math.cos(math.toRadians(angle))
    val sin = math.sin(math.toRadians(angle))
    val rh1 = new Point3D(1, 0, 0)
    val rh2 = new Point3D(0, cos, sin)
    val rh3 = new Point3D(0, -sin, cos)
    setOrientation((currentdir, leftdir, updir) * (rh1, rh2, rh3))
  }

  // Updates orientation matrix column by column
  def setOrientation(newOrientation: (Point3D, Point3D, Point3D)) = {
    currentdir = newOrientation._1
    leftdir = newOrientation._2
    updir = newOrientation._3
  }

  // Returns product of two 3x3 matrices
  // Each Point3D represents a matrix column
  implicit class MatrixMultiply(t: (Point3D, Point3D, Point3D)) {
    val tx = new Point3D(t._1.getX, t._2.getX, t._3.getX)
    val ty = new Point3D(t._1.getY, t._2.getY, t._3.getY)
    val tz = new Point3D(t._1.getZ, t._2.getZ, t._3.getZ)
    def *(p: (Point3D, Point3D, Point3D)) = {
      val nt1 = new Point3D(p._1.dotProduct(tx), p._1.dotProduct(ty), p._1.dotProduct(tz))
      val nt2 = new Point3D(p._2.dotProduct(tx), p._2.dotProduct(ty), p._2.dotProduct(tz))
      val nt3 = new Point3D(p._3.dotProduct(tx), p._3.dotProduct(ty), p._3.dotProduct(tz))
      (nt1, nt2, nt3)
    }
  }

  // Returns sum of two 3D vectors
  implicit class VectorSum(t: Point3D) {
    def +(p: Point3D) = new Point3D(t.x + p.x, t.y + p.y, t.z + p.z)
  }

  // Returns product of 3D vector with scalar
  implicit class ScalarMultiply(t: Point3D) {
    def *(s: Double) = new Point3D(t.x * s, t.y * s, t.z * s)
  }
}