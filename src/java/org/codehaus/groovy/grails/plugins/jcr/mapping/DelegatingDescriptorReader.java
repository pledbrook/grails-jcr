package org.codehaus.groovy.grails.plugins.jcr.mapping;

import org.apache.jackrabbit.ocm.mapper.impl.annotation.AnnotationDescriptorReader;
import org.apache.jackrabbit.ocm.mapper.impl.digester.DigesterDescriptorReader;
import org.apache.jackrabbit.ocm.mapper.model.MappingDescriptor;
import org.apache.jackrabbit.ocm.mapper.model.ClassDescriptor;
import org.apache.jackrabbit.ocm.mapper.DescriptorReader;
import org.codehaus.groovy.grails.commons.GrailsDomainClass;

import java.io.InputStream;
import java.util.*;

/**
 * TODO: write javadoc
 *
 * @author Sergey Nebolsin (nebolsin@gmail.com)
 */
class DelegatingDescriptorReader implements DescriptorReader {
    private List<Class> annotatedClasses = new ArrayList<Class>();
    private List<InputStream> xmlInputStreams = new ArrayList<InputStream>();
    private List<GrailsDomainClass> grailsDomainClasses = new ArrayList<GrailsDomainClass>();

    public MappingDescriptor loadClassDescriptors() {
        MappingDescriptor result = new MappingDescriptor();
        AnnotationDescriptorReader annotationDescriptorReader = new AnnotationDescriptorReader(annotatedClasses);
        MappingDescriptor descriptor = annotationDescriptorReader.loadClassDescriptors();
        for(ClassDescriptor classDescriptor : (Collection<ClassDescriptor>) descriptor.getAllClassDescriptors()) {
            result.addClassDescriptor(classDescriptor);
        }

        DigesterDescriptorReader digesterDescriptorReader = new DigesterDescriptorReader(xmlInputStreams.toArray(new InputStream[xmlInputStreams.size()]));
        descriptor = digesterDescriptorReader.loadClassDescriptors();
        for(ClassDescriptor classDescriptor : (Collection<ClassDescriptor>) descriptor.getAllClassDescriptors()) {
            result.addClassDescriptor(classDescriptor);
        }

        GrailsDescriptorReader grailsDescriptorReader = new GrailsDescriptorReader(grailsDomainClasses);
        descriptor = grailsDescriptorReader.loadClassDescriptors();
        for(ClassDescriptor classDescriptor : (Collection<ClassDescriptor>) descriptor.getAllClassDescriptors()) {
            result.addClassDescriptor(classDescriptor);
        }

        return result;
    }

    public void registerDigestedClass(InputStream inputStream) {
        xmlInputStreams.add(inputStream);
    }

    public void registerAnnotatedClass(Class annotatedClass) {
        annotatedClasses.add(annotatedClass);
    }

    public void registerGrailsDomainClass(GrailsDomainClass domainClass) {
        grailsDomainClasses.add(domainClass);
    }
}
