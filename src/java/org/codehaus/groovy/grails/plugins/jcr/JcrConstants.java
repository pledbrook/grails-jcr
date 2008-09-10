package org.codehaus.groovy.grails.plugins.jcr;

public interface JcrConstants {
    public static final String UUID_PROPERTY_NAME = "UUID";
    public static final String NAMESPACE_PROPERTY_NAME = "namespace";
    public static final String MIXIN_REFERENCEABLE = "mix:referenceable";
    public static final String GRAILS_NAMESPACE_KEY = "grails";
    public static final String GRAILS_NAMESPACE_URI = "http://grails.org/jcr/";


    public static final String CLASS_PROPERTY_NAME = GRAILS_NAMESPACE_KEY + ":type";
    public static final String MAP_KEY_PROPERTY_NAME = GRAILS_NAMESPACE_KEY + ":mapKey";
    public static final String MAP_VALUE_PROPERTY_NAME = GRAILS_NAMESPACE_KEY + ":mapValue";
    public static final String COLLECTION_VALUE_PROPERTY_NAME = GRAILS_NAMESPACE_KEY + ":collectionValue";

}
