package org.codehaus.groovy.grails.plugins.jcr

import javax.jcr.*
import org.testng.annotations.Test
import org.testng.annotations.AfterMethod

/**
 * TODO: write javadoc
 *
 * @author Sergey Nebolsin (nebolsin@gmail.com)
 */
class QueryOperationsTests extends AbstractJcrPluginTest {
    Class domainClass

    @Test
    void testFind() {
        def wikiEntry = domainClass.newInstance()

        wikiEntry.id = 1
        wikiEntry.title = "foo"
        wikiEntry.body = "bar"

        wikiEntry.save()

        wikiEntry = domainClass.newInstance()
        wikiEntry.id = 2
        wikiEntry.title = "test"
        wikiEntry.body = "body"
        wikiEntry.save()

        def result = domainClass.find("@title='foo'")
        assertNotNull result
        assertEquals 1, result.id
        assertEquals "foo", result.title
        assertEquals "bar", result.body
    }

    @Test
    void testFindAll() {
        def wikiEntry = domainClass.newInstance()

        wikiEntry.id = 1
        wikiEntry.title = "foo"
        wikiEntry.body = "bar"

        wikiEntry.save()

        wikiEntry = domainClass.newInstance()
        wikiEntry.id = 2
        wikiEntry.title = "test"
        wikiEntry.body = "body"
        wikiEntry.save()

        wikiEntry = domainClass.newInstance()
        wikiEntry.id = 3
        wikiEntry.title = "foo"
        wikiEntry.body = "hehe"
        wikiEntry.save()

        def result = domainClass.findAll("@title='foo'")
        assertNotNull result
        assertEquals 2, result.size()
        assertNotNull result.find { it.id == 1 }
        assertNotNull result.find { it.id == 3 }
    }

    @Test
    void testExecuteQuery() {
        def wikiEntry = domainClass.newInstance()

        wikiEntry.id = 1
        wikiEntry.title = "foo"
        wikiEntry.body = "bar"
        wikiEntry.save()

        wikiEntry = domainClass.newInstance()
        wikiEntry.id = 2
        wikiEntry.title = "test"
        wikiEntry.body = "body"
        wikiEntry.save()

        wikiEntry = domainClass.newInstance()
        wikiEntry.id = 3
        wikiEntry.title = "foo"
        wikiEntry.body = "hehe"
        wikiEntry.save()

        def result = domainClass.executeQuery("//WikiEntry//element(*, nt:unstructured)[@title='foo']")
        assertNotNull result
        assertEquals 2, result.size()
        assertNotNull result.find { it.id == 1 }
        assertNotNull result.find { it.id == 3 }

        result = domainClass.executeQuery("//WikiEntry//element(*, nt:unstructured)")
        assertNotNull result
        assertEquals 3, result.size()
    }

    void registerDomainClasses() {
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
    }

    void init() {
        domainClass = ga.getDomainClass("WikiEntry").getClazz()
    }

    @AfterMethod
    void deleteInstances() {
        domainClass.withSession {Session session ->
            if(session.itemExists("/WikiEntry")) {
                session.getRootNode().getNode("WikiEntry").getNodes().each {
                    it.remove()
                }
                session.save()
            }

        }
    }
}