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
package org.codehaus.groovy.grails.jcr.metaclass;

import junit.framework.TestCase;

/**
 * Unit tests for XPathMethodExpressionFactory.
 *
 * @author Graeme Rocher
 * @since 0.4
 *        <p/>
 *        Created: Feb 12, 2007
 *        Time: 4:43:54 PM
 */
public class XPathMethodExpressionFactoryTests extends TestCase {
    private MethodExpressionFactory expressionFactory;


    protected void setUp() throws Exception {
        this.expressionFactory = new XPathMethodExpressionFactory();
    }

    public void testNot() {
         Criterion c = expressionFactory.le("foo", Integer.valueOf(10));
        assertEquals("not(@foo<=10)",expressionFactory.not(c).toString());
    }

    public void testLe() {
        assertEquals("@foo<=10",expressionFactory.le("foo", Integer.valueOf(10)).toString());
    }

    public void testLt() {
       assertEquals("@foo<10",expressionFactory.lt("foo", Integer.valueOf(10)).toString());
    }

    public void testGe() {
        assertEquals("@foo>=10",expressionFactory.ge("foo", Integer.valueOf(10)).toString());
    }

    public void testGt() {
       assertEquals("@foo>10",expressionFactory.gt("foo", Integer.valueOf(10)).toString());
    }

    public void testAnd() {
        Criterion c1 = expressionFactory.le("foo", Integer.valueOf(10));

        Criterion c2 = expressionFactory.ge("foo", Integer.valueOf(5));
       assertEquals("(@foo>=5) and (@foo<=10)",expressionFactory.and(c2,c1).toString());

    }

    public void testOr() {
        Criterion c1 = expressionFactory.le("foo", Integer.valueOf(10));

        Criterion c2 = expressionFactory.ge("foo", Integer.valueOf(5));
       assertEquals("(@foo>=5) or (@foo<=10)",expressionFactory.or(c2,c1).toString());

    }

    public void testEq() {
      assertEquals("@foo=10",expressionFactory.eq("foo", Integer.valueOf(10)).toString());
    }

    public void testNe() {
      assertEquals("@foo!=10",expressionFactory.ne("foo", Integer.valueOf(10)).toString());
    }

    public void testEqWithNamespace() {
      expressionFactory = new XPathMethodExpressionFactory("bar");
      assertEquals("@bar:foo=10",expressionFactory.eq("foo", Integer.valueOf(10)).toString());
    }

    public void testConditionalOperators() {
       expressionFactory = new XPathMethodExpressionFactory("bar");

        assertEquals("and", expressionFactory.getConditionalOperator(MethodExpressionFactory.Conditionals.AND));
        assertEquals("or", expressionFactory.getConditionalOperator(MethodExpressionFactory.Conditionals.OR));
    }
}
