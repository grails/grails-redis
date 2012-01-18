package grails.plugin.redis.ast

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
import org.codehaus.groovy.syntax.Token
import org.codehaus.groovy.syntax.Types
import org.codehaus.groovy.transform.ASTTransformation
import org.codehaus.groovy.transform.GroovyASTTransformation
import org.codehaus.groovy.ast.expr.*
import org.codehaus.groovy.ast.builder.AstBuilder

/**
 */
@GroovyASTTransformation(phase = CompilePhase.CANONICALIZATION)
abstract class AbstractMemoizeASTTransformation implements ASTTransformation {

    protected static final String KEY = 'key'
    protected static final String MEMOIZE_KEY = 'memKey'
    protected static final String EXPIRE = 'expire'
    protected static final String CLAZZ = 'clazz'
    private static final String HASH_CODE = '#'
    private static final String GSTRING = '$'
    private static final String REDIS_SERVICE = "redisService"
    protected static final String THIS = "this"
    protected static final String PRINTLN = "println"

    void visit(ASTNode[] astNodes, SourceUnit sourceUnit) {
        try {
            MethodNode methodNode = (MethodNode) astNodes[1]

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
        } catch (Exception e) {
            addError("Error during Memoize AST Transformation: ${e}", astNodes[0], sourceUnit)
            throw e
        }
    }

    /**
     * method to add the key and expires and options if they exist
     * @param astNodes the ast nodes
     * @param sourceUnit the source unit
     * @param memoizeProperties map to put data in
     * @return
     */
    protected abstract void generateMemoizeProperties(ASTNode[] astNodes, SourceUnit sourceUnit, Map memoizeProperties)

    protected abstract ConstantExpression createRedisServiceConstantExpression()

    protected abstract ArgumentListExpression createRedisServiceArgumentListExpression(Map memoizeProperties)

    protected List<Statement> memoizeMethod(MethodNode methodNode, Map memoizeProperties) {
        BlockStatement body = new BlockStatement()
        createInterceptionLogging(body, 'memoized method')
        createRedisServiceMemoizeInvocation(body, methodNode, memoizeProperties)
        return body.statements
    }

    /**
     * this is just used for debugging during development
     * todo: remove this after all things are flushed out
     * @param body
     */
    protected void createInterceptionLogging(BlockStatement body, String message) {
        body.addStatement(
                new ExpressionStatement(
                        new MethodCallExpression(
                                new VariableExpression(THIS),
                                new ConstantExpression(PRINTLN),
                                new ArgumentListExpression(
                                        new ConstantExpression(message)
                                )
                        )
                )
        )
    }

    protected void createRedisServiceMemoizeInvocation(BlockStatement body, MethodNode methodNode, Map memoizeProperties) {
        ArgumentListExpression argumentListExpression = createRedisServiceArgumentListExpression(memoizeProperties)
        argumentListExpression.addExpression(createClosureExpression(methodNode))

        body.addStatement(
                new ReturnStatement(
                        new MethodCallExpression(
                                new VariableExpression(REDIS_SERVICE),
                                createRedisServiceConstantExpression(),
                                argumentListExpression
                        )
                )
        )
    }

     protected void createRedisServiceMemoizeKeyExpression(Map memoizeProperties, ArgumentListExpression argumentListExpression) {
        if(memoizeProperties.get(KEY).toString().contains(HASH_CODE)) {
            def ast = new AstBuilder().buildFromString("""
                "${memoizeProperties.get(KEY).toString().replace(HASH_CODE, GSTRING).toString()}"
           """)
            argumentListExpression.addExpression(ast[0].statements[0].expression)
        } else {
            argumentListExpression.addExpression(new VariableExpression(memoizeProperties.get(KEY).toString()))
        }
    }

    protected ClosureExpression createClosureExpression(MethodNode methodNode) {
        ClosureExpression closureExpression = new ClosureExpression(
                [] as Parameter[],
                new BlockStatement(methodNode.code.statements as Statement[], new VariableScope())
        )
        closureExpression.variableScope = methodNode.variableScope.copy()
        return closureExpression
    }

    protected ConstantExpression createConstantExpression(constantExpression) {
        return new ConstantExpression(constantExpression)
    }

    protected void addError(String msg, ASTNode node, SourceUnit source) {
        int line = node.lineNumber
        int col = node.columnNumber
        SyntaxException se = new SyntaxException("${msg}\n", line, col)
        SyntaxErrorMessage sem = new SyntaxErrorMessage(se, source)
        source.errorCollector.addErrorAndContinue(sem)
    }
}
