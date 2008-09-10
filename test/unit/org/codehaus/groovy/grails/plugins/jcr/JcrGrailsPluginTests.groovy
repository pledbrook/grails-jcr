package org.codehaus.groovy.grails.plugins.jcr

import grails.util.GrailsUtil

import javax.jcr.*
import javax.jcr.query.*
import org.springmodules.jcr.support.*
import org.springmodules.jcr.*
import org.springframework.mock.web.*
import org.codehaus.groovy.grails.support.*
import org.codehaus.groovy.grails.commons.*
import org.codehaus.groovy.grails.commons.metaclass.*
import org.apache.jackrabbit.core.*
import org.apache.commons.logging.*
import org.codehaus.groovy.runtime.*
import org.springframework.core.io.support.PathMatchingResourcePatternResolver

class JcrGrailsPluginTests extends GroovyTestCase {

    def interceptor = new OpenSessionInViewInterceptor()
    def request = new MockHttpServletRequest()
    Repository repository = null
    def gcl = new GroovyClassLoader()
    def appCtx = new MockApplicationContext()
    def resolver = new PathMatchingResourcePatternResolver()
    def originalHandler = null
    GrailsApplication ga = null

    void setUp() {

        ExpandoMetaClass.enableGlobally()
        super.setUp()


        Class pluginClass = gcl.parseClass(resolver.getResource("file:JcrGrailsPlugin.groovy").inputStream)

        repository = new TransientRepository()
        def factory = new JcrSessionFactory(repository: repository, credentials: new SimpleCredentials("user", "".toCharArray()))
        factory.afterPropertiesSet()

        appCtx.registerMockBean("jcrSessionFactory", factory)
        appCtx.registerMockBean("jcrTemplate", new JcrTemplate(sessionFactory: appCtx.getBean("jcrSessionFactory")))

        def plugin = pluginClass.newInstance()

        def log = LogFactory.getLog(pluginClass)
        pluginClass.metaClass.getLog << {-> log}

        gcl.parseClass("""\
class WikiEntry {
   static mapWith = 'jcr'
   static namespace = 'wiki'

   Long id
   Long version
   
   String UUID
   String title
   String body
}
""")

        ga = new DefaultGrailsApplication(gcl.loadedClasses, gcl)
        ApplicationHolder.application = ga
        ga.setApplicationContext appCtx
        ga.initialise()

        def setupDynamicMethods = plugin.doWithDynamicMethods
        setupDynamicMethods.delegate = [application: ga, log: LogFactory.getLog(getClass())]
        setupDynamicMethods(appCtx)

        interceptor.sessionFactory = appCtx.getBean('jcrSessionFactory')
        interceptor.preHandle(request, null, null)

    }

    void tearDown() {
        ga.getDomainClass("WikiEntry").getClazz().withSession {session ->
            def qm = session.workspace.queryManager
            def query = qm.createQuery("//wiki:WikiEntry", Query.XPATH)

            def result = query.execute().nodes
            while(result.hasNext()) {
                result.next().remove()
            }

            session.save()
        }
        interceptor.afterCompletion(request, null, null, null)
        repository.shutdown()
        ExpandoMetaClass.disableGlobally()
    }


    void testMetaClass() {
        Class wikiEntryClass = ga.getDomainClass("WikiEntry").getClazz()
        assertTrue GroovySystem.getMetaClassRegistry().getMetaClass(wikiEntryClass) instanceof DynamicMethodsExpandoMetaClass
    }

    void testRepositoryName() {
        def wikiEntryClass = ga.getDomainClass("WikiEntry").getClazz()

        assertEquals "wiki:WikiEntry", wikiEntryClass.getRepositoryName()
    }

    void testConfiguration() {
        def wikiEntryClass = ga.getDomainClass("WikiEntry").getClazz()

        def config = wikiEntryClass.getGrailsJcrConfiguration()
        assertNotNull config
        assertEquals "wiki:", config.namespace
    }

    void testFindAll() {
        println "Starting a test"
        def wikiEntryClass = ga.getDomainClass("WikiEntry").getClazz()
        def wikiEntry1 = ga.getDomainClass("WikiEntry").newInstance()
        def wikiEntry2 = ga.getDomainClass("WikiEntry").newInstance()
        def wikiEntry3 = ga.getDomainClass("WikiEntry").newInstance()

        wikiEntry1.title = "fred"
        wikiEntry1.body = "flintstone"


        wikiEntry2.title = "wilma"
        wikiEntry2.body = "flintstone"

        wikiEntry3.title = "fred"
        wikiEntry3.body = "xxx"

        println "Inited three entries: ${wikiEntry1.UUID}, ${wikiEntry2.UUID}, ${wikiEntry3.UUID}"

        wikiEntry1.save()
        wikiEntry2.save()
        wikiEntry3.save()

        println "Saved three entries: ${wikiEntry1.UUID}, ${wikiEntry2.UUID}, ${wikiEntry3.UUID}"

        def session = repository.login(new SimpleCredentials("Sergey Nebolsin", "passwd".toCharArray()));
        session.exportDocumentView("/", System.out, false, false)

        def results = wikiEntryClass.findAll("//wiki:WikiEntry[@wiki:title = 'fred']")

        println "Found: $results"

        assertEquals "Wrong result size", 2, results.size
        assertTrue "fred flinstone was not returned", results.any { it?.title == 'fred' && it?.body == 'flintstone' }
        assertTrue "fred xxx was not returned", results.any { it?.title == 'fred' && it?.body == 'xxx'}

    }

