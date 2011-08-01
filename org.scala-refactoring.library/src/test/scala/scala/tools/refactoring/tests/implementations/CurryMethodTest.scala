package scala.tools.refactoring
package tests.implementations

import implementations.CurryMethod
import tests.util.TestHelper
import tests.util.TestRefactoring
import org.junit.Ignore

class CurryMethodTest extends TestHelper with TestRefactoring {

  outer => 
    
  def curryMethod(splitPositions: List[List[Int]])(pro: FileSet) = new TestRefactoringImpl(pro) {
    val refactoring = new CurryMethod with SilentTracing with GlobalIndexes {
      val global = outer.global
      val cuIndexes = pro.trees map (_.pos.source.file) map (file => global.unitOfFile(file).body) map CompilationUnitIndex.apply
      val index = GlobalIndex(cuIndexes)
    }
    val changes = performRefactoring(splitPositions)
  }.changes
  
  @Test
  // TODO resolve pretty print issues
  def simpleCurrying = new FileSet {
    """
      package simpleCurrying
	  class A {
        def /*(*/add/*)*/(first: Int, second: Int) = first + second
      }
    """ becomes
    """
      package simpleCurrying
	  class A {
        def /*(*/add/*)*/(first: Int)( second: Int) = first + second
      }
    """ 
  } applyRefactoring(curryMethod(List(1::Nil)))
  
  @Test
  def multipleParamListCurrying = new FileSet {
    """
      package multipleParamListCurrying
	  class A {
        def /*(*/add/*)*/(first: Int, second: Int)(a: String, b: String, c: String) = first + second
      }
    """ becomes
    """
      package multipleParamListCurrying
	  class A {
        def /*(*/add/*)*/(first: Int)( second: Int)(a: String)( b: String)( c: String) = first + second
      }
    """ 
  } applyRefactoring(curryMethod(List(1::Nil, 1::2::Nil)))
  
  @Test
  def curryingWithMethodCall = new FileSet {
    """
      package curryingWithMethodCall
	  class A {
        def /*(*/add/*)*/(first: Int, second: Int)(a: String, b: String, c: String) = first + second
      }
	  class B {
        val a = new A
        val b = a.add(1, 2)("a", "b", "c")
      }
    """ becomes
    """
      package curryingWithMethodCall
	  class A {
        def /*(*/add/*)*/(first: Int)( second: Int)(a: String, b: String)( c: String) = first + second
      }
	  class B {
        val a = new A
        val b = a.add(1)( 2)("a",  "b")( "c")
      }
    """ 
  } applyRefactoring(curryMethod(List(1::Nil, 2::Nil)))
  
  @Test
  def curryingMethodSubclass = new FileSet {
    """
      package curryingMethodSubclass
      class Parent {
        def /*(*/method/*)*/(first: Int, second: Int)(a: String, b: String, c: String) = (first + second, a+b+c)
      }

      class Child extends Parent {
        override def method(first: Int, second: Int)(a: String, b: String, c: String) = (first, a)
      }
    """ becomes
    """
      package curryingMethodSubclass
      class Parent {
        def /*(*/method/*)*/(first: Int)( second: Int)(a: String, b: String)( c: String) = (first + second, a+b+c)
      }

      class Child extends Parent {
        override def method(first: Int)( second: Int)(a: String, b: String)( c: String) = (first, a)
      }
    """ 
  } applyRefactoring(curryMethod(List(1::Nil, 2::Nil)))
  
  @Test
  def curryingMethodSuperclass = new FileSet {
    """
      package curryingMethodSuperclass
      class Parent {
        def method(first: Int, second: Int)(a: String, b: String, c: String) = (first + second, a+b+c)
      }

      class Child extends Parent {
        override def /*(*/method/*)*/(first: Int, second: Int)(a: String, b: String, c: String) = (first, a)
      }
    """ becomes
    """
      package curryingMethodSuperclass
      class Parent {
        def method(first: Int)( second: Int)(a: String, b: String)( c: String) = (first + second, a+b+c)
      }

      class Child extends Parent {
        override def /*(*/method/*)*/(first: Int)( second: Int)(a: String, b: String)( c: String) = (first, a)
      }
    """ 
  } applyRefactoring(curryMethod(List(1::Nil, 2::Nil)))
  
  @Test 
  @Ignore
  // TODO implement
  def curriedMethodAliased= new FileSet {
    """
      package curriedMethodAliased
      class A {
        def /*(*/curriedAdd3/*)*/(a: Int, b: Int, c: Int) = a + b + c
        def alias = curriedAdd3 _
        val six = alias(1, 2, 3)
      }
    """ becomes
    """
      package curriedMethodAliased
      class A {
        def /*(*/curriedAdd3/*)*/(a: Int)(b: Int)( c: Int) = a + b + c
        def alias = curriedAdd3 _
        val six = alias(1)(2)(3)
      }
    """ 
  } applyRefactoring(curryMethod(List(1::2::Nil)))  
  
  @Test(expected=classOf[RefactoringException])
  def unorderedSplitPositions = new FileSet {
    """
      package unorderedSplitPositions
      class Foo {
        def /*(*/add/*)*/(first: Int, second: Int, third: Int) = first + second + third
      }
    """ becomes
    """
      package unorderedSplitPositions
      class Foo {
        def /*(*/add/*)*/(first: Int, second: Int, third: Int) = first + second + third
      }
    """
  } applyRefactoring(curryMethod(List(2::1::Nil)))
  
  @Test(expected=classOf[RefactoringException])
  def aboveBoundsSplitPosition = new FileSet {
    """
      package unorderedSplitPositions
      class Foo {
        def /*(*/add/*)*/(first: Int, second: Int, third: Int) = first + second + third
      }
    """ becomes
    """
      package unorderedSplitPositions
      class Foo {
        def /*(*/add/*)*/(first: Int, second: Int, third: Int) = first + second + third
      }
    """
  } applyRefactoring(curryMethod(List(3::Nil)))
  
  @Test(expected=classOf[RefactoringException])
  def belowBoundsSplitPosition = new FileSet {
    """
      package unorderedSplitPositions
      class Foo {
        def /*(*/add/*)*/(first: Int, second: Int, third: Int) = first + second + third
      }
    """ becomes
    """
      package unorderedSplitPositions
      class Foo {
        def /*(*/add/*)*/(first: Int, second: Int, third: Int) = first + second + third
      }
    """
  } applyRefactoring(curryMethod(List(0::Nil)))
  
  @Test(expected=classOf[RefactoringException])
  def duplicatedSplitPosition = new FileSet {
    """
      package unorderedSplitPositions
      class Foo {
        def /*(*/add/*)*/(first: Int, second: Int, third: Int) = first + second + third
      }
    """ becomes
    """
      package unorderedSplitPositions
      class Foo {
        def /*(*/add/*)*/(first: Int, second: Int, third: Int) = first + second + third
      }
    """
  } applyRefactoring(curryMethod(List(1::1::Nil)))
  
  @Test(expected=classOf[RefactoringException])
  def tooManySplitPositions = new FileSet {
    """
      package unorderedSplitPositions
      class Foo {
        def /*(*/add/*)*/(first: Int, second: Int, third: Int) = first + second + third
      }
    """ becomes
    """
      package unorderedSplitPositions
      class Foo {
        def /*(*/add/*)*/(first: Int, second: Int, third: Int) = first + second + third
      }
    """
  } applyRefactoring(curryMethod(List(1::Nil, 1::Nil)))
  
}