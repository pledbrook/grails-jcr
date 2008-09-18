package org.codehaus.groovy.grails.plugins.jcr.mapping;

import org.springmodules.jcr.SessionFactoryUtils;
import org.springmodules.jcr.SessionFactory;
import org.apache.jackrabbit.ocm.manager.impl.ObjectContentManagerImpl;
import org.apache.jackrabbit.ocm.manager.ObjectContentManager;
import org.apache.jackrabbit.ocm.mapper.Mapper;
import org.apache.log4j.Logger;
import org.codehaus.groovy.grails.plugins.jcr.exceptions.GrailsRepositoryException;
import org.springframework.beans.factory.InitializingBean;

import javax.jcr.Session;

import groovy.lang.Closure;

/**
 * TODO: write javadoc
 *
 * @author Sergey Nebolsin (nebolsin@gmail.com)
 */
class JcrOcmTemplate implements InitializingBean {
    private static final Logger log = Logger.getLogger(JcrOcmTemplate.class);

    Mapper jcrMapper;
    SessionFactory jcrSessionFactory;

    public Object execute(Closure action) {
        Session session = SessionFactoryUtils.getSession(jcrSessionFactory, true);
        boolean existingTransaction = SessionFactoryUtils.isSessionThreadBound(session, jcrSessionFactory);
        if(existingTransaction) {
            log.debug("Found thread-bound Session for JcrTemplate");
        }

        try {
            ObjectContentManager ocm = new ObjectContentManagerImpl(session, jcrMapper);
            return action.call(ocm);
        } catch (Exception ex) {
            throw new GrailsRepositoryException(ex);
        } finally {
            if(existingTransaction) {
                log.debug("Not closing pre-bound Jcr Session after JcrTemplate");
            } else {
                SessionFactoryUtils.releaseSession(session, jcrSessionFactory);
            }
        }
    }

    public void afterPropertiesSet() {
        if(jcrMapper == null) {
            throw new IllegalArgumentException("Property 'jcrMapper' is required.");
        }
        if(jcrSessionFactory == null) {
            throw new IllegalArgumentException("Property 'jcrSessionFactory' is required.");
        }
    }
}