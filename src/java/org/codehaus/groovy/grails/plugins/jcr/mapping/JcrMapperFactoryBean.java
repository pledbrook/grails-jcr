package org.codehaus.groovy.grails.plugins.jcr.mapping;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.apache.jackrabbit.ocm.mapper.Mapper;

import java.util.List;

/**
 * TODO: write javadoc
 *
 * @author Sergey Nebolsin (nebolsin@gmail.com)
 */
class JcrMapperFactoryBean implements FactoryBean, InitializingBean {
    List<Class> mappedClasses;
    Mapper mapper;

    public void setMappedClasses(List<Class> mappedClasses) {
        this.mappedClasses = mappedClasses;
    }

    public Object getObject() {
        return mapper;
    }

    public Class getObjectType() {
        return Mapper.class;
    }

    public boolean isSingleton() {
        return true;
    }

    public void afterPropertiesSet() {
        mapper = MapperConfigurator.configureMapper(mappedClasses);
    }
}