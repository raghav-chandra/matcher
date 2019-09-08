package com.rags.tools.matcher;

/**
 * @author ragha
 * @since 11-02-2019
 */
public enum MatchingStatus {
    P("PASS"), F("FAIL"), NE("NOT_EXISTS"), OM("OBJECT_MISMATCH");

    private final String desc;

    MatchingStatus(String desc) {
        this.desc = desc;
    }

    public String getDesc() {
        return desc;
    }
}
