package org.codehaus.groovy.grails.compiler

import org.codehaus.groovy.ast.ASTNode
import org.codehaus.groovy.ast.MethodNode
import org.codehaus.groovy.ast.Parameter
import org.codehaus.groovy.ast.VariableScope
import org.codehaus.groovy.ast.stmt.BlockStatement
import org.codehaus.groovy.ast.stmt.ExpressionStatement
import org.codehaus.groovy.ast.stmt.ReturnStatement
import org.codehaus.groovy.ast.stmt.Statement
import org.codehaus.groovy.control.CompilePhase
import org.codehaus.groovy.control.SourceUnit
import org.codehaus.groovy.control.messages.SyntaxErrorMessage
import org.codehaus.groovy.syntax.SyntaxException
import org.codehaus.groovy.syntax.Token
import org.codehaus.groovy.syntax.Types
import org.codehaus.groovy.transform.ASTTransformation
import org.codehaus.groovy.transform.GroovyASTTransformation
import org.codehaus.groovy.ast.expr.*

@GroovyASTTransformation(phase = CompilePhase.SEMANTIC_ANALYSIS)
class MemoizeASTTransformation implements ASTTransformation {

    void visit(ASTNode[] astNodes, SourceUnit sourceUnit) {
        MethodNode methodNode = (MethodNode) astNodes[1]
        def annotationExpression = astNodes[0].members.value

        if(annotationExpression.class != ClosureExpression) {
            addError("Internal Error: annotation doesn't contain key closure", astNodes[0], sourceUnit)
            return
        }

        def stmt = memoizeMethod(methodNode)
        methodNode.code.statements.clear()
        methodNode.code.statements.addAll(stmt)
    }

    private List<Statement> memoizeMethod(MethodNode methodNode) {
        BlockStatement body = new BlockStatement()
        //body.variableScope = methodNode.variableScope
        createInterceptionLogging(body)
        ClosureExpression closureExpression = createClosureExpression(methodNode);
        createClosureVariable(body, closureExpression)
        createRedisServiceMemoizeInvocation(body, methodNode)
        return body.statements
    }

    private def createInterceptionLogging(BlockStatement body) {
        body.addStatement(
                new ExpressionStatement(
                        new MethodCallExpression(
                                new VariableExpression("this"),
                                new ConstantExpression("println"),
                                new ArgumentListExpression(
                                        new ConstantExpression("memoized method")
                                )
                        )
                )
        )
    }

    private def createClosureVariable(BlockStatement body, ClosureExpression closureExpression) {
        body.addStatement(
                new ExpressionStatement(
                        new DeclarationExpression(
                                new VariableExpression("closure"),
                                new Token(Types.EQUALS, "=", -1, -1),
                                closureExpression
                        )
                )
        )
    }

    private def createRedisServiceMemoizeInvocation(BlockStatement body, MethodNode methodNode) {
        body.addStatement(
                new ReturnStatement(
                        new MethodCallExpression(
                                new VariableExpression("redisService"),
                                new ConstantExpression("memoize"),
                                new ArgumentListExpression(
                                        generateMemoizeKey(methodNode),
                                        new VariableExpression("closure")
                                )
                        )
                )
        )
    }

    private ClosureExpression createClosureExpression(MethodNode methodNode) {

        VariableScope variableScope = new VariableScope(methodNode.variableScope)

        ClosureExpression closureExpression = new ClosureExpression(
                [] as Parameter[],
                //new BlockStatement(methodNode.code.statements as Statement[], new VariableScope())
                //new BlockStatement(methodNode.code.statements as Statement[], methodNode.variableScope)
                new BlockStatement(methodNode.code.statements as Statement[], variableScope)
        )
        //closureExpression.variableScope = new VariableScope()
        //closureExpression.variableScope = methodNode.variableScope
        closureExpression.variableScope = variableScope
        return closureExpression
    }

    //todo generate a better key here
    private generateMemoizeKey(MethodNode methodNode) {
        new ConstantExpression(methodNode.name)
    }

    public void addError(String msg, ASTNode node, SourceUnit source) {
        int line = node.lineNumber
        int col = node.columnNumber
        SyntaxException se = new SyntaxException(msg + '\n', line, col)
        SyntaxErrorMessage sem = new SyntaxErrorMessage(se, source)
        source.errorCollector.addErrorAndContinue(sem)
    }
}