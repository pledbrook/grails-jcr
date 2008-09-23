package org.codehaus.groovy.grails.plugins.jcr.mapping;

import org.apache.jackrabbit.ocm.manager.impl.ObjectIterator;

import javax.jcr.RangeIterator;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * TODO: write javadoc
 *
 * @author Sergey Nebolsin (nebolsin@gmail.com)
 */
public class PaginationObjectIterator implements RangeIterator {
    private ObjectIterator delegate;
    private Long offset;
    private Long max;
    private int returnedObjects = 0;

    public PaginationObjectIterator(ObjectIterator delegate, Long offset, Long max) {
        this.delegate = delegate;
        this.offset = offset;
        this.max = max;
        if(offset == null) {
            this.offset = 0L;
        } else if(offset < 0) {
            throw new IllegalArgumentException("offset must not be negative");
        } else {
            delegate.skip(offset > delegate.getSize() ? delegate.getSize() : offset);
        }
    }

    public boolean hasNext() {
        if(max != null) {
            return returnedObjects < max && delegate.hasNext();
        }
        return delegate.hasNext();
    }

    public Object next() {
        if(max != null && returnedObjects >= max) throw new NoSuchElementException();
        returnedObjects++;
        return delegate.next();
    }

    public void remove() {
        delegate.remove();
    }

    public void skip(long l) {
        delegate.skip(l);
    }

    public long getSize() {
        return (max != null && max + offset < delegate.getSize()) ? max : delegate.getSize() - offset;
    }

    public long getPosition() {
        return returnedObjects;
    }
}
