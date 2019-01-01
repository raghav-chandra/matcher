package com.rags.tools.matcher;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

/**
 * @author Raghav Chandra (raghav.yo@gmail.com)
 * @version 1.0
 */
public interface Matcher {

    /**
     * Compares two JsonArray. Each element is matched across all elements to find the best match for a single element
     *
     * @param expected Expected Json Array
     * @param actual   Actual Json Array
     * @return Comparison results wrapped around a Json Object.
     */
    JsonObject compare(JsonArray expected, JsonArray actual);

    /**
     * Compares two Json Object. Json Objects are matched based on their attributes.
     *
     * @param expected Json Object 1
     * @param actual   Json Object 2
     * @return Comparison results wrapped around a Json Object.
     */
    JsonObject compare(JsonObject expected, JsonObject actual);
}
