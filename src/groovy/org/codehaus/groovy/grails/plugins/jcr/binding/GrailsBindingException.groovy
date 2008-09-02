package org.codehaus.groovy.grails.plugins.jcr.binding

/**
 * TODO: write javadoc
 *
 * @author Sergey Nebolsin (nebolsin@gmail.com)
 */
class GrailsBindingException extends RuntimeException {

    public GrailsBindingException() {
        super();
    }

    public GrailsBindingException(String message) {
        super(message);
    }

    public GrailsBindingException(String message, Throwable cause) {
        super(message, cause);
    }

    public GrailsBindingException(Throwable cause) {
        super(cause);
    }
}