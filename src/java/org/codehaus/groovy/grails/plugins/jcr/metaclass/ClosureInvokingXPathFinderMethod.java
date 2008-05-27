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
package org.codehaus.groovy.grails.plugins.jcr.metaclass;

import org.codehaus.groovy.grails.commons.GrailsApplication;

import java.util.List;
import java.util.regex.Pattern;

import groovy.lang.Closure;

/**
 * A dynamic finder that invokes a closure with the parsed method expressions using an
 * XPathMethodExpressionFactory
 *
 * @author Graeme Rocher
 * @since 0.5
 * 
 *        <p/>
 *        Created: Feb 12, 2007
 *        Time: 5:00:50 PM
 */
public class ClosureInvokingXPathFinderMethod extends AbstractFinderExpressionMethod {
    private Closure callable;

    public ClosureInvokingXPathFinderMethod(Pattern pattern,GrailsApplication application, String namespace, Closure callable) {
        super(pattern, application, new XPathMethodExpressionFactory(namespace));
        this.callable = callable;
    }

    protected Object doInvokeInternalWithExpressions(Class clazz, String methodName, Object[] arguments, List expressions, String operatorInUse) {
        Closure c = (Closure)callable.clone();
        c.setDelegate(clazz);
        return callable.call(new Object[]{methodName, arguments, expressions, operatorInUse});
    }
}
