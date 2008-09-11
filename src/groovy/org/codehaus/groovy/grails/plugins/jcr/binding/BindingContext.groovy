package org.codehaus.groovy.grails.plugins.jcr.binding

import javax.jcr.Node
import org.codehaus.groovy.grails.commons.GrailsApplication
import org.codehaus.groovy.grails.commons.ApplicationHolder
import org.codehaus.groovy.grails.plugins.jcr.binding.binders.BinderFactory
import org.codehaus.groovy.grails.plugins.jcr.binding.binders.Binder
import javax.jcr.Session

/**
 * TODO: write javadoc
 *
 * @author Sergey Nebolsin (nebolsin@gmail.com)
 */
class BindingContext {
    private objectsToNodesCache = [:]
    private nodesToObjectsCache = [:]

    GrailsApplication application = ApplicationHolder.application
    BinderFactory factory = new BinderFactory(this)
    ClassLoader classLoader = ClassLoader.getSystemClassLoader()
    Session session

    LinkedList configurations = new LinkedList()

    BindingConfiguration getConfig() {
        configurations.getFirst()
    }
    
    def configure(BindingConfiguration conf) {
        configurations.addFirst(conf)
    }

    def configure(Object object) {
        configure(new BindingConfiguration(object, this))
    }

    def restore() {
        def config = configurations.removeFirst()
        config.object
    }

    def propertyMissing(String name) {
        getConfig()."$name"
    }

    def methodMissing(String name, args) {
        getConfig().invokeMethod(name, args)
    }

    Binder resolveBinder(Class type) {
        def result = factory.resolveBinder(type)
        // println "Resolving $type to $result"
        return result
    }
}