package org.codehaus.groovy.grails.plugins.jcr.binding

import javax.jcr.Node
import javax.jcr.Value
import javax.jcr.Property
import javax.jcr.ValueFactory
import java.sql.Timestamp
import javax.jcr.RepositoryException
import org.codehaus.groovy.grails.web.servlet.mvc.GrailsWebRequest
import org.springframework.web.context.request.RequestContextHolder
import org.codehaus.groovy.grails.commons.GrailsDomainClass
import org.codehaus.groovy.grails.plugins.jcr.JcrConstants
import org.springframework.util.StringUtils
import org.codehaus.groovy.grails.commons.GrailsDomainClassProperty
import org.springframework.beans.BeanWrapper
import org.springframework.beans.PropertyAccessorFactory

/**
 * Created by IntelliJ IDEA.
 * User: nebolsin
 * Date: Jul 1, 2008
 * Time: 1:39:46 PM
 * To change this template use File | Settings | File Templates.
 */
class ExperimentalNodeBinder {

    GrailsDomainClass domainClass
    BeanWrapper target
    String namespace

    void setDomainClass(GrailsDomainClass domainClass) {
        this.domainClass = domainClass
        this.namespace = domainClass.getPropertyValue(JcrConstants.NAMESPACE_PROPERTY_NAME) ?: ""
        if(StringUtils.hasLength(this.namespace)) this.namespace += ":"
    }

    void setTarget(Object target) {
        this.target = PropertyAccessorFactory.forBeanPropertyAccess(target)
    }

    void bindFrom(Node node) {
        def deferredProperties = []
        filterBindableProperties(node).each { Property jcrProperty ->
            String propertyName = jcrProperty.getName() - namespace
            println "Working with $propertyName"
            GrailsDomainClassProperty grailsProperty = domainClass.getPropertyByName(propertyName)
            if(grailsProperty?.isPersistent()) {
                bindFromJcrProperty(jcrProperty, grailsProperty)
            } else {
                deferredProperties << jcrProperty
            }
            bindFromFreeJcrProperties(deferredProperties)
        }
    }

    void bindTo(Node node) {
        domainClass.persistantProperties.each { GrailsDomainClassProperty grailsProperty ->
            def type = target.getPropertyType(grailsProperty.name)
            def value = target.getPropertyValue(grailsProperty.name)
            def jcrPropertyName = "$namespace$grailsProperty.name"
            def jcrValue = null
            if(Collection.isAssignableFrom(type)) {

            } else if(type.isArray()) {

            } else if(Map.isAssignableFrom(type)) {
                jcrValue = bindMapToJcrProperty(node, jcrPropertyName, value)
            } else {
                jcrValue = createValue(value, node.getSession().getValueFactory())
            }
            node.setProperty(jcrPropertyName, jcrValue)
        }
    }

    private bindFromJcrProperty(Property jcrProperty, GrailsDomainClassProperty grailsProperty) {
        if(jcrProperty.getDefinition().isMultiple()) {
            // multi-valued property
        } else {
            target.setPropertyValue(grailsProperty.name, getValue(grailsProperty.getType(), jcrProperty.getValue()))
        }
    }

    private bindFromFreeJcrProperties(jcrProperties) {

    }

    private bindToJcrProperty(Node node, String propertyName, Object value) {

    }

    private bindMapToJcrProperty(Node node, String propertyName, Map value) {
        def mapNode = null
        try{
            mapNode = node.getNode(propertyName)
            // remove properties corresponded to keys which are not presented in provided Map
            filterBindableProperties(mapNode).findAll {!value.containsKey(getPlainName(it.name))}.each {Property jcrProperty ->
                if(jcrProperty.getDefinition().isMultiple()) {
                    jcrProperty.setValues(null)
                } else {
                    jcrProperty.setValue(null)
                }
            }
        } catch(javax.jcr.PathNotFoundException pnfe) {
            mapNode = node.addNode(propertyName)
        }

        value.each { key, value ->
            bindToJcrProperty(mapNode, getFullName(key), value)
        }
    }

