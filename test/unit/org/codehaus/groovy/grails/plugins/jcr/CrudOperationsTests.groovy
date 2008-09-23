package org.codehaus.groovy.grails.plugins.jcr

import javax.jcr.*
import org.testng.annotations.Test
import org.testng.annotations.AfterMethod

class CrudOperationsTests extends AbstractJcrPluginTest {
    Class domainClass

    @Test
    void testSave() {
        def wikiEntry = domainClass.newInstance()

        wikiEntry.id = 1
        wikiEntry.title = "foo"
        wikiEntry.body = "bar"

        wikiEntry.save()

        assertNotNull wikiEntry.UUID

        domainClass.withSession {session ->
            def saved = session.getNodeByUUID(wikiEntry.UUID)
            assertEquals "foo", saved.getProperty("title").getString()
            assertEquals "bar", saved.getProperty("body").getString()
        }

    }

    @Test (dependsOnMethods = ["testSave"])
    void testList() {
        def wikiEntry = domainClass.newInstance()
        def wikiEntry2 = domainClass.newInstance()

        wikiEntry.id = 1
        wikiEntry.title = "foo"
        wikiEntry.body = "bar"

        wikiEntry.save()

        wikiEntry2.id = 2
        wikiEntry2.title = "test"
        wikiEntry2.body = "post"
        wikiEntry2.save()


        List results = domainClass.list()

        assertNotNull(results)

        def one = results.find { it.title == "foo" }

        assertNotNull(one)

        def two = results.find { it.body == "post" }

        assertNotNull(two)
    }

    @Test (dependsOnMethods = ["testSave"])
    void testListWithOrderBy() {
        (0..9).each {
            def wikiEntry = domainClass.newInstance()
            wikiEntry.id = it
            wikiEntry.title = "title${(int) (it / 5)}"
            wikiEntry.body = "body${9 - it}"
            wikiEntry.save()
        }

        assertEquals([9, 8, 7, 6, 5, 4, 3, 2, 1, 0], domainClass.list([orderBy: 'body']).collect {it.id})
        assertEquals([9, 8, 7, 6, 5, 4, 3, 2, 1, 0], domainClass.list([orderBy: ['id': 'desc']]).collect {it.id})
        assertEquals([0, 1, 2, 3, 4, 5, 6, 7, 8, 9], domainClass.list([orderBy: ['id']]).collect {it.id})
        assertEquals([0, 1, 2, 3, 4, 5, 6, 7, 8, 9], domainClass.list([orderBy: ['body': 'desc']]).collect {it.id})
        assertEquals([4, 3, 2, 1, 0, 9, 8, 7, 6, 5], domainClass.list([orderBy: ['title': 'asc', 'body': 'asc']]).collect {it.id})
        assertEquals([5, 6, 7, 8, 9, 0, 1, 2, 3, 4], domainClass.list([orderBy: ['title': 'desc', 'id': 'asc']]).collect {it.id})
    }

    @Test (dependsOnMethods = ["testSave"])
    void testListWithOffsetAndMax() {
        (0..9).each {
            def wikiEntry = domainClass.newInstance()
            wikiEntry.id = it
            wikiEntry.title = "title$it"
            wikiEntry.body = "body$it"
            wikiEntry.save()
        }

        assertEquals([3,4,5], domainClass.list([offset:3, max:3, orderBy:'id']).collect {it.id})
        assertEquals([1,0], domainClass.list([offset:8, max:2, orderBy:['id':'desc']]).collect {it.id})
    }

    @Test (dependsOnMethods = ["testSave"])
    void testGet() {
        def wikiEntry = domainClass.newInstance()

        wikiEntry.id = 1
        wikiEntry.title = "foo"
        wikiEntry.body = "bar"
        wikiEntry.save()

        def result = domainClass.get("/WikiEntry/1")
        assertNotNull "result"
        assertEquals(1L, result.id)
        assertEquals("foo", result.title)
        assertEquals("bar", result.body)

        assertNull domainClass.get("/WikiEntry/2")
    }

    @Test (dependsOnMethods = ["testSave"])
    void testGetByUUID() {
        def wikiEntry = domainClass.newInstance()

        wikiEntry.id = 1
        wikiEntry.title = "foo"
        wikiEntry.body = "bar"
        wikiEntry.save()

        def result = domainClass.getByUUID(wikiEntry.UUID)
        assertNotNull "result"
        assertEquals(1L, result.id)
        assertEquals("foo", result.title)
        assertEquals("bar", result.body)

        shouldFail {
            domainClass.getByUUID("123qwqwe")
        }
    }

    @Test (dependsOnMethods = ["testSave"])
    void testDelete() {
        def wikiEntry = domainClass.newInstance()

        wikiEntry.id = 1
        wikiEntry.title = "foo"
        wikiEntry.body = "bar"
        wikiEntry.save()

        wikiEntry.delete()

        domainClass.withSession {Session session ->
            assertFalse session.itemExists("/WikiEntry/1")
        }
    }

    @Test (dependsOnMethods = ["testSave", "testDelete"])
    void testCount() {
        def wikiEntry = domainClass.newInstance()
        def wikiEntry2 = domainClass.newInstance()

        wikiEntry.id = 1
        wikiEntry.title = "foo"
        wikiEntry.body = "bar"

        wikiEntry.save()

        wikiEntry2.id = 2
        wikiEntry2.title = "test"
        wikiEntry2.body = "post"
        wikiEntry2.save()

        assertEquals 2, domainClass.count()

        wikiEntry2.delete()

        assertEquals 1, domainClass.count()

        wikiEntry.delete()

        assertEquals 0, domainClass.count()
    }

    @Test (dependsOnMethods = ["testSave", "testGet"])
    void testUpdate() {
        def wikiEntry = domainClass.newInstance()

        wikiEntry.id = 1
        wikiEntry.title = "foo"
        wikiEntry.body = "bar"

        wikiEntry.save()

        assertNotNull wikiEntry.UUID

        def wikiEntry2 = domainClass.get(wikiEntry.path)
        wikiEntry2.title = "changed"
        wikiEntry2.save()

        assertEquals wikiEntry.UUID, wikiEntry2.UUID

        domainClass.withSession {session ->
            def saved = session.getNodeByUUID(wikiEntry.UUID)
            assertEquals "changed", saved.getProperty("title").getString()
            assertEquals "bar", saved.getProperty("body").getString()
        }

        wikiEntry2 = domainClass.get(wikiEntry.path)
        wikiEntry2.title = "changed1"
        wikiEntry2.save()
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