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
package org.codehaus.groovy.grails.plugins.jcr.binding;

import junit.framework.TestCase;

import javax.jcr.*;

import org.apache.jackrabbit.core.TransientRepository;
import org.apache.commons.beanutils.BeanMap;

import java.util.Date;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.net.URL;
import java.net.URI;

/**
 * Tests for the NodeBinder class
 *
 * @author Graeme Rocher
 * @since 0.4
 *        <p/>
 *        Created: Feb 9, 2007
 *        Time: 6:08:10 PM
 */
public class NodeBinderTests extends TestCase {

    Repository repo;
    Session session;


    protected void setUp() throws Exception {
        super.setUp();
        repo = new TransientRepository();
        session = repo.login(new SimpleCredentials("Sergey Nebolsin", "passwd".toCharArray()));
    }

    protected void tearDown() throws Exception {
        session.logout();
    }

    public void testBindToObject() throws Exception {
        TestObject to = new TestObject();
        NodeBinder binder = NodeBinder.createNodeBinder(to,"myObject");


        Node root = session.getRootNode();

        Node testNode = root.addNode("testNode");

        testNode.setProperty("testBoolean", true);
        testNode.setProperty("string", "foo");
        testNode.setProperty("concreteLong", 12345678L);
        testNode.setProperty("primitiveLong", 987654321L);
        testNode.setProperty("integer", 12345);
        testNode.setProperty("primitiveInt", 98765);
        testNode.setProperty("date", new GregorianCalendar());
        testNode.setProperty("calendar", new GregorianCalendar());
        testNode.setProperty("url", new URL("http://grails.org").toString());
        testNode.setProperty("uri", new URI("http://grails.org").toString());
        

        binder.bind(testNode);

        assertTrue(to.isTestBoolean());
        assertEquals("foo", to.getString());
        assertEquals(new Integer(12345), to.getInteger());
        assertEquals(98765,to.getPrimitiveInt());        
        assertNotNull(to.getDate());
        assertNotNull(to.getCalendar());
        assertEquals(new URL("http://grails.org"), to.getUrl());
        assertEquals(new URI("http://grails.org"), to.getUri());
        session.logout();
    }

    public void testBindToNode() throws Exception {
        TestObject to = new TestObject();

        Calendar c = new GregorianCalendar();
        to.setCalendar(c);
        to.setDate(c.getTime());
        to.setPrimitiveInt(12345);
        to.setInteger(Integer.valueOf(6789));
        to.setString("bar");
        to.setUrl(new URL("http://groovy.codehaus.org"));
        to.setUri(new URI("http://groovy.codehaus.org"));
        
        Node root = session.getRootNode();

        Node testNode = root.addNode("testNode");

        NodeBinder binder =NodeBinder.createNodeBinder(testNode,"theNode");

        binder.bind(new BeanMap(to));

        assertEquals("http://groovy.codehaus.org", testNode.getProperty("url").getString());
        assertEquals("http://groovy.codehaus.org", testNode.getProperty("uri").getString());
        assertEquals(c, testNode.getProperty("calendar").getDate());
        assertEquals(c.getTime(), testNode.getProperty("date").getDate().getTime());
        assertEquals(12345,testNode.getProperty("primitiveInt").getLong());
        assertEquals(6789, Long.valueOf(testNode.getProperty("integer").getLong()).intValue());

    }

