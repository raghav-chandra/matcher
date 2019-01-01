package com.rags.tools.matcher;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * Compares 2 json objects/arrays and produces comparison results in form of a Json Object
 *
 * @author Raghav Chandra (raghav.yo@gmail.com)
 * @version 1.0
 */
public class JsonMatcher implements Matcher {
    private static final Logger LOGGER = LoggerFactory.getLogger(JsonMatcher.class);

    private static final int NEG_INFINITY = Integer.MIN_VALUE;
    private static final String MATCH_PASS = "P";
    private static final String MATCH_FAIL = "F";
    private static final String MATCH_NOT_EXISTS = "NE";

    private static final String STATUS = "status";
    private static final String EXPECTED = "exp";
    private static final String ACTUAL = "act";
    private static final String DIFFERENCE = "diff";
    private static final String ELEMENT_INDEX = "elemIndex";
    private static final String MATCHING_INDEX = "matIndex";
    private static final String COUNT = "count";

    @Override
    public JsonObject compare(JsonArray expected, JsonArray actual) {
        JsonObject status = new JsonObject().put(STATUS, MATCH_PASS);
        if (expected == null && actual == null) {
            return status;
        } else if (expected == null || actual == null) {
            return new JsonObject().put(STATUS, MATCH_FAIL).put(EXPECTED, expected).put(ACTUAL, actual).put(MATCHING_INDEX, -1);
        } else if (expected.isEmpty() && actual.isEmpty()) {
            return status;
        }

        AtomicInteger counter = new AtomicInteger(-1);
        List<List<JsonObject>> crossResults = expected.stream().map(exp -> {
            counter.set(counter.get() + 1);
            return findBestMatchingAttrCount(exp, counter.get(), actual);
        }).collect(Collectors.toList());
        return calculateBestMatching(expected, actual, crossResults);
    }

    private JsonObject calculateBestMatching(JsonArray expected, JsonArray actual, List<List<JsonObject>> crossResults) {
        boolean[][] matrix = new boolean[expected.size()][actual.size()];
        JsonObject diff = new JsonObject();
        AtomicBoolean finalStatus = new AtomicBoolean(true);
        List<List<JsonObject>> nonMatching = new LinkedList<>();
        crossResults.forEach(obj -> {
            List<JsonObject> matchingObjs = obj.stream().filter(data -> data.getString(STATUS).equals(MATCH_PASS)).collect(Collectors.toList());
            boolean matching = !matchingObjs.isEmpty();
            finalStatus.set(finalStatus.get() && matching);
            if (matching) {
                blockActualColumn(matrix, matchingObjs.get(0));
                diff.put(String.valueOf(obj.iterator().next().getInteger(ELEMENT_INDEX)), matchingObjs.get(0));
            } else {
                nonMatching.add(obj);
            }
        });

        nonMatching.forEach(obj -> diff.put(String.valueOf(obj.iterator().next().getInteger(ELEMENT_INDEX)), findBestMatchedItemAndPopulateMatrix(obj, matrix)));
        JsonObject finalObj = new JsonObject().put(STATUS, finalStatus.get() ? MATCH_PASS : MATCH_FAIL);
        if (!finalStatus.get()) {
            finalObj.put(ACTUAL, actual).put(EXPECTED, expected).put(DIFFERENCE, diff);
        }
        return finalObj;
    }

    private void blockActualColumn(boolean[][] matrix, JsonObject matchingObj) {
        for (int i = 0; i < matrix.length; i++) {
            matrix[i][matchingObj.getInteger(MATCHING_INDEX)] = true;
        }
    }

    private JsonObject findBestMatchedItemAndPopulateMatrix(List<JsonObject> allMatches, boolean[][] matrix) {
        List<JsonObject> sorted = allMatches.stream().sorted(Comparator.comparingInt(o -> o.getInteger(COUNT))).collect(Collectors.toList());
        JsonObject matchedObj = sorted.stream().filter(obj -> !matrix[obj.getInteger(ELEMENT_INDEX)][obj.getInteger(MATCHING_INDEX)]).findFirst().orElse(null);
        if (matchedObj != null) {
            blockActualColumn(matrix, matchedObj);
        } else {
            matchedObj = new JsonObject().put(STATUS, MATCH_NOT_EXISTS);
        }
        return matchedObj;
    }

