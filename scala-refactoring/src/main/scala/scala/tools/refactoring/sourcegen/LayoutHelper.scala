package scala.tools.refactoring
package sourcegen

import tools.nsc.util.RangePosition
import tools.nsc.util.SourceFile

trait LayoutHelper {
  
  self: Formatting with common.Tracing with common.PimpedTrees =>
  
  val global: scala.tools.nsc.interactive.Global
  import global._
  
  trait Layout {
    override def toString() = "ø"
  }
    
  case class LayoutFromFile(source: SourceFile, start: Int, end: Int) extends Layout {

    override def toString() = source.content.slice(start, end) mkString
      
    def splitAfter(cs: Char*): (Layout, Layout) = split(cs) match {
      case None => this → NoLayout
      case Some(i) => copy(end = i+1) → copy(start = i+1)
    }
    
    def splitBefore(cs: Char*): (Layout, Layout) = split(cs) match {
      case None => NoLayout → this
      case Some(i) => copy(end = i) →  copy(start = i)
    }
    
    private def split(cs: Seq[Char]): Option[Int] = cs.toList match {
      case Nil => 
        None
      case x :: xs if toString.indexOf(x) >= 0 =>
        Some(start + toString.indexOf(x))
      case _ :: xs => split(xs)
    }
  }
  
  case class LayoutFromString(override val toString: String) extends Layout
  
  case object NoLayout extends Layout {
    override def toString() = ""
  }
  
  def layout(start: Int, end: Int)(implicit s: SourceFile) = LayoutFromFile(s, start, end)
  def between(l: Tree, r: Tree)(implicit s: SourceFile) = layout(l.pos.end, r.pos.start)(s)

  def layoutForCompilationUnitRoot(t: Tree): (Layout, Layout) = 
    LayoutFromFile(t.pos.source, 0, t.pos.start) → 
    LayoutFromFile(t.pos.source, t.pos.end, t.pos.source.length)
    
  def layoutForSingleChild(t: Tree, p: Tree): (Layout, Layout) = 
    splitLayoutBetweenParentAndFirstChild(child = t, parent = p)._2 →     
    splitLayoutBetweenLastChildAndParent(child = t, parent = p)._1
    
  def layoutForLeftOuterChild(t: Tree, p: Tree, right: Tree): (Layout, Layout) = 
    splitLayoutBetweenParentAndFirstChild(child = t, parent = p)._2 → 
    splitLayoutBetweenSiblings(left = t, right = right)._1 
    
  def layoutForRightOuterChild(t: Tree, p: Tree, left: Tree): (Layout, Layout) = 
    splitLayoutBetweenSiblings(left = left, right = t)._2  → 
    splitLayoutBetweenLastChildAndParent(child = t, parent = p)._1
    
  def layoutForEnclosedChild(t: Tree, left: Tree, right: Tree): (Layout, Layout) = 
    splitLayoutBetweenSiblings(left = left, right = t)._2 →
    splitLayoutBetweenSiblings(left = t, right = right)._1

