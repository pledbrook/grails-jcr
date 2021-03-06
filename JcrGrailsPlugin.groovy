/*
 * Copyright 2006-2008 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

import javax.jcr.*
import javax.jcr.version.*
import org.codehaus.groovy.grails.exceptions.*
import org.codehaus.groovy.grails.plugins.jcr.JcrConstants
import org.codehaus.groovy.grails.commons.GrailsDomainClass
import org.codehaus.groovy.grails.commons.GrailsApplication
import org.springframework.context.ApplicationContext
import org.codehaus.groovy.grails.commons.metaclass.DynamicMethodsExpandoMetaClass
import org.apache.log4j.Logger
import org.springmodules.jcr.support.OpenSessionInViewInterceptor
import org.springframework.transaction.support.TransactionTemplate
import org.springframework.transaction.support.TransactionCallback
import org.codehaus.groovy.grails.plugins.jcr.JcrConfigurator
import org.codehaus.groovy.grails.commons.GrailsClassUtils as GCU
import org.codehaus.groovy.grails.plugins.jcr.mapping.JcrMapperFactoryBean
import org.apache.jackrabbit.ocm.manager.ObjectContentManager
import org.apache.jackrabbit.ocm.reflection.ReflectionUtils
import org.apache.jackrabbit.ocm.query.Query
import org.codehaus.groovy.grails.plugins.jcr.exceptions.GrailsRepositoryException
import org.codehaus.groovy.grails.plugins.jcr.query.QueryConstructor
import org.codehaus.groovy.grails.plugins.jcr.mapping.PaginationObjectIterator

/**
 * A plugin for the Grails framework (http://grails.org) that provides an ORM layer onto the
 * Java Content Repository (JCR) specification.
 *
 * The plugin detects Grails domain classes that have the static property mappedBy='jcr' and configures
 * dynamic methods that interact with a Apache JackRabbit repository via Spring
 *
 * @author Graeme Rocher
 * @author Sergey Nebolsin
 *
 * @since 0.4
 *
 *        <p/>
 *        Created: Feb 9, 2007
 *        Time: 5:45:29 PM
 */
class JcrGrailsPlugin {
    static final def log = Logger.getLogger(JcrGrailsPlugin.class)
    def version = "0.2-SNAPSHOT"
    def author = "Sergey Nebolsin"
    def authorEmail = "nebolsin@gmail.com"
    def title = "This plugin provides JCR-based persistence for Grails"
    def documentation = "http://grails.org/JCR+plugin"

    def dependsOn = [core: "1.0.2 > *"]
    def loadAfter = ['controllers']

    def doWithSpring = {
        if(manager?.hasGrailsPlugin("controllers")) {
            jcrOpenSessionInViewInterceptor(OpenSessionInViewInterceptor) {
                sessionFactory = ref('jcrSessionFactory')
            }
            grailsUrlHandlerMapping.interceptors << jcrOpenSessionInViewInterceptor
        }

        def classes = []
        application.domainClasses.each {GrailsDomainClass dc ->
            if(dc.mappingStrategy == "jcr") {
                classes << dc.clazz
            }
        }

        ReflectionUtils.classLoader = application.classLoader

        jcrMapper(JcrMapperFactoryBean) {
            mappedClasses = classes
        }

        jcrOcmTemplate(JcrOcmTemplate) {
            jcrMapper = ref('jcrMapper')
            jcrSessionFactory = ref('jcrSessionFactory')
        }
    }

    def doWithApplicationContext = {ctx ->
        if(!ctx.containsBean("jcrSessionFactory")) {
            throw new GrailsConfigurationException("Grails JCR plugin cannot be used without an implementation plugin, for example Grails JackRabbit plugin")
        }

        // create parent nodes for all domain classes
        application.domainClasses.each {GrailsDomainClass dc ->
            dc.clazz.withSession {Session session ->
                def path = dc.clazz.getDomainPath()[1..-1]
                if(!session.getRootNode().hasNode(path)) {
                    println "Creating base Domain Class node for class: ${dc.shortName}"
                    session.getRootNode().addNode(path)
                    session.save()
                }
            }
        }
    }

    def doWithDynamicMethods = {ctx ->
        application.domainClasses.each {GrailsDomainClass dc ->
            if(dc.mappingStrategy == "jcr") {
                if(!dc.hasProperty("UUID")) {
                    throw new GrailsConfigurationException("""JCR Mapped domain class [${dc.name}] must define \
                        a property called [UUID], which is used to store the unique id of the obj within \
                        the JCR repository""")
                }

                def mc = new DynamicMethodsExpandoMetaClass(dc.getClazz(), true)
                mc.initialize()

                registerDynamicMethods(dc, application, ctx)

            }
        }
    }

