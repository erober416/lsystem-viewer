package plants

import scalafx.Includes.observableList2ObservableBuffer
import scalafx.application.JFXApp3
import scalafx.scene.{Group, ParallelCamera, PerspectiveCamera, Scene, SubScene}
import scalafx.scene.layout.{HBox, VBox}
import scalafx.scene.paint.Color._
import scalafx.scene.paint._
import scalafx.stage.FileChooser
import scalafx.stage.FileChooser._
import scalafx.Includes._
import scalafx.event.ActionEvent
import scalafx.scene.control.{Button, Label, TextField}
import scalafx.scene.transform._
import scalafx.scene.input.{KeyCode, KeyEvent}
import javafx.beans.property.SimpleDoubleProperty
import org.fxyz3d.geometry.Point3D
import org.fxyz3d.shapes.composites.PolyLine3D
import org.fxyz3d.shapes.composites.PolyLine3D.LineType
import scalafx.animation.AnimationTimer
import scalafx.scene.image.Image
import scalafx.scene.shape.Line
import javafx.scene.paint.ImagePattern

import java.io.FileInputStream
import java.util
import java.util.{List => JList}
import scala.io.Source

object Main extends JFXApp3 {
  var ltree3D: Group = null
  var ltree2D: Group = null
  var gui: Scene = null
  var camera: PerspectiveCamera = null
  var camera2D: ParallelCamera = null
  var camscene: Scene = null
  var scene2D: Scene = null

  var prodmap: ProductionMap = null
  var axiom: Axiom = null

  override def start(): Unit = {
    initGUI
    initCamera
    initKeyControl

    stage = new JFXApp3.PrimaryStage
    stage.scene = gui
    stage.show()

  }

  def processFile(): (Axiom, ProductionMap, String) = {
    val fileChooser = new FileChooser {
      title = "Open Resource File"
      extensionFilters ++= Seq(
        new ExtensionFilter("All Files", "*")
      )
    }
    val selectedFile = fileChooser.showOpenDialog(stage)
    if (selectedFile != null) {
      val src = if (selectedFile.exists) {
        val source = Source.fromFile(selectedFile)
        try source.getLines mkString "\n" finally source.close()
      } else {
        "file not found"
      }

      val reader = new BaseReader(src, '\u0000')

      // Scanner to test!
      val scanner = new LScanner(reader)

      // Parser to test!
      val extension = selectedFile.getName.split("\\.").last
      val parser = extension match {
        case "instruction" => new InstructionParser(scanner)
        case _ => new ILParser(scanner)
      }
      val t = parser.parseCode()
      (t._1, t._2, selectedFile.getName)
    } else {
      null
    }
  }

  def initGUI: Unit = {
    val text1 = new Label {
      text = "No file selected"
    }

    val button1 = new Button {
      text = "Choose File"
      onAction = (_: ActionEvent) => {
        val c = processFile()
        text1.text = c._3
        axiom = c._1
        prodmap = c._2
      }
    }

    val button1hbox = new HBox()
    button1hbox.children.add(button1)
    button1hbox.children.add(text1)

    val button2 = new Button {
      text = "Iterate"
      onAction = (_: ActionEvent) => {
        axiom = axiom.iterate(prodmap)
      }
    }

    val button3 = new Button {
      text = "Display 2D"
      onAction = (_: ActionEvent) => {
        renderAxiom
        stage.scene = scene2D
      }
    }

    val button4 = new Button {
      text = "Display 3D"
      onAction = (_: ActionEvent) => {
        renderAxiom
        stage.scene = camscene
      }
    }

    val text5 = new Label {
      text = "No background selected"
    }

    val button5 = new Button {
      text = "Choose Background"
      onAction = (_: ActionEvent) => {
        val fileChooser = new FileChooser {
          title = "Open Resource File"
          extensionFilters ++= Seq(
            new ExtensionFilter("All Files", "*")
          )
        }
        val selectedFile = fileChooser.showOpenDialog(stage)
        val img = new Image(selectedFile.toURI.toString)
        camscene.setFill(new ImagePattern(img))
        text5.text = selectedFile.getName
      }
    }

    val button5hbox = new HBox()
    button5hbox.children.add(button5)
    button5hbox.children.add(text5)

    val textBox = new TextField {

    }

    val root = new Group()
    ltree3D = new Group()
    ltree2D = new Group()
    gui = new Scene(root, 900, 200)

    val box: VBox = new VBox()
    box.children.add(button1hbox)
    box.children.add(button2)
    box.children.add(button5hbox)
    box.children.add(button3)
    box.children.add(button4)
    box.children.add(textBox)
    root.children.add(box)
  }

