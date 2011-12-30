package org.codehaus.groovy.grails.compiler

import org.codehaus.groovy.ast.ASTNode
import org.codehaus.groovy.ast.MethodNode
import org.codehaus.groovy.ast.builder.AstBuilder
import org.codehaus.groovy.ast.stmt.Statement
import org.codehaus.groovy.control.CompilePhase
import org.codehaus.groovy.control.SourceUnit
import org.codehaus.groovy.transform.ASTTransformation
import org.codehaus.groovy.transform.GroovyASTTransformation

@GroovyASTTransformation(phase = CompilePhase.SEMANTIC_ANALYSIS)
class MemoizeASTTransformation implements ASTTransformation {

    public void visit(ASTNode[] nodes, SourceUnit sourceUnit) {
        MethodNode method = (MethodNode) nodes[1]

        MethodNode newMethod = method
        method.code.statements.clear()
        method.code.statements.add(0, memoizeMethod(newMethod))

    }

    def memoizeMethod(MethodNode method) {
        def key = method.parameters.collect{ it.name }.join("+ \":\" +")
        def ast = new AstBuilder().buildFromString(CompilePhase.SEMANTIC_ANALYSIS, false, """
            def key = "${method.name}:" + ${key}
            println "using " + key
            def val = redisService.memoize(key, [expire: 3600]){
                ${method.code.statements}
            }
            val
        """
        )
        Statement stmt = ast[0]
        stmt
    }

//    void visit(ASTNode[] astNodes, SourceUnit sourceUnit) {
//        println "in here"
//        MethodNode annotatedMethod = (MethodNode) astNodes[1]
//        println "go"
//        println annotatedMethod?.code?.toString()
//        MethodNode newMethod = memoizeMethod(annotatedMethod)
//        println "yes"
//        println newMethod?.code?.toString()
//        annotatedMethod.code.set = newMethod.code
//        println "done"
//    }
//
//    MethodNode memoizeMethod(MethodNode method) {
//        def buildNodes = new AstBuilder().buildFromSpec {
//            expression {
//                methodCall {
//                    variable "advice"
//                    constant "before"
//                    argumentList {
//                        constant method.name
//                        list {
//                            parameters.each {variable it.name}
//                        }
//                    }
//                }
//            }
//        }
//        Statement stmt = buildNodes[0]
////            (CompilePhase.INSTRUCTION_SELECTION
////        }, false, """
////        def ${methodNode.name}(){ println "hello" }
////        """)
//        //(MethodNode)newMethod[0]
//        //methodNode
//        stmt
//    }
}