package grails.plugin.redis.ast

import org.codehaus.groovy.ast.expr.ConstantExpression
import org.codehaus.groovy.control.CompilePhase
import org.codehaus.groovy.transform.GroovyASTTransformation

@GroovyASTTransformation(phase = CompilePhase.CANONICALIZATION)
class MemoizeHashASTTransformation extends MemoizeASTTransformation {

    @Override
    protected ConstantExpression makeRedisServiceConstantExpression() {
        new ConstantExpression('memoizeHash')
    }
}