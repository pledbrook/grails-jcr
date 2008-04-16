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

class JcrGrailsFinderQueryTests extends GroovyTestCase {

        def interceptor = new OpenSessionInViewInterceptor()
        def request = new MockHttpServletRequest()
        def gcl = new GroovyClassLoader()
        def ga = null
        def appCtx = new MockApplicationContext()
        def resolver = new PathMatchingResourcePatternResolver()
        def originalHandler = null

        void setUp() {

            originalHandler = InvokerHelper.getInstance()
                                    .getMetaRegistry()
                                    .metaClassCreationHandle

            InvokerHelper.getInstance()
                            .getMetaRegistry()
                            .metaClassCreationHandle = new ExpandoMetaClassCreationHandle();

            Class pluginClass = gcl.parseClass(resolver.getResource("file:JcrGrailsPlugin.groovy").inputStream)

            def factory = new JcrSessionFactory(repository:new TransientRepository(),
                                                credentials: new SimpleCredentials("user", "".toCharArray()))
            factory.afterPropertiesSet()
            appCtx.registerMockBean("jcrSessionFactory", factory )
            appCtx.registerMockBean("jcrTemplate", new JcrTemplate(sessionFactory:appCtx.getBean("jcrSessionFactory")))

            def plugin = pluginClass.newInstance()


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


            def setupDynamicMethods = plugin.doWithApplicationContext
            ga = new DefaultGrailsApplication(gcl.loadedClasses, gcl)
            setupDynamicMethods.delegate = [application:ga, log:LogFactory.getLog(getClass())]
            setupDynamicMethods(appCtx)

            interceptor.sessionFactory = appCtx.getBean('jcrSessionFactory')
            interceptor.preHandle(request, null,null)

        }

	void tearDown() {
		InvokerHelper.getInstance()
		.getMetaRegistry()
		.setMetaClassCreationHandle(originalHandler);

        interceptor.afterCompletion(request, null,null,null)
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