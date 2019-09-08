package com.rags.tools.matcher;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.junit.Before;
import org.junit.Test;

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
}
