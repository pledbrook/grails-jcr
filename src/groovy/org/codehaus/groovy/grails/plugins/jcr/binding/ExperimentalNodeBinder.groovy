package org.codehaus.groovy.grails.plugins.jcr.binding

import javax.jcr.Node
import javax.jcr.Value
import javax.jcr.ValueFactory
import java.sql.Timestamp
import javax.jcr.RepositoryException
import org.codehaus.groovy.grails.web.servlet.mvc.GrailsWebRequest
import org.springframework.web.context.request.RequestContextHolder
import org.codehaus.groovy.grails.plugins.jcr.JcrConstants
import org.springframework.util.ClassUtils
import org.codehaus.groovy.grails.commons.ApplicationHolder

/**
 * Universal binder for binding Object (possibly Grails Domain class) to JCR Node
 * and vise versa.
 *
 * @author Sergey Nebolsin (nebolsin@gmail.com)
 */
class ExperimentalNodeBinder {

    BindingContext context = new BindingContext()

    public void bindToNode(Node node, Object source) {
        bindToJcrNode(node, source)
    }

    private bindToJcrNode(Node node, Object source) {
        println "Binding $source to node: ${node.getPath()}"
        context.push source, node

        def type = source.getClass()
        node.setProperty(JcrConstants.CLASS_PROPERTY_NAME, ClassUtils.getQualifiedName(type))
        if(context.collection) {
            bindCollectionToJcrNode(node, (Collection) source)
        } else if(context.map) {
            bindMapToJcrNode(node, (Map) source, context)
        } else {
            println context.propertyValues
            context.propertyValues.each { propertyName, propertyValue ->
                bindToJcrProperty(node, propertyName, propertyValue)
            }
        }

        context.pop
    }

    private bindCollectionToJcrNode(Node collectionNode, Collection source) {
        collectionNode.getNodes().each { Node value -> value.remove() }

        source.eachWithIndex { value, index ->
            Node valueNode = collectionNode.addNode(index.toString())
            bindToJcrProperty(valueNode, JcrConstants.COLLECTION_VALUE_PROPERTY_NAME, value)
        }
    }

    private bindMapToJcrNode(Node mapNode, Map source) {
        mapNode.getNodes().each { Node value ->
            if(!source.containsKey(value.getName())) value.remove()
        }

        source.each { key, value ->
            Node valueNode = mapNode.addNode(key)
            bindToJcrProperty(valueNode, JcrConstants.MAP_KEY_PROPERTY_NAME, key)
            bindToJcrProperty(valueNode, JcrConstants.MAP_VALUE_PROPERTY_NAME, value)
        }
    }

    private bindToJcrProperty(Node node, String jcrPropertyName, Object value) {
        def type = value.getClass()
        if(Collection.isAssignableFrom(type)) {
            bindCollectionToJcrProperty(node, jcrPropertyName, (Collection) value)
        } else if(type.isArray()) {
            bindCollectionToJcrProperty(node, jcrPropertyName, value.toList())
        } else if(Map.isAssignableFrom(type)) {
            bindMapToJcrProperty(node, jcrPropertyName, (Map) value)
        } else {
            node.setProperty(jcrPropertyName, createValue(value, node.getSession().getValueFactory()))
        }
    }

    private bindCollectionToJcrProperty(Node node, String jcrPropertyName, Collection values) {
        def result = []
        def type = null
        def expandCollection = false
        values.each { value ->
            if(!type) {
                type = value.getClass()
            } else {
                if(type != value.getClass()) expandCollection = true
            }
            result << createValue(value, node.getSession().getValueFactory())
        }

        if(!expandCollection) {
            node.setProperty(jcrPropertyName, result as Value[])
        } else {
            Node collectionNode
            if(node.hasNode(jcrPropertyName)) {
                collectionNode = node.getNode(jcrPropertyName)
            } else {
                collectionNode = node.addNode(jcrPropertyName)
                collectionNode.addMixin(JcrConstants.MIXIN_REFERENCEABLE)
            }

            bindCollectionToJcrNode(collectionNode, values)
            node.setProperty(jcrPropertyName, collectionNode)
        }
    }

    private bindMapToJcrProperty(Node node, String jcrPropertyName, Map value) {
        Node mapNode
        if(node.hasNode(jcrPropertyName)) {
            mapNode = node.getNode(jcrPropertyName)
        } else {
            mapNode = node.addNode(jcrPropertyName)
            mapNode.addMixin(JcrConstants.MIXIN_REFERENCEABLE)
        }

        bindMapToJcrNode(mapNode, value)
        node.setProperty(jcrPropertyName, mapNode)
    }




//    void bindFrom(Node node) {
//        def deferredProperties = []
//        filterBindableProperties(node).each { Property jcrProperty ->
//            String propertyName = jcrProperty.getName() - namespace
//            GrailsDomainClassProperty grailsProperty = domainClass.getPropertyByName(propertyName)
//            if(grailsProperty?.isPersistent()) {
//                bindFromJcrProperty(jcrProperty, grailsProperty)
//            } else {
//                deferredProperties << jcrProperty
//            }
//            bindFromFreeJcrProperties(deferredProperties)
//        }
//    }
//
//    private bindFromJcrProperty(Property jcrProperty, GrailsDomainClassProperty grailsProperty) {
//        if(jcrProperty.getDefinition().isMultiple()) {
//            // multi-valued property
//        } else {
//            target.setPropertyValue(grailsProperty.name, getValue(grailsProperty.getType(), jcrProperty.getValue()))
//        }
//    }
//
//    private retrieveValue(Property jcrProperty, Class targetClass) {
//        if(Map.isAssignableFrom(targetClass)) {
//            return retrieveMapValue(jcrProperty)
//        } else if(Collection.isAssignableFrom(targetClass)) {
//            return retrieveCollectionValue(jcrProperty)
//        } else if(targetClass.isArray()) {
//            return retrieveCollectionValue(jcrProperty).toArray()
//        } else {
//            return getValue(targetClass, jcrProperty.getValue())
//        }
//    }
//
//    private retrieveMapValue(Property jcrProperty) {
//        Node mapNode = jcrProperty.getNode()
//        def result = []
//        filterBindableProperties(mapNode).each { Property jcrProp ->
//            result[getPlaneName(jcrProp.name)] = retrieveValue(jcrProp)
//
//        }
//    }
//
//    private retrieveCollectionValue(Property jcrProperty) {
//
//    }
//
//    private bindFromFreeJcrProperties(jcrProperties) {
//
//    }
//
//
//    /**
//     * Filters all JCR Node's properties and returns only "ours": those which start with the "$namespace:" prefix if
//     * namespace is specified, or properties without namespace, if domain class doesn't specify namespace.
//     */
//    private filterBindableProperties(Node node) {
//        node.getProperties().findAll {
//            def result = StringUtils.hasLength(namespace) ? it?.name?.startsWith(namespace) : it?.name?.indexOf(":") < 0
//            result && domainClass.hasProperty(it?.name)
//        }
//    }

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
//            return target.convertIfNecessary(value.getString(), targetClass)
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