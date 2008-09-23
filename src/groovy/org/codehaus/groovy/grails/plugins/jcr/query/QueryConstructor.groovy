package org.codehaus.groovy.grails.plugins.jcr.query;

import org.codehaus.groovy.grails.commons.GrailsDomainClass;
import org.apache.jackrabbit.ocm.query.Query;
import org.apache.jackrabbit.ocm.query.QueryManager;
import org.apache.jackrabbit.ocm.query.Filter;

import java.util.Map
import grails.util.GrailsUtil
import org.codehaus.groovy.grails.commons.GrailsClassUtils;

/**
 * TODO: write javadoc
 *
 * @author Sergey Nebolsin (nebolsin@gmail.com)
 */
class QueryConstructor {
    // operation - params count map
    private static final OPERATIONS = [
            "Contains": 1,
            "Between": 2,
            "EqualTo": 1,
            "GreaterOrEqualThan": 1,
            "GreaterThan": 1,
            "LessOrEqualThan": 1,
            "LessThan": 1,
            "Like": 1,
            "NotEqualTo": 1,
            "NotNull": 0,
            "IsNull": 0
    ]

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

    public Query createDynamicQuery(String expression, List params, Map args) {
        int paramIndex = 0
        Filter filter = configureBasicFilter()
        expression.split("Or").each {String orClause ->
            if(orClause) {
                Filter orFilter = configureBasicFilter();
                orClause.split("And").each {String andClause ->
                    if(andClause) {
                        Filter andFilter = configureBasicFilter();
                        def operation = OPERATIONS.find {key, value -> andClause.endsWith(key)}
                        String fieldName = andClause - operation?.key
                        if(!operation) {
                            operation = OPERATIONS.find {key, value -> "EqualTo" == key}
                        }

                        def finalParams = [convertPropertyName(fieldName)]
                        if(paramIndex + operation.value > params.size()) {
                            throw new IllegalArgumentException("Not enough params for dynamic finder: $expression, ${params.size()} params provided")
                        }
                        operation.value.times {finalParams << params[paramIndex++]}

                        andFilter.invokeMethod("add${operation.key}", finalParams as Object[])
                        orFilter.addAndFilter(andFilter)
                    }
                }
                filter.addOrFilter(orFilter)
            }
        }

        println filter

        Query result = configureFinalQuery(filter, args)
        return result
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

    private static String convertPropertyName(String prop) {
        if((Character.isUpperCase(prop.charAt(0)) && Character.isUpperCase(prop.charAt(1))) || Character.isDigit(prop.charAt(0))) {
            return prop;
        }
        return "${prop[0].toLowerCase()}${prop[1..-1]}"
    }
}
