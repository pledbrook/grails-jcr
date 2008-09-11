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
        println "Binding $value to property $propertyName"
        def referenceClass = value.getClass()
        if(referenceClass.metaClass.hasProperty(null, 'grailsJcrMapping')) {
            def mapping = referenceClass.getGrailsJcrMapping()
            Node referenceNode
            if(value.UUID) {
                // already saved object
                referenceNode = node.getSession().getNodeByUUID(value.UUID)
            } else {
                referenceNode = node.getSession().getRootNode().addNode(mapping.basePath)
                referenceNode.addMixin("mix:versionable")
                referenceNode.addMixin("mix:lockable")
            }
            def binder = context.resolveBinder(referenceClass)
            context.configure(value)
            binder.bindToNode(referenceNode)
            context.restore()
            node.setProperty(context.resolveJcrPropertyName(propertyName), referenceNode)
        } else {
            throw new GrailsBindingException("Unsupported class ${referenceClass}: object should have static getGrailsJcrMapping() method.")
        }
    }

    def bindFromNode(Node node) {
        context.object.UUID = node.UUID
        context.persistantProperties.findAll{k, v -> node.hasProperty(context.resolveJcrPropertyName(k))}.each { propertyName, propertyType ->
            def binder = context.resolveBinder(propertyType)
            binder.bindFromProperty(node, propertyName)
        }

        context.persistantProperties.findAll{k, v -> node.hasNode(k)}.each { propertyName, propertyType ->
            def binder = context.resolveBinder(propertyType)
            def childNode = node.getNode(propertyName)
            context.configure(binder.checkAndInstantiate(childNode, propertyType))
            binder.bindFromNode(childNode)
        }
    }

    def bindFromProperty(Node node, String propertyName) {
        throw new GrailsBindingException("Binding complex objects to properties is not supported")
    }

}