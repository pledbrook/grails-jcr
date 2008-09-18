package org.codehaus.groovy.grails.plugins.jcr.mapping;

/**
 * TODO: write javadoc
 *
 * @author Sergey Nebolsin (nebolsin@gmail.com)
 */
public class GrailsJcrMappingException extends RuntimeException {
    public GrailsJcrMappingException() {
    }

    public GrailsJcrMappingException(String message) {
        super(message);
    }

    public GrailsJcrMappingException(String message, Throwable cause) {
        super(message, cause);
    }

    public GrailsJcrMappingException(Throwable cause) {
        super(cause);
    }
}
