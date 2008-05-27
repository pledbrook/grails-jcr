package org.codehaus.groovy.grails.plugins.jcr;

/**
 * TODO: write javadoc
 *
 * @author Sergey Nebolsin (nebolsin@prophotos.ru)
 */
public class JcrUtils {
    /**
     * Creates a valid jcr label from the given one
     */
    public static String makeValidJCRPath(String label, boolean appendLeadingSlash) {
        if(appendLeadingSlash && !label.startsWith("/")) {
            label = "/" + label;
        }
        StringBuffer ret = new StringBuffer(label.length());
        for(int i = 0; i < label.length(); i++) {
            char c = label.charAt(i);
            if(c == '*' || c == '\'' || c == '\"') {
                c = '_';
                /* not quite correct: [] may be the index of a previously exported item. */
            } else if(c == '[') {
                c = '(';
            } else if(c == ']') {
                c = ')';
            }
            ret.append(c);
        }
        return ret.toString();
    }
}
