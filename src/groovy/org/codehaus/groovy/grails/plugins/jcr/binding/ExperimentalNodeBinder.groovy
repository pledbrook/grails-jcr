package org.codehaus.groovy.grails.plugins.jcr.binding

import javax.jcr.Node
import javax.jcr.Value
import org.codehaus.groovy.grails.plugins.jcr.JcrConstants
import org.springframework.util.ClassUtils
import javax.jcr.Property

/**
 * Universal binder for binding Object (possibly Grails Domain class) to JCR Node
 * and vise versa.
 *
 * @author Sergey Nebolsin (nebolsin@gmail.com)
 */
class ExperimentalNodeBinder {

    ClassLoader classLoader = System.getClassLoader()
    BindingContext context = new BindingContext()

    public ExperimentalNodeBinder() {}

    public ExperimentalNodeBinder(ClassLoader classLoader) {
        this.classLoader = classLoader
    }

    public void bindToNode(Node node, Object source) {
        bindToJcrNode(node, source)
    }

    public Object bindFromNode(Node node) {
        bindFromJcrNode(node)
    }


    // ==========================================
    //     Object -> Node
    // ==========================================

    private bindToJcrNode(Node node, Object source) {
        println "Binding $source to node: ${node.getPath()}"
        context.push source, node

        def type = source.getClass()
        node.setProperty(JcrConstants.CLASS_PROPERTY_NAME, ClassUtils.getQualifiedName(type))
        if(context.collection) {
            bindCollectionToJcrNode(node, (Collection) source)
        } else if(context.map) {
            bindMapToJcrNode(node, (Map) source)
        } else {
            println context.propertyValues
            context.propertyValues.each { propertyName, propertyValue ->
                bindToJcrProperty(node, propertyName, propertyValue)
            }
        }

        context.pop
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
            context.setNodePropertyValue(node, jcrPropertyName, value)
        }
    }


    // ==========================================
    //     Collection -> Node
    // ==========================================

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
            result << JcrHelper.createValue(value, node.getSession().getValueFactory())
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

    private bindCollectionToJcrNode(Node collectionNode, Collection source) {
        collectionNode.getNodes().each { Node value -> value.remove() }

        source.eachWithIndex { value, index ->
            Node valueNode = collectionNode.addNode(index.toString())
            bindToJcrProperty(valueNode, JcrConstants.COLLECTION_VALUE_PROPERTY_NAME, value)
        }
    }

    // ==========================================
    //     Map -> Node
    // ==========================================

    private bindMapToJcrProperty(Node node, String jcrPropertyName, Map value) {
        Node mapNode
        if(node.hasNode(jcrPropertyName)) {
            mapNode = node.getNode(jcrPropertyName)
        } else {
            mapNode = node.addNode(jcrPropertyName)
            mapNode.addMixin(JcrConstants.MIXIN_REFERENCEABLE)
        }

        bindToJcrNode(mapNode, value)

        node.setProperty(jcrPropertyName, mapNode)
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



    // ==========================================
    //     Node -> Object
    // ==========================================


    private Object bindFromJcrNode(Node node) {
        if(!node.hasProperty(JcrConstants.CLASS_PROPERTY_NAME)) {
            throw new GrailsBindingException("Node doesn't contain class information, cannot bind from this node")
        } else {
            String className = node.getProperty(JcrConstants.CLASS_PROPERTY_NAME).getString()
            Class targetClass = classLoader.loadClass(className)
            println "Loaded target class: $targetClass.name"
            Object target = targetClass.newInstance()
            context.push target, node

            context.propertyValues.findAll{k, v -> node.hasProperty(k)}.each { propertyName, propertyValue ->
                bindFromJcrProperty(node.getProperty(propertyName), target)
            }
            return target
        }
    }

    private bindFromJcrProperty(Property jcrProperty, Object target) {
        println "Binding $jcrProperty"
        if(jcrProperty.getDefinition().isMultiple()) {
            // multi-valued property
        } else {
            context.setObjectProperty jcrProperty.name, context.getNodePropertyValue(jcrProperty.getParent(), jcrProperty.getName(), context.getObjectPropertyType(jcrProperty.name))
        }
    }
}