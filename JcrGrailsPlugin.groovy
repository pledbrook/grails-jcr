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
import org.springmodules.beans.factory.config.MapToPropertiesFactoryBean;
import org.codehaus.groovy.grails.commons.metaclass.*
import org.codehaus.groovy.grails.exceptions.*   
import org.codehaus.groovy.grails.jcr.binding.* 
import org.codehaus.groovy.grails.jcr.metaclass.*
    
/**
 * A plugin for the Grails framework (http://grails.org) that provides an ORM layer onto the 
 * Java Content Repository (JCR) specification.
 *
 * The plugin detects Grails domain classes that have the static property mappedBy='jcr' and configures
 * dynamic methods that interact with a Apache JackRabbit repository via Spring
 *
 * @author Graeme Rocher
 * @since 0.4
 *
 *        <p/>
 *        Created: Feb 9, 2007
 *        Time: 5:45:29 PM
 */
class JcrGrailsPlugin {
	def version = 0.1
	def dependsOn = [core:0.4]
	def evict = ['hibernate']

	def doWithSpring = {
		
        jcrRepository(RepositoryFactoryBean) {
            configuration = "classpath:repository.xml"
            homeDir = "/repo"
        }

        jcrPassword(String, "")
        jcrCharArrayPassword(jcrPassword) { bean ->
            bean.factoryMethod = "toCharArray"
        }
	    jcrCredentials(SimpleCredentials, "user", jcrCharArrayPassword)
        jcrSessionFactory(org.springmodules.jcr.JcrSessionFactory) { bean ->
            bean.singleton = true        
            repository = jcrRepository
            credentials = jcrCredentials
        }
        jcrTemplate(org.springmodules.jcr.JcrTemplate) {
            sessionFactory = jcrSessionFactory
            allowCreate = true
        }
	}

