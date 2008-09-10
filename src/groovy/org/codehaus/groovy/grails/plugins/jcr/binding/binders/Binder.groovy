package org.codehaus.groovy.grails.plugins.jcr.binding.binders

import org.codehaus.groovy.grails.plugins.jcr.binding.BindingContext

import javax.jcr.Node

/**
 * TODO: write javadoc
 *
 * @author Sergey Nebolsin (nebolsin@gmail.com)
 */
abstract class Binder {
    BindingContext context

    Binder(BindingContext context) {
        this.context = context
    }

    abstract bindToNode(Node node);

    abstract bindToProperty(Node node, String propertyName, value);

    abstract bindFromNode(Node node);

    abstract bindFromProperty(Node node, String propertyName);
}