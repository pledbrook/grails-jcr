package org.codehaus.groovy.grails.plugins.jcr.mapping.config

import org.apache.commons.logging.LogFactory
import org.codehaus.groovy.grails.commons.GrailsDomainClass

/**
 * TODO: write javadoc
 *
 * @author Sergey Nebolsin (nebolsin@gmail.com)
 */
public class JcrMappingBuilder {
  static final LOG = LogFactory.getLog(JcrMappingBuilder.class)

  ClassMapping mapping
  GrailsDomainClass clazz

  JcrMappingBuilder(GrailsDomainClass clazz) {
    this.clazz = clazz
  }

  ClassMapping evaluate(ClassMapping defaultMapping, Closure mappingClosure) {
      mapping = defaultMapping
      mappingClosure.resolveStrategy = Closure.DELEGATE_ONLY
      mappingClosure.delegate = this
      mappingClosure.call()
      mapping
  }
}