  def splitLayoutBetweenParentAndFirstChild(child: Tree, parent: Tree): (Layout, Layout) = {
    
    implicit val currentFile = child.pos.source
    
    (parent, child) match {
      
      case (p: PackageDef, c) =>
        layout(p.pos.start, c.pos.start) → NoLayout
        
      case (p @ ClassDef(ModifierTree(Nil), _, _, _), c) =>
        layout(p.pos.start,       p.pos.point) → layout(p.pos.point + p.name.length, c.pos.start)
        
      case (p @ ClassDef(ModifierTree(mods), _, _, _), c) =>
        layout(p.pos.start, mods.head.pos.start) → NoLayout
        
      case (p @ ModuleDef(ModifierTree(Nil), _, _), c) =>
        layout(p.pos.start,       p.pos.point) → layout(p.pos.point + p.name.length, c.pos.start)
        
      case (p @ ModuleDef(ModifierTree(mods), _, _), c) =>
        layout(p.pos.start, mods.head.pos.start) → NoLayout
        
      case (p @ TypeDef(ModifierTree(Nil), _, _, _), c) =>
        layout(p.pos.start,       p.pos.point) → layout(p.pos.point + p.name.length, c.pos.start)
        
      case (p @ TypeDef(ModifierTree(mods), _, _, _), c) =>
        layout(p.pos.start, mods.head.pos.start) → NoLayout
        
      case (p: Template, c) =>
        layout(p.pos.start, c.pos.start) splitAfter ('{', '(')
        
      case (p: If, c) =>
        layout(p.pos.start, c.pos.start) splitAfter '('
        
      case (p: ValOrDefDef, c: ModifierTree) =>
        NoLayout → NoLayout
        
      case (p: ValOrDefDef, c) =>
        layout(p.pos.start, p.namePosition.start) → NoLayout
        
      case (p: Apply, c) =>
        layout(p.pos.start, c.pos.start) → NoLayout
        
      case (p: Select, c) =>
        NoLayout → NoLayout
        
      case (p: Block, c) =>
         layout(p.pos.start, c.pos.start) splitAfter '{'
         
      case (p: Import, c) =>
         layout(p.pos.start, p.pos.point) → NoLayout
         
      case (p: ImportSelectorTree, c) =>
         layout(p.pos.start, c.pos.start) → NoLayout
         
      case (p: SuperConstructorCall, c) =>
         layout(p.pos.start, c.pos.start) → NoLayout
         
      case (p: Ident, c) =>
         layout(p.pos.start, c.pos.start) → NoLayout
         
      case (p: Literal, c) =>
         layout(p.pos.start, c.pos.start) → NoLayout
         
      case (p: SelfTypeTree, c) =>
         layout(p.pos.start, c.pos.start) → NoLayout
         
      case (p: TypeApply, c) =>
         layout(p.pos.start, c.pos.start) → NoLayout
         
      case (p: Function, c) =>
         layout(p.pos.start, c.pos.start) → NoLayout
         
      case (p: TypeTree, c) =>
         layout(p.pos.start, c.pos.start) → NoLayout
         
      case (p: AppliedTypeTree, c) =>
         layout(p.pos.start, c.pos.start) → NoLayout
         
      case (p: TypeBoundsTree, c) =>
         layout(p.pos.start, c.pos.start) → NoLayout
         
      case (p: Return, c) =>
         layout(p.pos.start, c.pos.start) → NoLayout
         
      case (p: New, c) =>
         layout(p.pos.start, c.pos.start) → NoLayout
      
      case (p, t) => throw new Exception("Unhandled parent: "+ p.getClass.getSimpleName +", child: "+ t.getClass.getSimpleName)
    }
  }

