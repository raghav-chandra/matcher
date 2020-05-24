package com.rags.tools.matcher;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.junit.Before;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * @author Raghav Chandra (raghav.yo@gmail.com)
 * @version 1.0
 */
public class JsonMatcherTest {

    private Matcher matcher;

    @Before
    public void setup() {
        matcher = new JsonMatcher();
    }

    @Test
    public void testNull() {
        MatchingResult result = matcher.compare(null, null);
        assertEquals(MatchingStatus.P, result.getStatus());
    }

    @Test
    public void testOnlyOneNull() {
        MatchingResult result = matcher.compare(null, 23);
        assertEquals(MatchingStatus.F, result.getStatus());
        assertEquals(23, result.getAct());
        assertNull(result.getExp());
    }

    @Test
    public void testPrimitiveComparison() {
        MatchingResult result = matcher.compare("Raghav", "Chandra");
        assertEquals(MatchingStatus.F, result.getStatus());
        assertEquals("Raghav", result.getExp());
        assertEquals("Chandra", result.getAct());
    }

    @Test
    public void testComparableComparison() {
        MatchingResult result = matcher.compare(BigDecimal.valueOf(10), BigDecimal.valueOf(11.00));
        assertEquals(MatchingStatus.F, result.getStatus());
    }

    @Test
    public void testMatchListVsObject() {
        MatchingResult result = matcher.compare(new JsonObject().put("val", 1), new JsonArray().add(1));
        assertEquals(MatchingStatus.OM, result.getStatus());
    }

    @Test
    public void testBasicJsonObjectComparison() {
        JsonObject expected = new JsonObject()
                .put("name", "Raghav Chandra")
                .put("kerberos", "charag")
                .put("mobile", 8867987654L)
                .put("tension", "NO")
                .put("id", 10110);

        JsonObject actual = new JsonObject()
                .put("name", "Raghav Chandra")
                .put("kerberos", "charag")
                .put("mobile", 9065065882L)
                .put("id", 10110);

        MatchingResult result = matcher.compare(expected, actual);

        assertEquals("F", result.getStatus().name());
        assertEquals(expected, result.getExp());
        assertEquals(actual, result.getAct());
        assertEquals((Integer) 3, result.getCount());

        Map<String, Object> diff = result.getDiff();
        assertEquals(MatchingStatus.P, ((MatchingResult) diff.get("name")).getStatus());
        assertEquals(MatchingStatus.P, ((MatchingResult) diff.get("kerberos")).getStatus());
        assertEquals(MatchingStatus.F, ((MatchingResult) diff.get("mobile")).getStatus());
        assertEquals(8867987654L, ((MatchingResult) diff.get("mobile")).getExp());
        assertEquals(9065065882L, ((MatchingResult) diff.get("mobile")).getAct());
        assertEquals(MatchingStatus.P, ((MatchingResult) diff.get("id")).getStatus());

        assertEquals(MatchingStatus.F, ((MatchingResult) diff.get("tension")).getStatus());
        assertEquals("NO", ((MatchingResult) diff.get("tension")).getExp());
        assertNull(((MatchingResult) diff.get("tension")).getAct());
    }

