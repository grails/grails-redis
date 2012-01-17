package grails.plugin.redis.ast

import org.codehaus.groovy.ast.ASTNode
import org.codehaus.groovy.ast.expr.ArgumentListExpression
import org.codehaus.groovy.ast.expr.ClassExpression
import org.codehaus.groovy.ast.expr.ConstantExpression
import org.codehaus.groovy.control.CompilePhase
import org.codehaus.groovy.control.SourceUnit
import org.codehaus.groovy.transform.GroovyASTTransformation

@GroovyASTTransformation(phase = CompilePhase.CANONICALIZATION)
class MemoizeDomainListASTTransformation extends AbstractMemoizeASTTransformation {

    @Override
    protected void generateMemoizeProperties(ASTNode[] astNodes, SourceUnit sourceUnit, Map memoizeProperties) {
        def expire = astNodes[0]?.members?.expire?.text
        def keyString = astNodes[0]?.members?.key?.text
        def clazz = astNodes[0]?.members?.clazz

        if(!clazz?.class == ClassExpression) {
            addError("Internal Error: annotation doesn't contain clazz property", astNodes[0], sourceUnit)
            return
        }

        if(keyString?.class != String) {
            addError("Internal Error: annotation doesn't contain key String", astNodes[0], sourceUnit)
            return
        }

        if(expire && expire.class != String && !Integer.parseInt(expire)) {
            addError("Internal Error: provided expire is not an String (in millis)", astNodes[0], sourceUnit)
            return
        }
        //***************************************************************************

        memoizeProperties.put(KEY, keyString)
        memoizeProperties.put(CLAZZ, clazz)
        if(expire) {
            memoizeProperties.put(EXPIRE, expire)
        }
    }

    @Override
    protected ConstantExpression createRedisServiceConstantExpression() {
        return new ConstantExpression("memoizeDomainList")
    }

    @Override
    protected ArgumentListExpression createRedisServiceArgumentListExpression(Map memoizeProperties) {
        ArgumentListExpression argumentListExpression = new ArgumentListExpression()
        argumentListExpression.addExpression(memoizeProperties.get(CLAZZ))
        argumentListExpression.addExpression(createConstantExpression(memoizeProperties.get(KEY).toString().replace('#', '\$')))
        if(memoizeProperties.containsKey(EXPIRE)) {
            argumentListExpression.addExpression(createConstantExpression(Integer.parseInt(memoizeProperties.get(EXPIRE).toString())))
        }
        return argumentListExpression
    }
}