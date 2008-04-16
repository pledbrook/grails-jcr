/* Copyright 2004-2005 Graeme Rocher
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.codehaus.groovy.grails.jcr.binding;

import org.codehaus.groovy.grails.commons.GrailsClassUtils;
import org.codehaus.groovy.grails.commons.ApplicationHolder;
import org.codehaus.groovy.grails.commons.GrailsApplication;
import org.codehaus.groovy.grails.commons.DomainClassArtefactHandler;
import org.codehaus.groovy.grails.commons.metaclass.CreateDynamicMethod;
import org.codehaus.groovy.grails.exceptions.GrailsRuntimeException;
import org.codehaus.groovy.grails.web.binding.CurrencyEditor;
import org.codehaus.groovy.grails.web.binding.GrailsDataBinder;
import org.codehaus.groovy.grails.web.binding.TimeZoneEditor;
import org.codehaus.groovy.grails.web.binding.UriEditor;
import org.codehaus.groovy.grails.web.servlet.mvc.GrailsWebRequest;
import org.codehaus.groovy.grails.jcr.exceptions.GrailsRepositoryException;
import org.codehaus.groovy.runtime.InvokerHelper;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.PropertyValue;
import org.springframework.beans.propertyeditors.LocaleEditor;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.multipart.support.ByteArrayMultipartFileEditor;
import org.springframework.web.multipart.support.StringMultipartFileEditor;
import org.apache.commons.lang.StringUtils;

import javax.jcr.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URI;
import java.util.*;

import groovy.lang.MetaClass;
import groovy.lang.GroovyObject;

/**
 * A DataBinder that handles binding JCR Node instances to and from Groovy or Java objects
 *
 * @author Graeme Rocher
 * @since 0.4
 *
 *        <p/>
 *        Created: Feb 9, 2007
 *        Time: 5:45:29 PM
 */
public class NodeBinder extends GrailsDataBinder {
    private String namespace = "";

    /**
     * Create a new NodeBinder instance.
     *
     * @param target     target object to bind onto
     * @param objectName objectName of the target object
     */
    public NodeBinder(Object target, String objectName) {
        super(target, objectName);
    }

    /**
     * Create a new NodeBinder instance.
     *
     * @param target     target object to bind onto
     * @param objectName objectName of the target object
     * @param namespace The namespace of the target object within the repository
     */
    public NodeBinder(Object target, String objectName, String namespace) {
        super(target, objectName);
        if(!StringUtils.isBlank(namespace))
            this.namespace = namespace + ":";
    }

    /**
     * Utility method for creating a GrailsDataBinder instance
     *
     * @param target The target object to bind to
     * @param objectName The name of the object
     * @return A GrailsDataBinder instance
     */
    public static NodeBinder createNodeBinder(Object target, String objectName) {
        NodeBinder binder = new NodeBinder(target,objectName);
        registerPropertyEditors(binder);
		return binder;
    }

    private static void registerPropertyEditors(NodeBinder binder) {
        binder.registerCustomEditor( byte[].class, new ByteArrayMultipartFileEditor());
        binder.registerCustomEditor( String.class, new StringMultipartFileEditor());
        binder.registerCustomEditor( Currency.class, new CurrencyEditor());
        binder.registerCustomEditor( Locale.class, new LocaleEditor());
        binder.registerCustomEditor( TimeZone.class, new TimeZoneEditor());
        binder.registerCustomEditor( URI.class, new UriEditor());
    }

    public static NodeBinder createNodeBinder(Object target, String objectName, String namespace) {
        NodeBinder binder = new NodeBinder(target,objectName, namespace);
        registerPropertyEditors(binder);
		return binder;

    }



    protected void doBind(MutablePropertyValues mpvs) {
        Object target = getTarget();
        if(target instanceof Node) {
           Node node = (Node)target;

           PropertyValue[] propertyValues = mpvs.getPropertyValues();

            for (int i = 0; i < propertyValues.length; i++) {
                PropertyValue propertyValue = propertyValues[i];
                if(isAllowed(propertyValue.getName())) {
                    try {
                        bindAllowedValue(node, propertyValue);
                    } catch (RepositoryException e) {
                        throw new GrailsRepositoryException(e.getMessage(),e);
                    }
                }
            }
        }
        else {
            super.doBind(mpvs);
        }        
    }

    private void bindAllowedValue(Node node, PropertyValue propertyValue) throws RepositoryException {
        String propertyName = namespace+propertyValue.getName();
        Object value = propertyValue.getValue();
        GrailsApplication application = ApplicationHolder.getApplication();
        if(value != null) {
            Class type = value.getClass();
            if(String.class.isAssignableFrom(type)) {
               node.setProperty(propertyName,String.valueOf(value)); 
            }
            else if(GrailsClassUtils.isAssignableOrConvertibleFrom(Number.class, type)) {
               if(GrailsClassUtils.isAssignableOrConvertibleFrom(Double.class, type) || GrailsClassUtils.isAssignableOrConvertibleFrom(Float.class, type)) {
                   node.setProperty(propertyName, Double.valueOf(String.valueOf(value)).doubleValue());
               }
               else if(BigInteger.class.isAssignableFrom(type) || BigDecimal.class.isAssignableFrom(type)) {
                   node.setProperty(propertyName, String.valueOf(value));
               }
               else {
                   node.setProperty(propertyName, Long.valueOf(value.toString()).longValue());
               }
            }
            else if(Date.class.isAssignableFrom(type)) {
                GrailsWebRequest webRequest = (GrailsWebRequest) RequestContextHolder.getRequestAttributes();

                Calendar c;
                if(webRequest != null) {
                    c = Calendar.getInstance(webRequest.getLocale());
                }
                else {
                    c = Calendar.getInstance();
                }
                c.setTime((Date)value);
                node.setProperty(propertyName, c);
            }
            else if(Calendar.class.isAssignableFrom(type)) {
                node.setProperty(propertyName,(Calendar)value);
            }
            else if(application.isArtefactOfType(DomainClassArtefactHandler.TYPE,type)) {
                if(value != null) {
                    String uuid = (String)GrailsClassUtils.getPropertyOrStaticPropertyOrFieldValue(value, propertyName);
                    MetaClass mc = InvokerHelper
                            .getInstance()
                                        .getMetaRegistry()
                                        .getMetaClass(type);

                    Node n = (Node)mc.invokeStaticMethod(value, "getNode", new Object[]{uuid});
                    if(n!=null) {
                        node.setProperty(propertyName,n);
                    }

                }

            }
            else {
                node.setProperty(propertyName, String.valueOf(value));
            }
        }
    }