    @Test
    public void testNestedJsonObjectComparison() {
        JsonObject expected = new JsonObject()
                .put("name", "Raghav Chandra")
                .put("kerberos", "charag")
                .put("mobile", 8867987654L)
                .put("id", 10110)
                .put("add", new JsonObject()
                        .put("city", "Prayagraj")
                        .put("state", "UP")
                        .put("pin", 211002)
                        .put("landmark", "mosque"));

        JsonObject actual = new JsonObject()
                .put("name", "Raghav Chandra")
                .put("kerberos", "charag")
                .put("mobile", 8867987654L)
                .put("id", 10110)
                .put("add", new JsonObject()
                        .put("city", "Prayagraj")
                        .put("state", "UP")
                        .put("pin", 211002)
                        .put("landmark", "temple"));

        MatchingResult result = matcher.compare(expected, actual);


        assertEquals(MatchingStatus.F, result.getStatus());
        assertEquals(expected, result.getExp());
        assertEquals(actual, result.getAct());
        assertEquals((Integer) 4, result.getCount());

        Map<String, Object> diff = result.getDiff();
        assertEquals(MatchingStatus.P, ((MatchingResult) diff.get("name")).getStatus());
        assertEquals(MatchingStatus.P, ((MatchingResult) diff.get("kerberos")).getStatus());
        assertEquals(MatchingStatus.P, ((MatchingResult) diff.get("mobile")).getStatus());
        assertEquals(MatchingStatus.P, ((MatchingResult) diff.get("id")).getStatus());

        assertEquals(MatchingStatus.F, ((MatchingResult) diff.get("add")).getStatus());

        Map<String, Object> addDiff = ((MatchingResult) diff.get("add")).getDiff();
        assertEquals(MatchingStatus.P, ((MatchingResult) addDiff.get("city")).getStatus());
        assertEquals(MatchingStatus.P, ((MatchingResult) addDiff.get("state")).getStatus());
        assertEquals(MatchingStatus.P, ((MatchingResult) addDiff.get("pin")).getStatus());
        assertEquals(MatchingStatus.F, ((MatchingResult) addDiff.get("landmark")).getStatus());
        assertEquals("mosque", ((MatchingResult) addDiff.get("landmark")).getExp());
        assertEquals("temple", ((MatchingResult) addDiff.get("landmark")).getAct());
    }

    @Test
    public void testNestedJsonObjectComparisonWithIgnoredAttribute() {
        JsonObject expected = new JsonObject()
                .put("name", "Raghav Chandra")
                .put("kerberos", "charag")
                .put("mobile", 8867987654L)
                .put("id", 10110)
                .put("add", new JsonObject()
                        .put("city", "Prayagraj")
                        .put("state", "UP")
                        .put("pin", 211002)
                        .put("landmark", "mosque"));

        JsonObject actual = new JsonObject()
                .put("name", "Raghav Chandra")
                .put("kerberos", "charag")
                .put("mobile", 8867987654L)
                .put("id", 10110)
                .put("add", new JsonObject()
                        .put("city", "Prayagraj")
                        .put("state", "UP")
                        .put("pin", 211003)
                        .put("landmark", "temple"));

        Map<String, Object> internalIgnored = new HashMap<>();
        internalIgnored.putIfAbsent("landmark", true);
        Map<String, Object> ignored = new HashMap<>();
        ignored.putIfAbsent("add", internalIgnored);
        ignored.putIfAbsent("name", true);

        MatchingResult result = matcher.compare(expected, actual, ignored);
        assertEquals((Integer) 3, result.getCount());
        assertEquals(MatchingStatus.F, result.getStatus());

        Map<String, Object> diff = result.getDiff();
        assertEquals(MatchingStatus.IGN, ((MatchingResult) diff.get("name")).getStatus());
        assertEquals(MatchingStatus.P, ((MatchingResult) diff.get("kerberos")).getStatus());
        assertEquals(MatchingStatus.P, ((MatchingResult) diff.get("mobile")).getStatus());
        assertEquals(MatchingStatus.P, ((MatchingResult) diff.get("id")).getStatus());

        assertEquals(MatchingStatus.F, ((MatchingResult) diff.get("add")).getStatus());

        Map<String, Object> addDiff = ((MatchingResult) diff.get("add")).getDiff();
        assertEquals(MatchingStatus.P, ((MatchingResult) addDiff.get("city")).getStatus());
        assertEquals(MatchingStatus.P, ((MatchingResult) addDiff.get("state")).getStatus());
        assertEquals(MatchingStatus.F, ((MatchingResult) addDiff.get("pin")).getStatus());
        assertEquals(MatchingStatus.IGN, ((MatchingResult) addDiff.get("landmark")).getStatus());
        assertEquals(211002, ((MatchingResult) addDiff.get("pin")).getExp());
        assertEquals(211003, ((MatchingResult) addDiff.get("pin")).getAct());
    }


