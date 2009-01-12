package org.codehaus.groovy.grails.plugins.jcr.mapping.config

import org.codehaus.groovy.grails.commons.GrailsDomainClass
import org.codehaus.groovy.grails.commons.GrailsDomainClassProperty

/**
 * TODO: write javadoc
 *
 * @author Sergey Nebolsin (nebolsin@gmail.com)
 */

public class ClassMapping {

  GrailsDomainClass domainClass

  String jcrType = "nt:unstructured"
  List jcrMixinTypes = ["mix:referenceable", "mix:versionable", "mix:lockable"]
  boolean discriminator = true

  String className
  boolean isInterface

  Map<String, PropertyMapping> propertyMappings = [:]


  ClassMapping(GrailsDomainClass domainClass) {
    this.domainClass = domainClass
    this.className = domainClass.clazz.name
    this.isInterface = domainClass.clazz.isInterface()

    def persistentProperties = domainClass.persistentProperties.toList() + domainClass.identifier

    for(GrailsDomainClassProperty property : persistentProperties) {
      propertyMappings[property.name] = new PropertyMapping(property)
    }
  }


  boolean isInterface() {
    isInterface
  }

  String getJcrMixinTypes() {
    jcrMixinTypes.join(', ')
  }

  PropertyMapping getPropertyMapping(String name) {
    propertyMappings[name]
  }

  Collection<PropertyMapping> getPropertyMappings() {
    propertyMappings.values()
  }
}