  def initCamera: Unit = {
    val camera = new PerspectiveCamera(true)
    camera.setFarClip(6000)
    camera.setNearClip(0.01)
    val cgroup = new Group()
    cgroup.getChildren.add(ltree3D)

    val subScene = new SubScene(cgroup, 2000, 900)
    subScene.setCamera(camera)

    /*val fileChooser = new FileChooser {
      title = "Open Resource File"
      extensionFilters ++= Seq(
        new ExtensionFilter("All Files", "*")
      )
    }
    val selectedFile = fileChooser.showOpenDialog(stage)
    val img = new Image(selectedFile.toURI.toString)*/
    subScene.setFill(Transparent)

    val group = new Group()
    group.getChildren.add(subScene)
    this.camera = camera
    camscene = new Scene(group)
    //camscene.setFill(new ImagePattern(img))

    camera2D = new ParallelCamera()
    val tgroup = new Group()
    tgroup.getChildren.add(ltree2D)

    val subScene2D = new SubScene(tgroup, 900, 900)
    subScene2D.setCamera(camera2D)
    val group2D = new Group()
    group2D.getChildren.add(subScene2D)
    scene2D = new Scene(group2D)

  }

  private def initKeyControl: Unit = {
    val xRotate: Rotate = new Rotate (0, Rotate.XAxis)
    val yRotate: Rotate = new Rotate (0, Rotate.YAxis)
    val zRotate: Rotate = new Rotate (90, Rotate.ZAxis)
    val translate: Translate = new Translate(0, 0, -1000)

    val translate2D = new Translate(0, 0, 0)
    val scale = new Scale(1, 1)

    val angleX = new SimpleDoubleProperty(0)
    val angleY = new SimpleDoubleProperty(0)
    xRotate.angleProperty.bind(angleX)
    yRotate.angleProperty.bind(angleY)

    val translateX = new SimpleDoubleProperty(0)
    val translateY = new SimpleDoubleProperty(0)
    translate2D.xProperty.bind(translateX)
    translate2D.yProperty.bind(translateY)

    val scaleX = new SimpleDoubleProperty(1)
    scale.xProperty.bind(scaleX)
    scale.yProperty.bind(scaleX)

    camera.getTransforms.addAll(xRotate, yRotate, zRotate, translate, translate2D, scale)
    camera2D.getTransforms.addAll(translate2D, scale)

    var (left, right, up, down, w, a, s, d, sub, add) = (false, false, false, false, false, false, false, false, false, false)
    val timer = AnimationTimer(_ => {
      if (up) angleY.set(angleY.get() - .2)
      if (down) angleY.set(angleY.get() + .2)
      if (left) angleX.set(angleX.get() - .2)
      if (right) angleX.set(angleX.get() + .2)
      if (a) translateX.set(translateX.get() - 1 * scaleX.get())
      if (d) translateX.set(translateX.get() + 1 * scaleX.get())
      if (w) translateY.set(translateY.get() - 1 * scaleX.get())
      if (s) translateY.set(translateY.get() + 1 * scaleX.get())
      if (sub) scaleX.set(scaleX.get() * 1.01)
      if (add) scaleX.set(scaleX.get() * 0.99)
    })
    timer.start()

    def controls(event: KeyEvent) = {
      val c: KeyCode = event.getCode
      val pressed = event.eventType == KeyEvent.KeyPressed
      c match {
        case KeyCode.Up => up = pressed
        case KeyCode.Down => down = pressed
        case KeyCode.Left => left = pressed
        case KeyCode.Right => right = pressed
        case KeyCode.A => a = pressed
        case KeyCode.D => d = pressed
        case KeyCode.W => w = pressed
        case KeyCode.S => s = pressed
        case KeyCode.Add => add = pressed
        case KeyCode.Subtract => sub = pressed
        case KeyCode.Escape =>
          if (pressed) {
            angleX.set(0)
            angleY.set(0)
            translateX.set(0)
            translateY.set(0)
            scaleX.set(1)
            stage.scene = gui
          }
        case KeyCode.Space =>
          if (pressed) {
            axiom = axiom.iterate(prodmap)
            renderAxiom
          }
        case _ =>
      }
    }

    scene2D.setOnKeyPressed(event => controls(event))
    scene2D.setOnKeyReleased(event => controls(event))
    camscene.setOnKeyPressed(event => controls(event))
    camscene.setOnKeyReleased(event => controls(event))
  }

  def renderAxiom = {
    ltree3D.children.clear()
    ltree2D.children.clear()
    val turtle = new Turtle(axiom)
    val ls = turtle.process
    ls foreach {
      line =>
        if (ls.indexOf(line) < ls.size) {
          val x: JList[Point3D] = new util.ArrayList[Point3D]()
          x.add(line._1)
          x.add(line._2)
          ltree3D.children.add(new PolyLine3D(x, line._3, Color.Black, LineType.TRIANGLE))
          ltree2D.children.add(new Line() {
            startX = line._1.x
            endX = line._2.x
            startY = line._1.y
            endY = line._2.y
            strokeWidth = line._3
          })
        }
    }
  }
}



