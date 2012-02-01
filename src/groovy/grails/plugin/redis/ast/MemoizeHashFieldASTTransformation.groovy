package grails.plugin.redis.ast

import org.codehaus.groovy.ast.ASTNode
import org.codehaus.groovy.ast.expr.ConstantExpression
import org.codehaus.groovy.control.CompilePhase
import org.codehaus.groovy.control.SourceUnit
import org.codehaus.groovy.transform.GroovyASTTransformation

@GroovyASTTransformation(phase = CompilePhase.CANONICALIZATION)
class MemoizeHashFieldASTTransformation extends MemoizeASTTransformation {

    @Override
    protected void generateMemoizeProperties(ASTNode[] astNodes, SourceUnit sourceUnit, Map memoizeProperties) {
        super.generateMemoizeProperties(astNodes, sourceUnit, memoizeProperties)
        def member = astNodes[0]?.members?.member?.value

        if(!member || member?.class != String) {
            addError('Internal Error: member is required for score', astNodes[0], sourceUnit)
            return
        }

        memoizeProperties.put(MEMBER, member)
    }

    @Override
    protected ConstantExpression makeRedisServiceConstantExpression() {
        new ConstantExpression('memoizeHashField')
    }
}