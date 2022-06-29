# lsystem-viewer

# What is this
This is software for parsing and rendering Stochastic Parameteric 2L-Systems as well as various subsets. It is written in Scala using ScalaFX/JavaFX. Based on the theoretical foundations created by Aristid Lindenmayer as described in the textbook [*Algorithmic Beauty of Plants*](http://algorithmicbotany.org/papers/abop/abop.pdf). When writing the parser, I tried to keep a syntax as close to the textbook as possible.
# Features
* 2D and 3D L-System rendering
* Context sensitivity
* Stochastic productions
* C-Style preprocessor including #define
* On-the-fly interpretation of arithmetic expressions
* Supports creation of arbitrary nodes in axiom and productions
* Supports creation of arbitrary parameters in productions
* Supports adding a background image to the viewer
# How to build
This is an example of how to build using **Linux**. This software requires the JVM to be installed on a system before compiling, and SBT is the default tool used to build.
```shell
# Step 1: Get sources from GitHub
$ git clone https://github.com/erober416/lsystem-viewer.git
$ cd lsystem-viewer
```
```shell
# Step 2: Compile and run with sbt
$ sbt run
```
# Viewing your first L-System
When launching the software the GUI will at first display several buttons. The quickest way to get started with rendering is pressing ```choose file``` option and choosing a file from the ```examples``` directory. Several example L-System specification files are within. These are mostly classic examples from *Algorithmic Beauty of Plants*. It varies for each one whether 2D or 3D looks better. After choosing ```Display 2D``` or ```Display 3D```, the scene will switch to camera mode. It will be empty until the current L-System is iterated by pressing <kbd>space</kbd>.
# Controls
* Use <kbd>→</kbd> to rotate camera clockwise around z-axis
* Use <kbd>←</kbd> to rotate camera counter-clockwise around z-axis
* Use <kbd>↑</kbd> to move camera clockwise around x-axis
* Use <kbd>↓</kbd> to move camera counter-clockwise around x-axis
* Use <kbd>w</kbd> to move camera up along focal plane
* Use <kbd>s</kbd> to move camera down along focal plane
* Use <kbd>a</kbd> to move camera left along focal plane
* Use <kbd>d</kbd> to move camera right along focal plane
* Press <kbd>space</kbd> to rewrite the current string according to productions
* Press <kbd>esc</kbd> to return to menu
# Full Grammar
```text
<module>     ::= <letter>['('<simp>[','<simp>]*]')']
<op>         ::= ['*' | '/' | '+' | '-' | '<' | '>' | '=' | '!']+
<pword>      ::= [<letter>['('<simp>[','<simp>]*]')']]+
<atom>       ::= <number> | <bool> | '()' | <pletter> | '('<simp>')'
<uatom>      ::= [<op>]<atom>
<simp>       ::= <uatom>[<op><uatom>]*
<pred>       ::= [<pword> '<'] <pword> ['>' <pword>]
<axiom>      ::= "W:" <pword>
<production> ::= "P:" <pred> [':'<simp>] "->" <pword> [: <number>]
<lsystem>    ::= <axiom> [<production>]*
```
Under this grammar, most modules specify a matrix transformation to be applied to the turtle keeping track of current drawing location. The rest act only as nodes to carry data between string rewrites.
# Parametric letter controls
* F(x): Move forward a distance x while drawing a line.
* f(x): Move forward a distance x without drawing a line.
* G(x): Same as F(x).
* g(x): Same as f(x).
* +(x): Turn turtle x degrees.
* -(x): Turn turtle -x degrees.
* &(x): Pitch turtle x degrees.
* ^(x): Pitch turtle -x degrees.
* \(x): Roll turtle x degrees.
* /(x): Roll turtle -x degrees.
* $: Roll the turtle on its own axis so that the vector pointing to the turtle's left is in horizontal position.
* |: Turn turtle 180 degrees.
* \[: Push the current state of the turtle onto the stack.
* \]: Pop a turtle off of the stack and use it as new turtle.
* !(x): Change width of line to x.
