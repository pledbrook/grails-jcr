package org.codehaus.groovy.grails.plugins.jcr.mapping;

import org.apache.jackrabbit.ocm.mapper.DescriptorReader;
import org.apache.jackrabbit.ocm.mapper.model.*;
import org.codehaus.groovy.grails.commons.*;
import org.codehaus.groovy.grails.plugins.jcr.mapping.config.ClassMapping;
import org.codehaus.groovy.grails.plugins.jcr.mapping.config.JcrMappingBuilder;
import org.codehaus.groovy.grails.plugins.jcr.mapping.config.PropertyMapping;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

import groovy.lang.Closure;

/**
 * TODO: write javadoc
 *
 * @author Sergey Nebolsin (nebolsin@gmail.com)
 */
public class GrailsDescriptorReader implements DescriptorReader {
  private static final Map<String, ClassDescriptor> CLASS_DESCRIPTORS = new HashMap<String, ClassDescriptor>();

  private List<GrailsDomainClass> domainClasses = new ArrayList<GrailsDomainClass>();

  public GrailsDescriptorReader(List<GrailsDomainClass> domainClasses) {
    this.domainClasses.addAll(domainClasses);
  }

  public MappingDescriptor loadClassDescriptors() {
    for(GrailsDomainClass domainClass : domainClasses) {
      CLASS_DESCRIPTORS.put(domainClass.getFullName(), new ClassDescriptor());
    }
    MappingDescriptor descriptor = new MappingDescriptor();
    for(GrailsDomainClass domainClass : domainClasses) {
      ClassMapping mapping = evaluateMapping(domainClass);
      descriptor.addClassDescriptor(configureClassDescriptor(mapping));
    }
    return descriptor;
  }

  private ClassMapping evaluateMapping(GrailsDomainClass domainClass) {
    ClassMapping mapping = new ClassMapping(domainClass);
    Object mappingClosure = GrailsClassUtils.getStaticPropertyValue(domainClass.getClazz(), GrailsDomainClassProperty.MAPPING);
    if (mappingClosure instanceof Closure) {
        JcrMappingBuilder builder = new JcrMappingBuilder(domainClass);
        builder.evaluate(mapping, (Closure) mappingClosure);
    }
    return mapping;
  }

  private ClassDescriptor configureClassDescriptor(ClassMapping mapping) {
    ClassDescriptor descriptor = CLASS_DESCRIPTORS.get(mapping.getDomainClass().getFullName());

    descriptor.setClassName(mapping.getClassName());
    descriptor.setJcrType(mapping.getJcrType());
    descriptor.setJcrMixinTypes(mapping.getJcrMixinTypes());
    descriptor.setDiscriminator(mapping.isDiscriminator());
    descriptor.setInterface(mapping.isInterface());


    for(PropertyMapping propertyMapping : mapping.getPropertyMappings()) {
      descriptor.addFieldDescriptor(configureFieldDescriptor(propertyMapping));
    }

    return descriptor;
  }

  private FieldDescriptor configureFieldDescriptor(PropertyMapping propertyMapping) {
    FieldDescriptor fieldDescriptor = new FieldDescriptor();
    fieldDescriptor.setFieldName(propertyMapping.getFieldName());
    fieldDescriptor.setJcrName(propertyMapping.getJcrName());

    fieldDescriptor.setId(propertyMapping.isIdentity());
    fieldDescriptor.setPath(propertyMapping.isPath());
    fieldDescriptor.setUuid(propertyMapping.isUUID());

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
