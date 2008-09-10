package org.codehaus.groovy.grails.plugins.jcr.binding.binders

import org.codehaus.groovy.grails.plugins.jcr.binding.BindingContext

/**
 * TODO: write javadoc
 *
 * @author Sergey Nebolsin (nebolsin@gmail.com)
 */
class BinderFactory {

    def binders = []

    BinderFactory(BindingContext context) {
        binders = [new SimpleBinder(context), new CollectionBinder(context),new MapBinder(context), new ObjectBinder(context)]
    }

    def resolveBinder(Class type) {
        binders.find { binder -> binder.supports(type) }
    }

}