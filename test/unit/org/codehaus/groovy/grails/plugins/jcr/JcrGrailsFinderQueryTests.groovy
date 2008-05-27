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

class JcrGrailsFinderQueryTests extends GroovyTestCase {

    def interceptor = new OpenSessionInViewInterceptor()
    def request = new MockHttpServletRequest()
    def repository = null
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
        appCtx.registerMockBean("jcrTemplate", new JcrTemplate(sessionFactory: (SessionFactory) appCtx.getBean("jcrSessionFactory")))

        def plugin = pluginClass.newInstance()

        def log = LogFactory.getLog(pluginClass)
        pluginClass.metaClass.getLog << {-> log}

        gcl.parseClass("""
class WikiEntry {
   static mapWith = 'jcr'
   static namespace = 'wiki'

   Long id
   Long version

   String UUID
   String title
   String body
   Integer age
}
""")

        ga = new DefaultGrailsApplication(gcl.loadedClasses, gcl)
        ga.setApplicationContext appCtx
        ga.initialise()

        def setupDynamicMethods = plugin.doWithDynamicMethods
        setupDynamicMethods.delegate = [application: ga, log: LogFactory.getLog(getClass())]
        setupDynamicMethods(appCtx)

        interceptor.sessionFactory = (SessionFactory) appCtx.getBean('jcrSessionFactory')
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



    void testQueryForMethod() {
        def wikiEntryClass =  ga.getDomainClass("WikiEntry").getClazz()


        assertEquals "//wiki:WikiEntry[(@wiki:age>=18) and (@wiki:body='bar')]", wikiEntryClass.queryForAgeGreaterThanOrEqualAndBody(18, "bar")
        assertEquals '//wiki:WikiEntry[not(@wiki:age=25)]', wikiEntryClass.queryForAgeNot(25)    
        assertEquals '//wiki:WikiEntry[@wiki:age<18]', wikiEntryClass.queryForAgeLessThan(18)
        assertEquals '//wiki:WikiEntry[@wiki:age>18]', wikiEntryClass.queryForAgeGreaterThan(18)
        assertEquals '//wiki:WikiEntry[@wiki:title!=\'foo\']', wikiEntryClass.queryForTitleNotEqual("foo")
        assertEquals '//wiki:WikiEntry[@wiki:title=\'foo\']', wikiEntryClass.queryForTitle("foo")        
        assertEquals "//wiki:WikiEntry[(@wiki:title='foo') and (@wiki:body='bar')]", wikiEntryClass.queryForTitleAndBody("foo", "bar")
        assertEquals "//wiki:WikiEntry[(@wiki:age>18) and (@wiki:body='bar')]", wikiEntryClass.queryForAgeGreaterThanAndBody(18, "bar")
        assertEquals "//wiki:WikiEntry[(@wiki:title='foo') or (@wiki:body='bar')]", wikiEntryClass.queryForTitleOrBody("foo", "bar")

    }

    
}