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


        void testFindAll() {
                def wikiEntryClass =  ga.getDomainClass("WikiEntry").getClazz()
                def wikiEntry1 =  ga.getDomainClass("WikiEntry").newInstance()
                def wikiEntry2 =  ga.getDomainClass("WikiEntry").newInstance()

                wikiEntry1.title = "fred"
                wikiEntry1.body = "flintstone"


                wikiEntry2.title = "wilma"
                wikiEntry2.body = "flintstone"

                wikiEntry1.save()
                wikiEntry2.save()

                def results  =  wikiEntryClass.findAll("//wiki:WikiEntry[@wiki:title = 'fred']")

                assert results.nodes.find { it.title =='wilma' }
                assert results.nodes.find { it.title =='fred' }

        }

        void testFindByMethod() {
                def wikiEntryClass =  ga.getDomainClass("WikiEntry").getClazz()
                def wikiEntry =  ga.getDomainClass("WikiEntry").newInstance()

                wikiEntry.title = "foo"
                wikiEntry.body = "bar"

                wikiEntry.save()

                wikiEntry = null

                wikiEntry = wikiEntryClass.findByTitle("foo")

                assert wikiEntry

                wikiEntry = wikiEntryClass.findByTitleAndBody("foo", "bar")

                assert wikiEntry

        }
        void testSave() {
                def wikiEntryClass =  ga.getDomainClass("WikiEntry").getClazz()
                def wikiEntry =  ga.getDomainClass("WikiEntry").newInstance()

                wikiEntry.title = "foo"
                wikiEntry.body = "bar"

                wikiEntry.save()

                assert wikiEntry.UUID != null

                wikiEntryClass.withSession { session ->
                    def qm = session.workspace.queryManager
                    def query = qm.createQuery("//wiki:WikiEntry[@wiki:title='foo']", Query.XPATH)

                    def result = query.execute()

                    assert result.nodes.hasNext()

                    def saved = result.nodes.nextNode()
                    println saved.getProperties()
                    assertEquals "foo", saved.getProperty("wiki:title").getString()
                    assertEquals "bar", saved.getProperty("wiki:body").getString()
                }

         }

	    void testList() {
                def wikiEntryClass =  ga.getDomainClass("WikiEntry").getClazz()
                def wikiEntry =  ga.getDomainClass("WikiEntry").newInstance()
                def wikiEntry2 =  ga.getDomainClass("WikiEntry").newInstance()

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
                def wikiEntryClass =  ga.getDomainClass("WikiEntry").getClazz()

                wikiEntryClass.withSession { session ->
                        assert session != null        
                }

        }

        void testNamespaceInfo() {
                def wikiEntryClass =  ga.getDomainClass("WikiEntry").getClazz()

                assertEquals "wiki", wikiEntryClass.getNamespacePrefix()
                assertEquals "http://grails.org/wiki/0.1", wikiEntryClass.getNamespaceURI()

        }


         void testGet() {
                def wikiEntryClass =  ga.getDomainClass("WikiEntry").getClazz()
                def wikiEntry =  ga.getDomainClass("WikiEntry").newInstance()

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
                def wikiEntryClass =  ga.getDomainClass("WikiEntry").getClazz()
                def wikiEntry =  ga.getDomainClass("WikiEntry").newInstance()

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
                def wikiEntryClass =  ga.getDomainClass("WikiEntry").getClazz()
                def wikiEntry =  ga.getDomainClass("WikiEntry").newInstance()

                wikiEntry.title = "fred"
                wikiEntry.body = "flintstone"

                wikiEntry.save()

                wikiEntry = null

                wikiEntry =  wikiEntryClass.find("//wiki:WikiEntry[@wiki:title = 'fred']")

                assert wikiEntry != null

                assertEquals "fred", wikiEntry.title
                assertEquals "flintstone", wikiEntry.body

        }



        void testLockAndUnlock() {
                def wikiEntryClass =  ga.getDomainClass("WikiEntry").getClazz()
                def wikiEntry =  ga.getDomainClass("WikiEntry").newInstance()

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