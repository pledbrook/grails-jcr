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
import org.testng.annotations.Test
import org.testng.annotations.BeforeClass
import org.testng.annotations.AfterClass
import org.testng.annotations.AfterMethod

/**
 * TODO: write javadoc
 *
 * @author Sergey Nebolsin (nebolsin@gmail.com)
 */
class AbstractJcrPluginTest extends GroovyTestCase {
    protected interceptor = new OpenSessionInViewInterceptor()
    protected request = new MockHttpServletRequest()
    protected Repository repository = null
    protected gcl = new GroovyClassLoader()
    protected appCtx = new MockApplicationContext()
    protected resolver = new PathMatchingResourcePatternResolver()
    protected originalHandler = null
    protected GrailsApplication ga = null

    @BeforeClass
    void configureEnvironment() {

        ExpandoMetaClass.enableGlobally()
        super.setUp()

        gcl.parseClass("""\
import org.apache.jackrabbit.ocm.mapper.impl.annotation.Field;
import org.apache.jackrabbit.ocm.mapper.impl.annotation.Node;

@Node(jcrMixinTypes="mix:referenceable, mix:versionable")
class WikiEntry {
   static mapWith = 'jcr'
   static namespace = 'wiki'

   @Field(id=true) Long id
   String version

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

    }

    @AfterClass
    void clearEnvironment() {
        interceptor.afterCompletion(request, null, null, null)
        repository.shutdown()
        ApplicationHolder.application = null
        ExpandoMetaClass.disableGlobally()
    }
}