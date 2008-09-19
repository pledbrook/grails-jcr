package org.codehaus.groovy.grails.plugins.jcr.mapping;

import org.apache.jackrabbit.ocm.mapper.Mapper;
import org.apache.jackrabbit.ocm.mapper.impl.annotation.Node;
import org.codehaus.groovy.grails.commons.GrailsApplication;
import org.codehaus.groovy.grails.commons.ApplicationHolder;
import org.codehaus.groovy.grails.commons.GrailsDomainClass;
import org.codehaus.groovy.grails.commons.DomainClassArtefactHandler;

import java.io.InputStream;
import java.util.List;

/**
 * TODO: write javadoc
 *
 * @author Sergey Nebolsin (nebolsin@gmail.com)
 */
public class MapperConfigurator {
    static Mapper configureMapper(List<Class> classes) {
        DelegatingMapper mapper = new DelegatingMapper();
        GrailsApplication application = ApplicationHolder.getApplication();
        for(Class clazz : classes) {
            if(clazz.getAnnotation(Node.class) != null) {
                mapper.registerAnnotatedClass(clazz);
            } else {
                InputStream xmlStream = MapperConfigurator.class.getClassLoader().getResourceAsStream("jcrmapping-${ClassUtils.getQualifiedName(clazz)}.xml");
                if(xmlStream != null) {
                    mapper.registerDigestedClass(xmlStream);
                } else {
                    if(application != null && application.isArtefactOfType(DomainClassArtefactHandler.TYPE, clazz)) {
                        GrailsDomainClass domainClass = (GrailsDomainClass) application.getArtefact(DomainClassArtefactHandler.TYPE, clazz.getName());
                        mapper.registerGrailsDomainClass(domainClass);
                    }
                }
            }
        }
        mapper.buildMapper();
        return mapper;
    }
}