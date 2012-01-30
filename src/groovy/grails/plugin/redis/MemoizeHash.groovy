package grails.plugin.redis

import java.lang.annotation.ElementType
import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy
import java.lang.annotation.Target
import org.codehaus.groovy.transform.GroovyASTTransformationClass

/**
 */
@Retention(RetentionPolicy.SOURCE)
@Target([ElementType.METHOD])
@GroovyASTTransformationClass(['grails.plugin.redis.ast.MemoizeHashASTTransformation'])
@interface MemoizeHash {
    Class value() default {true};
    String key() default '';
    String expire() default '';
}
