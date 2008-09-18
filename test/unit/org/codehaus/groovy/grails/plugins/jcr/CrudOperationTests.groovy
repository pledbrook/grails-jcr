package org.codehaus.groovy.grails.plugins.jcr

import javax.jcr.*
import javax.jcr.query.*
import org.springmodules.jcr.support.*
import org.springmodules.jcr.*
import org.springframework.mock.web.*
import org.codehaus.groovy.grails.support.*
import org.codehaus.groovy.grails.commons.*
import org.apache.jackrabbit.core.*
import org.apache.commons.logging.*
import org.springframework.core.io.support.PathMatchingResourcePatternResolver

import org.codehaus.groovy.grails.plugins.jcr.mapping.MapperConfigurator
import org.codehaus.groovy.grails.plugins.jcr.mapping.JcrOcmTemplate
import org.apache.jackrabbit.ocm.reflection.ReflectionUtils

class CrudOperationTests extends GroovyTestCase {

    def interceptor = new OpenSessionInViewInterceptor()
    def request = new MockHttpServletRequest()
    Repository repository = null
    def gcl = new GroovyClassLoader()
    def appCtx = new MockApplicationContext()
    def resolver = new PathMatchingResourcePatternResolver()
    def originalHandler = null
    GrailsApplication ga = null
    Class domainClass

    void setUp() {

        ExpandoMetaClass.enableGlobally()
        super.setUp()

        gcl.parseClass("""\
import org.apache.jackrabbit.ocm.mapper.impl.annotation.Field;
import org.apache.jackrabbit.ocm.mapper.impl.annotation.Node;

@Node(jcrMixinTypes="mix:referenceable")
class WikiEntry {
   static mapWith = 'jcr'
   static namespace = 'wiki'

   @Field(id=true) Long id
   Long version

   @Field(path=true) String path
   @Field(uuid=true) String UUID
   @Field String title
   @Field String body
}
""")


        Class pluginClass = gcl.parseClass(resolver.getResource("file:JcrGrailsPlugin.groovy").inputStream)

        repository = new TransientRepository()
        def factory = new JcrSessionFactory(repository: repository, credentials: new SimpleCredentials("user", "".toCharArray()))
        factory.afterPropertiesSet()

        appCtx.registerMockBean("jcrSessionFactory", factory)
        appCtx.registerMockBean("jcrTemplate", new JcrTemplate(sessionFactory: appCtx.getBean("jcrSessionFactory")))

        def plugin = pluginClass.newInstance()

        def log = LogFactory.getLog(pluginClass)
        pluginClass.metaClass.getLog << {-> log}

        ga = new DefaultGrailsApplication(gcl.loadedClasses, gcl)
        ApplicationHolder.application = ga
        ga.setApplicationContext appCtx
        ga.initialise()

        ReflectionUtils.classLoader = ga.classLoader

        appCtx.registerMockBean("jcrMapper", MapperConfigurator.configureMapper(ga.domainClasses.collect {it.clazz}))
        appCtx.registerMockBean("jcrOcmTemplate", new JcrOcmTemplate(jcrSessionFactory:appCtx.getBean("jcrSessionFactory"), jcrMapper:appCtx.getBean("jcrMapper")))

        def setupDynamicMethods = plugin.doWithDynamicMethods
        setupDynamicMethods.delegate = [application: ga, log: LogFactory.getLog(getClass())]
        setupDynamicMethods(appCtx)


        def setupApplicationContext = plugin.doWithApplicationContext
        setupApplicationContext.delegate = [application: ga, log: LogFactory.getLog(getClass())]
        setupApplicationContext(appCtx)

        interceptor.sessionFactory = appCtx.getBean('jcrSessionFactory')
        interceptor.preHandle(request, null, null)

        domainClass = ga.getDomainClass("WikiEntry").getClazz()

    }

    void tearDown() {
        getDomainClass().withSession { Session session ->
            if(session.itemExists("/WikiEntry")) {
                session.getRootNode().getNode("WikiEntry").remove()
                session.save()
            }

        }

        interceptor.afterCompletion(request, null, null, null)
        repository.shutdown()
        ApplicationHolder.application = null
        ExpandoMetaClass.disableGlobally()
    }

    void testSave() {
        def wikiEntry = createDomainInstance()

        wikiEntry.id=1
        wikiEntry.title = "foo"
        wikiEntry.body = "bar"

        wikiEntry.save()

        assertNotNull wikiEntry.UUID

        domainClass.withSession {session ->
            def saved = session.getNodeByUUID(wikiEntry.UUID)
            assertEquals "foo", saved.getProperty("title").getString()
            assertEquals "bar", saved.getProperty("body").getString()
        }

    }

    void testList() {
        def wikiEntry = createDomainInstance()
        def wikiEntry2 = createDomainInstance()

        wikiEntry.id=1
        wikiEntry.title = "foo"
        wikiEntry.body = "bar"

        wikiEntry.save()

        wikiEntry2.id=2
        wikiEntry2.title = "test"
        wikiEntry2.body = "post"
        wikiEntry2.save()


        List results = domainClass.list()

        assertNotNull(results)

        def one = results.find { it.title == "foo" }

        assertNotNull(one)

        def two = results.find { it.body == "post" }

        assertNotNull(two)
    }

    void testGet() {
        def wikiEntry = createDomainInstance()

        wikiEntry.id = 1
        wikiEntry.title = "foo"
        wikiEntry.body = "bar"
        wikiEntry.save()

        def result = domainClass.get("/WikiEntry/1")
        assertNotNull "result"
        assertEquals(1L, result.id)
        assertEquals("foo", result.title)
        assertEquals("bar", result.body)

        assertNull domainClass.get("/WikiEntry/2")
    }

    void testGetByUUID() {
        def wikiEntry = createDomainInstance()

        wikiEntry.id = 1
        wikiEntry.title = "foo"
        wikiEntry.body = "bar"
        wikiEntry.save()

        def result = domainClass.getByUUID(wikiEntry.UUID)
        assertNotNull "result"
        assertEquals(1L, result.id)
        assertEquals("foo", result.title)
        assertEquals("bar", result.body)

        shouldFail {
            domainClass.getByUUID("123qwqwe")
        }
    }

    void testCount() {
        def wikiEntry = createDomainInstance()
        def wikiEntry2 = createDomainInstance()

        wikiEntry.id=1
        wikiEntry.title = "foo"
        wikiEntry.body = "bar"

        wikiEntry.save()

        wikiEntry2.id=2
        wikiEntry2.title = "test"
        wikiEntry2.body = "post"
        wikiEntry2.save()

        assertEquals 2, domainClass.count()

        wikiEntry2.delete()

        assertEquals 1, domainClass.count()

        wikiEntry.delete()

        assertEquals 0, domainClass.count()
    }

    Object createDomainInstance() {
        domainClass.newInstance()
    }
}