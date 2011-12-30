package org.codehaus.groovy.grails.compiler

import org.codehaus.groovy.transform.GroovyASTTransformationClass
import java.lang.annotation.ElementType
import java.lang.annotation.Target
import java.lang.annotation.RetentionPolicy
import java.lang.annotation.Retention

/**
 */
@Retention(RetentionPolicy.SOURCE)
@Target([ElementType.METHOD])
@GroovyASTTransformationClass(["org.codehaus.groovy.grails.compiler.MemoizeASTTransformation"])
public @interface Memoize {
}
