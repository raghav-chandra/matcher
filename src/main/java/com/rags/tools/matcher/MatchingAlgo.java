package com.rags.tools.matcher;

public enum MatchingAlgo {
    M("MAX_COUNT"), K("BUSINESS_KEY");

    private final String desc;

    MatchingAlgo(String desc) {
        this.desc = desc;
    }

    public String getDesc() {
        return desc;
    }
}