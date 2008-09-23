package org.codehaus.groovy.grails.plugins.jcr

import javax.jcr.*
import org.testng.annotations.Test
import org.testng.annotations.AfterMethod

/**
 * TODO: write javadoc
 *
 * @author Sergey Nebolsin (nebolsin@gmail.com)
 */
class DynamicFindersTests extends AbstractJcrPluginTest {
    Class domainClass

    @Test
    void testSimpleQueryFor() {
        domainClass.queryForTitleAndBody([aaa:'bbb',ccc:'ddd'])
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