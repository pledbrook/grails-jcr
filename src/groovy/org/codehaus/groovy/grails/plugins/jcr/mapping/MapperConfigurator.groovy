package org.codehaus.groovy.grails.plugins.jcr.mapping

import org.apache.jackrabbit.ocm.mapper.Mapper
import org.apache.jackrabbit.ocm.mapper.impl.annotation.Node
import org.springframework.util.ClassUtils

/**
 * TODO: write javadoc
 *
 * @author Sergey Nebolsin (nebolsin@gmail.com)
 */
class MapperConfigurator {
    static Mapper configureMapper(classes) {
        def mapper = new DelegatingMapper()
        classes?.each { Class clazz ->
            println "Configuring mapping for class: ${clazz.name}"
            if(clazz.getAnnotation(Node)) {
                mapper.registerAnnotatedClass clazz
            } else {
                InputStream xmlStream = MapperConfigurator.getClassLoader().getResourceAsStream("jcrmapping-${ClassUtils.getQualifiedName(clazz)}.xml")
                if(xmlStream) {
                    mapper.registerDigestedClass xmlStream
                } else {
                    // Conventional configuration for Grails Domain Classes
                }
            }
        }
        mapper.buildMapper()
        return mapper
    }
}