    @Test
    public void testSimpleArrayComparison() {
        JsonObject expected = new JsonObject()
                .put("name", "Raghav Chandra")
                .put("fakeName", new JsonArray().add("Baba").add("Santru"));

        JsonObject actual = new JsonObject()
                .put("name", "Raghav Chandra")
                .put("fakeName", new JsonArray().add("Baba"));
        MatchingResult result = matcher.compare(expected, actual);

        assertNotNull(result);

        assertEquals(MatchingStatus.F, result.getStatus());
        assertEquals(expected, result.getExp());
        assertEquals(actual, result.getAct());
        assertEquals((Integer) 1, result.getCount());

        Map<String, Object> diff = result.getDiff();
        assertEquals(MatchingStatus.P, ((MatchingResult) diff.get("name")).getStatus());

        assertEquals(MatchingStatus.F, ((MatchingResult) diff.get("fakeName")).getStatus());
        Map<String, Object> fNDiff = ((MatchingResult) diff.get("fakeName")).getDiff();
        assertEquals(MatchingStatus.P, ((MatchingResult) fNDiff.get("0")).getStatus());
        assertEquals(MatchingStatus.NE, ((MatchingResult) fNDiff.get("1")).getStatus());
    }

    @Test
    public void testArrayComparisonWhenActualExpectingZeroElem() {
        JsonObject expected = new JsonObject()
                .put("name", "Raghav Chandra")
                .put("fakeName", new JsonArray().add("Baba").add("Santru"));

        JsonObject actual = new JsonObject()
                .put("name", "Raghav Chandra")
                .put("fakeName", new JsonArray());
        MatchingResult result = matcher.compare(expected, actual);

        assertNotNull(result);

        assertEquals("F", result.getStatus().name());
        assertEquals(expected, result.getExp());
        assertEquals(actual, result.getAct());
        assertEquals((Integer) 1, result.getCount());

        Map<String, Object> diff = result.getDiff();
        assertEquals(MatchingStatus.P, ((MatchingResult) diff.get("name")).getStatus());

        assertEquals(MatchingStatus.F, ((MatchingResult) diff.get("fakeName")).getStatus());
        Map<String, Object> fNDiff = ((MatchingResult) diff.get("fakeName")).getDiff();
        assertEquals(MatchingStatus.NE, ((MatchingResult) fNDiff.get("0")).getStatus());
        assertEquals(MatchingStatus.NE, ((MatchingResult) fNDiff.get("1")).getStatus());
    }

    @Test
    public void testComplexArrayComparison() {
        JsonObject expected = new JsonObject()
                .put("name", "Raghav Chandra")
                .put("fakeName", new JsonArray()
                        .add(new JsonObject()
                                .put("firstName", "Foo")
                                .put("secondName", "Bar"))
                        .add("Santru"));

        JsonObject actual = new JsonObject()
                .put("name", "Raghav Chandra")
                .put("fakeName", new JsonArray()
                        .add(new JsonObject()
                                .put("firstName", "Foo")
                                .put("secondName", "Bar23"))
                        .add("Santru"));

        MatchingResult result = matcher.compare(expected, actual);

        assertNotNull(result);

        assertEquals("F", result.getStatus().name());
        assertEquals(expected, result.getExp());
        assertEquals(actual, result.getAct());
        assertEquals((Integer) 1, result.getCount());

        Map<String, Object> diff = result.getDiff();
        assertEquals(MatchingStatus.P, ((MatchingResult) diff.get("name")).getStatus());
        assertEquals(MatchingStatus.F, ((MatchingResult) diff.get("fakeName")).getStatus());

        Map<String, Object> fNDiff = ((MatchingResult) diff.get("fakeName")).getDiff();
        assertEquals(MatchingStatus.P, ((MatchingResult) fNDiff.get("1")).getStatus());

        assertEquals(MatchingStatus.F, ((MatchingResult) fNDiff.get("0")).getStatus());
        Map<String, Object> fnArrDiff = ((MatchingResult) fNDiff.get("0")).getDiff();
        assertEquals(MatchingStatus.P, ((MatchingResult) fnArrDiff.get("firstName")).getStatus());
        assertEquals(MatchingStatus.F, ((MatchingResult) fnArrDiff.get("secondName")).getStatus());

        assertEquals("Bar", ((MatchingResult) fnArrDiff.get("secondName")).getExp());
        assertEquals("Bar23", ((MatchingResult) fnArrDiff.get("secondName")).getAct());
    }

