package kiama.rewriting

import junit.framework.Assert._
import junit.framework.TestCase
import org.scalacheck._
import org.scalacheck.Prop._ 
import org.scalatest.junit.JUnit3Suite 
import org.scalatest.prop.Checkers 
import kiama.example.imperative.TestBase

/**
 * Tests based on examples from the paper "Uniform boilerplate and list
 * processing" by Mitchell and Runciman, from Haskell Workshop 2007.
 */
class UniplateTests extends TestCase with JUnit3Suite with Checkers
                    with Rewriter with TestBase {
                      
    import kiama.example.imperative.AST._
    
    /**
     * A simple numeric test expression.
     */
    val numexp = Num (42)
    
    /**
     * A simple test expression involving variables.
     */
    val varexp = Div (Mul (Var ("var1"), Var ("var2")), Var ("var1"))
                  
    /**
     * Tests that collect variable references.
     */
    def testVariables () {
	    /**
         *  Direct style: local management of the collection.
         */
	    def variables (e : Exp) : Set[String] = {
	        var vars = Set[String]()
	        everywheretd (query { case Var (s) => vars += s }) (e)
	        vars
	    }
	    check ((e : Exp) => variables (e) == e.vars)
	        
	    // Indirect: using the collects combinator to manage the set
	    val variabless = collects { case Var (s) => s }
	    check ((e : Exp) => variabless (e) == e.vars)
	
	    // Simple check of set and list versions of collect
	    val variablesl = collectl { case Var (s) => s }
	    assertEquals (variabless (numexp), Set ())
	    assertEquals (variablesl (numexp), List ())
	    assertEquals (variabless (varexp), Set ("var1", "var2"))
	    assertEquals (variablesl (varexp), List ("var1", "var2", "var1"))
    }                      
    
	/**
	 * Tests that search for divisions by literal zero.
	 */
	def testDivsByZero () {
	  	object TestDivsByZero extends TestBase {
		    override def genDiv (sz : Int) =
		        Gen.frequency ((1, genDivByZero (sz)), (1, super.genDiv (sz)))
		    def genDivByZero (sz : Int) =
		        for { l <- genExp (sz/2) } yield Div (l, Num (0))
         	def divsbyzero = count { case Div (_, Num (0)) => 1 }
            assertEquals (divsbyzero (numexp), 0)
            assertEquals (divsbyzero (varexp), 0)
            check ((e : Exp) => divsbyzero (e) == e.divsbyzero)
		}
	    TestDivsByZero ()	  
	}
     
	/**
	 * Tests of arithmetic simplification transformations.
	 */
	def testSimplification () {
	    def simplify : Exp => Exp =
	        rewrite (everywheretd (rule {
	            case Sub (x, y)           => simplify (Add (x, Neg (y)))
	            case Add (x, y) if x == y => Mul (Num (2), x)
	        }))
	    assertEquals (simplify (numexp), numexp)
	    assertEquals (simplify (varexp), varexp)
	
	    val e = Sub (Add (Var ("a"), Var ("a")),
	                 Add (Sub (Var ("b"), Num (1)), Sub (Var ("b"), Num (1))))
	    val simpe = Add (Mul (Num (2), Var ("a")),
	                     Neg (Mul (Num (2), Add (Var ("b"), Neg (Num (1))))))
	    assertEquals (simplify (e), simpe)
	
	    val f = Sub (Neg (Num (1)), Num (1))
	    val simpf = Mul (Num (2), Neg (Num (1)))
	    assertEquals (simplify (f), simpf)
	
	    check ((e : Exp) => simplify (e).value == e.value)
	}

	/**
	 * Tests that remove double negations.
	 */
	def testDoubleNegSimplification () {
	    object TestDoubleNegSimplification extends TestBase {
		    override def genNeg (sz : Int) = 
		        Gen.frequency ((1, genDoubleNeg (sz)), (1, super.genNeg (sz)))
		    def genDoubleNeg (sz : Int) =
		        for { e <- super.genNeg (sz) } yield Neg (e)
      	    def doubleneg : Exp => Exp =
                rewrite (everywherebu ( rule { case Neg (Neg (x)) => x }))
            assertEquals (doubleneg (numexp), numexp)
            assertEquals (doubleneg (varexp), varexp)
            check ((e : Exp) => doubleneg (e).value == e.value)
		}
        TestDoubleNegSimplification ()
	}
 
	/**
	 * Tests of reciprocal division conversion to multiplication.
	 */
	def testReciprocal () {
	    def reciprocal : Exp => Exp =
	        rewrite (everywherebu (rule {
	            case Div (n, m) => Mul (n, Div (Num (1), m))
	        }))
     
        val e1 = Div (Num (1), Num (2))
        assertEquals (reciprocal (e1).value, 0.5)
     
        val e2 = Mul (Num (2), Div (Num (3), Num (4)))
        assertEquals (reciprocal (e2).value, 1.5)
    }

	/**
	 * Tests that rename variables to be unique
	 */
	def testUniqueVars () {
	    def uniquevars : Exp => Exp =
	        rewrite ({
	            var count = 0
	            everywheretd (rule { case Var (s) => count = count + 1; Var ("x" + count) })
	        })
	    assertEquals (uniquevars (numexp), numexp)
	    // Run this twice to make sure that count is not shared
	    assertEquals (uniquevars (varexp), Div (Mul (Var ("x1"), Var ("x2")), Var ("x3")))
	    assertEquals (uniquevars (varexp), Div (Mul (Var ("x1"), Var ("x2")), Var ("x3")))
	    check ((e : Exp) => uniquevars (e).value == e.value)
	}

	/**
	 * Tests that calculate expression depth.
	 */
	def testDepth () {
	    def maximum (l : Seq[Int]) : Int = l.drop (1).foldLeft (l.first)(_.max(_))
	    def depth = para ((t : Any, cs : Seq[Int]) => 1 + maximum (List (0) ++ cs))
	    assertEquals (depth (numexp), 2)
	    assertEquals (depth (varexp), 4)
	    check ((e : Exp) => depth (e) == e.depth)
	}
	
	/**
	 * Tests that rename variables.  Note that in the Uniplate paper and the Compos
	 * paper before it, this is a multi-type example, but we don't bother since many
     * of the other rewriting tests are multi-type. 
	 */
	def testRenameVar () {
	    def rename : Exp => Exp =
	        rewrite (everywheretd (rule { case Var (s) => Var ("_" + s) }))
	    check ((e : Exp) => rename (e).vars == e.vars.map ("_" + _))
	}
 	
	/**
	 * Test optimisation of integer addition.
	 */
	def testOptimiseAdd () {
	    object OptimiseAdd extends TestBase {
	        override def genAdd (sz : Int) = 
	            Gen.frequency ((1, genIntAdd (sz)), (1, super.genAdd (sz)))
            def genIntAdd (sz : Int) =
                for { l <- genNum; r <- genNum } yield Add (l, r)
		    def optimiseadd : Exp => Exp =
		        rewrite (everywherebu (rule {
		            case Add (Num (n), Num (m)) => Num (n + m)
		        }))
		    check ((e : Exp) => {
		        val eopt = optimiseadd (e)
		        (eopt.intadds == 0) && (eopt.value == e.value)
		    })
        }
        OptimiseAdd ()
	}
                      
}
