# lsystem-viewer

# What is this
This is software for parsing and rendering Stochastic Parameteric 2L-Systems as well as various subsets. It is written in Scala using ScalaFX/JavaFX. Based on the theoretical foundations created by Aristid Lindenmayer as described in the textbook [Algorithmic Beauty of Plants](http://algorithmicbotany.org/papers/abop/abop.pdf).
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
# Step 2: Compile and run with SBT
$ sbt run
```
# Controls
* Use <kbd>→</kbd> to rotate camera clockwise around z-axis
* Use <kbd>←</kbd> to rotate camera counter-clockwise around z-axis
* Use <kbd>↑</kbd> to move camera clockwise around x-axis
* Use <kbd>↓</kbd> to move camera counter-clockwise around x-axis
* Use <kbd>w</kbd> to move camera up along focal plane
* Use <kbd>s</kbd> to move camera down along focal plane
* Use <kbd>a</kbd> to move camera left along focal plane
* Use <kbd>d</kbd> to move camera right along focal plane
* Press <kbd>esc</kbd> to return to menu
# Grammar
<module>     ::= <letter>['('<simp>[','<simp>]*]')']
<op>         ::= ['*' | '/' | '+' | '-' | '<' | '>' | '=' | '!']+
<pword>      ::= [<letter>['('<simp>[','<simp>]*]')']]+
<atom>       ::= <number> | <bool> | '()' | <pletter> | '('<simp>')'
<uatom>      ::= [<op>]<atom>
<simp>       ::= <uatom>[<op><uatom>]*
<pred>       ::= [<pword> '<'] <pword> ['>' <pword>]
<axiom>      ::= "W:" <pword>
<production> ::= "P:" <pred> [':'<simp>] "->" <pword>
<lsystem>    ::= <axiom> [<production>]*
