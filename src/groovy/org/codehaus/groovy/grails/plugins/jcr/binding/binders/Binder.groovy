package org.codehaus.groovy.grails.plugins.jcr.binding.binders

import org.codehaus.groovy.grails.plugins.jcr.binding.BindingContext

import javax.jcr.Node
import org.codehaus.groovy.grails.plugins.jcr.binding.GrailsBindingException
import org.codehaus.groovy.grails.plugins.jcr.JcrConstants

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

    def checkAndInstantiate(Node node, Class requiredClass) {
        if(!node.hasProperty(JcrConstants.CLASS_PROPERTY_NAME)) {
            throw new GrailsBindingException("Node doesn't contain class information, cannot bind from this node")
        } else {
            String className = node.getProperty(JcrConstants.CLASS_PROPERTY_NAME).getString()
            Class targetClass = context.classLoader.loadClass(className)
            if(targetClass != requiredClass) {
                throw new GrailsBindingException("Node with $targetClass.name class cannot be binded to ${requiredClass.name} class")
            }
            return targetClass.newInstance()
        }
    }
}