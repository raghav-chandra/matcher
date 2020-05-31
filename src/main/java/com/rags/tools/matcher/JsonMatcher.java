package com.rags.tools.matcher;

import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * Compares two Objects and produces uniform Matching results across any kind of Objects.
 * Runs best count matching algorithm to perform matching of Arrays
 *
 * @author Raghav Chandra (raghav.yo@gmail.com)
 * @version 1.1.0
 */
public class JsonMatcher implements Matcher {
    private static final Logger LOGGER = LoggerFactory.getLogger(JsonMatcher.class);

    private static final int NEG_INFINITY = Integer.MIN_VALUE;

    @Override
    public MatchingResult compare(Object expected, Object actual) {
        return compare(expected, actual, new HashMap<>());
    }

    @Override
    public MatchingResult compare(Object expected, Object actual, Map<String, Object> ignored) {
        return compare(expected, actual, ignored, new HashMap<>());
    }

    @Override
    public MatchingResult compare(Object expected, Object actual, Map<String, Object> ignored, Map<String, Object> businessKey) {

        JsonObject ignoreAttributes = validate(ignored);
        JsonObject key = validate(businessKey);
        //Key and Ignored shouldn't be same at any level.
        validateKeyAndIgnored(ignoreAttributes, key);

        MatchingResult.Builder result = createStatus(MatchingStatus.P);
        if (expected == null && actual == null) {
            return result.create();
        } else if (expected == null || actual == null) {
            return result
                    .setMatchingStatus(MatchingStatus.F)
                    .setExpectedValue(expected)
                    .setActualValue(actual)
                    .setMatchingIndex(-1)
                    .create();
        } else if (isPrimitive(expected) && isPrimitive(actual)) {
            boolean isMatching = expected.equals(actual);
            if (!isMatching) {
                assignStatusAndExpAct(expected, actual, result.setMatchingCount(1), MatchingStatus.F);
            }
            return result.create();
        } else if (isComparable(expected) && isComparable(actual)) {
            boolean isMatching = ((Comparable) expected).compareTo(actual) == 0;
            if (!isMatching) {
                assignStatusAndExpAct(expected, actual, result.setMatchingCount(1), MatchingStatus.F);
            }
            return result.create();
        }

        boolean isExpList = expected instanceof List || expected instanceof JsonArray;
        boolean isActList = actual instanceof List || actual instanceof JsonArray;
        if (isExpList && !isActList || !isExpList && isActList) {
            return result
                    .setMatchingStatus(MatchingStatus.OM)
                    .setActualValue(actual)
                    .setExpectedValue(expected).create();
        }

        if (isExpList) {
            return compare(Json.encodeToBuffer(expected).toJsonArray(), Json.encodeToBuffer(actual).toJsonArray(), ignoreAttributes, key);
        }

        return compare(Json.encodeToBuffer(expected).toJsonObject(), Json.encodeToBuffer(actual).toJsonObject(), ignoreAttributes, key);
    }

    private void validateKeyAndIgnored(JsonObject ignoreAttributes, JsonObject key) {
        for (String attr : key.fieldNames()) {
            if (ignoreAttributes.containsKey(attr)) {
                if (ignoreAttributes.getValue(attr) instanceof JsonObject && key.getValue(attr) instanceof JsonObject) {
                    validateKeyAndIgnored((JsonObject) ignoreAttributes.getValue(attr), (JsonObject) key.getValue(attr));
                } else if (!(ignoreAttributes.getValue(attr) instanceof JsonObject) && !(key.getValue(attr) instanceof JsonObject)) {
                    throw new RuntimeException("Ignored attribute and Key can not be same.");
                }
            }
        }
    }

    private MatchingResult.Builder createStatus(MatchingStatus p) {
        return new MatchingResult.Builder().setMatchingStatus(p);
    }

