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
        assertEquals("P", ((Map<String, Object>) diff.get("name")).get("status"));
        assertEquals("P", ((Map<String, Object>) diff.get("kerberos")).get("status"));
        assertEquals("F", ((Map<String, Object>) diff.get("mobile")).get("status"));
        assertEquals(8867987654L, ((Map<String, Object>) diff.get("mobile")).get("exp"));
        assertEquals(9065065882L, ((Map<String, Object>) diff.get("mobile")).get("act"));
        assertEquals("P", ((Map<String, Object>) diff.get("id")).get("status"));

        assertEquals("F", ((Map<String, Object>) diff.get("tension")).get("status"));
        assertEquals("NO", ((Map<String, Object>) diff.get("tension")).get("exp"));
        assertNull(((Map<String, Object>) diff.get("tension")).get("act"));
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


        assertEquals("F", result.getStatus().name());
        assertEquals(expected, result.getExp());
        assertEquals(actual, result.getAct());
        assertEquals((Integer) 4, result.getCount());

        Map<String, Object> diff = result.getDiff();
        assertEquals("P", ((Map<String, Object>) diff.get("name")).get("status"));
        assertEquals("P", ((Map<String, Object>) diff.get("kerberos")).get("status"));
        assertEquals("P", ((Map<String, Object>) diff.get("mobile")).get("status"));
        assertEquals("P", ((Map<String, Object>) diff.get("id")).get("status"));

        assertEquals("F", ((Map<String, Object>) diff.get("add")).get("status"));

        Map<String, Object> addDiff = (Map<String, Object>) ((Map<String, Object>) diff.get("add")).get("diff");
        assertEquals("P", ((Map<String, Object>) addDiff.get("city")).get("status"));
        assertEquals("P", ((Map<String, Object>) addDiff.get("state")).get("status"));
        assertEquals("P", ((Map<String, Object>) addDiff.get("pin")).get("status"));
        assertEquals("F", ((Map<String, Object>) addDiff.get("landmark")).get("status"));
        assertEquals("mosque", ((Map<String, Object>) addDiff.get("landmark")).get("exp"));
        assertEquals("temple", ((Map<String, Object>) addDiff.get("landmark")).get("act"));
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

        assertEquals("F", result.getStatus().name());
        assertEquals(expected, result.getExp());
        assertEquals(actual, result.getAct());
        assertEquals((Integer) 1, result.getCount());

        Map<String, Object> diff = result.getDiff();
        assertEquals("P", ((Map<String, Object>) diff.get("name")).get("status"));

        assertEquals("F", ((Map<String, Object>) diff.get("fakeName")).get("status"));
        Map<String, Object> fNDiff = (Map<String, Object>) ((Map<String, Object>) diff.get("fakeName")).get("diff");
        assertEquals("P", ((Map<String, Object>) fNDiff.get("0")).get("status"));
        assertEquals("NE", ((Map<String, Object>) fNDiff.get("1")).get("status"));


        JsonObject actual1 = new JsonObject()
                .put("name", "Raghav Chandra")
                .put("fakeName", new JsonArray());
        matcher.compare(expected, actual1);

        System.out.println(result);

    }
}
