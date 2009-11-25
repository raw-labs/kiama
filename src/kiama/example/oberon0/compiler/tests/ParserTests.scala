/*
 * This file is part of Kiama.
 *
 * Copyright (C) 2009 Anthony M Sloane, Macquarie University.
 *
 * Contributed by Ben Mockler.
 *
 * Kiama is free software: you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version.
 *
 * Kiama is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License for
 * more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Kiama.  (See files COPYING and COPYING.LESSER.)  If not, see
 * <http://www.gnu.org/licenses/>.
 */

package kiama.example.oberon0.compiler.tests

import org.scalacheck.Prop._
import org.scalatest.FunSuite
import org.scalatest.prop.Checkers
import kiama.example.oberon0.compiler.Parser

/**
 * Oberon0 parsing test cases.
 */
class ParserTests extends FunSuite with Checkers with Parser {

    import kiama.example.oberon0.compiler.AST._

    /**
     * Return true if the given parser result is a failure regardless of the
     * message or position.  Otherwise return false.
     */
    def isFail[T] (r : ParseResult[T]) : Boolean =
        r match {
            case Failure (_, _) => true
            case _              => false
        }

    /**
     * Try to parse a string and expect a given result.  Also check that
     * there is no more input left.
     */
    def expect[T] (parser : Parser[T], str : String, result : T) {
        parseAll (parser, str) match {
            case Success (r, in) =>
                if (r != result) fail ("found " + r + " not " + result)
                if (!in.atEnd) fail ("input remaining at " + in.pos)
            case f =>
                fail ("parse failure: " + f)
        }
    }

    test ("parse identifiers") {
        expect (ident, "a", Ident ("a"))
        expect (ident, "total", Ident ("total"))
        expect (ident, "var786", Ident ("var786"))
    }

    test ("parse integer literals") {
        expect (number, "5", IntegerLiteral (5))
        assert (isFail (parseAll (number, "x")))
    }

    test ("parse expressions") {
        expect (expression, "1", IntegerLiteral (1))
        expect (expression, "1+2", Plus (IntegerLiteral (1), IntegerLiteral (2)))
        expect (expression, "1+2+3", Plus (Plus (IntegerLiteral (1), IntegerLiteral (2)), IntegerLiteral (3)))
        expect (expression, "1+2*3", Plus (IntegerLiteral(1), Mult (IntegerLiteral (2), IntegerLiteral (3))))
        expect (expression, "(1+2)*3", Mult (Plus (IntegerLiteral (1), IntegerLiteral (2)), IntegerLiteral (3)))
    }

    test ("keywords are rejected as identifiers") {
        assert (isFail (parseAll (assignment, "WHILE := 3")))
    }

    test ("parse assignment statements") {
        expect (assignment, "a := 5", Assignment (Ident ("a"), IntegerLiteral (5)))
        expect (assignment, "a := b", Assignment (Ident ("a"), Ident ("b")))
    }

    test ("parse statement sequences") {
        expect(statementSequence, "", Nil)
        expect(statementSequence, "v := 1; v := 2",
            List (Assignment (Ident ("v"), IntegerLiteral (1)),
                  Assignment (Ident ("v"), IntegerLiteral (2))))
    }

    test ("parse while statements") {
        expect (whileStatement, "WHILE x DO x:= 1 END",
                WhileStatement (Ident ("x"),
                    List (Assignment (Ident ("x"), IntegerLiteral (1)))))
    }
    
    test ("parse factorial program") {
        val program =
"""
MODULE Factorial;
    
CONST
    limit = 10;

VAR
    v : INTEGER;
    c : INTEGER;
    fact : INTEGER;

BEGIN
    Read (v);
    IF (v < 0) OR (v > limit) THEN
        WriteLn (-1)
    ELSE
        c := 0;
        fact := 1;
        WHILE c < v DO
            c := c + 1;
            fact := fact * c
        END;
        WriteLn (fact)
    END
END Factorial.
"""
        expect (start, program,
            ModuleDecl ("Factorial",
                List (ConstDecl ("limit", IntegerLiteral (10)),
                      VarDecl ("v", IntegerType),
                      VarDecl ("c", IntegerType),
                      VarDecl ("fact", IntegerType)),
                List (ProcedureCall (Ident ("Read"), List (Ident ("v"))),
                      IfStatement (Or (LessThan (Ident ("v"), IntegerLiteral (0)),
                                       GreaterThan (Ident ("v"), Ident ("limit"))),
                          List (ProcedureCall (Ident ("WriteLn"), List (Neg (IntegerLiteral (1))))),
                          List (Assignment (Ident ("c"), IntegerLiteral (0)),
                                Assignment (Ident ("fact"), IntegerLiteral (1)),
                                WhileStatement (LessThan (Ident ("c"), Ident ("v")),
                                     List (Assignment (Ident ("c"), Plus (Ident ("c"), IntegerLiteral (1))),
                                           Assignment (Ident ("fact"), Mult (Ident ("fact"), Ident ("c"))))),
                                ProcedureCall (Ident ("WriteLn"), List (Ident ("fact")))))),
                "Factorial",
                ModuleType ()))

    }

}
