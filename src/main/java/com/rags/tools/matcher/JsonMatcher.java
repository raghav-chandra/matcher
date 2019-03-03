package com.rags.tools.matcher;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
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
    public MatchingResult compare(JsonArray expected, JsonArray actual) {
        MatchingResult.Builder result = new MatchingResult.Builder().setMatchingStatus(MatchingStatus.P);
        if (expected == null && actual == null) {
            return result.create();
        } else if (expected == null || actual == null) {
            return result
                    .setMatchingStatus(MatchingStatus.F)
                    .setExpectedValue(expected)
                    .setActualValue(actual)
                    .setMatchingIndex(-1)
                    .create();
        } else if (expected.isEmpty() && actual.isEmpty()) {
            return result.create();
        }

        AtomicInteger counter = new AtomicInteger(-1);
        List<List<MatchingResult>> crossResults = expected.stream().map(exp -> {
            counter.set(counter.get() + 1);
            return findBestMatchingAttrCount(exp, counter.get(), actual);
        }).collect(Collectors.toList());
        return calculateBestMatching(expected, actual, crossResults);
    }

    private MatchingResult calculateBestMatching(JsonArray expected, JsonArray actual, List<List<MatchingResult>> crossResults) {
        boolean[][] matrix = new boolean[expected.size()][actual.size()];
        Map<String, Object> diffObj = new HashMap<>();

        AtomicBoolean finalStatus = new AtomicBoolean(true);
        List<List<MatchingResult>> nonMatching = new LinkedList<>();
        crossResults.forEach(obj -> {
            List<MatchingResult> matchingObjs = obj.stream().filter(data -> data.getStatus() == MatchingStatus.P).collect(Collectors.toList());
            boolean matching = !matchingObjs.isEmpty();
            finalStatus.set(finalStatus.get() && matching);
            if (matching) {
                blockActualColumn(matrix, matchingObjs.get(0));
                diffObj.put(String.valueOf(obj.iterator().next().getElemIndex()), matchingObjs.get(0));
            } else {
                nonMatching.add(obj);
            }
        });

        nonMatching.forEach(obj -> diffObj.put(String.valueOf(obj.iterator().next().getElemIndex()), findBestMatchedItemAndPopulateMatrix(obj, matrix)));

        MatchingResult.Builder result = new MatchingResult.Builder()
                .setMatchingStatus(finalStatus.get() ? MatchingStatus.P : MatchingStatus.F);
        if (!finalStatus.get()) {
            result.setActualValue(result).setExpectedValue(expected).setDifference(diffObj);
        }
        return result.create();
    }

    private void blockActualColumn(boolean[][] matrix, MatchingResult result) {
        for (int i = 0; i < matrix.length; i++) {
            matrix[i][result.getMatIndex()] = true;
        }
    }

    private MatchingResult findBestMatchedItemAndPopulateMatrix(List<MatchingResult> allMatches, boolean[][] matrix) {
        List<MatchingResult> sorted = allMatches.stream().sorted(Comparator.comparingInt(MatchingResult::getCount)).collect(Collectors.toList());
        MatchingResult matchedObj = sorted.stream().filter(obj -> !matrix[obj.getElemIndex()][obj.getMatIndex()]).findFirst().orElse(null);
        if (matchedObj != null) {
            blockActualColumn(matrix, matchedObj);
        } else {
            matchedObj = new MatchingResult.Builder().setMatchingStatus(MatchingStatus.NE).create();
        }
        return matchedObj;
    }

    private List<MatchingResult> findBestMatchingAttrCount(Object exp, int elemIndex, JsonArray array) {
        if (exp == null || array == null) {
            LOGGER.info("Either obj to match or array is null");
            return new LinkedList<>();
        }
        AtomicInteger bestMatchIndex = new AtomicInteger(-1);
        return array.stream().map(act -> {
            bestMatchIndex.set(bestMatchIndex.get() + 1);
            MatchingResult.Builder result = new MatchingResult.Builder()
                    .setMatchingStatus(MatchingStatus.F)
                    .setMatchingCount(0)
                    .setMatchingIndex(bestMatchIndex.get())
                    .setElementIndex(elemIndex);
            if (isPrimitive(exp) && isPrimitive(act)) {
                if (exp.equals(act)) {
                    result.setMatchingStatus(MatchingStatus.P).setMatchingCount(NEG_INFINITY);
                }
            } else if (exp instanceof JsonObject && act instanceof JsonObject) {
                result = compare((JsonObject) exp, (JsonObject) act).newBuilder().setMatchingIndex(bestMatchIndex.get()).setElementIndex(elemIndex);
            }
            if (result.getMatchingStatus() == MatchingStatus.F) {
                result.setMatchingIndex(bestMatchIndex.get()).setExpectedValue(exp).setActualValue(act).setDifference(result.getDifference());
            }
            return result.create();
        }).collect(Collectors.toList());
    }

    public MatchingResult compare(JsonObject exp, JsonObject act) {
        MatchingResult.Builder finalStatusObj = new MatchingResult.Builder().setMatchingStatus(MatchingStatus.P);
        if (exp == null && act == null) {
            LOGGER.info("Either obj to match or actual is null");
            return finalStatusObj.setMatchingCount(NEG_INFINITY).create();
        } else if (exp == null || act == null) {
            return finalStatusObj.setMatchingStatus(MatchingStatus.F).setActualValue(act).setExpectedValue(exp).create();
        }
        AtomicInteger matchingCount = new AtomicInteger(0);
        Map<String, Object> diffObj = new HashMap<>();
        finalStatusObj.setMatchingCount(NEG_INFINITY).setDifference(diffObj);

        exp.iterator().forEachRemaining(item -> {
            String attr = item.getKey();
            Object expVal = item.getValue();
            Object actVal = act.getValue(attr);
//            JsonObject internalDiff = new JsonObject().put(STATUS, MATCH_PASS);
            Map<String, Object> internalDiffObj = new HashMap<>();
            internalDiffObj.put(STATUS, MATCH_PASS);
            diffObj.put(attr, internalDiffObj);
            if (expVal == null && actVal == null) {
                matchingCount.set(matchingCount.get() + 1);
            } else if (expVal == null || actVal == null) {
                internalDiffObj.put(STATUS, MATCH_FAIL);
                internalDiffObj.put(EXPECTED, expVal);
                internalDiffObj.put(ACTUAL, actVal);
                finalStatusObj.setMatchingStatus(MatchingStatus.F);
            } else if (isPrimitive(expVal) && isPrimitive(actVal)) {
                boolean isMatching = expVal.equals(actVal);
                matchingCount.set(matchingCount.get() + (isMatching ? 1 : 0));
                if (!isMatching) {
                    internalDiffObj.put(STATUS, MATCH_FAIL);
                    internalDiffObj.put(EXPECTED, expVal);
                    internalDiffObj.put(ACTUAL, actVal);
                    finalStatusObj.setMatchingStatus(MatchingStatus.F);
                }
            } else if (expVal instanceof JsonObject && actVal instanceof JsonObject) {
                MatchingResult result = compare((JsonObject) expVal, (JsonObject) actVal);
                if (result.getStatus() == MatchingStatus.P) {
                    matchingCount.set(matchingCount.get() + 1);
                } else {
                    internalDiffObj.put(STATUS, MATCH_FAIL);
                    internalDiffObj.put(EXPECTED, expVal);
                    internalDiffObj.put(ACTUAL, actVal);
                    internalDiffObj.put(DIFFERENCE, result.getDiff());
                    finalStatusObj.setMatchingStatus(MatchingStatus.F);
                }
            } else if (expVal instanceof JsonArray && actVal instanceof JsonArray) {
                JsonArray expValArray = (JsonArray) expVal;
                JsonArray actValArray = (JsonArray) actVal;
                MatchingResult result = compare(expValArray, actValArray);
                if (result.getStatus() == MatchingStatus.P) {
                    matchingCount.set(matchingCount.get() + 1);
                } else {
                    internalDiffObj.put(STATUS, MATCH_FAIL);
                    internalDiffObj.put(EXPECTED, expVal);
                    internalDiffObj.put(ACTUAL, actVal);
                    internalDiffObj.put(DIFFERENCE, result.getDiff());
                    finalStatusObj.setMatchingStatus(MatchingStatus.F);
                }
            }
        });
        if (finalStatusObj.getMatchingStatus() == MatchingStatus.F) {
            finalStatusObj.setActualValue(act).setExpectedValue(exp).setMatchingCount(matchingCount.get());
        }
        return finalStatusObj.create();
    }

    private boolean isPrimitive(Object o) {
        return o instanceof String || o instanceof Double || o instanceof Float || o instanceof Integer || o instanceof Boolean || o instanceof Long;
    }
}
