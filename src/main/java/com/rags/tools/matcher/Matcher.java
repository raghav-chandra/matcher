package com.rags.tools.matcher;

import java.util.Map;

/**
 * Compares two Objects and produces uniform Matching results across any kind of Objects.
 *
 * @author Raghav Chandra (raghav.yo@gmail.com)
 * @version 1.0
 */
public interface Matcher {

    /**
     * Compares two Objects and produces uniform Matching results across any kind of Objects.
     *
     * @param expected expected object, can be Primitive, Complex or Array
     * @param actual   expected object, can be Primitive, Complex or Array
     * @return Matching Results
     */
    MatchingResult compare(Object expected, Object actual);

    /**
     * Compares two Objects and produces uniform Matching results across any kind of Objects.
     *
     * @param expected         expected object, can be Primitive, Complex or Array
     * @param actual           expected object, can be Primitive, Complex or Array
     * @param ignoreAttributes Ignore attributes (nested, based on the nesting level comparison)
     * @return Matching Results
     */
    MatchingResult compare(Object expected, Object actual, Map<String, Object> ignoreAttributes);

    /**
     * Compares two Objects and produces uniform Matching results across any kind of Objects.
     *
     * @param expected         expected object, can be Primitive, Complex or Array
     * @param actual           expected object, can be Primitive, Complex or Array
     * @param ignoreAttributes Ignore attributes (nested, based on the nesting level comparison)
     * @param businessKey      Business Key to match the elements
     * @return Matching Results
     */
    MatchingResult compare(Object expected, Object actual, Map<String, Object> ignoreAttributes, Map<String, Object> businessKey);

}
