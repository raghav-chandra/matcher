package com.rags.tools.matcher;

/**
 * @author ragha
 * @since 11-02-2019
 */
public enum MatchingStatus {
    P("PASS"), PK("KEY_MATCHING"),  F("FAIL"), NE("NOT_EXISTS"), NW("NEW"), OM("OBJECT_MISMATCH"), IGN("IGNORED");

    private final String desc;

    MatchingStatus(String desc) {
        this.desc = desc;
    }

    public String getDesc() {
        return desc;
    }
}
