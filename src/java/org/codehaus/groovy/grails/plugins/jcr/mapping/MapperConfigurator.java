package org.codehaus.groovy.grails.plugins.jcr.mapping;

import org.apache.jackrabbit.ocm.mapper.Mapper;
import org.apache.jackrabbit.ocm.mapper.impl.annotation.Node;

import java.io.InputStream;
import java.util.List;

/**
 * TODO: write javadoc
 *
 * @author Sergey Nebolsin (nebolsin@gmail.com)
 */
class MapperConfigurator {
    static Mapper configureMapper(List<Class> classes) {
        DelegatingMapper mapper = new DelegatingMapper();
        for(Class clazz : classes) {
            if(clazz.getAnnotation(Node.class) != null) {
                mapper.registerAnnotatedClass(clazz);
            } else {
                InputStream xmlStream = MapperConfigurator.class.getClassLoader().getResourceAsStream("jcrmapping-${ClassUtils.getQualifiedName(clazz)}.xml");
                if(xmlStream != null) {
                    mapper.registerDigestedClass(xmlStream);
                } else {
                    // Conventional configuration for Grails Domain Classes
                }
            }
        }
        mapper.buildMapper();
        return mapper;
    }
}