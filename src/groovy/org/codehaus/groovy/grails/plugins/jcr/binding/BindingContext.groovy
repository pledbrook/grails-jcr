package org.codehaus.groovy.grails.plugins.jcr.binding

import javax.jcr.Node
import org.codehaus.groovy.grails.commons.GrailsApplication
import org.codehaus.groovy.grails.commons.ApplicationHolder

/**
 * TODO: write javadoc
 *
 * @author Sergey Nebolsin (nebolsin@gmail.com)
 */
class BindingContext {
    private objectsToNodesCache = [:]
    private nodesToObjectsCache = [:]

    GrailsApplication application = ApplicationHolder.application


    LinkedList configurations = new LinkedList()

    BindingConfiguration getConfig() {
        configurations.getFirst()
    }
    
    def push(BindingConfiguration conf) {
        configurations.addFirst conf
        objectsToNodesCache[conf.object] = conf.item
        nodesToObjectsCache[conf.item] = conf.object
    }

    def push(Object object, Node node) {
        push(new BindingConfiguration(object, node, this))
    }

    def pop() {
        configurations.removeFirst
    }

    Node getNode(Object object) {
        objectsToNodesCache[object]
    }

    Object getObject(Node node) {
        nodesToObjectsCache[node]
    }

    def propertyMissing(String name) {
        getConfig()."$name"
    }

    def methodMissing(String name, args) {
        getConfig().invokeMethod(name, args)
    }
}