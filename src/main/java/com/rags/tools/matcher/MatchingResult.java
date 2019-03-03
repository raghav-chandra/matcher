package com.rags.tools.matcher;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Map;

/**
 * @author ragha  11-02-2019.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MatchingResult {
    private final MatchingStatus status;
    private final Integer matIndex;
    private final Integer elemIndex;
    private final Integer count;
    private final Map<String, Object> diff;
    private final Object exp;
    private final Object act;

    public MatchingResult(MatchingStatus status, Map<String, Object> diff, Object act, Object exp, Integer count, Integer matIndex, Integer elemIndex) {
        this.status = status;
        this.diff = diff;
        this.exp = exp;
        this.act = act;
        this.matIndex = matIndex;
        this.elemIndex = elemIndex;
        this.count = count;
    }

    public MatchingStatus getStatus() {
        return status;
    }

    public Integer getMatIndex() {
        return matIndex;
    }

    public Integer getElemIndex() {
        return elemIndex;
    }

    public Map<String, Object> getDiff() {
        return diff;
    }

    public Object getExp() {
        return exp;
    }

    public Object getAct() {
        return act;
    }

    public Integer getCount() {
        return count;
    }

    public Builder newBuilder() {
        return new Builder()
                .setMatchingStatus(status)
                .setDifference(diff)
                .setExpectedValue(exp)
                .setActualValue(act)
                .setMatchingIndex(matIndex)
                .setElementIndex(elemIndex)
                .setMatchingCount(count);

    }

    static class Builder {
        private MatchingStatus matchingStatus;
        private Integer matchingIndex;
        private Integer elementIndex;
        private Integer matchingCount;
        private Map<String, Object> difference;
        private Object expectedValue;
        private Object actualValue;

        public Builder setMatchingStatus(MatchingStatus matchingStatus) {
            this.matchingStatus = matchingStatus;
            return this;
        }

        public Builder setMatchingIndex(Integer matIndex) {
            this.matchingIndex = matIndex;
            return this;
        }

        public Builder setElementIndex(Integer elemIndex) {
            this.elementIndex = elemIndex;
            return this;
        }

        public Builder setDifference(Map<String, Object> diff) {
            this.difference = diff;
            return this;
        }

        public Map<String, Object> getDifference() {
            return difference;
        }

        public Builder setExpectedValue(Object exp) {
            this.expectedValue = exp;
            return this;
        }

        public Builder setActualValue(Object act) {
            this.actualValue = act;
            return this;
        }

        public Builder setMatchingCount(Integer matchingCount) {
            this.matchingCount = matchingCount;
            return this;
        }

        public MatchingStatus getMatchingStatus() {
            return matchingStatus;
        }

        MatchingResult create() {
            return new MatchingResult(matchingStatus, difference, actualValue, expectedValue, matchingCount, matchingIndex, elementIndex);
        }
    }
}
