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
        ApplicationHolder.application = null
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

        def config = wikiEntryClass.getGrailsJcrMapping()
        assertNotNull config
        assertEquals "wiki:", config.namespace
    }

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
}