    /**
     * Filters all JCR Node's properties and returns only "ours": those which start with the "$namespace:" prefix if
     * namespace is specified, or properties without namespace, if domain class doesn't specify namespace.
     */
    private filterBindableProperties(Node node) {
        node.getProperties().findAll {
            def result = StringUtils.hasLength(namespace) ? it?.name?.startsWith(namespace) : it?.name?.indexOf(":") < 0
            result && domainClass.hasProperty(it?.name)
        }
    }

    private String getFullName(String name) {
        if(name.indexOf(":") < 0) {
            return namespace + name
        }
        name
    }

    private String getPlaneName(String name) {
        def index = name.indexOf(":")
        if(name.indexOf(":") < 0) {
            return name
        } else {
            return name.substring(index + 1)
        }
    }

    /**
     * Converts gives object to appropriate JCR Value instance.
     */
    Value createValue(Object fieldValue, ValueFactory valueFactory) {
        Class sourceClass = fieldValue.getClass();
        if(sourceClass == String.class) {
            return valueFactory.createValue((String) fieldValue);
        } else if(Date.isAssignableFrom(sourceClass)) {
            return valueFactory.createValue(getLocalizedCalendarInstance(((Date) fieldValue).getTime()));
        } else if(Calendar.isAssignableFrom(sourceClass)) {
            return valueFactory.createValue((Calendar) fieldValue);
        } else if(sourceClass == InputStream.class) {
            return valueFactory.createValue((InputStream) fieldValue);
        } else if(sourceClass.isArray() && sourceClass.getComponentType() == byte.class) {
            return valueFactory.createValue(new ByteArrayInputStream((byte[]) fieldValue));
        } else if(sourceClass == Integer.class || sourceClass == int.class) {
            return valueFactory.createValue((Integer) fieldValue);
        } else if(sourceClass == Long.class || sourceClass == long.class) {
            return valueFactory.createValue((Long) fieldValue);
        } else if(sourceClass == Double.class || sourceClass == double.class) {
            return valueFactory.createValue((Double) fieldValue);
        } else if(sourceClass == Boolean.class || sourceClass == boolean.class) {
            return valueFactory.createValue((Boolean) fieldValue);
        } else if(sourceClass == Locale.class) {
            return valueFactory.createValue(String.valueOf((Locale) fieldValue));
        } else if(sourceClass.isEnum()) {
            return valueFactory.createValue(String.valueOf(fieldValue))
        } else {
            return valueFactory.createValue(String.valueOf(fieldValue))
        }
    }

    Object getValue(Class targetClass, Value value) throws RepositoryException, IOException {
        if(targetClass == String.class) {
            return value.getString();
        } else if(targetClass == Date.class) {
            return value.getDate().getTime();
        } else if(targetClass == Timestamp.class) {
            return new Timestamp(value.getDate().getTimeInMillis());
        } else if(targetClass == Calendar.class) {
            return value.getDate();
        } else if(targetClass == InputStream.class) {
            return value.getStream();
        } else if(targetClass.isArray() && targetClass.getComponentType() == byte.class) {
            // byte array...we need to read from the stream
            return readBytes(value.getStream());
        } else if(targetClass == Integer.class || targetClass == int.class) {
            return (int) value.getDouble();
        } else if(targetClass == Long.class || targetClass == long.class) {
            return value.getLong();
        } else if(targetClass == Double.class || targetClass == double.class) {
            return value.getDouble();
        } else if(targetClass == Boolean.class || targetClass == boolean.class) {
            return value.getBoolean();
        } else if(targetClass.isEnum()) {
            return Enum.valueOf(targetClass, value.getString());
        } else {
            return target.convertIfNecessary(value.getString(), targetClass)
        }
    }

    static byte[] readBytes(InputStream input) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            // Transfer bytes from in to out
            byte[] buf = new byte[1024];
            int len;
            while((len = input.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
        } finally {
            input.close();
            out.close();
        }
        return out.toByteArray();
    }

    static Calendar getLocalizedCalendarInstance(long millis) {
        GrailsWebRequest webRequest = (GrailsWebRequest) RequestContextHolder.getRequestAttributes();
        Calendar calendar = webRequest != null ? Calendar.getInstance(webRequest.getLocale()) : Calendar.getInstance();
        calendar.setTimeInMillis(millis);
        return calendar;
    }

}