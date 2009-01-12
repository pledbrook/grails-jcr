package org.codehaus.groovy.grails.plugins.jcr

import javax.jcr.*
import org.testng.annotations.Test
import org.testng.annotations.AfterMethod

class RelationshipTests extends AbstractJcrPluginTest {
    Class authorClass
    Class bookClass

    //@Test
    void testSimpleOneToOneRelationship() {
        def author = authorClass.newInstance()
        author.name = "Graeme Rocher"
        author.save()

        def book = bookClass.newInstance()
        book.title = "The Definitive Guide To Grails"
        book.author = author
        book.save()

        book = bookClass.get(book.path)
        assertNotNull book.author
        assertEquals book.author.path, author.path
    }

    void registerDomainClasses() {
        gcl.parseClass("""\
class Author {
   static mapWith = 'jcr'
   static namespace = 'wiki'

   Long id
   String version

   String path
   String UUID

   String name
}
""")

        gcl.parseClass("""\
class Book {
   static mapWith = 'jcr'
   static namespace = 'wiki'

   Long id
   String version

   String path
   String UUID

   String title
   Author author
}
""")
    }

    void init() {
        bookClass = ga.getDomainClass("Book").getClazz()
        authorClass = ga.getDomainClass("Author").getClazz()
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