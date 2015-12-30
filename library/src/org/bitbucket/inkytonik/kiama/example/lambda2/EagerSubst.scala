/*
 * This file is part of Kiama.
 *
 * Copyright (C) 2009-2015 Anthony M Sloane, Macquarie University.
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

package org.bitbucket.inkytonik.kiama
package example.lambda2

/**
 * Eager evaluation of lambda calculus with term-level substitution and
 * arithmetic operations.
 */
trait EagerSubst extends ReduceSubst {

    import LambdaTree._
    import org.bitbucket.inkytonik.kiama.rewriting.Rewriter._
    import org.bitbucket.inkytonik.kiama.rewriting.Strategy

    /**
     * Evaluate applications, local bindings and operands, then try to
     * reduce the expression itself, repeating until no change.
     */
    override lazy val s : Strategy =
        attempt (App (s, s) + Let (id, id, s, s) + Opn (s, id, s)) <*
        attempt (lambda <* s)

}

class EagerSubstEvaluator extends EagerSubst
