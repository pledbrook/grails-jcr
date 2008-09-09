package org.codehaus.groovy.grails.plugins.jcr.binding

import javax.jcr.Node
import org.codehaus.groovy.grails.commons.GrailsDomainClass
import java.beans.PropertyDescriptor
import org.springframework.beans.BeanWrapper
import org.codehaus.groovy.grails.commons.GrailsDomainClassProperty
import org.springframework.beans.PropertyAccessorFactory
import org.codehaus.groovy.grails.plugins.jcr.JcrConstants
import javax.jcr.Value

/**
 * TODO: write javadoc
 *
 * @author Sergey Nebolsin (nebolsin@gmail.com)
 */
class BindingConfiguration {
    private static final disallowedProperties = ["metaClass", "properties"]

    BindingContext context
    Object object
    BeanWrapper objectWrapper
    Node node
    GrailsDomainClass domainClass
    def propertyValues = [:]
    boolean collection
    boolean map

    // default mapping configuration
    def mapping = [
            namespace: ''
    ]

    public BindingConfiguration(Object object, Node node, BindingContext context) {
        this.object = object
        if(object != null) {
            objectWrapper = PropertyAccessorFactory.forBeanPropertyAccess(object)
        }
        this.node = node
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
            // set namespace if configured in class
            if(objectWrapper.isReadableProperty(JcrConstants.NAMESPACE_PROPERTY_NAME)) {
                mapping.namespace = objectWrapper.getPropertyValue(JcrConstants.NAMESPACE_PROPERTY_NAME) ?: ""
                if(StringUtils.hasLength(mapping.namespace)) mapping.namespace += ":"
            }

            if(context.application && context.application.isDomainClass(type)) {
                domainClass = context.application.getDomainClass(type)

                // we will use DSL configuration here in the future
                domainClass.persistantProperties.each {GrailsDomainClassProperty prop ->
                    propertyValues[prop.name] = object."$prop.name"
                }

            } else {
                objectWrapper.getPropertyDescriptors().findAll {PropertyDescriptor pd ->
                    !disallowedProperties.contains(pd.getName())
                }.each {PropertyDescriptor pd ->
                    propertyValues[pd.getName()] = objectWrapper.getPropertyValue(pd.getName())
                }
            }

        }

        println "Configured $type.name - $mapping"
    }

    boolean isDomainClass() {
        return domainClass != null
    }

    def propertyMissing(String name) {
        mapping[name]
    }

    Object getNodePropertyValue(Node node, String propertyName, Class targetClass) {
        if(!node.hasProperty(propertyName)) {
            throw new GrailsBindingException("Node ${node.getPath()} doesn't have property with name $propertyName")
        }
        Value value = node.getProperty(propertyName).getValue()
        return JcrHelper.getOptimalValue(value, targetClass)
    }

    void setNodePropertyValue(Node node, String propertyName, Object value) {
        node.setProperty(propertyName, JcrHelper.createValue(value, node.getSession().getValueFactory()))
    }

    Class getObjectPropertyType(String propertyName) {
        String name = removeNamespaceIfNecessary(propertyName)
        return objectWrapper.getPropertyType(name)
    }

    void setObjectProperty(String propertyName, Object value) {
        String name = removeNamespaceIfNecessary(propertyName)
        objectWrapper.setPropertyValue name, value
    }

    String addNamespaceIfNecessaty(String name) {
        if(name.indexOf(':') < 0) {
            return mapping.namespace + name
        } else {
            return name
        }
    }

    String removeNamespaceIfNecessary(String propertyName) {
        if(propertyName.startsWith(mapping.namespace)) return propertyName - mapping.namespace
        return propertyName
    }

    Object convertIfNecessary(Object value, Class targetClass) {
        return objectWrapper.convertIfNecessary(value, targetClass)
    }
}