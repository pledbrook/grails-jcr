package org.codehaus.groovy.grails.plugins.jcr.mapping.config

import org.codehaus.groovy.grails.commons.GrailsDomainClassProperty

/**
 * TODO: write javadoc
 *
 * @author Sergey Nebolsin (nebolsin@gmail.com)
 */

public class PropertyMapping {
  GrailsDomainClassProperty domainClassProperty

  String fieldName
  String jcrName

  boolean identity
  boolean path
  boolean UUID
  
  PropertyMapping(GrailsDomainClassProperty domainClassProperty) {
    this.domainClassProperty = domainClassProperty

    this.fieldName = domainClassProperty.name
    this.jcrName = domainClassProperty.name

    this.identity = domainClassProperty.isIdentity()
    this.path = ("path" == domainClassProperty.name)
    this.UUID = ("UUID" == domainClassProperty.name)

  }
}