    /**
     * Bind another object onto this object (or potential Node)
     *
     * @param values The object to bind onto this object
     */
    public void bind(Map values) {
        doBind(new MutablePropertyValues(values));
    }


    /**
     * Binds the values from a JCR Node instance onto the properties of the target object.
     *
     * @param node The node instance
     */
    public void bind(Node node) {
        MutablePropertyValues mpvs = new MutablePropertyValues();

        try {
            for (PropertyIterator i = node.getProperties(); i.hasNext();) {
                Property p = i.nextProperty();

                String propertyName = p.getName();
                if(propertyName.startsWith(namespace) || StringUtils.isBlank(namespace)) {
                    propertyName = propertyName.substring(namespace.length());
                    if(bean.isWritableProperty(propertyName)) {
                        Class type = bean.getPropertyType(propertyName);

                        Object value = retrieveOptimalValue(type, p);
                        if(value != null) {
                            mpvs.addPropertyValue(propertyName, value);
                        }

                    }

                }
            }
            super.autoCreateIfPossible(mpvs);
            super.bind(mpvs);
        } catch (RepositoryException e) {
            throw new GrailsRuntimeException("Repository exception thrown whilst performing data binding: " + e.getMessage(),e);
        }

    }

    /**
     * This method will retrieve the optimal value for the target type to bind to and the given JCR
     * property instance.
     *
     * @param type The target type to bind to
     * @param property The JCR Property instance
     * @return The optimal binding value or null
     * @throws javax.jcr.RepositoryException When a repository error occurs
     */
    protected Object retrieveOptimalValue(Class type, Property property) throws RepositoryException {
        int propertyType = property.getType();
        switch(propertyType) {
            case PropertyType.STRING:
                return property.getString();
            case PropertyType.BOOLEAN:
                return retrieveOptimalValueForBoolean(type, property);
            case PropertyType.LONG:
                return retrieveOptimalValueForLong(type, property.getLong());
            case PropertyType.DOUBLE:
                return retrieveOptimalValueForDouble(type, property.getDouble());
            case PropertyType.DATE:
                return retrieveOptimalValueForDate(type, property.getDate());
            case PropertyType.REFERENCE:
                return retrieveReference(type, property.getNode());
        }

        return null;
    }

    private Object retrieveReference(Class type, Node node) {
        MetaClass mc = InvokerHelper
                .getInstance()
                            .getMetaRegistry()
                            .getMetaClass(type);
        if(mc!=null) {
            return mc.invokeStaticMethod(type, CreateDynamicMethod.METHOD_NAME, new Object[]{node});
        }

        return null;
    }


    protected Object retrieveOptimalValueForDouble(Class type, double aDouble) {
        if(GrailsClassUtils.isAssignableOrConvertibleFrom(Double.class,type)) {
            return Double.valueOf(aDouble);
        }
        else {
            return String.valueOf(aDouble);
        }

    }

    protected Object retrieveOptimalValueForLong(Class type, long aLong) {
        if(GrailsClassUtils.isAssignableOrConvertibleFrom(Long.class,type)) {
            return Long.valueOf(aLong);
        }
        else {
            return String.valueOf(aLong);
        }

    }

    protected Object retrieveOptimalValueForDate(Class type, Calendar date) {
        if(Date.class.isAssignableFrom(type)) {
            return date.getTime();
        }
        else if(Calendar.class.isAssignableFrom(type)) {
            return date;
        }
        else if(GrailsClassUtils.isAssignableOrConvertibleFrom(Long.class, type)) {
            return Long.valueOf(date.getTime().getTime());
        }
        else {
            return date.getTime().toString();
        }

    }

    protected Object retrieveOptimalValueForBoolean(Class type, Property property) throws RepositoryException {
        if(GrailsClassUtils.isAssignableOrConvertibleFrom(Boolean.class, type)) {
            return Boolean.valueOf(property.getBoolean());
        }
        else {
            return String.valueOf(property.getBoolean());
        }

    }

    protected Object retrieveOptimalValueForNumber(Class type, Property property) throws RepositoryException {
        if(GrailsClassUtils.isAssignableOrConvertibleFrom(Integer.class,type)) {
            return Integer.valueOf((int)property.getLong());
        }
        else if(GrailsClassUtils.isAssignableOrConvertibleFrom(Long.class,type)) {
            return Long.valueOf(property.getLong());
        }
        else if(GrailsClassUtils.isAssignableOrConvertibleFrom(Double.class,type)) {
            return Double.valueOf(property.getDouble());
        }
        else if(GrailsClassUtils.isAssignableOrConvertibleFrom(Short.class,type)) {
            return Short.valueOf((short)property.getLong());
        }
        else if(GrailsClassUtils.isAssignableOrConvertibleFrom(BigInteger.class,type)) {
            return BigInteger.valueOf(property.getLong());
        }
        return null;
    }

}
