package com.rags.tools.matcher;

import com.fasterxml.jackson.annotation.JsonIgnore;
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
    private final Map<String, MatchingResult> diff;
    private final Object exp;
    private final Object act;
    private MatchingAlgo algo;

    public MatchingResult(MatchingStatus status, Map<String, MatchingResult> diff, Object act, Object exp, Integer count, Integer matIndex, Integer elemIndex) {
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

    public Map<String, MatchingResult> getDiff() {
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

    @JsonIgnore
    public boolean isAllMatching() {
        return status == MatchingStatus.P;
    }

    @JsonIgnore
    public boolean isOnlyKeyMatching() {
        return status == MatchingStatus.PK;
    }

    public MatchingAlgo getAlgo() {
        return algo;
    }

    public void setAlgo(MatchingAlgo algo) {
        this.algo = algo;
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
        private Map<String, MatchingResult> difference;
        private Object expectedValue;
        private Object actualValue;
        private MatchingAlgo algo;

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

        public Builder setDifference(Map<String, MatchingResult> diff) {
            this.difference = diff;
            return this;
        }

        public Map<String, MatchingResult> getDifference() {
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

        public void setAlgo(MatchingAlgo algo) {
            this.algo = algo;
        }

        public MatchingAlgo getAlgo() {
            return algo;
        }

        public boolean isPassing() {
            return matchingStatus == MatchingStatus.P;
        }

        public boolean isFailing() {
            return matchingStatus == MatchingStatus.F;
        }

        public MatchingStatus getMatchingStatus() {
            return matchingStatus;
        }

        MatchingResult create() {
            MatchingResult matchingResult = new MatchingResult(matchingStatus, difference, actualValue, expectedValue, matchingCount, matchingIndex, elementIndex);
            matchingResult.setAlgo(algo);
            return matchingResult;
        }
    }
}
