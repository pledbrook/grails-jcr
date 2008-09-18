package org.codehaus.groovy.grails.plugins.jcr.mapping;

import org.apache.jackrabbit.ocm.mapper.DescriptorReader;
import org.apache.jackrabbit.ocm.mapper.model.MappingDescriptor;

import java.util.List;

/**
 * TODO: write javadoc
 *
 * @author Sergey Nebolsin (nebolsin@gmail.com)
 */
public class GrailsDescriptorReader implements DescriptorReader {

    public GrailsDescriptorReader(List<Class> classes) {
    }

    public MappingDescriptor loadClassDescriptors() {
        MappingDescriptor descriptor = new MappingDescriptor();
        return descriptor;
    }
}
