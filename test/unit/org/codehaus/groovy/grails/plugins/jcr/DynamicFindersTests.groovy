package org.codehaus.groovy.grails.plugins.jcr

import javax.jcr.*
import org.testng.annotations.Test
import org.testng.annotations.AfterMethod
import org.testng.annotations.BeforeMethod

/**
 * TODO: write javadoc
 *
 * @author Sergey Nebolsin (nebolsin@gmail.com)
 */
class DynamicFindersTests extends AbstractJcrPluginTest {
    Class domainClass

    @Test
    void testFind() {
        def result = domainClass.findByTitle("title3")
        assertNotNull result
        assertEquals 3, result.id

        result = domainClass.findByTitleAndBody("title4", "body4")
        assertNotNull result
        assertEquals 4, result.id

        result = domainClass.findByTitleAndBody("title4", "body2")
        assertNull result
    }

    void registerDomainClasses() {
        gcl.parseClass("""\
class WikiEntry {
   static mapWith = 'jcr'
   static namespace = 'wiki'

   Long id
   String version

   String path
   String UUID
   String title
   String body
   Integer   age
}
""")
    }

    void init() {
        domainClass = ga.getDomainClass("WikiEntry").getClazz()
    }

    @BeforeMethod
    void populateWikiEntries() {
        (0..9).each {
            def wikiEntry = domainClass.newInstance()
            wikiEntry.id = it
            wikiEntry.title = "title$it"
            wikiEntry.body = "body$it"
            wikiEntry.age = 20 + it
            wikiEntry.save()
        }
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