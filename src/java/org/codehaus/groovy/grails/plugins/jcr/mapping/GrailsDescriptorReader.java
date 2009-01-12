package org.codehaus.groovy.grails.plugins.jcr.mapping;

import org.apache.jackrabbit.ocm.mapper.DescriptorReader;
import org.apache.jackrabbit.ocm.mapper.model.*;
import org.codehaus.groovy.grails.commons.*;

import java.util.List;
import java.util.ArrayList;

/**
 * TODO: write javadoc
 *
 * @author Sergey Nebolsin (nebolsin@gmail.com)
 */
public class GrailsDescriptorReader implements DescriptorReader {
    List<GrailsDomainClass> domainClasses = new ArrayList<GrailsDomainClass>();

    public GrailsDescriptorReader(List<GrailsDomainClass> domainClasses) {
        this.domainClasses.addAll(domainClasses);
    }

    public MappingDescriptor loadClassDescriptors() {
        MappingDescriptor descriptor = new MappingDescriptor();
        for(GrailsDomainClass domainClass : domainClasses) {
            descriptor.addClassDescriptor(configureClassDescriptor(domainClass));
        }
        return descriptor;
    }

    private ClassDescriptor configureClassDescriptor(GrailsDomainClass domainClass) {
        ClassDescriptor descriptor = new ClassDescriptor();
        descriptor.setClassName(domainClass.getClazz().getName());
        descriptor.setJcrType("nt:unstructured");
        descriptor.setJcrMixinTypes("mix:referenceable, mix:versionable, mix:lockable");
        descriptor.setDiscriminator(true);
        descriptor.setInterface(domainClass.getClazz().isInterface());

        descriptor.addFieldDescriptor(configureFieldDescriptor(domainClass.getIdentifier()));
        for(GrailsDomainClassProperty property : domainClass.getPersistentProperties()) {
            if(property.isAssociation()) {

            } else {
                descriptor.addFieldDescriptor(configureFieldDescriptor(property));
            }
        }

        return descriptor;
    }

    private FieldDescriptor configureFieldDescriptor(GrailsDomainClassProperty property) {
        FieldDescriptor fieldDescriptor = new FieldDescriptor();
        fieldDescriptor.setFieldName(property.getName());
        fieldDescriptor.setJcrName(property.getName());

        fieldDescriptor.setId(property.isIdentity());
        fieldDescriptor.setPath("path".equals(property.getName()));
        fieldDescriptor.setUuid("UUID".equals(property.getName()));

        fieldDescriptor.setJcrAutoCreated(false);
        fieldDescriptor.setJcrMandatory(false);
        fieldDescriptor.setJcrProtected(false);

        return fieldDescriptor;
    }

    private BeanDescriptor configureBeanDescriptor(GrailsDomainClassProperty property) {
        BeanDescriptor beanDescriptor = new BeanDescriptor();
        beanDescriptor.setJcrName(property.getName());
        beanDescriptor.setJcrMandatory(false);
        beanDescriptor.setJcrAutoCreated(false);
        return beanDescriptor;
    }

    private CollectionDescriptor configureCollectionDescriptor(GrailsDomainClassProperty property) {
        CollectionDescriptor collectionDescriptor = new CollectionDescriptor();
        return collectionDescriptor;
    }
}