    @Test
    public void testComplexArrayComparisonWithIgnored() {
        JsonObject expected = new JsonObject()
                .put("name", "Raghav Chandra")
                .put("fakeName", new JsonArray()
                        .add(new JsonObject()
                                .put("firstName", "Foo")
                                .put("secondName", "Bar"))
                        .add("Santru"));

        JsonObject actual = new JsonObject()
                .put("name", "Raghav Chandra")
                .put("fakeName", new JsonArray()
                        .add(new JsonObject()
                                .put("firstName", "Foo")
                                .put("secondName", "Bar23"))
                        .add("Santru"));

        Map<String, Object> internalIgnored = new HashMap<>();
        internalIgnored.putIfAbsent("firstName", true);
        Map<String, Object> ignored = new HashMap<>();
        ignored.putIfAbsent("fakeName", internalIgnored);

        MatchingResult result = matcher.compare(expected, actual, ignored);

        assertNotNull(result);

        assertEquals("F", result.getStatus().name());
        assertEquals(expected, result.getExp());
        assertEquals(actual, result.getAct());
        assertEquals((Integer) 1, result.getCount());

        Map<String, Object> diff = result.getDiff();
        assertEquals(MatchingStatus.P, ((MatchingResult) diff.get("name")).getStatus());
        assertEquals(MatchingStatus.F, ((MatchingResult) diff.get("fakeName")).getStatus());

        Map<String, Object> fNDiff = ((MatchingResult) diff.get("fakeName")).getDiff();
        assertEquals(MatchingStatus.P, ((MatchingResult) fNDiff.get("1")).getStatus());

        assertEquals(MatchingStatus.F, ((MatchingResult) fNDiff.get("0")).getStatus());
        Map<String, Object> fnArrDiff = ((MatchingResult) fNDiff.get("0")).getDiff();

        assertEquals(MatchingStatus.IGN, ((MatchingResult) fnArrDiff.get("firstName")).getStatus());
        assertEquals(MatchingStatus.F, ((MatchingResult) fnArrDiff.get("secondName")).getStatus());

        assertEquals("Bar", ((MatchingResult) fnArrDiff.get("secondName")).getExp());
        assertEquals("Bar23", ((MatchingResult) fnArrDiff.get("secondName")).getAct());
    }

    @Test
    public void testMissingElement() {
        List<String> expected = List.of("Raghav", "Chandra");
        List<String> actual = List.of("Raghav");
        MatchingResult result = new JsonMatcher().compare(expected, actual);
        assertEquals(MatchingStatus.F, result.getStatus());

        Map<String, Object> diff = result.getDiff();
        assertEquals(MatchingStatus.P, ((MatchingResult) diff.get("0")).getStatus());
        assertEquals(MatchingStatus.NE, ((MatchingResult) diff.get("1")).getStatus());
    }

    @Test
    public void testComplexArrayComparisonWithBusinessKey() {
        JsonArray expected = new JsonArray()
                .add(new JsonObject()
                        .put("name", "Raghav")
                        .put("id", 1234)
                        .put("No", 654321))
                .add(new JsonObject()
                        .put("name", "Chandra")
                        .put("id", 1)
                        .put("No", 987654321));

        JsonArray actual = new JsonArray()
                .add(new JsonObject()
                        .put("name", "Chandra")
                        .put("id", 1234)
                        .put("No", 987654321));

        Map<String, Object> key = new HashMap<>();
        key.putIfAbsent("id", true);
        MatchingResult results = new JsonMatcher().compare(expected, actual, new HashMap<>(), key);

        assertEquals(MatchingStatus.F, results.getStatus());
        Map<String, Object> diff = results.getDiff();

        assertEquals(MatchingStatus.F, ((MatchingResult) diff.get("0")).getStatus());
        assertEquals(MatchingStatus.NE, ((MatchingResult) diff.get("1")).getStatus());

    }
}