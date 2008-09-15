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
import org.springframework.util.ClassUtils

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
    }

    void tearDown() {
        session.workspace.queryManager.createQuery("//Author", Query.XPATH).execute().nodes.each { Node node ->
            node.remove()
        }
        session.workspace.queryManager.createQuery("//Book", Query.XPATH).execute().nodes.each { Node node ->
            node.remove()
        }
        session.save()
        session.logout()
        ExpandoMetaClass.disableGlobally()
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

    void testUnideirectionalManyToOneReverse() {
        Node bookNode = session.getRootNode().addNode(bookDomainClass.clazz.getGrailsJcrMapping().basePath)
        bookNode.addMixin 'mix:referenceable'
        bookNode.setProperty(JcrConstants.CLASS_PROPERTY_NAME, ClassUtils.getQualifiedName(bookDomainClass.clazz))
        bookNode.setProperty('title', 'The Definitive Guide To Grails')

        Node authorNode = session.getRootNode().addNode(authorDomainClass.clazz.getGrailsJcrMapping().basePath)
        authorNode.setProperty(JcrConstants.CLASS_PROPERTY_NAME, ClassUtils.getQualifiedName(authorDomainClass.clazz))
        authorNode.addMixin 'mix:referenceable'
        authorNode.setProperty('name', 'Graeme Rocher')

        bookNode.setProperty('author', authorNode)
        session.save()

        def binder = new ExperimentalNodeBinder(gcl)
        def result = binder.bindFromNode(bookNode, bookDomainClass.clazz)

        assertNotNull result
        assertNotNull result.UUID
        assertEquals 'The Definitive Guide To Grails', result.title
        assertNotNull result.author
        assertEquals 'Graeme Rocher', result.author.name

    }
}