   def splitLayoutBetweenLastChildAndParent(child: Tree, parent: Tree): (Layout, Layout) = {
     
     implicit val currentFile = child.pos.source
     
     (child, parent) match {
       
       case (c: Block, p) =>
         layout(c.pos.end, p.pos.end) splitAfter '}'
       
       case (c, p: PackageDef) =>
         layout(c.pos.end, p.pos.end) splitAfter '\n'
         
       case (c, p @ (_: ClassDef | _: ModuleDef)) =>
         layout(c.pos.end, p.pos.end) splitAfter '}'
         
       case (c, p: Template) =>
         layout(c.pos.end, p.pos.end) splitBefore (')', '\n')
         
       case (c, p: If) =>
         layout(c.pos.end, p.pos.end) splitBefore (')')
         
       case (c, p: ValOrDefDef) =>
         layout(c.pos.end, p.pos.end) splitAfter '}' // in case there are { } around a single statement.
         
       case (c, p: Apply) =>
         NoLayout → layout(c.pos.end, p.pos.end)
       
       case (c, p: Select) =>
         NoLayout → NoLayout
        
       case (c, p: Block) =>
         layout(c.pos.end, p.pos.end) splitAfter '\n'
         
       case (c, p: Import) =>
         NoLayout → layout(c.pos.end, p.pos.end) // for renames, associate the '}' to the parent
         
       case (c, p: ImportSelectorTree) =>
         NoLayout → layout(c.pos.end, p.pos.end)
         
       case (c, p: SuperConstructorCall) =>
         NoLayout → layout(c.pos.end, p.pos.end)
         
       case (c, p: Ident) =>
         NoLayout → layout(c.pos.end, p.pos.end)
         
       case (c, p: Literal) =>
         NoLayout → layout(c.pos.end, p.pos.end)
         
       case (c, p: SelfTypeTree) =>
         NoLayout → layout(c.pos.end, p.pos.end)
         
       case (c, p: TypeApply) =>
         NoLayout → layout(c.pos.end, p.pos.end)
         
       case (c, p: Function) =>
         NoLayout → layout(c.pos.end, p.pos.end)
         
       case (c, p: TypeTree) =>
         NoLayout → layout(c.pos.end, p.pos.end)
         
       case (c, p: AppliedTypeTree) =>
         NoLayout → layout(c.pos.end, p.pos.end)
         
       case (c, p: TypeBoundsTree) =>
         NoLayout → layout(c.pos.end, p.pos.end)
         
       case (c, p: Return) =>
         NoLayout → layout(c.pos.end, p.pos.end)
         
       case (c, p: New) =>
         NoLayout → layout(c.pos.end, p.pos.end)
         
       case (c, p: TypeDef) =>
         NoLayout → layout(c.pos.end, p.pos.end)
       
       case (c, p) => throw new Exception("Unhandled parent: "+ p.getClass.getSimpleName +", child: "+ c.getClass.getSimpleName)
     }
   }
 
  def  splitLayoutBetweenSiblings(left: Tree, right: Tree): (Layout, Layout) = {
        
    def split(layout: String) = {
      val Class = """(.*?)(class.*)""".r
      val EmptyParens = """(.*?\(\s*\)\s*)(.*)""".r
      val OpeningBrace = """(.*?\()(.*)""".r
      val Colon = """(.*?:\s+)(.*)""".r
      val Arrow = """(.*?=>\s?)(.*)""".r
      val Dot = """(.*)(\..*)""".r
      val Equals = """(.*?=\s?)(.*)""".r
      val ClosingBrace = """(?ms)(.*?)(\).*)""".r
      val Comma = """(.*?),\s?(.*)""".r
      val NewLine = """(?ms)(.*?)(\n.*)""".r
      val ImportStatementNewline = """(?ms)(.*)(\n.*?import.*)""".r // imports don't include leading lines, handle in partitioner instead?
      val ImportStatement = """(?ms)(.*)(.*?import.*)""".r
      
      (layout match {
        case Class(l, r)           => Some(l, r, "Class")
        case Colon(l, r)           => Some(l, r, "Colon")
        case EmptyParens(l, r)     => Some(l, r, "EmptyParens")
        case OpeningBrace(l, r)    => Some(l, r, "OpeningBrace")
        case Arrow(l, r)           => Some(l, r, "Arrow")
        case Equals(l, r)          => Some(l, r, "Equals")
        case ClosingBrace(l, r)    => Some(l, r, "ClosingBrace")
        case _                     => None
      }) orElse (layout match { // Work around https://lampsvn.epfl.ch/trac/scala/ticket/1133
        case ImportStatementNewline(l, r) => Some(l, r, "ImportStatement Newline")
        case ImportStatement(l, r) => Some(l, r, "ImportStatement")
        case NewLine(l, r)         => Some(l, r, "NewLine")
        case Comma(l, r)           => Some(l, r, "Comma")
        case Dot(l, r)             => Some(l, r, "Dot")
        case s                     => Some(s, "", "NoMatch")
      }) get
    }
    
    (left, right) match {
      case (_, EmptyTree) | (EmptyTree, _) => NoLayout → NoLayout
      case (l, r) =>
        val (ll, lr, rule) = split(between(l, r)(left.pos.source).toString)
        trace("Rule %s splits (%s, %s) layout into %s and %s", rule, l.getClass.getSimpleName, r.getClass.getSimpleName, ll, lr)
        LayoutFromString(ll) → LayoutFromString(lr)
    }
  }
}