	def doWithApplicationContext = { ctx ->

        def theSession = ctx.jcrSessionFactory.session
        def namespaceRegistry = theSession.workspace.namespaceRegistry

		application.domainClasses.each { domainClass ->
		    if(domainClass.mappingStrategy == "jcr") {              
				if(!domainClass.hasProperty("UUID"))
					throw new GrailsConfigurationException("JCR Mapped domain class [${domainClass.name}] must define a property called [UUID], which is used to store the unique id of the obj within the JCR repository")
					
				//def metaClass = domainClass.metaClass
				def metaClass = new DynamicMethodsExpandoMetaClass(domainClass.getClazz(), true)
				metaClass.initialize()
				
	              def ns = domainClass.getPropertyValue('namespace')				  
	              if(ns instanceof String) {
                       ns = ns ? [(ns):"http://grails.org/$ns/$version".toString()] : [(ns):"http://grails.org/gorm/$version".toString()]
                  }


                  if(ns instanceof Map) {
                      log.info "Registering name space in JCR Content Repository: $ns"
                      def entry = ns.entrySet().iterator().next()  
				  	  metaClass.'static'.getNamespacePrefix = {-> entry.key }
				  	  metaClass.'static'.getNamespaceURI = {-> entry.value }				
                      try {
                          namespaceRegistry.getURI(entry.key)
                      }
                      catch(NamespaceException ne) {
                            namespaceRegistry.registerNamespace(entry.key, entry.value)
                      }

                  }
                   
				  /**
				   *  save() dynamic method. Persists an instance to the JCR repository
				   **/
                  metaClass.save = {->  
				    def node = null 
				    def obj = delegate
				    withSession { session -> 
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

			             def binder = NodeBinder.createNodeBinder(node, getRepositoryName(), getNamespacePrefix() )
						 def values = [:]
			             def props = domainClass.persistantProperties.findAll { p -> !p.isAssociation() && p.name!='UUID' }
						 props.each { values[it.name] = obj[it.name] }                        
						
			      		 binder.bind(values) 
   						 session.save()
			  			 if(isExisting) { 
				            node.checkin()
						 }	              
						 obj.UUID = node.UUID										
					}
                  }  
                     
				  /**
				   *  getRepositoryName() dynamic method. Retrieves the repository name of the class including optional
				   *  name space prefix
				   **/
				  metaClass.'static'.getRepositoryName = {->
						def pfx = getNamespacePrefix()
						def repoName = "$pfx:${domainClass.shortName}"
				  }   
				  /**
				   *  Generic withSession { session -> } method for working with a JCR Session instance
				   **/
                  metaClass.'static'.withSession = { Closure c ->                        
                        ctx.jcrTemplate.execute( { Session session ->
                            c.call(session)
                        } as JcrCallback )
                  }
				  /**
				   *  list() dynamic methods. Returns all instances of this class in the JCR repository
				   **/      
				  metaClass.'static'.list = {->
						list(null)
				  }                               
				  metaClass.'static'.list = {Map args->
					 	if(!args) args = [:]
						def offset = args.offset? args.offset.toInteger() : 0
						def max = args.max? args.max.toInteger() : null              
						
						def queryResult = executeQuery("//${getRepositoryName()}")
						def nodeIterator = queryResult.nodes
						def results = []               						
						if(nodeIterator.size > offset) {
							if(offset>0) {
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
						// return results
						results
				  }   
				  /**
				   * find(String query) dynamic method. Finds and returns the first result of the XPath query or null
				   **/				   				
				  metaClass.'static'.find = {String query->
						def queryResult = executeQuery(query)
						def result = queryResult.nodes.hasNext() ? create(queryResult.nodes.iterator().next()) : null
				  }  
				  /**
				   * findAll(String query) dynamic method. Finds and returns the results of the XPath query or an empty list
				   **/				   				
				  metaClass.'static'.findAll = {String query->
						def result = []					
						if(query) {
							def queryResult = executeQuery(query)
							queryResult.nodes.each { n ->
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
				   **/				   				
				  metaClass.'static'.count = {->
						def queryResult = executeQuery("//${getRepositoryName()}")
						def count = 0
						if(queryResult.nodes.hasNext()) {
							count = queryResult.nodes.size()
						}                                   
						count
				  }					
				  /**
				   *  executeQuery(String) dynamic method. Allows the executing of arbitrary XPath queries onto a JCR 
				   *  content repository
				   **/				   
				  metaClass.'static'.executeQuery = {String query->
                          executeQuery(query, Collections.EMPTY_MAP)
				  }     
				  metaClass.'static'.executeQuery = {String query, Map args->
						 withSession { session ->     
								if(log.isDebugEnabled())
									log.debug "Attempting to execute query: $query"
									
								def queryManager = session.workspace.queryManager
								def q
								if(args?.lang == 'sql')
									q = queryManager.createQuery(query, Query.SQL)
								else   
									q = queryManager.createQuery(query, Query.XPATH)
								
								q.execute()
						 }					
				  } 
	   			  /**
				   * Dynamic findBy* method that uses finder expressions to formulate an XPath to be executed against a
				   * JCR content repository. Example findByTitleAndReleaseDate
				   * */  		  
				  metaClass.'static'./^(findBy)(\w+)$/ = { matcher, args ->
						def query = domainClass.getClazz()."queryFor${matcher.group(2)}"(*args)
						def result = executeQuery(query.toString())
						def obj = null
						if(result.nodes.hasNext()) {
						   obj = create(result.nodes.iterator().nextNode()) 
						}             
						obj
				  }  
	   			  /**
				   * Dynamic countBy* method that uses finder expressions to formulate an XPath to be executed against a
				   * JCR content repository. Example findByTitleAndReleaseDate
				   * */  		  
				  metaClass.'static'./^(countBy)(\w+)$/ = { matcher, args ->
						def query = domainClass.getClazz()."queryFor${matcher.group(2)}"(*args)
						def queryResult = executeQuery(query.toString())
						def count = 0
						if(queryResult.nodes.hasNext()) {
                               count = queryResult.nodes.size()
						}             
						count
				  }				   
	   			  /**
				   * Dynamic findAllBy* method that uses finder expressions to formulate an XPath to be executed against a
				   * JCR content repository. Example findAllByTitleAndReleaseDate
				   * */				
				  metaClass.'static'./^(findAllBy)(\w+)$/ = { matcher, args ->
						def query = domainClass.getClazz()."queryFor${matcher.group(2)}"(*args)
						def result = executeQuery(query.toString())
						def results = []
						if(result.nodes.hasNext()) {
						   result.nodes.each { node ->
						   		results << create(node) 
						   }						   
						}             
						results
					
				  }
	   			  /**
				   * Dynamic queryFor* method. Returns a String query for given finder expression. Example queryForTitleAndReleaseDate
				   * */				
				  metaClass.'static'./^(queryFor)(\w+)$/ = { matcher, args ->					
						def method = new ClosureInvokingXPathFinderMethod(~/^(queryFor)(\w+)$/,
																			application, 
																			getNamespacePrefix()) { methodName, arguments, expressions, operator ->
							 def query = new StringBuffer("//${getRepositoryName()}")
							
							 // begin predicate
							query << "["
								if(expressions.size() == 1) {
								   query << expressions.iterator().next().criterion.toString()
								}                                                   
								else {
								   def criterions = expressions.criterion.collect { "(${it.toString()})"}
								   query << criterions.join(" $operator ")                	
								}
							// end predicate
							query << "]" 
							query.toString()     
						}       
						method.invoke(domainClass.getClazz(), matcher.group(), args)					
				  }   
				  /**
				   * create(Node) method. Allows the creation of instances by passing a JCR Node instance as an argument
				   * Automatic type conversion and data binding occurs from node -> instance
				   **/				
				  metaClass.'static'.create = { Node node ->
						def obj
						if(node) {                
							def pfx = getNamespacePrefix() 							
							obj = domainClass.newInstance()
							obj.UUID = node.UUID
						   	def binder = NodeBinder.createNodeBinder(obj, obj.getClass().getName(), pfx)
							binder.bind(node)
						}  
						// result  				
						obj
				  }  
				  metaClass.'static'.create = {->
					   domainClass.newInstance()
				  }
				  /**
				   * bind(Object, Node) method. Binds the properties of the specified Node onto the properties of the specified
				   * Object performing necessary type conversion and so forth
				   **/				
				  metaClass.'static'.bind = { Object obj, Node node ->
					   	def binder = NodeBinder.createNodeBinder(obj, obj.getClass().getName(), getNamespacePrefix())
						binder.bind(node) 
						// return obj
						obj   				
				  } 
	   			  /**
				   * get(UUID) method. Retrieves an instance from a JCR repository using the repository generated UUID
				   **/				
				  metaClass.'static'.get = {String uuid->
						def node = getNode(uuid) 
						create(node)  					
				  }    
	   			  /**
				   * get(Node) method. Retrieves an instance from a JCR repository using the repository Node
				   **/				
				  metaClass.'static'.get = {Node node-> 
						create(node)  					
				  }	
				 
	   			  /**
				   * lock(boolean) method. Attempts to obtain a lock on a Node or Object. 
				   * If a lock cannot be obtained null is returned
				   **/				
				  metaClass.lock = {boolean isSessionScoped-> 
					    def lock = null
						if(delegate.UUID) {
							def node = getNode(delegate.UUID) 
							if(!node?.locked) {   
								try {
									lock = node?.lock(true, isSessionScope)}
								catch ( LockException e) { 
									log.debug("Lock cannot be obtained on node " + node?.path, e )
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
				   **/
				  metaClass.getLock = {->
						def node = getNode(delegate.UUID)
						try {
							node?.getLock()
						}
						catch ( LockException e) { 
							log.debug("Lock cannot be obtained on node " + node?.path, e )
							// ignore
						}
											
				  }   
				  /**
				   * unlock() method. Removes the lock held on the current node, or returns null
				   **/				
				  metaClass.unlock = {->
						if(delegate.properties['UUID']) {
							def node = getNode(delegate.UUID)
							if(node?.locked)
								node.unlock()						   
						}					
				  }	 
				  /**
				   * isLocked() method. Returns true if there is a lock on the specified object's Node
				   **/				
				  metaClass.isLocked = {->
						def node = getNode(delegate.UUID)
						node?.isLocked()						   
				  }
				  /**
				   * getVersionHistory() method. Returns the JCR VersionHistory for this object
				   **/				   
				  metaClass.getVersionHistory = {->
						def node = getNode(delegate.UUID)
						node?.getVersionHistory()					
				  }  
				  /**
				   * eachVersion { v-> } method. Allows iteration over each version of this object. Invoking the specified
				   * closure on each iteration
				   **/				
				  metaClass.eachVersion = { Closure callable->
						getVersionHistory().allVersions.each { v ->
							callable(v)
						}											
				  } 
				  /**
				   * findVersion { v-> } method. Iterates over each version of the VersionHistory and returns the first one that
				   * matches the specified predicate (closure that returns a boolean)
				   **/				
				  metaClass.findVersion = {Closure callable->
						def versions = getVersionHistory().allVersions
						def version = null
						for(v in versions) {
							if(callable(v)) {
								version = v
								break
							}
						}	   
						// return version 
						version			
				  }
				  /**
				   * getBaseVersion() method. Returns the JCR Version instance that represents the base Version of this object
				   **/				
				  metaClass.getBaseVersion ={->
						def node = getNode(delegate.UUID)
						node?.getBaseVersion()											
				  }
				  /**
				   * restore(Version vesrion) method. Restores the object to the version represented by the JCR Version instance
				   * Note that this method will NOT remove existing versions
				   **/				  	 
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
				
	   			  /**
				   * getNode(UUID) method. Retrieves a JCR Node from a JCR repository using the repository generated UUID
				   **/				
				  metaClass.'static'.getNode = {String uuid->
					   def node = null
					   if(uuid) {
						   def queryResult = executeQuery("//${getRepositoryName()}[jcr:uuid='$uuid']")
						   if(queryResult.nodes.hasNext()) {
								node = queryResult.nodes.nextNode()
						   }    						
					   }                  
					   node
				 }  
				
            }
	    }
	}
	def onChange = { event ->
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
