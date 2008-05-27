/* Copyright 2004-2005 Graeme Rocher
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
 */

import org.springmodules.jcr.jackrabbit.*
import org.springmodules.jcr.*
import javax.jcr.*
import javax.jcr.query.*
import javax.jcr.lock.*
import javax.jcr.version.*
import org.codehaus.groovy.grails.exceptions.*
import org.codehaus.groovy.grails.plugins.jcr.binding.*
import org.codehaus.groovy.grails.plugins.jcr.metaclass.*
import org.codehaus.groovy.grails.commons.GrailsDomainClass
import org.codehaus.groovy.grails.commons.GrailsApplication
import org.springframework.context.ApplicationContext
import org.codehaus.groovy.grails.commons.metaclass.DynamicMethodsExpandoMetaClass
import org.apache.log4j.Logger
import org.springmodules.jcr.support.OpenSessionInViewInterceptor
import org.codehaus.groovy.grails.web.servlet.mvc.GrailsUrlHandlerMapping

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
    def dependsOn = [core: "1.0.2"]
    def loadAfter = ['controllers']

    def doWithSpring = {

        jcrRepository(RepositoryFactoryBean) {
            configuration = "classpath:repository.xml"
            homeDir = "/repo"
        }

        jcrPassword(String, "")
        jcrCharArrayPassword(jcrPassword) {bean ->
            bean.factoryMethod = "toCharArray"
        }
        jcrCredentials(SimpleCredentials, "user", jcrCharArrayPassword)
        jcrSessionFactory(org.springmodules.jcr.JcrSessionFactory) {bean ->
            bean.singleton = true
            repository = jcrRepository
            credentials = jcrCredentials
        }
        jcrTemplate(org.springmodules.jcr.JcrTemplate) {
            sessionFactory = jcrSessionFactory
            allowCreate = true
        }
        
        if (manager?.hasGrailsPlugin("controllers")) {
            jcrOpenSessionInViewInterceptor(OpenSessionInViewInterceptor) {
                sessionFactory = jcrSessionFactory
            }
            grailsUrlHandlerMapping.interceptors << jcrOpenSessionInViewInterceptor
        }
    }

    def doWithDynamicMethods = {ctx ->
        application.domainClasses.each { GrailsDomainClass dc ->
            if(dc.mappingStrategy == "jcr") {
                if(!dc.hasProperty("UUID"))
                    throw new GrailsConfigurationException("JCR Mapped domain class [${dc.name}] must define a property called [UUID], which is used to store the unique id of the obj within the JCR repository")

                def metaClass = new DynamicMethodsExpandoMetaClass(dc.getClazz(), true)
                metaClass.initialize()


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

    private addCommonMethods(GrailsDomainClass dc, GrailsApplication application, ApplicationContext ctx) {
        def metaClass = dc.metaClass
        /**
         * bind(Object, Node) method. Binds the properties of the specified Node onto the properties of the specified
         * Object performing necessary type conversion and so forth
         */
        metaClass.'static'.bind = {Object dest, Node sourceNode ->
            def binder = NodeBinder.createNodeBinder(dest, dest.getClass().getName(), getNamespacePrefix())
            binder.bind(sourceNode)
            dest
        }

        /**
         * getNode(UUID) method. Retrieves a JCR Node from a JCR repository using the repository generated UUID
         */
        metaClass.'static'.getNode = {String uuid ->
            def result = null
            if(uuid) {
                def queryResult = executeQuery("//${getRepositoryName()}[jcr:uuid='$uuid']")
                if(queryResult.nodes.hasNext()) {
                    result = queryResult.nodes.nextNode()
                }
            }
            result
        }

        /**
         * getRepositoryName() dynamic method. Retrieves the repository name of the class including optional
         * name space prefix
         */
        metaClass.'static'.getRepositoryName = {->
            def pfx = delegate.getNamespacePrefix()
            "$pfx:${dc.shortName}"
        }

        /**
         * Generic withSession  { session -> }  method for working with a JCR Session instance
         */
        metaClass.'static'.withSession = {Closure closure ->
            ctx.jcrTemplate.execute({Session session ->
                closure.call(session)
            } as JcrCallback)
        }
    }

    private addNamespaceMethods(GrailsDomainClass dc, GrailsApplication application, ApplicationContext ctx) {
        def theSession = ctx.jcrSessionFactory.session
        def namespaceRegistry = theSession.workspace.namespaceRegistry

        def ns = dc.getPropertyValue('namespace')
        if(ns instanceof String) {
            ns = ns ? [(ns): "http://grails.org/$ns/$version".toString()] : [(ns): "http://grails.org/gorm/$version".toString()]
        }

        if(ns instanceof Map) {
            log.info "Registering name space in JCR Content Repository: $ns"
            def entry = ns.entrySet().iterator().next()
            metaClass.'static'.getNamespacePrefix = {-> entry.key}
            metaClass.'static'.getNamespaceURI = {-> entry.value}
            try {
                namespaceRegistry.getURI(entry.key)
            }
            catch (NamespaceException ne) {
                namespaceRegistry.registerNamespace(entry.key, entry.value)
            }

        }
    }

    private addBasicPersistenceMethods(GrailsDomainClass dc, GrailsApplication application, ApplicationContext ctx) {
        def sessionFactory = ctx.jcrSessionFactory
        def metaClass = dc.metaClass

        /**
         * create(Node) method. Allows the creation of instances by passing a JCR Node instance as an argument
         * Automatic type conversion and data binding occurs from node -> instance
         */
        metaClass.'static'.create = {Node node ->
            def result = null
            if(node) {
                def pfx = getNamespacePrefix()
                result = dc.newInstance()
                result.UUID = node.UUID
                def binder = NodeBinder.createNodeBinder(result, result.getClass().getName(), pfx)
                binder.bind(node)
            }
            // result
            result
        }

        metaClass.'static'.create = {->
            dc.newInstance()
        }

        /**
         * get(UUID) method. Retrieves an instance from a JCR repository using the repository generated UUID
         */
        metaClass.'static'.get = {param ->
            def node
            if(param instanceof String) node = getNode(param)
            else if(param instanceof Node) node = param
            else return null
            create(node)
        }
        
        metaClass.'static'.delete = {->
            def session = sessionFactory.session
            def node = null
            def obj = delegate
            if(obj.UUID) {
                node = getNode(obj.UUID)
                node?.remove()
            }
            session.save()
        }

        /**
         * save() dynamic method. Persists an instance to the JCR repository
         */
        metaClass.save = {->
            withSession { session ->
                def node = null
                def obj = delegate
                // When the node has a UUID get the existing node otherwise add a new versionable node
                boolean isExisting = false
                if(obj.UUID) {
                    node = getNode(obj.UUID)
                    node?.checkout()
                    isExisting = true
                }
                if(!node) {
                    def root = session.rootNode
                    node = root.addNode(getRepositoryName())
                    node.addMixin("mix:versionable")
                    node.addMixin("mix:lockable")
                }

                def binder = NodeBinder.createNodeBinder(node, getRepositoryName(), getNamespacePrefix())
                def values = [:]
                dc.persistantProperties.findAll {p ->
                    !p.isAssociation() && p.name != 'UUID'
                }.each {
                    values[it.name] = obj[it.name]
                }

                binder.bind(values)
                session.save()
                if(isExisting) {
                    node.checkin()
                }
                obj.UUID = node.UUID
            }
        }

    }

    private addQueryMethods(GrailsDomainClass dc, GrailsApplication application, ApplicationContext ctx) {
        def metaClass = dc.metaClass

        /**
         * list() dynamic methods. Returns all instances of this class in the JCR repository
         */
        metaClass.'static'.list = {->
            list(null)
        }

        metaClass.'static'.list = {Map args ->
            if(!args) args = [:]
            def offset = args.offset ? args.offset.toInteger() : 0
            def max = args.max ? args.max.toInteger() : null

            def queryResult = executeQuery("//${getRepositoryName()}")
            def nodeIterator = queryResult.nodes
            def results = []
            if(nodeIterator.size > offset) {
                if(offset > 0) {
                    nodeIterator.skip(offset)
                }
                max = max ? max : nodeIterator.size
                def i = 0
                for(n in nodeIterator) {
                    if(i >= max) break
                    results << create(n)
                    i++
                }
            }
            results
        }

        /**
         * find(String query) dynamic method. Finds and returns the first result of the XPath query or null
         */
        metaClass.'static'.find = {String query ->
            def queryResult = executeQuery(query)
            queryResult.nodes.hasNext() ? create(queryResult.nodes.iterator().next()) : null
        }

        /**
         * findAll(String query) dynamic method. Finds and returns the results of the XPath query or an empty list
         */
        metaClass.'static'.findAll = {String query ->
            def result = []
            if(query) {
                def queryResult = executeQuery(query)
                queryResult.nodes.each {n ->
                    result << create(n)
                }
            }
            else {
                result = list(null)
            }
            result
        }

        /**
         * count() dynamic method. Returns the number of Objects in the JCR repository
         */
        metaClass.'static'.count = {->
            def queryResult = executeQuery("//${getRepositoryName()}")
            def result = 0
            if(queryResult.nodes.hasNext()) {
                result = queryResult.nodes.size
            }
            result
        }

        /**
         * executeQuery(String) dynamic method. Allows the executing of arbitrary XPath queries onto a JCR
         * content repository
         */
        metaClass.'static'.executeQuery = {String query ->
            executeQuery(query, Collections.EMPTY_MAP)
        }

        metaClass.'static'.executeQuery = {String queryClause, Map args ->
            withSession { session ->
                if(log.debugEnabled) log.debug "Attempting to execute query: $queryClause"

                def queryManager = session.workspace.queryManager
                def query
                if(args?.lang == 'sql') query = queryManager.createQuery(queryClause, Query.SQL)
                else query = queryManager.createQuery(queryClause, Query.XPATH)

                query.execute()
            }
        }

    }

    private addDynamicFinderSupport(GrailsDomainClass dc, GrailsApplication application, ApplicationContext ctx) {
        def metaClass = dc.metaClass

        /**
         * Dynamic findBy* method that uses finder expressions to formulate an XPath to be executed against a
         * JCR content repository. Example findByTitleAndReleaseDate
         */
        metaClass.'static'./^(findBy)(\w+)$/ = {matcher, args ->
            def result = null
            def query = dc.getClazz()."queryFor${matcher.group(2)}"(* args)
            def queryResults = executeQuery(query.toString())
            if(queryResults.nodes.hasNext()) {
                result = create(queryResults.nodes.iterator().nextNode())
            }
            result
        }

        /**
         * Dynamic countBy* method that uses finder expressions to formulate an XPath to be executed against a
         * JCR content repository. Example findByTitleAndReleaseDate
         */
        metaClass.'static'./^(countBy)(\w+)$/ = {matcher, args ->
            def result = 0
            def query = dc.getClazz()."queryFor${matcher.group(2)}"(* args)
            def queryResult = executeQuery(query.toString())
            if(queryResult.nodes.hasNext()) {
                result = queryResult.nodes.size
            }
            result
        }

        /**
         * Dynamic findAllBy* method that uses finder expressions to formulate an XPath to be executed against a
         * JCR content repository. Example findAllByTitleAndReleaseDate
         */
        metaClass.'static'./^(findAllBy)(\w+)$/ = {matcher, args ->
            def query = dc.getClazz()."queryFor${matcher.group(2)}"(* args)
            def result = executeQuery(query.toString())
            def results = []
            if(result.nodes.hasNext()) {
                result.nodes.each {node ->
                    results << create(node)
                }
            }
            results
        }

        /**
         * Dynamic queryFor* method. Returns a String query for given finder expression. Example queryForTitleAndReleaseDate
         */
        metaClass.'static'./^(queryFor)(\w+)$/ = {matcher, args ->
            def method = new ClosureInvokingXPathFinderMethod(~/^(queryFor)(\w+)$/,
                    application,
                    getNamespacePrefix()) {methodName, arguments, expressions, operator ->
                def query = new StringBuffer("//${getRepositoryName()}")

                // begin predicate
                query << "["
                if(expressions.size == 1) {
                    query << expressions.iterator().next().criterion.toString()
                }
                else {
                    def criterions = expressions.criterion.collect {"(${it.toString()})"}
                    query << criterions.join(" $operator ")
                }
                // end predicate
                query << "]"
                query.toString()
            }
            method.invoke(dc.getClazz(), matcher.group(), args)
        }

    }

    private addLockingSupport(GrailsDomainClass dc, GrailsApplication application, ApplicationContext ctx) {
        def metaClass = dc.metaClass

        /**
         * lock(boolean) method. Attempts to obtain a lock on a Node or Object.
         * If a lock cannot be obtained null is returned
         */
        metaClass.lock = {boolean isSessionScoped ->
            def lock = null
            if(delegate.UUID) {
                def node = getNode(delegate.UUID)
                if(!node?.locked) {
                    try {
                        lock = node?.lock(true, isSessionScoped)}
                    catch (LockException e) {
                        log.debug("Lock cannot be obtained on node " + node?.path, e)
                        e.printStackTrace()
                        // ignore
                    }

                }

            }
            // return the lock
            lock
        }
        /**
         * getLock() method. Retrieves the lock held on the current Node, otherwise returns null
         */
        metaClass.getLock = {->
            def node = getNode(delegate.UUID)
            try {
                node?.getLock()
            } catch (LockException e) {
                log.debug("Lock cannot be obtained on node " + node?.path, e)
                // ignore
            }

        }
        /**
         * unlock() method. Removes the lock held on the current node, or returns null
         * */
        metaClass.unlock = {->
            if(delegate.properties['UUID']) {
                def node = getNode(delegate.UUID)
                if(node?.locked)
                    node.unlock()
            }
        }
        /**
         * isLocked() method. Returns true if there is a lock on the specified object's Node
         */
        metaClass.isLocked = {->
            def node = getNode(delegate.UUID)
            node?.isLocked()
        }

    }

    private addVersioningSupport(GrailsDomainClass dc, GrailsApplication application, ApplicationContext ctx) {
        def metaClass = dc.metaClass

        /**
         * getVersionHistory() method. Returns the JCR VersionHistory for this object
         * */
        metaClass.getVersionHistory = {->
            def node = getNode(delegate.UUID)
            node?.getVersionHistory()
        }
        /**
         * eachVersion    {    v->    }    method. Allows iteration over each version of this object. Invoking the specified
         * closure on each iteration
         * */
        metaClass.eachVersion = {Closure callable ->
            getVersionHistory().allVersions.each {v ->
                callable(v)
            }
        }
        /**
         * findVersion    {    v->    }    method. Iterates over each version of the VersionHistory and returns the first one that
         * matches the specified predicate (closure that returns a boolean)
         * */
        metaClass.findVersion = {Closure callable ->
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
         * */
        metaClass.getBaseVersion = {->
            def node = getNode(delegate.UUID)
            node?.getBaseVersion()
        }
        /**
         * restore(Version vesrion) method. Restores the object to the version represented by the JCR Version instance
         * Note that this method will NOT remove existing versions
         * */
        metaClass.restore = {Version v ->
            def node = getNode(delegate.UUID)
            node.restore(v, false)
            bind(delegate, node)
        }
        metaClass.restore = {Version v, boolean removeExisting ->
            def node = getNode(delegate.UUID)
            node.restore(v, removeExisting)
            bind(delegate, node)
        }
    }


    def doWithApplicationContext = {ctx ->

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