    public void testNodeToNodeWithNamespace() throws Exception {
        TestObject to = new TestObject();

        Calendar c = new GregorianCalendar();
        to.setCalendar(c);
        to.setDate(c.getTime());
        to.setPrimitiveInt(12345);
        to.setInteger(Integer.valueOf(6789));
        to.setString("bar");
        to.setUrl(new URL("http://groovy.codehaus.org"));
        to.setUri(new URI("http://groovy.codehaus.org"));


        try {
            session.getWorkspace().getNamespaceRegistry().getURI("test");
        } catch (NamespaceException ne) {
            session.getWorkspace().getNamespaceRegistry().registerNamespace("test", "http://grails.org/test/1.0");
        }

        Node root = session.getRootNode();

        Node testNode = root.addNode("test:testNode");

        NodeBinder binder =NodeBinder.createNodeBinder(testNode,"theNode", "test");

        binder.bind(new BeanMap(to));

        assertEquals("http://groovy.codehaus.org", testNode.getProperty("test:url").getString());
        assertEquals("http://groovy.codehaus.org", testNode.getProperty("test:uri").getString());
        assertEquals(c, testNode.getProperty("test:calendar").getDate());
        assertEquals(c.getTime(), testNode.getProperty("test:date").getDate().getTime());
        assertEquals(12345,testNode.getProperty("test:primitiveInt").getLong());
        assertEquals(6789, Long.valueOf(testNode.getProperty("test:integer").getLong()).intValue());

    }


    public void testBindToObjectWithNamespace() throws Exception {
        TestObject to = new TestObject();
        NodeBinder binder = NodeBinder.createNodeBinder(to,"myObject", "test");

        try {
            session.getWorkspace().getNamespaceRegistry().getURI("test");
        } catch (NamespaceException ne) {
            session.getWorkspace().getNamespaceRegistry().registerNamespace("test", "http://grails.org/test/1.0");
        }

        Node root = session.getRootNode();

        Node testNode = root.addNode("test:testNode");

        testNode.setProperty("test:testBoolean", true);
        testNode.setProperty("test:string", "foo");
        testNode.setProperty("test:concreteLong", 12345678L);
        testNode.setProperty("test:primitiveLong", 987654321L);
        testNode.setProperty("test:integer", 12345);
        testNode.setProperty("test:primitiveInt", 98765);
        testNode.setProperty("test:date", new GregorianCalendar());
        testNode.setProperty("test:calendar", new GregorianCalendar());
        testNode.setProperty("test:url", new URL("http://grails.org").toString());
        testNode.setProperty("test:uri", new URI("http://grails.org").toString());


        binder.bind(testNode);

        assertTrue(to.isTestBoolean());
        assertEquals("foo", to.getString());
        assertEquals(new Integer(12345), to.getInteger());
        assertEquals(98765,to.getPrimitiveInt());
        assertNotNull(to.getDate());
        assertNotNull(to.getCalendar());
        assertEquals(new URL("http://grails.org"), to.getUrl());
        assertEquals(new URI("http://grails.org"), to.getUri());
    }

    public class TestObject {
        private String string;
        private Long concreteLong;
        private long primitiveLong;
        private Date date;
        private Calendar calendar;
        private Integer integer;
        private int primitiveInt;
        private boolean testBoolean;
        private URL url;
        private URI uri;


        public URI getUri() {
            return uri;
        }

        public void setUri(URI uri) {
            this.uri = uri;
        }

        public URL getUrl() {
            return url;
        }

        public void setUrl(URL url) {
            this.url = url;
        }

        public boolean isTestBoolean() {
            return testBoolean;
        }

        public void setTestBoolean(boolean testBoolean) {
            this.testBoolean = testBoolean;
        }

        public String getString() {
            return string;
        }

        public void setString(String string) {
            this.string = string;
        }

        public Long getConcreteLong() {
            return concreteLong;
        }

        public void setConcreteLong(Long concreteLong) {
            this.concreteLong = concreteLong;
        }

        public long getPrimitiveLong() {
            return primitiveLong;
        }

        public void setPrimitiveLong(long primitiveLong) {
            this.primitiveLong = primitiveLong;
        }

        public Date getDate() {
            return date;
        }

        public void setDate(Date date) {
            this.date = date;
        }

        public Calendar getCalendar() {
            return calendar;
        }

        public void setCalendar(Calendar calendar) {
            this.calendar = calendar;
        }

        public Integer getInteger() {
            return integer;
        }

        public void setInteger(Integer integer) {
            this.integer = integer;
        }

        public int getPrimitiveInt() {
            return primitiveInt;
        }

        public void setPrimitiveInt(int primitiveInt) {
            this.primitiveInt = primitiveInt;
        }
    }
}