    private registerDynamicMethods(GrailsDomainClass dc, GrailsApplication application, ApplicationContext ctx) {
        addCommonMethods(dc, application, ctx)
        addNamespaceMethods(dc, application, ctx)
        addBasicPersistenceMethods(dc, application, ctx)
        addQueryMethods(dc, application, ctx)
        addDynamicFinderSupport(dc, application, ctx)
        addLockingSupport(dc, application, ctx)
        addVersioningSupport(dc, application, ctx)
    }

    private void addCommonMethods(GrailsDomainClass dc, GrailsApplication application, ApplicationContext ctx) {
        def mc = dc.metaClass

        def configuration = JcrConfigurator.readConfiguration(dc)

        mc.'static'.getGrailsJcrMapping = {->
            configuration
        }

        mc.'static'.getDomainPath = {->
            "/$dc.shortName"
        }

        /**
         * Generic withSession { session -> } method for working with a JCR Session instance
         */
        mc.'static'.withSession = {Closure closure ->
            closure.delegate = delegate
            withOcm {ocm ->
                closure.call(ocm.session)
            }
        }

        mc.'static'.withOcm = {Closure closure ->
            closure.delegate = delegate
            ctx.jcrOcmTemplate.execute({ObjectContentManager ocm ->
                closure.call(ocm)
            })
        }
    }

    private void addTransactionSupport(GrailsDomainClass dc, GrailsApplication application, ApplicationContext ctx) {
        def mc = dc.metaClass

        mc.'static'.withTransaction = {Closure callable ->
            new TransactionTemplate(ctx.jcrTransactionManager).execute({status ->
                callable.call(status)
            } as TransactionCallback)
        }
    }

    private void addNamespaceMethods(GrailsDomainClass dc, GrailsApplication application, ApplicationContext ctx) {
        def mc = dc.metaClass

        def ns = dc.getPropertyValue('namespace')
        if(ns instanceof String) {
            ns = ns ? [(ns): "http://grails.org/$ns/".toString()] : [(ns): "http://grails.org/gorm/".toString()]
        }

        if(ns instanceof Map) {
            def entry = ns.entrySet().iterator().next()
            mc.'static'.ensureNamespaceIsRegistered = {
                withSession {session ->
                    log.info "Registering name space in JCR Content Repository: $ns"
                    NamespaceRegistry namespaceRegistry = session.workspace.namespaceRegistry
                    try {
                        namespaceRegistry.getURI(JcrConstants.GRAILS_NAMESPACE_KEY)
                    } catch (NamespaceException ne) {
                        namespaceRegistry.registerNamespace(JcrConstants.GRAILS_NAMESPACE_KEY, JcrConstants.GRAILS_NAMESPACE_URI)
                    }
                    try {
                        namespaceRegistry.getURI(entry.key)
                    } catch (NamespaceException ne) {
                        namespaceRegistry.registerNamespace(entry.key, entry.value)
                    }
                }
            }

            mc.'static'.getNamespacePrefix = {->
                ensureNamespaceIsRegistered()
                mc.'static'.getNamespacePrefix = {-> entry.key}
                entry.key
            }

            mc.'static'.getNamespaceURI = {->
                ensureNamespaceIsRegistered()
                mc.'static'.getNamespaceURI = {-> entry.value}
                entry.value
            }
        }
    }