    private List<JsonObject> findBestMatchingAttrCount(Object exp, int elemIndex, JsonArray array) {
        if (exp == null || array == null) {
            LOGGER.info("Either obj to match or array is null");
            return new LinkedList<>();
        }
        AtomicInteger bestMatchIndex = new AtomicInteger(-1);
        return array.stream().map(act -> {
            bestMatchIndex.set(bestMatchIndex.get() + 1);
            JsonObject status = new JsonObject().put(STATUS, MATCH_FAIL).put(COUNT, 0).put(MATCHING_INDEX, bestMatchIndex.get()).put(ELEMENT_INDEX, elemIndex);
            if (isPrimitive(exp) && isPrimitive(act)) {
                if (exp.equals(act)) {
                    status.put(STATUS, MATCH_PASS).put(COUNT, NEG_INFINITY);
                }
            } else if (exp instanceof JsonObject && act instanceof JsonObject) {
                status = compare((JsonObject) exp, (JsonObject) act).put(MATCHING_INDEX, bestMatchIndex.get()).put(ELEMENT_INDEX, elemIndex);
            }
            if (status.getString(STATUS).equals(MATCH_FAIL)) {
                status.put(MATCHING_INDEX, bestMatchIndex.get()).put(EXPECTED, exp).put(ACTUAL, act).put(DIFFERENCE, status.getJsonObject(DIFFERENCE));
            }
            return status;
        }).collect(Collectors.toList());
    }

    public JsonObject compare(JsonObject exp, JsonObject act) {
        if (exp == null && act == null) {
            LOGGER.info("Either obj to match or actual is null");
            return new JsonObject().put(STATUS, MATCH_PASS).put(COUNT, NEG_INFINITY);
        } else if (exp == null || act == null) {
            return new JsonObject().put(STATUS, MATCH_FAIL).put(ACTUAL, act).put(EXPECTED, exp);
        }
        AtomicInteger
                matchingCount = new AtomicInteger(0);
        JsonObject finalStatusObj = new JsonObject().put(STATUS, MATCH_PASS).put(COUNT, NEG_INFINITY);
        JsonObject diff = new
                JsonObject();
        finalStatusObj.put(DIFFERENCE, diff);
        exp.iterator().forEachRemaining(item -> {
            String attr = item.getKey();
            Object expVal = item.getValue();
            Object actVal = act.getValue(attr);
            JsonObject internalDiff = new JsonObject().put(STATUS, MATCH_PASS);
            diff.put(attr, internalDiff);
            if (expVal == null && actVal == null) {
                matchingCount.set(matchingCount.get() + 1);
            } else if (expVal == null || actVal == null) {
                internalDiff.put(STATUS, MATCH_FAIL).put(EXPECTED, expVal).put(ACTUAL, actVal);
                finalStatusObj.put(STATUS, MATCH_FAIL);
            } else if (isPrimitive(expVal) && isPrimitive(actVal)) {
                boolean isMatching = expVal.equals(actVal);
                matchingCount.set(matchingCount.get() + (isMatching ? 1 : 0));
                if (!isMatching) {
                    internalDiff.put(STATUS, MATCH_FAIL).put(EXPECTED, expVal).put(ACTUAL, actVal);
                    finalStatusObj.put(STATUS, MATCH_FAIL);
                }
            } else if (expVal instanceof JsonObject && actVal instanceof JsonObject) {
                JsonObject status = compare((JsonObject) expVal, (JsonObject) actVal);
                if (status.getString(STATUS).equals(MATCH_PASS)) {
                    matchingCount.set(matchingCount.get() + 1);
                } else {
                    internalDiff.put(STATUS, MATCH_FAIL).put(EXPECTED, expVal).put(ACTUAL, actVal).put(DIFFERENCE, status.getJsonObject(DIFFERENCE));
                    finalStatusObj.put(STATUS, MATCH_FAIL);
                }
            } else if (expVal instanceof JsonArray && actVal instanceof JsonArray) {
                JsonArray expValArray = (JsonArray) expVal;
                JsonArray actValArray = (JsonArray) actVal;
                JsonObject status = compare(expValArray, actValArray);
                if (status.getString(STATUS).equals(MATCH_PASS)) {
                    matchingCount.set(matchingCount.get() + 1);
                } else {
                    internalDiff.put(STATUS, MATCH_FAIL).put(EXPECTED, expVal).put(ACTUAL, actVal).put(DIFFERENCE, status.getJsonObject(DIFFERENCE));
                    finalStatusObj.put(STATUS, MATCH_FAIL);
                }
            }
        });
        if (finalStatusObj.getString(STATUS).equals(MATCH_FAIL)) {
            finalStatusObj.put(STATUS, MATCH_FAIL).put(ACTUAL, act).put(EXPECTED, exp).put(COUNT, matchingCount.get());
        }
        return finalStatusObj;
    }

    private boolean isPrimitive(Object o) {
        return o instanceof String || o instanceof Double || o instanceof Float || o instanceof Integer || o instanceof Boolean || o instanceof Long;
    }
}
