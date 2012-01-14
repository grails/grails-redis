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

    private static final String KEY = 'key'
    private static final String EXPIRE = 'expire'

    void visit(ASTNode[] astNodes, SourceUnit sourceUnit) {
        MethodNode methodNode = (MethodNode) astNodes[1]
        //def keyExpression = astNodes[0]?.members?.value

        def memoizeProperties = [:]
        generateMemoizeProperties(astNodes, sourceUnit, memoizeProperties)
        if(!memoizeProperties.containsKey(KEY) || !memoizeProperties.get(KEY)) {
            return
        }

        def stmt = memoizeMethod(methodNode, memoizeProperties)
        methodNode.code.statements.clear()
        methodNode.code.statements.addAll(stmt)

        VariableScopeVisitor scopeVisitor = new VariableScopeVisitor(sourceUnit);
        sourceUnit.AST.classes.each {
            scopeVisitor.visitClass(it)
        }
    }

    /**
     * method to add the key and expires and options if they exist
     * @param astNodes the ast nodes
     * @param sourceUnit the source unit
     * @param memoizeProperties map to put data in
     * @return
     */
    private Map generateMemoizeProperties(ASTNode[] astNodes, SourceUnit sourceUnit, Map memoizeProperties) {
        def expire = astNodes[0]?.members?.expire?.text
        def keyString = astNodes[0]?.members?.key?.text
        def keyClosure = astNodes[0]?.members?.value

        //do some validation on the key for closure or key property ****************
        if(keyClosure?.class != ClosureExpression && keyString.class != String) {
            addError("Internal Error: annotation doesn't contain key closure or key property", astNodes[0], sourceUnit)
            return
        }

        if(keyClosure && keyClosure.code?.statements[0]?.expression?.value?.class != String) {
            addError("Internal Error: annotation doesn't contain string key closure", astNodes[0], sourceUnit)
            return
        }

        if(expire && expire.class != String && !Integer.parseInt(expire)) {
            addError("Internal Error: provided expire is not an String (in millis)", astNodes[0], sourceUnit)
            return
        }
        //***************************************************************************

        memoizeProperties.put(KEY, (keyClosure) ? keyClosure?.code?.statements[0]?.expression?.value : keyString)
        if(expire) {
            memoizeProperties.put(EXPIRE, expire)
        }
    }

    private List<Statement> memoizeMethod(MethodNode methodNode, Map memoizeProperties) {
        BlockStatement body = new BlockStatement()

        // todo: remove this call after development
        createInterceptionLogging(body, 'memoized method')

        createRedisServiceMemoizeInvocation(body, methodNode, memoizeProperties)
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

    private void createRedisServiceMemoizeInvocation(BlockStatement body, MethodNode methodNode, Map memoizeProperties) {

        //todo: refactor this to new method? *************************
        ArgumentListExpression argumentListExpression = new ArgumentListExpression()
        argumentListExpression.addExpression(createConstantExpression(methodNode, methodNode.name + ":" + memoizeProperties.get(KEY).toString().replace('#', '\$')))
        if(memoizeProperties.containsKey(EXPIRE)){
            argumentListExpression.addExpression(createConstantExpression(methodNode, Integer.parseInt(memoizeProperties.get(EXPIRE).toString())))
        }
        argumentListExpression.addExpression(createClosureExpression(methodNode))
        //**************************************************************

        body.addStatement(
                new ReturnStatement(
                        new MethodCallExpression(
                                new VariableExpression("redisService"),
                                new ConstantExpression("memoize"),
                                argumentListExpression
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
    private ConstantExpression createConstantExpression(MethodNode methodNode, constantExpression) {
        return new ConstantExpression(constantExpression)
    }

    public void addError(String msg, ASTNode node, SourceUnit source) {
        int line = node.lineNumber
        int col = node.columnNumber
        SyntaxException se = new SyntaxException(msg + '\n', line, col)
        SyntaxErrorMessage sem = new SyntaxErrorMessage(se, source)
        source.errorCollector.addErrorAndContinue(sem)
    }
}