    private void addBasicPersistenceMethods(GrailsDomainClass dc, GrailsApplication application, ApplicationContext ctx) {
        def mc = dc.metaClass

        mc.delete = {->
            withOcm {ObjectContentManager ocm ->
                ocm.remove(delegate.path)
                ocm.save()
            }
        }

        mc.save = {->
            withOcm {ObjectContentManager ocm ->
                if(delegate.path) {
                    ocm.checkout delegate.path
                    ocm.update delegate
                    ocm.save()
                    ocm.checkin delegate.path
                } else {
                    // TODO: implement path creation
                    delegate.path = "${getDomainPath()}/${delegate.id}"
                    ocm.insert delegate
                    ocm.save()
                    ocm.checkin delegate.path
                }
            }
        }

        /**
         * Domain.get(String path) dynamic method. Returns domain object by path.
         */
        mc.'static'.get = {String path ->
            withOcm {ObjectContentManager ocm ->
                ocm.getObject(dc.clazz, path)
            }
        }

        /**
         * Domain.getByUUID(String uuid) dynamic method. Returns domain object by UUID.
         */
        mc.'static'.getByUUID = {String uuid ->
            withOcm {ObjectContentManager ocm ->
                ocm.getObjectByUuid(uuid)
            }
        }

        /**
         * Domain.list() dynamic methods. Returns all instances of this class in the JCR repository.
         */
        mc.'static'.list = {->
            list(null)
        }

        /**
         * Domain.list(Map args) dynamic methods. Returns all instances of this class in the JCR repository.
         * with respect to 'offset' and 'max' arguments in args.
         */
        mc.'static'.list = {Map args ->
            getObjectIterator(args).toList()
        }


        /**
         * Domain.count() dynamic method. Returns the number of Objects in the JCR repository
         */
        mc.'static'.count = {->
            def iterator = getObjectIterator(null)
            iterator.size
        }

        mc.'static'.getObjectIterator = {Map args ->
            withOcm {ObjectContentManager ocm ->
                Query query = new QueryConstructor(ocm.queryManager, dc).createQuery(args)
                return getObjectIterator(query, args)
            }
        }

        mc.'static'.getObjectIterator = {Query query, Map args ->
            withOcm {ObjectContentManager ocm ->
                return new PaginationObjectIterator(ocm.getObjectIterator(query), args?.offset?.toInteger(), args?.max?.toInteger())
            }
        }

        mc.'static'.getObjectIterator = {String query, Map args ->
            withOcm {ObjectContentManager ocm ->
                return new PaginationObjectIterator(
                        ocm.getObjectIterator(
                                query,
                                args?.lang == 'sql' ? javax.jcr.query.Query.SQL : javax.jcr.query.Query.XPATH
                        ), args?.offset?.toInteger(), args?.max?.toInteger()
                )
            }
        }
    }

    private void addQueryMethods(GrailsDomainClass dc, GrailsApplication application, ApplicationContext ctx) {
        def mc = dc.metaClass


        mc.'static'.find = {String queryString -> find(queryString, null) }

        /**
         * find(String query) dynamic method. Finds and returns the first result of the XPath query or null
         */
        mc.'static'.find = {String queryString, Map args ->
            withOcm {ObjectContentManager ocm ->
                Query query = new QueryConstructor(ocm.getQueryManager(), dc).createJCRExpressionQuery(queryString, args)
                def iterator = ocm.getObjectIterator(query)
                iterator.hasNext() ? iterator.next() : null
            }
        }


        mc.'static'.findAll = {String queryString -> findAll(queryString, null) }
        
        /**
         * findAll(String query) dynamic method. Finds and returns the results of the XPath query or an empty list
         */
        mc.'static'.findAll = {String queryString, Map args ->
            withOcm {ObjectContentManager ocm ->
                Query query = new QueryConstructor(ocm.getQueryManager(), dc).createJCRExpressionQuery(queryString, args)
                getObjectIterator(query, args).toList()
            }
        }


        mc.'static'.executeQuery = {String query ->
            executeQuery(query, Collections.EMPTY_MAP)
        }

        mc.'static'.executeQuery = {String queryClause, Map args ->
            withOcm {ObjectContentManager ocm ->
                if(log.debugEnabled) log.debug "Attempting to execute query: $queryClause"
                getObjectIterator(queryClause, args).toList()
            }
        }
    }

    private void addLockingSupport(GrailsDomainClass dc, GrailsApplication application, ApplicationContext ctx) {
        def mc = dc.metaClass

        mc.lock = {->
            lock(true)
        }

        /**
         * lock(boolean) method. Attempts to obtain a lock on a Node or Object.
         * If a lock cannot be obtained null is returned
         */
        mc.lock = {boolean isSessionScoped ->
            withOcm {ObjectContentManager ocm ->
                if(ocm.isLocked(delegate.path)) {
                    return null
                } else {
                    try {
                        return ocm.lock(delegate.path, true, isSessionScoped)
                    } catch (Exception e) {
                        throw new GrailsRepositoryException("Lock cannot be obtained on node ${delegate.path}", e)
                    }

                }
            }
        }

        /**
         * unlock() method. Removes the lock held on the current node, or returns null
         * */
        mc.unlock = {->
            withOcm {ObjectContentManager ocm ->
                if(!ocm.isLocked(delegate.path)) {
                    return
                } else {
                    try {
                        ocm.unlock(delegate.path, null)
                    } catch (Exception e) {
                        throw new GrailsRepositoryException("Cannot unlock node ${delegate.path}", e)
                    }
                }
            }
        }

        /**
         * isLocked() method. Returns true if there is a lock on the specified object's Node
         */
        mc.isLocked = {->
            withOcm {ObjectContentManager ocm ->
                ocm.isLocked(delegate.path)
            }
        }
    }