    void testFindByMethod() {
        def wikiEntryClass = ga.getDomainClass("WikiEntry").getClazz()
        def wikiEntry = ga.getDomainClass("WikiEntry").newInstance()

        wikiEntry.title = "foo"
        wikiEntry.body = "bar"

        wikiEntry.save()

        wikiEntry = wikiEntryClass.findByTitle("foo")

        assertNotNull wikiEntry

        wikiEntry = wikiEntryClass.findByTitleAndBody("foo", "bar")

        assertNotNull wikiEntry

    }

    void testSave() {
        def wikiEntryClass = ga.getDomainClass("WikiEntry").getClazz()
        def wikiEntry = ga.getDomainClass("WikiEntry").newInstance()

        wikiEntry.title = "foo"
        wikiEntry.body = "bar"

        wikiEntry.save()

        assertNotNull wikiEntry.UUID

        wikiEntryClass.withSession {session ->
            def saved = session.getNodeByUUID(wikiEntry.UUID)
            assertEquals "foo", saved.getProperty("wiki:title").getString()
            assertEquals "bar", saved.getProperty("wiki:body").getString()
        }

    }

    void testList() {
        def wikiEntryClass = ga.getDomainClass("WikiEntry").getClazz()
        def wikiEntry = ga.getDomainClass("WikiEntry").newInstance()
        def wikiEntry2 = ga.getDomainClass("WikiEntry").newInstance()

        wikiEntry.title = "foo"
        wikiEntry.body = "bar"

        wikiEntry.save()

        wikiEntry2.title = "test"
        wikiEntry2.body = "post"
        wikiEntry2.save()


        List results = wikiEntryClass.list()

        assertNotNull(results)

        def one = results.find { it.title == "foo" }

        assertNotNull(one)

        def two = results.find { it.body == "post" }

        assertNotNull(two)


    }

    /*void testListWithOffsetMax() {
            def wikiEntryClass =  ga.getDomainClass("WikiEntry").getClazz()
            def wikiEntry =  ga.getDomainClass("WikiEntry").newInstance()
            def wikiEntry2 =  ga.getDomainClass("WikiEntry").newInstance()

            wikiEntry.title = "foo"
            wikiEntry.body = "bar"

            wikiEntry.save()

            wikiEntry2.title = "test"
            wikiEntry2.body = "post"
            wikiEntry2.save()


            List results = wikiEntryClass.list(offset:1, max:1)

            assertEquals 1, results.size()

    }*/

    void testWithSession() {
        def wikiEntryClass = ga.getDomainClass("WikiEntry").getClazz()

        wikiEntryClass.withSession {session ->
            assert session != null
        }

    }

    void testNamespaceInfo() {
        def wikiEntryClass = ga.getDomainClass("WikiEntry").getClazz()

        assertEquals "wiki", wikiEntryClass.getNamespacePrefix()
        assertEquals "http://grails.org/wiki/", wikiEntryClass.getNamespaceURI()

    }


    void testGet() {
        def wikiEntryClass = ga.getDomainClass("WikiEntry").getClazz()
        def wikiEntry = ga.getDomainClass("WikiEntry").newInstance()

        wikiEntry.title = "foo"
        wikiEntry.body = "bar"

        wikiEntry.save()

        assert wikiEntry.UUID != null
        def UUID = wikiEntry.UUID
        wikiEntry = null

        wikiEntry = wikiEntryClass.get(UUID)

        assert wikiEntry != null

        assertEquals "foo", wikiEntry.title
        assertEquals "bar", wikiEntry.body
    }

    void testUpdate() {
        def wikiEntryClass = ga.getDomainClass("WikiEntry").getClazz()
        def wikiEntry = ga.getDomainClass("WikiEntry").newInstance()

        wikiEntry.title = "foo"
        wikiEntry.body = "bar"

        wikiEntry.save()

        assert wikiEntry.UUID != null
        def UUID = wikiEntry.UUID
        wikiEntry = null

        wikiEntry = wikiEntryClass.get(UUID)

        assert wikiEntry != null

        assertEquals "foo", wikiEntry.title
        assertEquals "bar", wikiEntry.body

        wikiEntry.title = "bar"
        wikiEntry.body = "foo"

        wikiEntry.save()

        wikiEntry = null

        wikiEntry = wikiEntryClass.get(UUID)

        assert wikiEntry != null

        assertEquals "bar", wikiEntry.title
        assertEquals "foo", wikiEntry.body
    }


    void testFind() {
        def wikiEntryClass = ga.getDomainClass("WikiEntry").getClazz()
        def wikiEntry = ga.getDomainClass("WikiEntry").newInstance()

        wikiEntry.title = "fred"
        wikiEntry.body = "flintstone"

        wikiEntry.save()

        wikiEntry = null

        wikiEntry = wikiEntryClass.find("//wiki:WikiEntry[@wiki:title = 'fred']")

        assert wikiEntry != null

        assertEquals "fred", wikiEntry.title
        assertEquals "flintstone", wikiEntry.body

    }



    void testLockAndUnlock() {
        def wikiEntryClass = ga.getDomainClass("WikiEntry").getClazz()
        def wikiEntry = ga.getDomainClass("WikiEntry").newInstance()

        wikiEntry.title = "dino"
        wikiEntry.body = "dinosaur"

        wikiEntry.save()

        def lock = wikiEntry.lock(true)
        assertNotNull lock

        assertTrue wikiEntry.isLocked()
        wikiEntry.unlock()
        assertFalse wikiEntry.isLocked()

    }


}