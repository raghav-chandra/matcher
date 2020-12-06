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

        Map<String, MatchingResult> diff = result.getDiff();
        assertEquals(MatchingStatus.P, diff.get("name").getStatus());
        assertEquals(MatchingStatus.P, diff.get("kerberos").getStatus());
        assertEquals(MatchingStatus.F, diff.get("mobile").getStatus());
        assertEquals(8867987654L, diff.get("mobile").getExp());
        assertEquals(9065065882L, diff.get("mobile").getAct());
        assertEquals(MatchingStatus.P, diff.get("id").getStatus());

        assertEquals(MatchingStatus.NE, diff.get("tension").getStatus());
        assertEquals("NO", diff.get("tension").getExp());
        assertNull(diff.get("tension").getAct());
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

        Map<String, MatchingResult> diff = result.getDiff();
        assertEquals(MatchingStatus.P, diff.get("name").getStatus());
        assertEquals(MatchingStatus.P, diff.get("kerberos").getStatus());
        assertEquals(MatchingStatus.P, diff.get("mobile").getStatus());
        assertEquals(MatchingStatus.P, diff.get("id").getStatus());

        assertEquals(MatchingStatus.F, diff.get("add").getStatus());

        Map<String, MatchingResult> addDiff = diff.get("add").getDiff();
        assertEquals(MatchingStatus.P, addDiff.get("city").getStatus());
        assertEquals(MatchingStatus.P, addDiff.get("state").getStatus());
        assertEquals(MatchingStatus.P, addDiff.get("pin").getStatus());
        assertEquals(MatchingStatus.F, addDiff.get("landmark").getStatus());
        assertEquals("mosque", addDiff.get("landmark").getExp());
        assertEquals("temple", addDiff.get("landmark").getAct());
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
        JsonObject ignored = new JsonObject()
                .put("add", internalIgnored)
                .put("name", true);

        MatchingResult result = matcher.compare(expected, actual, ignored.getMap());
        assertEquals((Integer) 3, result.getCount());
        assertEquals(MatchingStatus.F, result.getStatus());

        Map<String, MatchingResult> diff = result.getDiff();
        assertEquals(MatchingStatus.IGN, diff.get("name").getStatus());
        assertEquals(MatchingStatus.P, diff.get("kerberos").getStatus());
        assertEquals(MatchingStatus.P, diff.get("mobile").getStatus());
        assertEquals(MatchingStatus.P, diff.get("id").getStatus());

        assertEquals(MatchingStatus.F, diff.get("add").getStatus());

        Map<String, MatchingResult> addDiff = diff.get("add").getDiff();
        assertEquals(MatchingStatus.P, addDiff.get("city").getStatus());
        assertEquals(MatchingStatus.P, addDiff.get("state").getStatus());
        assertEquals(MatchingStatus.F, addDiff.get("pin").getStatus());
        assertEquals(MatchingStatus.IGN, addDiff.get("landmark").getStatus());
        assertEquals(211002, addDiff.get("pin").getExp());
        assertEquals(211003, addDiff.get("pin").getAct());
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

        Map<String, MatchingResult> diff = result.getDiff();
        assertEquals(MatchingStatus.P, diff.get("name").getStatus());

        assertEquals(MatchingStatus.F, diff.get("fakeName").getStatus());
        Map<String, MatchingResult> fNDiff = diff.get("fakeName").getDiff();
        assertEquals(MatchingStatus.P, fNDiff.get("0").getStatus());
        assertEquals(MatchingStatus.NE, fNDiff.get("1").getStatus());
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

        Map<String, MatchingResult> diff = result.getDiff();
        assertEquals(MatchingStatus.P, diff.get("name").getStatus());

        assertEquals(MatchingStatus.F, diff.get("fakeName").getStatus());
        Map<String, MatchingResult> fNDiff = diff.get("fakeName").getDiff();
        assertEquals(MatchingStatus.NE, fNDiff.get("0").getStatus());
        assertEquals(MatchingStatus.NE, fNDiff.get("1").getStatus());
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

        Map<String, MatchingResult> diff = result.getDiff();
        assertEquals(MatchingStatus.P, diff.get("name").getStatus());
        assertEquals(MatchingStatus.F, diff.get("fakeName").getStatus());

        Map<String, MatchingResult> fNDiff = diff.get("fakeName").getDiff();
        assertEquals(MatchingStatus.P, fNDiff.get("1").getStatus());

        assertEquals(MatchingStatus.F, fNDiff.get("0").getStatus());
        Map<String, MatchingResult> fnArrDiff = fNDiff.get("0").getDiff();
        assertEquals(MatchingStatus.P, fnArrDiff.get("firstName").getStatus());
        assertEquals(MatchingStatus.F, fnArrDiff.get("secondName").getStatus());

        assertEquals("Bar", fnArrDiff.get("secondName").getExp());
        assertEquals("Bar23", fnArrDiff.get("secondName").getAct());
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

        Map<String, MatchingResult> diff = result.getDiff();
        assertEquals(MatchingStatus.P, diff.get("name").getStatus());
        assertEquals(MatchingStatus.F, diff.get("fakeName").getStatus());

        Map<String, MatchingResult> fNDiff = diff.get("fakeName").getDiff();
        assertEquals(MatchingStatus.P, fNDiff.get("1").getStatus());

        assertEquals(MatchingStatus.F, fNDiff.get("0").getStatus());
        Map<String, MatchingResult> fnArrDiff = fNDiff.get("0").getDiff();

        assertEquals(MatchingStatus.IGN, fnArrDiff.get("firstName").getStatus());
        assertEquals(MatchingStatus.F, fnArrDiff.get("secondName").getStatus());

        assertEquals("Bar", fnArrDiff.get("secondName").getExp());
        assertEquals("Bar23", fnArrDiff.get("secondName").getAct());
    }

    @Test
    public void testMissingElement() {
        List<String> expected = List.of("Raghav", "Chandra");
        List<String> actual = List.of("Raghav");
        MatchingResult result = new JsonMatcher().compare(expected, actual);
        assertEquals(MatchingStatus.F, result.getStatus());

        Map<String, MatchingResult> diff = result.getDiff();
        assertEquals(MatchingStatus.P, diff.get("0").getStatus());
        assertEquals(MatchingStatus.NE, diff.get("1").getStatus());
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

        JsonObject key = new JsonObject().put("id", new JsonObject());
        MatchingResult results = new JsonMatcher().compare(expected, actual, new HashMap<>(), key.getMap());

        assertEquals(MatchingStatus.F, results.getStatus());
        Map<String, MatchingResult> diff = results.getDiff();

        assertEquals(MatchingStatus.PK, diff.get("0").getStatus());
        Map<String, MatchingResult> pkDiff = diff.get("0").getDiff();

        assertEquals(MatchingStatus.NE, diff.get("1").getStatus());
    }

    @Test
    public void testNestedObjectComparisonWithBusinessKey() {
        JsonObject expected = new JsonObject()
                .put("firstName", new JsonObject().put("name", "Raghav").put("id", 1234).put("No", 51951))
                .put("secondName", new JsonObject().put("name", "Chandra").put("id", 1).put("No", 987654321));

        JsonObject actual = new JsonObject()
                .put("firstName", new JsonObject().put("name", "Raghav").put("id", 1234).put("No", 654321))
                .put("secondName", new JsonObject().put("name", "Chandra").put("id", 5).put("No", 987654321));

        JsonObject businessKey = new JsonObject()
                .put("firstName", new JsonObject().put("id", true))
                .put("secondName", new JsonObject().put("id", true));

        MatchingResult results = new JsonMatcher().compare(expected, actual, new HashMap<>(), businessKey.getMap());

        assertEquals(MatchingStatus.NE, results.getStatus());
    }

    @Test
    public void testArrayWithNestedObjectComparisonWithBusinessKey() {
        JsonObject expected1 = new JsonObject()
                .put("firstName", new JsonObject().put("name", "Raghav").put("id", 1234).put("No", 51951))
                .put("secondName", new JsonObject().put("name", "Chandra").put("id", 1).put("No", 987654321));

        JsonObject expected2 = new JsonObject()
                .put("firstName", new JsonObject().put("name", "Raghav").put("id", 1234).put("No", 654321))
                .put("secondName", new JsonObject().put("name", "Chandra").put("id", 5).put("No", 987654321));

        JsonObject actual1 = new JsonObject()
                .put("firstName", new JsonObject().put("name", "Raghav").put("id", 1234).put("No", 51951))
                .put("secondName", new JsonObject().put("name", "Chandra").put("id", 1).put("No", 987654321));

        JsonObject actual2 = new JsonObject()
                .put("firstName", new JsonObject().put("name", "Raghav").put("id", 1234).put("No", 654321))
                .put("secondName", new JsonObject().put("name", "Chandra").put("id", 90).put("No", 987654321));

        JsonArray expected = new JsonArray().add(expected1).add(expected2);
        JsonArray actual = new JsonArray().add(actual1).add(actual2);

        JsonObject businessKey = new JsonObject()
                .put("firstName", new JsonObject().put("id", true))
                .put("secondName", new JsonObject().put("id", true));

        MatchingResult results = new JsonMatcher().compare(expected, actual, new HashMap<>(), businessKey.getMap());

        assertEquals(MatchingStatus.F, results.getStatus());

        assertEquals(MatchingStatus.P, results.getDiff().get("0").getStatus());
        assertEquals(MatchingStatus.NE, results.getDiff().get("1").getStatus());
    }

    @Test
    public void testKeyWithIgnored() {
        JsonObject expected = new JsonObject()
                .put("firstName", new JsonObject().put("name", "Raghav").put("id", 1234).put("No", 51951))
                .put("secondName", new JsonObject().put("name", "Chandra").put("id", 1).put("No", 987654321));

        JsonObject actual = new JsonObject()
                .put("firstName", new JsonObject().put("name", "Raghav").put("id", 1234).put("No", 654321))
                .put("secondName", new JsonObject().put("name", "Chandra").put("id", 5).put("No", 987654321));

        JsonObject businessKey = new JsonObject()
                .put("firstName", new JsonObject().put("id", true))
                .put("secondName", new JsonObject().put("id", true));

        JsonObject ignored = new JsonObject().put("secondName", true);

        MatchingResult result = new JsonMatcher().compare(expected, actual, ignored.getMap(), businessKey.getMap());
        assertEquals(MatchingStatus.P, result.getStatus());
    }

    @Test(expected = RuntimeException.class)
    public void testFailWhenKeyIsIgnored() {
        JsonObject expected = new JsonObject()
                .put("firstName", new JsonObject().put("name", "Raghav").put("id", 1234).put("No", 51951))
                .put("secondName", new JsonObject().put("name", "Chandra").put("id", 1).put("No", 987654321));

        JsonObject actual = new JsonObject()
                .put("firstName", new JsonObject().put("name", "Raghav").put("id", 1234).put("No", 654321))
                .put("secondName", new JsonObject().put("name", "Chandra").put("id", 5).put("No", 987654321));

        JsonObject businessKey = new JsonObject()
                .put("firstName", new JsonObject().put("id", true))
                .put("secondName", new JsonObject().put("id", true));

        JsonObject ignored = new JsonObject()
                .put("firstName", new JsonObject().put("id", true));

        new JsonMatcher().compare(expected, actual, ignored.getMap(), businessKey.getMap());
    }

    @Test
    public void testIgnoredContainingDiff() {
        JsonObject expected = new JsonObject()
                .put("name", new JsonArray().add(new JsonObject().put("second", "Chandra")));

        JsonObject actual = new JsonObject()
                .put("name", new JsonArray().add(new JsonObject().put("second", "BlahBlah")));

        JsonObject ignored = new JsonObject().put("name", new JsonObject().put("second", true));

        MatchingResult result = new JsonMatcher().compare(expected, actual, ignored.getMap());
        assertEquals(MatchingStatus.P, result.getStatus());
    }

    @Test
    public void testNewAttributeInActual() {

        JsonArray nestedArr1 = new JsonArray()
                .add(new JsonObject().put("name", "Raghav").put("sec", "Blah"))
                .add(new JsonObject().put("name", "Raga").put("sec", "Blah1"))
                .add(new JsonObject().put("name", "Chandra").put("sec", "BB"));

        JsonArray nestedArr2 = new JsonArray()
                .add(new JsonObject().put("name", "Raghav").put("sec", "Blah1"))
                .add(new JsonObject().put("name", "Chandra").put("sec", "BB"));

        JsonArray arr1 = new JsonArray()
                .add(new JsonObject().put("name", "Raghav").put("sec", "Blah").put("comp", new JsonArray()))
                .add(new JsonObject().put("name", "Chandra").put("sec", "BB").put("comp", nestedArr1));

        JsonArray arr2 = new JsonArray()
                .add(new JsonObject().put("name", "Raghav").put("sec", "Blah").put("comp", new JsonArray()))
                .add(new JsonObject().put("name", "Chandra").put("sec", "BB").put("comp", nestedArr2));

        JsonObject expected = new JsonObject()
                .put("first", "Rags")
                .put("fullName", arr1)
                .put("add", "India");

        JsonObject actual = new JsonObject()
                .put("first", "Rags")
                .put("fullName", arr2)
                .put("add", "India");

        JsonObject key = new JsonObject().put("fullName", new JsonObject().put("comp", new JsonObject().put("name", true)));

        MatchingResult result = new JsonMatcher().compare(expected, actual, new HashMap<>(), key.getMap());
        assertEquals(MatchingStatus.F, result.getStatus());
    }

    @Test
    public void testNewAttributesFailsComparison () {
        JsonObject expected = new JsonObject()
                .put("first", "Rags")
                .put("add", "India");

        JsonObject actual = new JsonObject()
                .put("first", "Rags")
                .put("second", "Chand")
                .put("add", "India");

        MatchingResult result = new JsonMatcher().compare(expected, actual);
        assertEquals(MatchingStatus.F, result.getStatus());
        Map<String, MatchingResult> diff = result.getDiff();

        assertEquals(MatchingStatus.NW, diff.get("second").getStatus());
        assertEquals("Chand", diff.get("second").getAct());
    }
}