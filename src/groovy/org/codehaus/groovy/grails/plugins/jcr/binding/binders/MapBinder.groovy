package org.codehaus.groovy.grails.plugins.jcr.binding.binders

import org.codehaus.groovy.grails.plugins.jcr.binding.BindingContext
import javax.jcr.Node
import org.codehaus.groovy.grails.plugins.jcr.JcrConstants

/**
 * TODO: write javadoc
 *
 * @author Sergey Nebolsin (nebolsin@gmail.com)
 */
class MapBinder extends Binder {

    MapBinder(BindingContext context) {
        super(context)
    }
    
    def supports(Class type) {
        return Map.isAssignableFrom(type)
    }

    def bindToNode(Node node) {
        node.getNodes().each { Node value ->
            if(!source.containsKey(value.getName())) value.remove()
        }

        context.object.each { key, value ->
            Node valueNode = node.addNode(key)
            def binder = context.resolveBinder(key.getClass())
            binder.bindToProperty(valueNode, JcrConstants.MAP_KEY_PROPERTY_NAME, key)
            binder = context.resolveBinder(value.getClass())
            binder.bindToProperty(valueNode, JcrConstants.MAP_VALUE_PROPERTY_NAME, value)
        }
    }

    def bindToProperty(Node node, String propertyName, value) {
        def jcrPropertyName = context.resolveJcrPropertyName(propertyName)
        Node mapNode
        if(node.hasNode(jcrPropertyName)) {
            mapNode = node.getNode(jcrPropertyName)
        } else {
            mapNode = node.addNode(jcrPropertyName)
            mapNode.addMixin(JcrConstants.MIXIN_REFERENCEABLE)
        }

        context.configure(value)
        bindToNode(mapNode)
        context.restore()
    }

    def bindFromNode(Node node) {
    }

    def bindFromProperty(Node node, String propertyName) {
    }

}