    private addDynamicFinderSupport(GrailsDomainClass dc, GrailsApplication application, ApplicationContext ctx) {
        def mc = dc.metaClass

        /**
         * Dynamic findBy* method that uses finder expressions to formulate an XPath to be executed against
         * a JCR content repository. Example findByTitleAndReleaseDate
         */
        mc.'static'./^(findBy)(\w+)$/ = {matcher, args ->
            def query = dc.getClazz()."queryFor${matcher.group(2)}"(* args)
            def iterator = getObjectIterator(query, null)
            iterator.hasNext() ? iterator.next() : null
        }

        /**
         * Dynamic countBy* method that uses finder expressions to formulate an XPath to be executed against
         * a JCR content repository. Example findByTitleAndReleaseDate
         */
        mc.'static'./^(countBy)(\w+)$/ = {matcher, args ->
            def query = dc.getClazz()."queryFor${matcher.group(2)}"(* args)
            def iterator = getObjectIterator(query, null)
            iterator.size()
        }

        /**
         * Dynamic findAllBy* method that uses finder expressions to formulate an XPath to be executed against
         * a JCR content repository. Example findAllByTitleAndReleaseDate
         */
        mc.'static'./^(findAllBy)(\w+)$/ = {matcher, args ->
            def query = dc.getClazz()."queryFor${matcher.group(2)}"(* args)
            def iterator = getObjectIterator(query, null)
            iterator.toList()
        }

        /**
         * Dynamic queryFor* method. Returns a Query for given finder expression.
         * Example queryForTitleAndReleaseDate
         */
        mc.'static'./^(queryFor)(\w+)$/ = {matcher, args ->
            withOcm { ObjectContentManager ocm ->
                new QueryConstructor(ocm.getQueryManager(), dc).createDynamicQuery(matcher.group(2), args.toList(), null)
            }
        }
    }


    private addVersioningSupport(GrailsDomainClass dc, GrailsApplication application, ApplicationContext ctx) {
        def mc = dc.metaClass

        /**
         * getVersionHistory() method. Returns the JCR VersionHistory for this object
         */
        mc.getVersionHistory = {->
            def node = getNode(delegate.UUID)
            node?.getVersionHistory()
        }

        /**
         * eachVersion { version -> } method. Allows iteration over each version of this object. Invoking the specified
         * closure on each iteration
         */
        mc.eachVersion = {Closure callable ->
            getVersionHistory().allVersions.each {v ->
                callable(v)
            }
        }

        /**
         * findVersion { v -> } method. Iterates over each version of the VersionHistory and returns the first one that
         * matches the specified predicate (closure that returns a boolean)
         */
        mc.findVersion = {Closure callable ->
            def versions = getVersionHistory().allVersions
            def result = null
            for(version in versions) {
                if(callable(version)) {
                    result = version
                    break
                }
            }
            // return version
            result
        }

        /**
         * getBaseVersion() method. Returns the JCR Version instance that represents the base Version of this object
         */
        mc.getBaseVersion = {->
            def node = getNode(delegate.UUID)
            node?.getBaseVersion()
        }

        /**
         * restore(Version vesrion) method. Restores the object to the version represented by the JCR Version instance
         * Note that this method will NOT remove existing versions
         */
        mc.restore = {Version v ->
            def node = getNode(delegate.UUID)
            node.restore(v, false)
            bind(delegate, node)
        }

        mc.restore = {Version v, boolean removeExisting ->
            def node = getNode(delegate.UUID)
            node.restore(v, removeExisting)
            bind(delegate, node)
        }
    }


    def onChange = {event ->
        def configurator = event.ctx.grailsConfigurator
        def application = event.application
        def manager = event.manager

        assert configurator
        assert application
        assert manager

        if(GCU.isDomainClass(event.source)) {
            // refresh whole application
            application.refresh()
            // rebuild context
            configurator.reconfigure(event.ctx, manager.servletContext, false)
        }
    }
}
