package org.codehaus.groovy.grails.plugins.jcr.binding.binders

import org.codehaus.groovy.grails.plugins.jcr.binding.BindingContext
import org.codehaus.groovy.grails.plugins.jcr.JcrConstants
import javax.jcr.Value
import javax.jcr.Node
import org.springframework.util.ClassUtils
import javax.jcr.Property

/**
 * TODO: write javadoc
 *
 * @author Sergey Nebolsin (nebolsin@gmail.com)
 */
class CollectionBinder extends Binder {
    def CollectionBinder(BindingContext context) {
        super(context)
    }

    def supports(Class type) {
        return Collection.isAssignableFrom(type) || type.isArray()
    }

    def bindToNode(Node node) {
        node.getNodes().each { Node value -> value.remove() }

        context.object.eachWithIndex { value, index ->
            Node valueNode = node.addNode(index.toString())
            def binder = context.resolveBinder(value.getClass())
            binder.bindToProperty(valueNode, JcrConstants.COLLECTION_VALUE_PROPERTY_NAME, value)
        }
    }

    def bindToProperty(Node node, String propertyName, values) {
        def jcrPropertyName = context.resolveJcrPropertyName(propertyName)
        def result = []
        def type = null
        def expandCollection = false
        values.each { value ->
            if(!type) {
                type = value.getClass()
            } else {
                if(type != value.getClass()) expandCollection = true
            }
            result << context.convertToJcr(value)
        }

        if(!expandCollection) {
            node.setProperty(jcrPropertyName, result as Value[])
            node.setProperty(constructTypePropertyName(propertyName), ClassUtils.getQualifiedName(values.getClass()))
            node.setProperty(constructValueTypePropertyName(propertyName), ClassUtils.getQualifiedName(type))
        } else {
            Node collectionNode
            if(node.hasNode(jcrPropertyName)) {
                collectionNode = node.getNode(jcrPropertyName)
            } else {
                collectionNode = node.addNode(jcrPropertyName)
                collectionNode.addMixin(JcrConstants.MIXIN_REFERENCEABLE)
            }

            context.configure(values)
            bindToNode(collectionNode)
            context.restore()
        }
    }


    def bindFromNode(Node node) {
        node.getNodes().each { Node value ->

        }
    }

    def bindFromProperty(Node node, String propertyName) {
        def jcrPropertyName = context.resolveJcrPropertyName(propertyName)
        Property property = node.getProperty(jcrPropertyName)
        def values = []
        if(property.getDefinition().isMultiple()) {
            values = property.getValues().toList()
        } else {
            values << property.getValue()
        }
        Class collectionType = context.classLoader.loadClass(node.getProperty(constructTypePropertyName(propertyName)).getString())
        Class valueType = context.classLoader.loadClass(node.getProperty(constructValueTypePropertyName(propertyName)).getString())
        def collection = collectionType.newInstance()
        values.each { value ->
            collection << context.convertToJava(value, valueType)
        }
        context.setObjectProperty(propertyName, collection)
    }

    def constructTypePropertyName(String propertyName) {
        JcrConstants.GRAILS_NAMESPACE_KEY + ":${propertyName}Type"
    }

    def constructValueTypePropertyName(String propertyName) {
        JcrConstants.GRAILS_NAMESPACE_KEY + ":${propertyName}ValueType"
    }
}