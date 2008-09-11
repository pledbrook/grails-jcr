package org.codehaus.groovy.grails.plugins.jcr

import org.codehaus.groovy.grails.commons.GrailsDomainClass
import org.codehaus.groovy.grails.commons.ApplicationHolder
import org.codehaus.groovy.grails.commons.GrailsDomainClassProperty
import org.springframework.util.StringUtils

/**
 * TODO: write javadoc
 *
 * @author Sergey Nebolsin (nebolsin@gmail.com)
 */
class JcrConfigurator {

    private static final disallowedProperties = ["UUID"]


    static Map readConfiguration(GrailsDomainClass dc) {
        def application = ApplicationHolder.application
        def mapping = [:]
        mapping.namespace = dc.getPropertyValue(JcrConstants.NAMESPACE_PROPERTY_NAME) ?: ""
        if(StringUtils.hasLength(mapping.namespace)) mapping.namespace += ":"

        def persistantProperties = [:]
        def collectionTypes = [:]
        // we will use DSL configuration here in the future
        dc.persistantProperties.findAll { !disallowedProperties.contains(it.name) }.each {GrailsDomainClassProperty prop ->
            persistantProperties[prop.name] = prop.type
            if(prop.oneToMany || prop.manyToMany) {
                collectionTypes[prop.name] = prop.referencedPropertyType
            }
        }

        mapping.persistantProperties = persistantProperties
        mapping.collectionTypes = collectionTypes

        def basePath = "${mapping.namespace}${dc.shortName}"
        def applicationName = application?.metadata?.'app.name'
        basePath = applicationName ? "$applicationName$basePath/" : basePath
        mapping.basePath = basePath

        mapping
    }

}