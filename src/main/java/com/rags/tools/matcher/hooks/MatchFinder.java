package com.rags.tools.matcher.hooks;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public interface MatchFinder {

    /**
     *
     * @param matchingObject Object to be matched
     * @param matches all matches done across all objects of array
     * @return returns best matched Object
     */
    JsonObject findMatch(JsonObject matchingObject, JsonArray matches);
}
