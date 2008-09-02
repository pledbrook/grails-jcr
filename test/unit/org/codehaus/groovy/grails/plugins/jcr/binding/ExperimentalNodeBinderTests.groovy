package org.codehaus.groovy.grails.plugins.jcr.binding

import org.apache.jackrabbit.core.TransientRepository
import javax.jcr.SimpleCredentials
import javax.jcr.Session
import javax.jcr.Repository
import javax.jcr.Node
import org.codehaus.groovy.grails.commons.GrailsDomainClass
import org.codehaus.groovy.grails.commons.DefaultGrailsDomainClass
import javax.jcr.NamespaceException
import org.codehaus.groovy.grails.plugins.jcr.JcrConstants

/**
 * Test cases for ExperimentalNodeBinder.
 */
class ExperimentalNodeBinderTests extends GroovyTestCase {
    Repository repo
    Session session
    GroovyClassLoader gcl = new GroovyClassLoader()
    GrailsDomainClass testDomainClass
    Node testNode

    void setUp() {
        super.setUp();
        repo = new TransientRepository();
        session = repo.login(new SimpleCredentials("Sergey Nebolsin", "passwd".toCharArray()));
        def namespaceRegistry = session.getWorkspace().getNamespaceRegistry()
        try {
            namespaceRegistry.getURI(JcrConstants.GRAILS_NAMESPACE_KEY)
        } catch (NamespaceException ne) {
            namespaceRegistry.registerNamespace(JcrConstants.GRAILS_NAMESPACE_KEY, JcrConstants.GRAILS_NAMESPACE_URI)
        }

        def testClass = gcl.parseClass("""\
class TestClass {
    Long id
    Long version

    String string
    Boolean booleanObject
    boolean booleanPrimitive
    Long longObject
    long longPrimitive
    Integer intObject
    int intPrimitive
    Date date
    Calendar calendar
    URL url
    URI uri

    Map map
}
""")
        testDomainClass = new DefaultGrailsDomainClass(testClass)

        testNode = session.getRootNode().addNode("testNode")
    }

    void tearDown() {
        testNode.remove()
        session.logout()
    }

    void testSimpleBindFromNode() {

        def cal = new GregorianCalendar()

        testNode.setProperty("string", "foo");
        testNode.setProperty("booleanObject", true);
        testNode.setProperty("booleanPrimitive", false);
        testNode.setProperty("longObject", 12345678L);
        testNode.setProperty("longPrimitive", 987654321L);
        testNode.setProperty("intObject", 12345);
        testNode.setProperty("intPrimitive", 98765);
        testNode.setProperty("date", cal);
        testNode.setProperty("calendar", cal);
        testNode.setProperty("url", "http://grails.org")
        testNode.setProperty("uri", "http://grails.org")

        def target = testDomainClass.newInstance()

        def binder = new ExperimentalNodeBinder()
        binder.bindFrom(testNode)

        assertEquals "foo", target.string
        assertTrue target.booleanObject
        assertFalse target.booleanPrimitive
        assertEquals 12345678L, target.longObject
        assertEquals 987654321L, target.longPrimitive
        assertEquals 12345, target.intObject
        assertEquals 98765, target.intPrimitive
        assertEquals cal.getTime(), target.date
        assertEquals cal, target.calendar
        assertEquals new URL("http://grails.org"), target.url
        assertEquals new URI("http://grails.org"), target.uri
    }

    void testSimpleBindToNode() {
        def cal = new GregorianCalendar()
        def target = testDomainClass.newInstance()

        target.string = "foo"
        target.booleanObject = true
        target.booleanPrimitive = false
        target.longObject = 12345678L
        target.longPrimitive = 987654321L
        target.intObject = 12345
        target.intPrimitive = 98765
        target.date = cal.getTime()
        target.calendar = cal
        target.url = new URL("http://grails.org")
        target.uri = new URI("http://grails.org")

        ExperimentalNodeBinder binder = new ExperimentalNodeBinder()
        binder.bindToNode(testNode, target)

        assertEquals "foo", testNode.getProperty("string").getString()
        assertTrue testNode.getProperty("booleanObject").getBoolean()
        assertFalse testNode.getProperty("booleanPrimitive").getBoolean()
        assertEquals 12345678L, testNode.getProperty("longObject").getLong()
        assertEquals 987654321L, testNode.getProperty("longPrimitive").getLong()
        assertEquals 12345, testNode.getProperty("intObject").getLong()
        assertEquals 98765, testNode.getProperty("intPrimitive").getLong()
        assertEquals cal.getTime(), testNode.getProperty("date").getDate().getTime()
        assertEquals cal, testNode.getProperty("calendar").getDate()
        assertEquals "http://grails.org", testNode.getProperty("url").getString()
        assertEquals "http://grails.org", testNode.getProperty("uri").getString()
    }

    void testBindMapToNode() {
        def target = testDomainClass.newInstance()

        target.map = [aaa: "bbb", bbb: 123L]

        def binder = new ExperimentalNodeBinder()
        binder.bindToNode(testNode, target)

        assertTrue testNode.hasNode("map")
        def mapNode = testNode.getNode("map")

        assertTrue mapNode.hasNode("aaa")
        def valueNode = mapNode.getNode("aaa")
        assertEquals "aaa", valueNode.getProperty("grails:mapKey").getString()
        assertEquals "bbb", valueNode.getProperty("grails:mapValue").getString()

        assertTrue mapNode.hasNode("bbb")
        valueNode = mapNode.getNode("bbb")
        assertEquals "bbb", valueNode.getProperty("grails:mapKey").getString()
        assertEquals 123L, valueNode.getProperty("grails:mapValue").getLong()
    }
}