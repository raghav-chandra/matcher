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
        JsonObject ignoreAttributes = ignored == null ? new JsonObject() : JsonObject.mapFrom(ignored);
        JsonObject key = businessKey == null ? new JsonObject() : JsonObject.mapFrom(businessKey);

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
        } else if (isPrimitive(expected) && isPrimitive(actual)) {
            boolean isMatching = expected.equals(actual);
            if (!isMatching) {
                result.setMatchingCount(1)
                        .setMatchingStatus(MatchingStatus.F)
                        .setExpectedValue(expected)
                        .setActualValue(actual);
            }
            return result.create();
        } else if (isComparable(expected) && isComparable(actual)) {
            boolean isMatching = ((Comparable) expected).compareTo(actual) == 0;
            if (!isMatching) {
                result.setMatchingCount(1)
                        .setMatchingStatus(MatchingStatus.F)
                        .setExpectedValue(expected)
                        .setActualValue(actual);
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

    private MatchingResult compare(JsonArray expected, JsonArray actual, JsonObject ignoreAttributes, JsonObject businessKey) {
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
            return findBestMatchingAttrCount(exp, counter.get(), actual, ignoreAttributes, businessKey);
        }).collect(Collectors.toList());
        return calculateMaxMatchingAndMatch(expected, actual, crossResults, businessKey);
    }

    private MatchingResult calculateMaxMatchingAndMatch(JsonArray expected, JsonArray actual, List<List<MatchingResult>> crossResults, JsonObject businessKey) {
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

        nonMatching.sort((matchingResults1, matchingResults2) -> matchingResults2.stream().mapToInt(val -> val.getCount() == null ? 0 : val.getCount()).max().orElse(0) - matchingResults1.stream().mapToInt(val -> val.getCount() == null ? 0 : val.getCount()).max().orElse(0));

        nonMatching.forEach(obj -> diffObj.put(String.valueOf(obj.iterator().next().getElemIndex()), findBestMatchedItemAndPopulateMatrix(obj, matrix, businessKey)));

        MatchingResult.Builder result = new MatchingResult.Builder()
                .setMatchingStatus(finalStatus.get() ? MatchingStatus.P : MatchingStatus.F);
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

    private MatchingResult findBestMatchedItemAndPopulateMatrix(List<MatchingResult> allMatches, boolean[][] matrix, JsonObject businessKey) {
//        List<MatchingResult> sorted = allMatches.stream().sorted(Comparator.comparingInt(MatchingResult::getCount)).collect(Collectors.toList());

        List<MatchingResult> matches = getMatchingObject(allMatches, businessKey);

        MatchingResult matchedObj = matches.stream().filter(obj -> obj.getElemIndex() != null && obj.getMatIndex() != null && !matrix[obj.getElemIndex()][obj.getMatIndex()]).findFirst().orElse(null);
        if (matchedObj != null) {
            blockActualColumn(matrix, matchedObj);
        } else {
            matchedObj = new MatchingResult.Builder().setMatchingStatus(MatchingStatus.NE).create();
        }
        return matchedObj;
    }

    private List<MatchingResult> getMatchingObject(List<MatchingResult> allMatches, JsonObject businessKey) {
        List<MatchingResult> matches = allMatches.stream().filter(match -> {
            Object actual = match.getAct();
            Object expected = match.getExp();
            if (businessKey != null && !isPrimitive(actual) && !isPrimitive(expected) && !isComparable(expected) && !isComparable(actual)) {
                boolean isExpList = expected instanceof List || expected instanceof JsonArray;
                boolean isActList = actual instanceof List || actual instanceof JsonArray;
                if (isExpList && isActList) {
                    throw new RuntimeException("List of list is not supported for comparison");
                }

                JsonObject act = (JsonObject) actual;
                JsonObject exp = (JsonObject) expected;

                return isMatching(act, exp, businessKey);
            }
            return true;
        }).collect(Collectors.toList());
        return matches.stream().sorted(Comparator.comparingInt(MatchingResult::getCount)).collect(Collectors.toList());
    }

    private boolean isMatching(JsonObject act, JsonObject exp, JsonObject businessKey) {
        if (businessKey == null) {
            return false;
        }
        if (act.fieldNames().containsAll(businessKey.fieldNames()) && exp.fieldNames().containsAll(businessKey.fieldNames())) {
            return businessKey.fieldNames().stream().allMatch(attr -> {
                Object actVal = act.getValue(attr);
                Object expVal = exp.getValue(attr);
                if (isPrimitive(actVal) && isPrimitive(expVal)) {
                    return expVal.equals(actVal);
                } else if (isComparable(expVal) && isComparable(actVal)) {
                    return ((Comparable) expVal).compareTo(actVal) == 0;
                } else if (actVal instanceof JsonObject && expVal instanceof JsonObject) {
                    if (businessKey.getValue(attr) instanceof JsonObject) {
                        return isMatching((JsonObject) actVal, (JsonObject) expVal, (JsonObject) businessKey.getValue(attr));
                    } else {
                        return false;
                    }
                } else if (actVal instanceof JsonArray && expVal instanceof JsonArray) {
                    if (businessKey.getValue(attr) instanceof JsonObject) {
                        //TODO: Array matching to be implemented
                        return isMatching((JsonArray) actVal, (JsonArray) expVal, (JsonObject) businessKey.getValue(attr));
                    } else {
                        return false;
                    }
                } else {
                    return false;
                }
            });
        }
        return false;
    }

    //Nested Array check based on the Key.
    private boolean isMatching(JsonArray actVal, JsonArray expVal, JsonObject businessKey) {
        /*if (businessKey != null) {
            return actVal.stream().anyMatch(act -> {
                return expVal.stream().anyMatch(exp -> {
                    return isMatching(act, exp, businessKey);
                });
            });
        }*/
        return false;
    }

    private List<MatchingResult> findBestMatchingAttrCount(Object exp, int elemIndex, JsonArray array, JsonObject ignored, JsonObject businessKey) {
        if (exp == null || array == null) {
            LOGGER.info("Either obj to match or array is null");
            return new LinkedList<>();
        } else if (array.isEmpty()) {
            //If Actual array is blank, all elemnts from expected are not matching
            return Collections.singletonList(new MatchingResult.Builder()
                    .setMatchingStatus(MatchingStatus.F)
                    .setElementIndex(elemIndex)
                    .setExpectedValue(exp)
                    .create());
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
                result = compare((JsonObject) exp, (JsonObject) act, ignored, businessKey).newBuilder().setMatchingIndex(bestMatchIndex.get()).setElementIndex(elemIndex);
            }
            if (result.getMatchingStatus() == MatchingStatus.F) {
                result.setMatchingIndex(bestMatchIndex.get()).setExpectedValue(exp).setActualValue(act).setDifference(result.getDifference());
            }
            return result.create();
        }).collect(Collectors.toList());
    }

    private MatchingResult compare(JsonObject exp, JsonObject act, JsonObject ignored, JsonObject businessKey) {
        MatchingResult.Builder finalStatusObj = new MatchingResult.Builder().setMatchingStatus(MatchingStatus.P);
        if (exp == null && act == null) {
            return finalStatusObj.setMatchingCount(NEG_INFINITY).create();
        } else if (exp == null || act == null) {
            LOGGER.info("Either obj to match or actual is null");
            return finalStatusObj.setMatchingStatus(MatchingStatus.F).setActualValue(act).setExpectedValue(exp).create();
        }
        AtomicInteger matchingCount = new AtomicInteger(0);
        Map<String, Object> diffObj = new HashMap<>();
        finalStatusObj.setMatchingCount(NEG_INFINITY).setDifference(diffObj);

        exp.iterator().forEachRemaining(item -> {
            String attr = item.getKey();
            Object expVal = item.getValue();
            Object actVal = act.getValue(attr);
            MatchingResult.Builder internalDiff = new MatchingResult.Builder().setMatchingStatus(MatchingStatus.P);
            if (expVal == null && actVal == null) {
                matchingCount.set(matchingCount.get() + 1);
            } else if (expVal == null || actVal == null) {
                if (ignored.containsKey(attr)) {
                    internalDiff.setMatchingStatus(MatchingStatus.IGN);
                } else {
                    internalDiff.setMatchingStatus(MatchingStatus.F);
                    finalStatusObj.setMatchingStatus(MatchingStatus.F);
                }
                internalDiff.setExpectedValue(expVal);
                internalDiff.setActualValue(actVal);
            } else if (isPrimitive(expVal) && isPrimitive(actVal)) {
                if (ignored.containsKey(attr)) {
                    internalDiff.setMatchingStatus(MatchingStatus.IGN);
                    internalDiff.setExpectedValue(expVal);
                    internalDiff.setActualValue(actVal);
                } else {
                    boolean isMatching = expVal.equals(actVal);
                    matchingCount.set(matchingCount.get() + (isMatching ? 1 : 0));
                    if (!isMatching) {
                        internalDiff.setMatchingStatus(MatchingStatus.F);
                        internalDiff.setExpectedValue(expVal);
                        internalDiff.setActualValue(actVal);
                        finalStatusObj.setMatchingStatus(MatchingStatus.F);
                    }
                }
            } else if (isComparable(expVal) && isComparable(actVal)) {
                if (ignored.containsKey(attr)) {
                    internalDiff.setMatchingStatus(MatchingStatus.IGN);
                    internalDiff.setExpectedValue(expVal);
                    internalDiff.setActualValue(actVal);
                } else {
                    boolean isMatching = ((Comparable) expVal).compareTo(actVal) == 0;
                    matchingCount.set(matchingCount.get() + (isMatching ? 1 : 0));
                    if (!isMatching) {
                        internalDiff.setMatchingStatus(MatchingStatus.F);
                        internalDiff.setExpectedValue(expVal);
                        internalDiff.setActualValue(actVal);
                        finalStatusObj.setMatchingStatus(MatchingStatus.F);
                    }
                }
            } else if (expVal instanceof JsonObject && actVal instanceof JsonObject) {

                JsonObject ignAttributes = new JsonObject();
                if (ignored.containsKey(attr)) {
                    //TODO: check the value of attr. If its Json pass it otherwise ignore the whole
                    ignAttributes = ignored.getJsonObject(attr);
                }

                JsonObject bussKey = new JsonObject();
                if (businessKey.containsKey(attr)) {
                    bussKey = businessKey.getJsonObject(attr);
                }

                MatchingResult result = compare((JsonObject) expVal, (JsonObject) actVal, ignAttributes, bussKey);
                if (result.getStatus() == MatchingStatus.P) {
                    matchingCount.set(matchingCount.get() + 1);
                } else {
                    internalDiff.setMatchingStatus(MatchingStatus.F);
                    internalDiff.setExpectedValue(expVal);
                    internalDiff.setActualValue(actVal);
                    internalDiff.setDifference(result.getDiff());
                    finalStatusObj.setMatchingStatus(MatchingStatus.F);
                }
            } else if (expVal instanceof JsonArray && actVal instanceof JsonArray) {
                JsonObject ignAttributes = new JsonObject();
                if (ignored.containsKey(attr)) {
                    //TODO: check the value of attr. If its Json pass it otherwise ignore the whole
                    ignAttributes = ignored.getJsonObject(attr);
                }

                JsonObject bussKey = new JsonObject();
                if (businessKey.containsKey(attr)) {
                    bussKey = businessKey.getJsonObject(attr);
                }

                JsonArray expValArray = (JsonArray) expVal;
                JsonArray actValArray = (JsonArray) actVal;
                MatchingResult result = compare(expValArray, actValArray, ignAttributes, bussKey);
                if (result.getStatus() == MatchingStatus.P) {
                    matchingCount.set(matchingCount.get() + 1);
                } else {
                    internalDiff.setMatchingStatus(MatchingStatus.F);
                    internalDiff.setExpectedValue(expVal);
                    internalDiff.setActualValue(actVal);
                    internalDiff.setDifference(result.getDiff());
                    finalStatusObj.setMatchingStatus(MatchingStatus.F);
                }
            }

            diffObj.put(attr, internalDiff.create());
        });
        if (finalStatusObj.getMatchingStatus() == MatchingStatus.F) {
            finalStatusObj.setActualValue(act).setExpectedValue(exp).setMatchingCount(matchingCount.get());
        }
        return finalStatusObj.create();
    }

    private boolean isPrimitive(Object o) {
        return o instanceof String || o instanceof Double || o instanceof Float || o instanceof Integer || o instanceof Boolean || o instanceof Long;
    }

    private boolean isComparable(Object expVal) {
        return expVal instanceof Comparable;
    }

}
