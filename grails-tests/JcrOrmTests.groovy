class JcrOrmTests extends GroovyTestCase {

	void testSave() {
        def b = new WikiEntry(title:"foo")
        b.save()
	}
}
