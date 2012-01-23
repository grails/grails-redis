package grails.plugin.redis.ast

import grails.plugin.redis.RedisService
import org.codehaus.groovy.ast.builder.AstBuilder
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
import org.codehaus.groovy.ast.*
import org.codehaus.groovy.ast.expr.*
import static org.springframework.asm.Opcodes.ACC_PRIVATE
import static org.springframework.asm.Opcodes.ACC_PUBLIC

/**
 */
@GroovyASTTransformation(phase = CompilePhase.CANONICALIZATION)
abstract class AbstractMemoizeASTTransformation implements ASTTransformation {

    protected static final String KEY = 'key'
    protected static final String MEMOIZE_KEY = 'memKey'
    protected static final String EXPIRE = 'expire'
    protected static final String CLAZZ = 'clazz'
    protected static final String MEMBER = 'member'
    protected static final String HASH_CODE = '#'
    protected static final String GSTRING = '$'
    protected static final String REDIS_SERVICE = "redisService"
    protected static final String THIS = "this"
    protected static final String PRINTLN = "println"

    void visit(ASTNode[] astNodes, SourceUnit sourceUnit) {
        //map to hold the params we will pass to the memoize[?] method
        def memoizeProperties = [:]

        try {
            injectRedisService(sourceUnit)
            generateMemoizeProperties(astNodes, sourceUnit, memoizeProperties)
            //if the key is missing there is an issue with the annotation
            if(!memoizeProperties.containsKey(KEY) || !memoizeProperties.get(KEY)) {
                return
            }
            createMemoizedStatements((MethodNode) astNodes[1], memoizeProperties)
            visitVariableScopes(sourceUnit)
        } catch (Exception e) {
            addError("Error during Memoize AST Transformation: ${e}", astNodes[0], sourceUnit)
            throw e
        }
    }

    /**
     * Create the statements for the memoized method, clear the node and then readd the memoized code back to the method.
     * @param methodNode The MethodNode we will be clearing and replacing with the redisService.memoize[?] method call with.
     * @param memoizeProperties The map of properties to use for th
     * @return
     */
    private def createMemoizedStatements(MethodNode methodNode, LinkedHashMap memoizeProperties) {
        def stmt = memoizeMethod(methodNode, memoizeProperties)
        methodNode.code.statements.clear()
        methodNode.code.statements.addAll(stmt)
    }

    /**
     * Fix the variable scopes for closures.  Without this closures will be missing the input params being passed from the parent scope.
     * @param sourceUnit The SourceUnit to visit and add the variable scopes.
     */
    private void visitVariableScopes(SourceUnit sourceUnit) {
        VariableScopeVisitor scopeVisitor = new VariableScopeVisitor(sourceUnit);
        sourceUnit.AST.classes.each {
            scopeVisitor.visitClass(it)
        }
    }

    /**
     * Determine if the user missed injecting the redisService into the class with the @Memoized method.
     * @param sourceUnit SourceUnit to detect and/or inject service into
     */
    private void injectRedisService(SourceUnit sourceUnit) {
        if(!((ClassNode) sourceUnit.ast.classes.toArray()[0]).properties?.any { it?.field?.name == REDIS_SERVICE }) {
            println "Adding redisService to class ${sourceUnit.ast.classes[0].name} since it is missing..."
            sourceUnit.AST.addImport("RedisService", ClassHelper.make(RedisService))
            addRedisServiceProperty((ClassNode) sourceUnit.ast.classes.toArray()[0], REDIS_SERVICE)
        }
    }

    /**
     * This method adds a new property to the class. Groovy automatically handles adding the getters and setters so you
     * don't have to create special methods for those.  This could be reused for other properties.
     * @param cNode Node to inject property onto.  Usually a ClassNode for the current class.
     * @param propertyName The name of the property to inject.
     * @param propertyType The object class of the property. (defaults to Object.class)
     * @param initialValue Initial value of the property. (defaults null)
     */
    private void addRedisServiceProperty(ClassNode cNode, String propertyName, Class propertyType = java.lang.Object.class, Expression initialValue = null) {
        FieldNode field = new FieldNode(
                propertyName,
                ACC_PRIVATE,
                new ClassNode(propertyType),
                new ClassNode(cNode.class),
                initialValue
        )

        cNode.addProperty(new PropertyNode(field, ACC_PUBLIC, null, null))
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