    private JsonObject validate(Map<String, Object> map) {
        if (map == null) {
            return new JsonObject();
        }
        JsonObject obj = JsonObject.mapFrom(map);

        List<String> allValidFields = obj.fieldNames().stream()
                .filter(field -> obj.getValue(field) instanceof JsonObject && validate(obj.getJsonObject(field).getMap()) instanceof JsonObject || obj.getValue(field) instanceof Boolean).collect(Collectors.toList());

        if (allValidFields.size() == obj.fieldNames().size()) {
            return obj;
        }

        throw new RuntimeException("Ignored/BusinessKey is not in correct form. It should be nested Json based on the nested Ignore/businessKeys attributes or True if its leaf level.");
    }

    private MatchingResult compare(JsonArray expected, JsonArray actual, JsonObject ignoreAttributes, JsonObject businessKey) {
        MatchingResult.Builder result = createStatus(MatchingStatus.P);
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
            return findBestMatchingAttrCount(exp, counter.get(), actual, ignoreAttributes, businessKey);
        }).collect(Collectors.toList());
        return calculateMaxMatchingAndMatch(expected, actual, crossResults, businessKey);
    }

    private MatchingResult calculateMaxMatchingAndMatch(JsonArray expected, JsonArray actual, List<List<MatchingResult>> crossResults, JsonObject businessKey) {
        boolean[][] matrix = new boolean[expected.size()][actual.size()];
        Map<String, MatchingResult> diffObj = new HashMap<>();

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

        //Sort to make sure that BusinessKey matches are prioritized over non business Keys matches
        nonMatching.sort((lsr1, lsr2) -> {
            lsr1.sort(this::sort);
            lsr2.sort(this::sort);

            MatchingResult r1 = lsr1.iterator().next();
            MatchingResult r2 = lsr2.iterator().next();

            return sort(r1, r2);
        });

        nonMatching.forEach(obj -> diffObj.put(String.valueOf(obj.iterator().next().getElemIndex()), findBestMatchedItemAndPopulateMatrix(obj, matrix)));

        MatchingResult.Builder result = createStatus(finalStatus.get() ? MatchingStatus.P : MatchingStatus.F);
        if (!finalStatus.get()) {
            result.setActualValue(result.create()).setExpectedValue(expected).setDifference(diffObj);
        }
        return result.create();
    }

    private void blockActualColumn(boolean[][] matrix, MatchingResult result) {
        for (int i = 0; i < matrix.length; i++) {
            matrix[i][result.getMatIndex()] = true;
        }
    }

    private MatchingResult findBestMatchedItemAndPopulateMatrix(List<MatchingResult> allMatches, boolean[][] matrix) {
        //Sort to make sure that BusinessKey matches are prioritized over non business Keys matches
        allMatches.sort(this::sort);

        MatchingResult matchedObj = allMatches.stream().filter(obj -> obj.getElemIndex() != null && obj.getMatIndex() != null && !matrix[obj.getElemIndex()][obj.getMatIndex()]).findFirst().orElse(null);
        if (matchedObj != null) {
            blockActualColumn(matrix, matchedObj);
        } else {
            matchedObj = createStatus(MatchingStatus.NE).create();
        }
        return matchedObj;
    }

    private int sort(MatchingResult r1, MatchingResult r2) {
        return r1.isOnlyKeyMatching() && !r1.isOnlyKeyMatching()
                ? -1 : r1.isOnlyKeyMatching() && r2.isOnlyKeyMatching() && r1.getCount() > r2.getCount()
                ? -1 : r1.isOnlyKeyMatching() && r2.isOnlyKeyMatching() && r1.getCount() != null && r1.getCount().equals(r2.getCount())
                ? 0 : r2.isOnlyKeyMatching() && !r1.isOnlyKeyMatching()
                ? 1 : r1.getCount() != null && r2.getCount() != null ? r1.getCount() - r2.getCount() : 0;
    }

    private List<MatchingResult> findBestMatchingAttrCount(Object exp, int elemIndex, JsonArray array, JsonObject ignored, JsonObject businessKey) {
        if (exp == null || array == null) {
            LOGGER.info("Either obj to match or array is null");
            return new LinkedList<>();
        } else if (array.isEmpty()) {
            //If Actual array is blank, all elemnts from expected are not matching
            return Collections.singletonList(createStatus(MatchingStatus.F)
                    .setElementIndex(elemIndex)
                    .setExpectedValue(exp)
                    .create());
        }

        AtomicInteger bestMatchIndex = new AtomicInteger(-1);
        return array.stream().map(act -> {
            bestMatchIndex.set(bestMatchIndex.get() + 1);
            MatchingResult.Builder result = createStatus(MatchingStatus.F)
                    .setMatchingCount(0)
                    .setMatchingIndex(bestMatchIndex.get())
                    .setElementIndex(elemIndex);
            if (isPrimitive(exp) && isPrimitive(act)) {
                if (exp.equals(act)) {
                    result.setMatchingStatus(MatchingStatus.P).setMatchingCount(NEG_INFINITY);
                }
            } else if (exp instanceof JsonObject && act instanceof JsonObject) {
                result = compare((JsonObject) exp, (JsonObject) act, ignored, businessKey).newBuilder().setMatchingIndex(bestMatchIndex.get()).setElementIndex(elemIndex);
            }

            //TODO: CODE FOR [[],[],[]]

            if (result.getMatchingStatus() == MatchingStatus.F) {
                failMatchingStatus(exp, act, result.setMatchingIndex(bestMatchIndex.get()), result.getDifference());
            }
            return result.create();
        }).collect(Collectors.toList());
    }

    private MatchingResult compare(JsonObject exp, JsonObject act, JsonObject ignored, JsonObject businessKey) {
        MatchingResult.Builder finalStatusObj = createStatus(MatchingStatus.P);
        if (exp == null && act == null) {
            return finalStatusObj.setMatchingCount(NEG_INFINITY).create();
        } else if (exp == null || act == null) {
            LOGGER.info("Either obj to match or actual is null");
            return finalStatusObj.setMatchingStatus(MatchingStatus.F).setActualValue(act).setExpectedValue(exp).create();
        }
        AtomicInteger matchingCount = new AtomicInteger(0);
        Map<String, MatchingResult> diffObj = new HashMap<>();
        finalStatusObj.setMatchingCount(NEG_INFINITY).setDifference(diffObj);

        exp.iterator().forEachRemaining(item -> {
            String attr = item.getKey();
            Object expVal = item.getValue();
            Object actVal = act.getValue(attr);
            MatchingResult.Builder internalDiff = createStatus(MatchingStatus.P);

            boolean keyComparison = businessKey.containsKey(attr);
            boolean ignoreAttr = ignored.containsKey(attr);

            internalDiff.setAlgo(keyComparison ? MatchingAlgo.K : MatchingAlgo.M);

            if (expVal == null && actVal == null) {
                matchingCount.set(matchingCount.get() + 1);
            } else if (expVal == null || actVal == null) {
                assignStatusAndExpAct(expVal, actVal, internalDiff, MatchingStatus.P);
                if (ignoreAttr) {
                    internalDiff.setMatchingStatus(MatchingStatus.IGN);
                } else {
                    internalDiff.setMatchingStatus(MatchingStatus.F);
                    finalStatusObj.setMatchingStatus(MatchingStatus.F);
                }

            } else if (isPrimitive(expVal) && isPrimitive(actVal)) {
                if (ignoreAttr) {
                    assignStatusAndExpAct(expVal, actVal, internalDiff, MatchingStatus.IGN);
                } else {
                    boolean isMatching = expVal.equals(actVal);
                    matchingCount.set(matchingCount.get() + (isMatching ? 1 : 0));
                    if (!isMatching) {
                        assignStatusAndExpAct(expVal, actVal, internalDiff, MatchingStatus.F);
                        finalStatusObj.setMatchingStatus(MatchingStatus.F);
                    }
                }
            } else if (isComparable(expVal) && isComparable(actVal)) {
                if (ignoreAttr) {
                    assignStatusAndExpAct(expVal, actVal, internalDiff, MatchingStatus.IGN);
                } else {
                    boolean isMatching = ((Comparable) expVal).compareTo(actVal) == 0;
                    matchingCount.set(matchingCount.get() + (isMatching ? 1 : 0));
                    if (!isMatching) {
                        assignStatusAndExpAct(expVal, actVal, internalDiff, MatchingStatus.F);
                        finalStatusObj.setMatchingStatus(MatchingStatus.F);
                    }
                }
            } else if (expVal instanceof JsonObject && actVal instanceof JsonObject) {
                if (ignoreAttr && (ignored.getValue(attr) instanceof Boolean) && ignored.getBoolean(attr)) {
                    assignStatusAndExpAct(expVal, actVal, internalDiff, MatchingStatus.IGN);
                } else {
                    MatchingResult result = compare((JsonObject) expVal, (JsonObject) actVal,
                            ignoreAttr && ignored.getValue(attr) instanceof JsonObject ? ignored.getJsonObject(attr) : new JsonObject(),
                            keyComparison ? businessKey.getJsonObject(attr) : new JsonObject());
                    if (result.isAllMatching() || result.isOnlyKeyMatching()) {
                        matchingCount.set(matchingCount.get() + 1);
                    } else {
                        failMatchingStatus(expVal, actVal, internalDiff.setMatchingStatus(result.getStatus()), result.getDiff());
                        finalStatusObj.setMatchingStatus(result.getStatus());
                    }
                }
            } else if (expVal instanceof JsonArray && actVal instanceof JsonArray) {

                if (ignoreAttr && (ignored.getValue(attr) instanceof Boolean) && ignored.getBoolean(attr)) {
                    assignStatusAndExpAct(expVal, actVal, internalDiff, MatchingStatus.IGN);
                } else {
                    JsonArray expValArray = (JsonArray) expVal;
                    JsonArray actValArray = (JsonArray) actVal;
                    MatchingResult result = compare(expValArray, actValArray,
                            ignoreAttr && ignored.getValue(attr) instanceof JsonObject ? ignored.getJsonObject(attr) : new JsonObject(),
                            keyComparison ? businessKey.getJsonObject(attr) : new JsonObject());
                    if (result.getStatus() == MatchingStatus.P) {
                        matchingCount.set(matchingCount.get() + 1);
                    } else {
                        failMatchingStatus(expVal, actVal, internalDiff.setMatchingStatus(result.getStatus()), result.getDiff());
                        finalStatusObj.setMatchingStatus(result.getStatus());
                    }
                }
            }

            diffObj.put(attr, internalDiff.create());
        });

        if (!finalStatusObj.isPassing()) {
            List<MatchingResult> keyMatches = finalStatusObj.getDifference().values().stream().filter(res -> res.getAlgo() == MatchingAlgo.K).collect(Collectors.toList());
            if (!businessKey.fieldNames().isEmpty()
                    && exp.fieldNames().containsAll(businessKey.fieldNames())) {
                if (!keyMatches.isEmpty() && keyMatches.stream().allMatch(MatchingResult::isAllMatching)) {
                    finalStatusObj.setMatchingStatus(MatchingStatus.PK);
                } else {
                    finalStatusObj.setMatchingStatus(MatchingStatus.NE);
                }
            }
            finalStatusObj.setActualValue(act).setExpectedValue(exp).setMatchingCount(matchingCount.get());
        }

        return finalStatusObj.create();
    }

    private void assignStatusAndExpAct(Object expVal, Object actVal, MatchingResult.Builder diff, MatchingStatus status) {
        diff.setMatchingStatus(status).setExpectedValue(expVal).setActualValue(actVal);
    }

    private void failMatchingStatus(Object expVal, Object actVal, MatchingResult.Builder builder, Map<String, MatchingResult> diff) {
        builder.setExpectedValue(expVal).setActualValue(actVal).setDifference(diff);
    }

    private boolean isPrimitive(Object o) {
        return o instanceof String || o instanceof Double || o instanceof Float || o instanceof Integer || o instanceof Boolean || o instanceof Long;
    }

    private boolean isComparable(Object expVal) {
        return expVal instanceof Comparable;
    }
}