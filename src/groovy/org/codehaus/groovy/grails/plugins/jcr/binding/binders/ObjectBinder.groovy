package org.codehaus.groovy.grails.plugins.jcr.binding.binders

import org.codehaus.groovy.grails.plugins.jcr.binding.BindingContext
import org.codehaus.groovy.grails.plugins.jcr.binding.GrailsBindingException
import javax.jcr.Node
import javax.jcr.Property
import javax.jcr.Value

import org.codehaus.groovy.grails.plugins.jcr.JcrConstants
import org.springframework.util.ClassUtils

/**
 * TODO: write javadoc
 *
 * @author Sergey Nebolsin (nebolsin@gmail.com)
 */
class ObjectBinder extends Binder {

    def ObjectBinder(BindingContext context) {
        super(context)
    }

    def supports(Class type) {
        return true
    }

    def bindToNode(Node node) {
        node.setProperty(JcrConstants.CLASS_PROPERTY_NAME, ClassUtils.getQualifiedName(context.object.getClass()))
        context.persistantProperties.each { propertyName, propertyType ->
            def value = context.getObjectPropertyValue(propertyName)
            if(value != null) {
                def binder = context.resolveBinder(propertyType)
                binder.bindToProperty(node, propertyName, value)
            }
        }
    }

    def bindToProperty(Node node, String propertyName, value) {
        throw new GrailsBindingException("Binding complex objects to properties is not supported")
    }

    def bindFromNode(Node node) {
        if(!node.hasProperty(JcrConstants.CLASS_PROPERTY_NAME)) {
            throw new GrailsBindingException("Node doesn't contain class information, cannot bind from this node")
        } else {
            String className = node.getProperty(JcrConstants.CLASS_PROPERTY_NAME).getString()
            Class targetClass = context.classLoader.loadClass(className)
            println "Loaded target class: $targetClass.name"
            if(targetClass != context.object.getClass()) {
                throw new GrailsBindingException("Node with $targetClass.name class cannot be binded to ${context.object.getClass()} class")
            }

            context.persistantProperties.findAll{k, v -> node.hasProperty(k)}.each { propertyName, propertyType ->
                def binder = context.resolveBinder(propertyType)
                binder.bindFromProperty(node, propertyName)
            }
        }
    }

    def bindFromProperty(Node node, String propertyName) {
        throw new GrailsBindingException("Binding complex objects to properties is not supported")
    }

}