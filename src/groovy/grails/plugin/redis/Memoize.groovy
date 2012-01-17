package grails.plugin.redis

import org.codehaus.groovy.transform.GroovyASTTransformationClass
import java.lang.annotation.ElementType
import java.lang.annotation.Target
import java.lang.annotation.RetentionPolicy
import java.lang.annotation.Retention

/**
 */
@Retention(RetentionPolicy.SOURCE)
@Target([ElementType.METHOD])
@GroovyASTTransformationClass(["grails.plugin.redis.MemoizeASTTransformation"])
public @interface Memoize {
    Class value() default {true};
    String key() default 'bad:key';
    String expire() default '';
}
