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

import org.apache.commons.lang.StringUtils;
import org.codehaus.groovy.grails.commons.GrailsClassUtils;

/**
 * A method expression factory that creates XPath criterions that can be placed inside an
 * Xpath expression's predicate (the [] brackets at the end!).
 *
 * @author Graeme Rocher
 * @since 0.5
 * 
 *        <p/>
 *        Created: Feb 12, 2007
 *        Time: 4:21:11 PM
 */
public class XPathMethodExpressionFactory implements MethodExpressionFactory {
    private String namespace = "";
    
    private static final String XPATH_AND = "and";
    private static final String XPATH_OR = "or";


    public XPathMethodExpressionFactory() {
    }


    public XPathMethodExpressionFactory(String namespace) {

        if(!StringUtils.isBlank(namespace)) {
            this.namespace = namespace + ":";
        }
    }

    interface XPathCriterion extends Criterion {
        // marker interface
    }
    public Criterion not(final Criterion criterion) {
        return new XPathCriterion() {
            public String toString() {
                return "not("+criterion.toString()+")";
            }
        };
    }

    public Criterion le(final String propertyName, final Object argument) {
        return new XPathCriterion() {
            public String toString() {
                return getPropertyName(propertyName) +"<="+ formatArgument(argument);
            }
        };
    }

    private Object formatArgument(Object argument) {
        if(argument != null) {
            if(GrailsClassUtils.isAssignableOrConvertibleFrom(Number.class, argument.getClass())) {
                return argument;
            }
            else {
                return "'" + argument + "'";
            }
        }

        return argument;
    }

    public Criterion lt(final String propertyName, final Object argument) {
        return new XPathCriterion() {
            public String toString() {
                return getPropertyName(propertyName) +"<"+ formatArgument(argument);
            }
        };

    }

    public Criterion ge(final String propertyName, final Object argument) {
        return new XPathCriterion() {
            public String toString() {
                return getPropertyName(propertyName) +">="+ formatArgument(argument);
            }
        };
    }

    public Criterion gt(final String propertyName, final Object argument) {
        return new XPathCriterion() {
            public String toString() {
                return getPropertyName(propertyName) +">"+ formatArgument(argument);
            }
        };
    }

    public Criterion like(String propertyName, Object argument) {
        throw new UnsupportedOperationException("The like criteria is not supported in XPath dynamic finder implementation");
    }

    public Criterion isNotNull(String propertyName) {
        throw new UnsupportedOperationException("The isNotNull criteria is not supported in XPath dynamic finder implementation");
    }

    public Criterion eq(final String propertyName, final Object argument) {
        return new XPathCriterion() {
            public String toString() {
                return getPropertyName(propertyName) +"="+ formatArgument(argument);
            }
        };
    }

    public Criterion isNull(String propertyName) {
        throw new UnsupportedOperationException("The isNull criteria is not supported in XPath dynamic finder implementation");
    }

    public Criterion between(String propertyName, Object left, Object right) {
        throw new UnsupportedOperationException("The Between criteria is not supported in XPath dynamic finder implementation");
    }

    public Criterion ne(final String propertyName, final Object argument) {
        return new XPathCriterion() {
            public String toString() {
                return getPropertyName(propertyName) +"!="+ formatArgument(argument);
            }
        };
    }

    private String getPropertyName(String propertyName) {
        return "@"+namespace+propertyName;
    }

    public Criterion and(final Criterion left, final Criterion right) {
        return new XPathCriterion() {
            public String toString() {
                return "("+left.toString()+") and ("+right.toString()+")";
            }
        };
    }

    public Criterion or(final Criterion left, final Criterion right) {
        return new XPathCriterion() {
            public String toString() {
                return "("+left.toString()+") or ("+right.toString()+")";
            }
        };
    }

    public String getConditionalOperator(String name) {
        if(Conditionals.AND.equals(name)) {
            return XPATH_AND;
        }
        else if(Conditionals.OR.equals(name)) {
            return XPATH_OR;
        }
        else {
            throw new IllegalArgumentException("Unsupported conditional operator ["+name+"] for XPath dialect");
        }
    }
}
