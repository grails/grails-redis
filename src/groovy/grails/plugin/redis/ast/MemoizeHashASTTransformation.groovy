package grails.plugin.redis.ast

import org.codehaus.groovy.ast.ASTNode
import org.codehaus.groovy.ast.expr.ArgumentListExpression
import org.codehaus.groovy.ast.expr.ClosureExpression
import org.codehaus.groovy.ast.expr.ConstantExpression
import org.codehaus.groovy.control.CompilePhase
import org.codehaus.groovy.control.SourceUnit
import org.codehaus.groovy.transform.GroovyASTTransformation

@GroovyASTTransformation(phase = CompilePhase.CANONICALIZATION)
class MemoizeHashASTTransformation extends MemoizeASTTransformation {
    @Override
    protected ConstantExpression makeRedisServiceConstantExpression() {
        return new ConstantExpression("memoizeHash")
    }
}