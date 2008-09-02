package org.codehaus.groovy.grails.plugins.jcr.binding

import javax.jcr.Item
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

    def push(Object object, Item item) {
        push(new BindingConfiguration(object, item, this))
    }

    def pop() {
        configurations.removeFirst
    }

    Item getItem(Object object) {
        objectsToNodesCache[object]
    }

    Object getObject(Item item) {
        nodesToObjectsCache[item]
    }

    def propertyMissing(String name) {
        getConfig()."$name"
    }
}