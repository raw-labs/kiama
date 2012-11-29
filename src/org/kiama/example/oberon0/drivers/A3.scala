package org.kiama
package example.oberon0
package drivers

trait A3Phases extends base.TransformingDriver
    with L3.Parser
    with L3.source.PrettyPrinter
    with L3.NameAnalyser
    with L3.TypeAnalyser
    with L2.Lifter
    with L2.Desugarer {

    def artefact : String = "A3"
    def langlevel : Int = 3
    def tasklevel : Int = 3

}

object A3 extends A3Phases
