package org.codehaus.groovy.grails.plugins.jcr.binding

import javax.jcr.Node

/**
 * Universal binder for binding Object (possibly Grails Domain class) to JCR Node
 * and vise versa.
 *
 * @author Sergey Nebolsin (nebolsin@gmail.com)
 */
class ExperimentalNodeBinder {

    ClassLoader classLoader = System.getClassLoader()
    BindingContext context = new BindingContext()

    public ExperimentalNodeBinder() {}

    public ExperimentalNodeBinder(ClassLoader classLoader) {
        context.classLoader = classLoader
    }

    public void bindToNode(Node node, Object source) {
        def binder = context.resolveBinder(source.getClass())
        context.configure source
        binder.bindToNode(node)
        context.restore()
    }

    public Object bindFromNode(Node node, Object target) {
        def binder = context.resolveBinder(target.getClass())
        context.configure target
        binder.bindFromNode(node)
        context.restore()
    }
}