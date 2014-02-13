package grails.plugin.redis.ast

import org.codehaus.groovy.ast.ASTNode
import org.codehaus.groovy.ast.expr.ArgumentListExpression
import org.codehaus.groovy.ast.expr.ClosureExpression
import org.codehaus.groovy.ast.expr.ConstantExpression
import org.codehaus.groovy.control.CompilePhase
import org.codehaus.groovy.control.SourceUnit
import org.codehaus.groovy.transform.GroovyASTTransformation

@GroovyASTTransformation(phase = CompilePhase.CANONICALIZATION)
class MemoizeASTTransformation extends AbstractMemoizeASTTransformation {

    @Override
    protected void generateMemoizeProperties(ASTNode[] astNodes, SourceUnit sourceUnit, Map memoizeProperties) {
        def expire = astNodes[0]?.members?.expire?.text
        def keyString = astNodes[0]?.members?.key?.text
        def keyClosure = astNodes[0]?.members?.value

        if(!validateMemoizeProperties(keyClosure, keyString, astNodes, sourceUnit, expire)) {
            return
        }
        //***************************************************************************

        memoizeProperties.put(KEY, (keyClosure) ? keyClosure?.code?.statements[0]?.expression?.value : keyString)
        if(expire) {
            memoizeProperties.put(EXPIRE, expire)
        }
    }

    private Boolean validateMemoizeProperties(keyClosure, keyString, ASTNode[] astNodes, SourceUnit sourceUnit, expire) {
        if(keyClosure?.class != ClosureExpression && keyString.class != String) {
            addError('Internal Error: annotation does not contain key closure or key property', astNodes[0], sourceUnit)
            return false
        }

        if(keyClosure && keyClosure.code?.statements[0]?.expression?.value?.class != String) {
            addError('Internal Error: annotation does not contain string key closure', astNodes[0], sourceUnit)
            return false
        }

        if(expire && expire.class != String && !Integer.parseInt(expire)) {
            addError('Internal Error: provided expire is not an String (in millis)', astNodes[0], sourceUnit)
            return false
        }
        true
    }

    @Override
    protected ConstantExpression makeRedisServiceConstantExpression() {
        new ConstantExpression('memoize')
    }

    @Override
    protected ArgumentListExpression makeRedisServiceArgumentListExpression(Map memoizeProperties) {
        ArgumentListExpression argumentListExpression = new ArgumentListExpression()
        addRedisServiceMemoizeKeyExpression(memoizeProperties, argumentListExpression)
        if(memoizeProperties.containsKey(MEMBER)) {
            argumentListExpression.addExpression(makeConstantExpression(memoizeProperties.get(MEMBER).toString()))
        }
        if(memoizeProperties.containsKey(EXPIRE)) {
            addRedisServiceMemoizeExpireExpression(memoizeProperties, argumentListExpression)
        }
        argumentListExpression
    }
}