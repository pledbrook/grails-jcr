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
    void testFindBy() {
        def result = domainClass.findByTitle("title3")
        assertNotNull result
        assertEquals 3, result.id

        result = domainClass.findByTitleAndBody("title4", "body4")
        assertNotNull result
        assertEquals 4, result.id

        result = domainClass.findByTitleAndBody("title4", "body2")
        assertNull result

        result = domainClass.findByAgeBetween(23, 25)
        assertNotNull result

        result = domainClass.findByAgeBetween(33, 35)
        assertNull result
    }

    @Test
    void testFindAllBy() {
        def results = domainClass.findAllByTitle("title7")
        assertEquals 1, results.size()

        results = domainClass.findAllByTitle("title13")
        assertEquals 0, results.size()

        results = domainClass.findAllByTitleLike("title%")
        assertEquals 10, results.size()

        results = domainClass.findAllByBodyContains("Groovy")
        assertEquals 1, results.size()

        results = domainClass.findAllByAgeBetweenOrAge(23, 25, 40)
        assertEquals 4, results.size()
    }

    @Test
    void testCountBy() {
        assertEquals 1, domainClass.countByTitle("title7")

        assertEquals 0, domainClass.countByTitle("title13")

        assertEquals 10, domainClass.countByTitleLike("title%")

        assertEquals 1, domainClass.countByBodyContains("Groovy")

        assertEquals 4, domainClass.countByAgeBetweenOrAge(23, 25, 40)
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
        def wikiEntry = domainClass.newInstance()
        wikiEntry.id = 10
        wikiEntry.title = "Groovy In Action"
        wikiEntry.body = "Groovy In Action is a cool book"
        wikiEntry.age = 40
        wikiEntry.save()

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