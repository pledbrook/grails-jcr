package org.codehaus.groovy.grails.plugins.jcr.binding

import javax.jcr.Item
import org.codehaus.groovy.grails.commons.GrailsDomainClass
import java.beans.PropertyDescriptor
import org.springframework.beans.BeanWrapper
import org.codehaus.groovy.grails.commons.GrailsDomainClassProperty
import org.springframework.beans.PropertyAccessorFactory
import org.codehaus.groovy.grails.plugins.jcr.JcrConstants

/**
 * TODO: write javadoc
 *
 * @author Sergey Nebolsin (nebolsin@gmail.com)
 */
class BindingConfiguration {
    private static final disallowedProperties = ["metaClass", "properties"]

    BindingContext context
    Object object
    Item item
    GrailsDomainClass domainClass
    def propertyValues = [:]
    boolean collection
    boolean map

    // default mapping configuration
    def mapping = [
            namespace: ''
    ]

    public BindingConfiguration(Object object, Item item, BindingContext context) {
        this.object = object
        this.item = item
        this.context = context
        configure()
    }

    def configure() {

        def type = object.getClass()

        println "Configuring $type.name - $object"

        if(Collection.isAssignableFrom(type)) {
            collection = true
        } else if(type.isArray()) {
            collection = true
            object = object.toList()
        } else if(Map.isAssignableFrom(type)) {
            map = true
        } else {
            BeanWrapper src = PropertyAccessorFactory.forBeanPropertyAccess(object)

            // set namespace if configured in class
            if(src.isReadableProperty(JcrConstants.NAMESPACE_PROPERTY_NAME)) {
                mapping.namespace = src.getPropertyValue(JcrConstants.NAMESPACE_PROPERTY_NAME) ?: ""
                if(StringUtils.hasLength(mapping.namespace)) mapping.namespace += ":"
            }

            if(context.application && context.application.isDomainClass(type)) {
                domainClass = context.application.getDomainClass(type)

                // we will use DSL configuration here in the future
                domainClass.persistantProperties.each {GrailsDomainClassProperty prop ->
                    propertyValues[getFullName(prop.name)] = object."$prop.name"
                }

            } else {
                src.getPropertyDescriptors().findAll {PropertyDescriptor pd ->
                    !disallowedProperties.contains(pd.getName())
                }.each {PropertyDescriptor pd ->
                    propertyValues[getFullName(pd.getName())] = src.getPropertyValue(pd.getName())
                }
            }

        }

        println "Configured $type.name - $mapping"
    }

    def getFullName(String name) {
        if(name.indexOf(':') < 0) {
            return mapping.namespace + name
        } else {
            return name
        }
    }

    boolean isDomainClass() {
        return domainClass != null
    }

    def propertyMissing(String name) {
        mapping[name]
    }

}