package org.codehaus.groovy.grails.compiler

import org.codehaus.groovy.ast.ASTNode
import org.codehaus.groovy.ast.MethodNode
import org.codehaus.groovy.ast.Parameter
import org.codehaus.groovy.ast.VariableScope
import org.codehaus.groovy.ast.stmt.BlockStatement
import org.codehaus.groovy.ast.stmt.ExpressionStatement
import org.codehaus.groovy.ast.stmt.ReturnStatement
import org.codehaus.groovy.ast.stmt.Statement
import org.codehaus.groovy.classgen.VariableScopeVisitor
import org.codehaus.groovy.control.CompilePhase
import org.codehaus.groovy.control.SourceUnit
import org.codehaus.groovy.control.messages.SyntaxErrorMessage
import org.codehaus.groovy.syntax.SyntaxException
import org.codehaus.groovy.transform.ASTTransformation
import org.codehaus.groovy.transform.GroovyASTTransformation
import org.codehaus.groovy.ast.expr.*

@GroovyASTTransformation(phase = CompilePhase.CANONICALIZATION)
class MemoizeASTTransformation implements ASTTransformation {

    void visit(ASTNode[] astNodes, SourceUnit sourceUnit) {
        MethodNode methodNode = (MethodNode) astNodes[1]
        def annotationExpression = astNodes[0].members.value

        if(!validateMemoizeKey(annotationExpression)) {
            return
        }

        def stmt = memoizeMethod(methodNode, annotationExpression)
        methodNode.code.statements.clear()
        methodNode.code.statements.addAll(stmt)

        VariableScopeVisitor scopeVisitor = new VariableScopeVisitor(sourceUnit);
        sourceUnit.AST.classes.each {
            scopeVisitor.visitClass(it)
        }
    }

    private boolean validateMemoizeKey(annotationExpression) {
        if(annotationExpression.class != ClosureExpression) {
            addError("Internal Error: annotation doesn't contain key closure", astNodes[0], sourceUnit)
            return false
        }

        if(annotationExpression?.code?.statements[0]?.expression?.value?.class != String) {
            addError("Internal Error: annotation doesn't contain string key closure", astNodes[0], sourceUnit)
            return false
        }
        return true
    }

    private List<Statement> memoizeMethod(MethodNode methodNode, ClosureExpression annotationExpression) {
        BlockStatement body = new BlockStatement()

        // todo: remove this call after development
        createInterceptionLogging(body, 'memoized method')

        createRedisServiceMemoizeInvocation(body, methodNode, annotationExpression)
        return body.statements
    }

    /**
     * this is just used for debugging during development
     * todo: remove this after all things are flushed out
     * @param body
     */
    private void createInterceptionLogging(BlockStatement body, String message) {
        body.addStatement(
                new ExpressionStatement(
                        new MethodCallExpression(
                                new VariableExpression("this"),
                                new ConstantExpression("println"),
                                new ArgumentListExpression(
                                        new ConstantExpression(message)
                                )
                        )
                )
        )
    }

    private void createRedisServiceMemoizeInvocation(BlockStatement body, MethodNode methodNode, ClosureExpression annotationExpression) {
        body.addStatement(
                new ReturnStatement(
                        new MethodCallExpression(
                                new VariableExpression("redisService"),
                                new ConstantExpression("memoize"),
                                new ArgumentListExpression(
                                        generateMemoizeKey(methodNode, annotationExpression),
                                        createClosureExpression(methodNode)
                                )
                        )
                )
        )
    }

    private ClosureExpression createClosureExpression(MethodNode methodNode) {

        ClosureExpression closureExpression = new ClosureExpression(
                [] as Parameter[],
                new BlockStatement(methodNode.code.statements as Statement[], new VariableScope())
        )
        closureExpression.variableScope = methodNode.variableScope.copy()
        return closureExpression
    }

    //todo generate a better key here
    private ConstantExpression generateMemoizeKey(MethodNode methodNode, ClosureExpression annotationExpression) {
        String key = annotationExpression.code?.statements[0]?.expression?.value
//        println key.replace('#','\$')
        return new ConstantExpression(key.replace('#','\$'))
    }

    public void addError(String msg, ASTNode node, SourceUnit source) {
        int line = node.lineNumber
        int col = node.columnNumber
        SyntaxException se = new SyntaxException(msg + '\n', line, col)
        SyntaxErrorMessage sem = new SyntaxErrorMessage(se, source)
        source.errorCollector.addErrorAndContinue(sem)
    }
}