package org.codehaus.groovy.grails.plugins.jcr.mapping

import org.testng.annotations.Test
import org.testng.annotations.BeforeClass
import org.testng.annotations.AfterClass
import org.codehaus.groovy.grails.commons.DefaultGrailsDomainClass
import org.codehaus.groovy.grails.commons.GrailsDomainClass
import org.apache.jackrabbit.ocm.mapper.model.MappingDescriptor
import org.apache.jackrabbit.ocm.mapper.model.ClassDescriptor
import org.apache.jackrabbit.ocm.mapper.model.FieldDescriptor

/**
 * TODO: write javadoc
 *
 * @author Sergey Nebolsin (nebolsin@gmail.com)
 */
class GrailsDescriptorReaderTests extends GroovyTestCase {
    GrailsDomainClass wikiEntryDomainClass

    @Test
    void testSimpleMapping() {
        GrailsDescriptorReader descriptorReader = new GrailsDescriptorReader([wikiEntryDomainClass])
        MappingDescriptor mappingDescriptor = descriptorReader.loadClassDescriptors()

        assertNotNull mappingDescriptor.allClassDescriptors
        assertEquals 1, mappingDescriptor.allClassDescriptors.size()

        ClassDescriptor classDescriptor = mappingDescriptor.getClassDescriptorByName('WikiEntry')
        assertNotNull "MappingDescriptor doesn't contain class descriptor for class WikiEntry", classDescriptor
        assertEquals "WikiEntry", classDescriptor.getClassName()
        assertEquals "nt:unstructured", classDescriptor.getJcrType()
        assertEquals(['mix:referenceable', 'mix:versionable', 'mix:lockable'], classDescriptor.getJcrMixinTypes().toList())
        assertTrue classDescriptor.hasDiscriminator()

        FieldDescriptor field = classDescriptor.getIdFieldDescriptor()
        assertNotNull("Class descriptor doesn't have id field descriptor", field)
        assertEquals 'id', field.getFieldName()
        assertEquals 'id', field.getJcrName()

        field = classDescriptor.getPathFieldDescriptor()
        assertNotNull("Class descriptor doesn't have path field descriptor", field)
        assertEquals 'path', field.getFieldName()
        assertEquals 'path', field.getJcrName()

        field = classDescriptor.getUuidFieldDescriptor()
        assertNotNull("Class descriptor doesn't have UUID field descriptor", field)
        assertEquals 'UUID', field.getFieldName()
        assertEquals 'UUID', field.getJcrName()

        field = classDescriptor.getFieldDescriptor('title')
        assertNotNull("Class descriptor doesn't have field descriptor for 'title'", field)
        assertEquals 'title', field.getFieldName()
        assertEquals 'title', field.getJcrName()

        field = classDescriptor.getFieldDescriptor('body')
        assertNotNull("Class descriptor doesn't have field descriptor for 'body'", field)
        assertEquals 'body', field.getFieldName()
        assertEquals 'body', field.getJcrName()
    }

    GroovyClassLoader gcl = new GroovyClassLoader()

    @BeforeClass
    void setUp() {
        Class wikiEntryClass = gcl.parseClass("""\
class WikiEntry {
   Long id
   String version

   String path
   String UUID
   String title
   String body
}
""")
        wikiEntryDomainClass = new DefaultGrailsDomainClass(wikiEntryClass)

    }

    @AfterClass
    void tearDown() {

    }
}