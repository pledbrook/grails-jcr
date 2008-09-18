package org.codehaus.groovy.grails.plugins.jcr

import org.testng.annotations.AfterMethod
import javax.jcr.Session
import org.testng.annotations.Test
import org.apache.jackrabbit.ocm.manager.ObjectContentManager
import org.apache.jackrabbit.ocm.lock.Lock

/**
 * TODO: write javadoc
 *
 * @author Sergey Nebolsin (nebolsin@gmail.com)
 */
class LockingOperationsTests extends AbstractJcrPluginTest {
    Class domainClass

    @Test
    void testLocking() {
        def wikiEntry = domainClass.newInstance()

        wikiEntry.id = 1
        wikiEntry.title = "foo"
        wikiEntry.body = "bar"
        wikiEntry.save()

        domainClass.withOcm {ObjectContentManager ocm ->
            assertFalse ocm.isLocked(wikiEntry.path)
        }

        Lock lock = wikiEntry.lock()

        assertTrue lock.isSessionScoped()

        domainClass.withOcm {ObjectContentManager ocm ->
            assertTrue ocm.isLocked(wikiEntry.path)
        }
    }

    @Test
    void testOpenScopedLocking() {
        def wikiEntry = domainClass.newInstance()

        wikiEntry.id = 1
        wikiEntry.title = "foo"
        wikiEntry.body = "bar"
        wikiEntry.save()

        domainClass.withOcm {ObjectContentManager ocm ->
            assertFalse ocm.isLocked(wikiEntry.path)
        }

        Lock lock = wikiEntry.lock(false)

        assertFalse lock.isSessionScoped()

        domainClass.withOcm {ObjectContentManager ocm ->
            assertTrue ocm.isLocked(wikiEntry.path)
        }
    }

    @Test
    void testUnlocking() {
        def wikiEntry = domainClass.newInstance()

        wikiEntry.id = 1
        wikiEntry.title = "foo"
        wikiEntry.body = "bar"
        wikiEntry.save()

        wikiEntry.lock()

        wikiEntry.unlock()

        domainClass.withOcm {ObjectContentManager ocm ->
            assertFalse ocm.isLocked(wikiEntry.path)
        }
    }

    @Test
    void testIsLocked() {
        def wikiEntry = domainClass.newInstance()

        wikiEntry.id = 1
        wikiEntry.title = "foo"
        wikiEntry.body = "bar"
        wikiEntry.save()

        wikiEntry.lock()

        assertTrue wikiEntry.isLocked()

        wikiEntry.unlock()

        assertFalse wikiEntry.isLocked()
    }

    void registerDomainClasses() {
        gcl.parseClass("""\
import org.apache.jackrabbit.ocm.mapper.impl.annotation.Field;
import org.apache.jackrabbit.ocm.mapper.impl.annotation.Node;

@Node(jcrMixinTypes="mix:referenceable, mix:versionable, mix:lockable")
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