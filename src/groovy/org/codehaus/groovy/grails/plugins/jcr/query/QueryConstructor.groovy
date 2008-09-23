package org.codehaus.groovy.grails.plugins.jcr.query;

import org.codehaus.groovy.grails.commons.GrailsDomainClass;
import org.apache.jackrabbit.ocm.query.Query;
import org.apache.jackrabbit.ocm.query.QueryManager;
import org.apache.jackrabbit.ocm.query.Filter;

import java.util.Map;

/**
 * TODO: write javadoc
 *
 * @author Sergey Nebolsin (nebolsin@gmail.com)
 */
class QueryConstructor {
    private QueryManager manager;
    private GrailsDomainClass domainClass;

    public QueryConstructor(QueryManager manager, GrailsDomainClass domainClass) {
        this.manager = manager;
        this.domainClass = domainClass;
    }

    /**
     * Creates query for all instances of the domain class.
     */
    public Query createQuery(Map args) {
        Filter filter = configureBasicFilter()
        return configureFinalQuery(filter, args)
    }

    public Query createJCRExpressionQuery(String jcrExpression, Map args) {
        Filter filter = configureBasicFilter()
        filter.addJCRExpression(jcrExpression)
        return configureFinalQuery(filter, args)
    }

    public Query createDynamicQuery(String expression, Object[] params, Map args) {
        Filter filter = configureBasicFilter()

        return configureFinalQuery(filter, args)
    }


    private Filter configureBasicFilter() {
        Filter filter = manager.createFilter(domainClass.getClazz())
        filter.setScope("${domainClass.clazz.getDomainPath()}//")
        return filter
    }

    private Query configureFinalQuery(Filter filter, Map args) {
        Query query = manager.createQuery(filter);
        return confirureOrdering(query, args)
    }


    /**
     * Configures ordering in Query based on given params.
     *
     * TODO: throw exception if domain class has no such field
     */
    private Query confirureOrdering(Query query, Map args) {
        if(args?.orderBy) {
            if(args.orderBy instanceof Map) {
                args.orderBy.each {fieldName, type ->
                    switch(type?.toString()) {
                        case 'asc':
                            query.addOrderByAscending(fieldName)
                            break;
                        case 'desc':
                            query.addOrderByDescending(fieldName)
                            break;
                        default:
                            throw new IllegalArgumentException("only 'asc' and 'desc' are allowed order types")
                    }
                }
            } else if(args.orderBy instanceof Collection) {
                args.orderBy.each {it ->
                    query.addOrderByAscending(it.toString())
                }
            } else {
                query.addOrderByAscending(args.orderBy.toString())
            }

        }

        return query;
    }
}
