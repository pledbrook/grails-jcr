package org.codehaus.groovy.grails.plugins.jcr.binding.binders

import org.codehaus.groovy.grails.plugins.jcr.binding.BindingContext
import org.codehaus.groovy.grails.plugins.jcr.binding.JcrHelper
import org.codehaus.groovy.grails.plugins.jcr.JcrConstants
import javax.jcr.Value
import javax.jcr.Node

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
            result << JcrHelper.createValue(value, node.getSession().getValueFactory())
        }

        if(!expandCollection) {
            node.setProperty(jcrPropertyName, result as Value[])
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
    }

    def bindFromProperty(Node node, String propertyName) {
    }
}