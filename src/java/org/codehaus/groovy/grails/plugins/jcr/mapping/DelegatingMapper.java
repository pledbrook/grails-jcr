package org.codehaus.groovy.grails.plugins.jcr.mapping;

import org.apache.jackrabbit.ocm.mapper.impl.AbstractMapperImpl;
import org.codehaus.groovy.grails.commons.GrailsDomainClass;

import java.io.InputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.List;

/**
 * TODO: write javadoc
 *
 * @author Sergey Nebolsin (nebolsin@gmail.com)
 */
class DelegatingMapper extends AbstractMapperImpl {

    public DelegatingMapper() {
        this.descriptorReader = new DelegatingDescriptorReader();
    }

    public void registerAnnotatedClass(Class clazz) {
        ((DelegatingDescriptorReader)descriptorReader).registerAnnotatedClass(clazz);
    }

    public void registerDigestedClass(String fileName) {
        try {
            ((DelegatingDescriptorReader)descriptorReader).registerDigestedClass(new FileInputStream(fileName));
        }
        catch (FileNotFoundException e) {
            throw new GrailsJcrMappingException("Mapping file not found : ${fileName}", e);
        }
    }

    public void registerDigestedClass(List<String> fileNames) {
        for(String fileName : fileNames) {
            registerDigestedClass(fileName);
        }
    }

    public void registerDigestedClass(InputStream inputStream) {
        ((DelegatingDescriptorReader)descriptorReader).registerDigestedClass(inputStream);
    }

    public void registerGrailsDomainClass(GrailsDomainClass domainClass) {
        ((DelegatingDescriptorReader)descriptorReader).registerGrailsDomainClass(domainClass);
    }
}