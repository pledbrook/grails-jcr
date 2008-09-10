package org.codehaus.groovy.grails.plugins.jcr.binding.binders

import java.sql.Time
import java.sql.Timestamp
import org.codehaus.groovy.grails.plugins.jcr.binding.BindingContext
import javax.jcr.Property
import javax.jcr.Node
import org.codehaus.groovy.grails.plugins.jcr.binding.GrailsBindingException
import org.codehaus.groovy.grails.plugins.jcr.binding.JcrHelper
import javax.jcr.Value

/**
 * TODO: write javadoc
 *
 * @author Sergey Nebolsin (nebolsin@gmail.com)
 */
class SimpleBinder extends Binder {

    static final SIMPLE_TYPES = [
            String,
            boolean, Boolean,
            int, Integer,
            long, Long,
            float, Float,
            double, Double,
            Date, Time, Timestamp, Calendar,
            URI, URL, Locale,
            InputStream
    ]

    def SimpleBinder(BindingContext context) {
        super(context)
    }
    
    def supports(Class type) {
        return SIMPLE_TYPES.contains(type)
    }

    def bindToNode(Node node) {
        throw new GrailsBindingException("Binding of simple types to nodes is currently not supported")
    }

    def bindToProperty(Node node, String propertyName, value) {
        node.setProperty(context.resolveJcrPropertyName(propertyName), JcrHelper.createValue(value, node.getSession().getValueFactory()))
    }

    def bindFromNode(Node node) {
        throw new GrailsBindingException("Binding of simple types to nodes is currently not supported")
    }

    def bindFromProperty(Node node, String propertyName) {
        Value value = node.getProperty(context.resolveJcrPropertyName(propertyName)).getValue()
        Class requiredType = context.getObjectPropertyType(propertyName)
        context.setObjectProperty propertyName, JcrHelper.getOptimalValue(value, requiredType)
    }
}