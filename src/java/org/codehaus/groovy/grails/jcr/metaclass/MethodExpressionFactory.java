package org.codehaus.groovy.grails.jcr.metaclass;

/**
 * Class description here.
 *
 * @author Graeme Rocher
 * @since 0.4
 *        <p/>
 *        Created: Feb 12, 2007
 *        Time: 3:20:40 PM
 */
public interface MethodExpressionFactory {

    /**
     * Conditional operators inside dynamic finder method expressions
     */
    public final class Conditionals {
        public static final String AND = "And";
        public static final String OR = "Or";
        public static final String OR_EQUAL = "OrEqual";
    }
    /**
     * Negates the specified criterion
     *
     * @param criterion The criterion to negate
     * @return The negated version of the criterion
     */
    Criterion not(Criterion criterion);

    /**
     * Adds a criterion that ensure the specified property propertyName is less than
     * or equal to the specified argument
     *
     * @param propertyName The propertyName of the property
     * @param argument The argument it should be less than
     * @return The criterion
     */
    Criterion le(String propertyName, Object argument);

    /**
     * Adds a criterion that ensure the specified property name is less than
     * the specified argument
     *
     * @param propertyName The name of the property
     * @param argument The argument it should be less than
     * @return The criterion
     */

    Criterion lt(String propertyName, Object argument);

    /**
     * Adds a criterion that ensure the specified property propertyName is greater than
     * or equal to the specified argument
     *
     * @param propertyName The propertyName of the property
     * @param argument The argument it should be less than
     * @return The criterion
     */

    Criterion ge(String propertyName, Object argument);

    /**
     * Adds a criterion that ensure the specified property propertyName is greater than
     * the specified argument
     *
     * @param propertyName The propertyName of the property
     * @param argument The argument it should be less than
     * @return The criterion
     */
    Criterion gt(String propertyName, Object argument);

    /**
     * Adds a criterion performs a SQL "like" expression on the specified argument 
     *
     * @param propertyName The propertyName of the property
     * @param argument The argument it should be less than
     * @return The criterion
     */

    Criterion like(String propertyName, Object argument);

    /**
     * Adds a criterion that ensures that the specified property is not null
     *
     * @param propertyName The property name
     * @return The criterion
     */
    Criterion isNotNull(String propertyName);

    /**
     * Adds a criterion that ensure that the specified property is equal to the
     * specified argument
     *
     * @param propertyName The property name
     * @param argument The argument
     * @return True if it is
     */
    Criterion eq(String propertyName, Object argument);

    /**
     * Adds a criterion that ensures that the specified property is null
     *
     * @param propertyName The property name
     * @return The criterion
     */
    Criterion isNull(String propertyName);

    /**
     * Adds a criterian that ensure that the specified property is between the specified values
     *
     * @param propertyName The property name
     * @param left The left hand side
     * @param right The right hand side
     * @return
     */
    Criterion between(String propertyName, Object left, Object right);

    /**
     * Adds a criterion that ensure that the specified property is not equal to the
     * specified argument
     *
     * @param propertyName The property name
     * @param argument The argument
     * @return True if it is
     */

    Criterion ne(String propertyName, Object argument);

    /**
     * Makes sure both criterion expressions evaluate to true
     *
     * @param left The left hand side
     * @param right The right hande side
     * @return A new criterion
     */
    Criterion and(Criterion left, Criterion right);

    /**
     * Makes sure at least one of the criterion expressions evaluate to true (an "or" expression)
     *
     * @param left The left hand side
     * @param right The right hande side
     * @return A new criterion
     */
    Criterion or(Criterion left, Criterion right);


    /**
     * Returns the native conditional operator for the specified finder operator "And", "Or" etc.
     * @param name The name of the conditional
     * @return  The conditional
     */
    String getConditionalOperator(String name);

}
