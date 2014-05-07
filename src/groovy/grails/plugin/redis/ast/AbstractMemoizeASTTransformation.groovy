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
    protected static final String REDIS_SERVICE = 'redisService'
    protected static final String THIS = 'this'
    protected static final String PRINTLN = 'println'

    void visit(ASTNode[] astNodes, SourceUnit sourceUnit) {
        //map to hold the params we will pass to the memoize[?] method
        def memoizeProperties = [:]

        try {
            injectService(sourceUnit, REDIS_SERVICE, RedisService)
            generateMemoizeProperties(astNodes, sourceUnit, memoizeProperties)
            //if the key is missing there is an issue with the annotation
            if(!memoizeProperties.containsKey(KEY) || !memoizeProperties.get(KEY)) {
                return
            }
            addMemoizedStatements((MethodNode) astNodes[1], memoizeProperties)
            visitVariableScopes(sourceUnit)
        } catch (Exception e) {
            addError("Error during Memoize AST Transformation: ${e}", astNodes[0], sourceUnit)
            throw e
        }
    }

    /**
     * Create the statements for the memoized method, clear the node and then readd the memoized code back to the method.
     * @param methodNode The MethodNode we will be clearing and replacing with the redisService.memoize[?] method call with.
     * @param memoizeProperties The map of properties to use for the service invocation
     */
    private void addMemoizedStatements(MethodNode methodNode, LinkedHashMap memoizeProperties) {
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
     * @param serviceName name of the service to detect and/or inject
     * @param serviceClass Class of the service
     */
    protected void injectService(SourceUnit sourceUnit, String serviceName, Class serviceClass) {
        if(!((ClassNode) sourceUnit.AST.classes.toArray()[0]).properties?.any { it?.field?.name == serviceName }) {
            if(!sourceUnit.AST.imports.any {it.className == ClassHelper.make(serviceClass).name}
                    && !sourceUnit.AST.starImports.any {it.packageName == "${ClassHelper.make(serviceClass).packageName}."}) {
                sourceUnit.AST.addImport(serviceClass.simpleName, ClassHelper.make(serviceClass))
            }
            addProperty((ClassNode) sourceUnit.AST.classes.toArray()[0], serviceName)
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
    private void addProperty(ClassNode cNode, String propertyName, Class propertyType = java.lang.Object.class, Expression initialValue = null) {
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

    protected abstract ConstantExpression makeRedisServiceConstantExpression()

    protected abstract ArgumentListExpression makeRedisServiceArgumentListExpression(Map memoizeProperties)

    protected List<Statement> memoizeMethod(MethodNode methodNode, Map memoizeProperties) {
        BlockStatement body = new BlockStatement()
        addRedisServiceMemoizeInvocation(body, methodNode, memoizeProperties)
        body.statements
    }

    protected void addRedisServiceMemoizeInvocation(BlockStatement body, MethodNode methodNode, Map memoizeProperties) {
        ArgumentListExpression argumentListExpression = makeRedisServiceArgumentListExpression(memoizeProperties)
        argumentListExpression.addExpression(makeClosureExpression(methodNode))

        body.addStatement(
                new ReturnStatement(
                        new MethodCallExpression(
                                new VariableExpression(REDIS_SERVICE),
                                makeRedisServiceConstantExpression(),
                                argumentListExpression
                        )
                )
        )
    }

    protected void addRedisServiceMemoizeKeyExpression(Map memoizeProperties, ArgumentListExpression argumentListExpression) {
        if(memoizeProperties.get(KEY).toString().contains(HASH_CODE)) {
            def ast = new AstBuilder().buildFromString("""
                "${memoizeProperties.get(KEY).toString().replace(HASH_CODE, GSTRING).toString()}"
           """)
            argumentListExpression.addExpression(ast[0].statements[0].expression)
        } else {
            argumentListExpression.addExpression(new ConstantExpression(memoizeProperties.get(KEY).toString()))
        }
    }

	protected void addRedisServiceMemoizeExpireExpression(Map memoizeProperties, ArgumentListExpression argumentListExpression) {
		if(memoizeProperties.get(EXPIRE).toString().contains(HASH_CODE)) {
			def ast = new AstBuilder().buildFromString("""
                Integer.parseInt("${memoizeProperties.get(EXPIRE).toString().replace(HASH_CODE, GSTRING).toString()}")
           """)
			
			argumentListExpression.addExpression(ast[0].statements[0].expression)
		} else {
			argumentListExpression.addExpression(makeConstantExpression(Integer.parseInt(memoizeProperties.get(EXPIRE).toString())))
		}
	}
	
    protected ClosureExpression makeClosureExpression(MethodNode methodNode) {
        ClosureExpression closureExpression = new ClosureExpression(
                [] as Parameter[],
                new BlockStatement(methodNode.code.statements as Statement[], new VariableScope())
        )
        closureExpression.variableScope = new VariableScope()
        closureExpression
    }

    protected ConstantExpression makeConstantExpression(constantExpression) {
        new ConstantExpression(constantExpression)
    }

    protected void addError(String msg, ASTNode node, SourceUnit source) {
        int line = node.lineNumber
        int col = node.columnNumber
        SyntaxException se = new SyntaxException("${msg}\n", line, col)
        SyntaxErrorMessage sem = new SyntaxErrorMessage(se, source)
        source.errorCollector.addErrorAndContinue(sem)
    }
}
