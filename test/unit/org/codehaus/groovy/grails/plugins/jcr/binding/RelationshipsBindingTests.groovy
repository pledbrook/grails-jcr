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
import org.codehaus.groovy.grails.plugins.jcr.JcrConfigurator
import javax.jcr.query.Query

/**
 * TODO: write javadoc
 *
 * @author Sergey Nebolsin (nebolsin@gmail.com)
 */
class RelationshipsBindingTests extends GroovyTestCase {
    Repository repo
    Session session
    GroovyClassLoader gcl = new GroovyClassLoader()
    GrailsDomainClass authorDomainClass, bookDomainClass

    void setUp() {
        super.setUp();
        ExpandoMetaClass.enableGlobally()
        repo = new TransientRepository();
        session = repo.login(new SimpleCredentials("Sergey Nebolsin", "passwd".toCharArray()));
        def namespaceRegistry = session.getWorkspace().getNamespaceRegistry()
        try {
            namespaceRegistry.getURI(JcrConstants.GRAILS_NAMESPACE_KEY)
        } catch (NamespaceException ne) {
            namespaceRegistry.registerNamespace(JcrConstants.GRAILS_NAMESPACE_KEY, JcrConstants.GRAILS_NAMESPACE_URI)
        }

        def authorClass = gcl.parseClass("""\
class Author {
    Long id
    Long version

    String UUID
    String name
}
""")
        authorDomainClass = new DefaultGrailsDomainClass(authorClass)
        def mc = authorDomainClass.metaClass
        def authorConf = JcrConfigurator.readConfiguration(authorDomainClass)
        mc.'static'.getGrailsJcrMapping = { ->
            authorConf
        }

        def bookClass = gcl.parseClass("""\
class Book {
    Long id
    Long version

    String UUID
    Author author
    String title
}
""")
        bookDomainClass = new DefaultGrailsDomainClass(bookClass)
        mc = bookDomainClass.metaClass
        def bookConf = JcrConfigurator.readConfiguration(bookDomainClass)
        mc.'static'.getGrailsJcrMapping = { ->
            bookConf
        }

        println bookClass.grailsJcrMapping
        println authorClass.grailsJcrMapping
    }

    void tearDown() {
        session.workspace.queryManager.createQuery("//Author", Query.XPATH).execute().nodes.each { Node node ->
            node.remove()
        }
        session.workspace.queryManager.createQuery("//Book", Query.XPATH).execute().nodes.each { Node node ->
            node.remove()
        }
        session.logout()
    }

    void testUnidirectionalManyToOne() {
        def author = authorDomainClass.newInstance()
        author.name = 'Graeme Rocher'
        def book = bookDomainClass.newInstance()
        book.title = 'The Definitive Guide To Grails'
        book.author = author

        Node node = session.getRootNode().addNode(book.getGrailsJcrMapping().basePath)
        def binder = new ExperimentalNodeBinder(gcl)
        binder.bindToNode(node, book)

        assertTrue node.hasProperty("author")
        Node authorNode = node.getProperty("author").getNode()
        assertEquals "/Author", authorNode.getPath()
        assertEquals "Graeme Rocher", authorNode.getProperty("name").getString()
    }
}