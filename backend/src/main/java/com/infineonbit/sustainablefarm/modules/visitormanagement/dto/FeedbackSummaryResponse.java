package com.infineonbit.sustainablefarm.modules.visitormanagement.dto;

import java.util.Map;

/**
 * Compiled satisfaction statistics over a date range: average rating,
 * number of responses, % of responses with a recommendation and the
 * 1..5 rating distribution.
 */
public class FeedbackSummaryResponse {

    private double averageRating;
    private long total;
    private double recommendPct;
    private Map<Integer, Long> distribution;

    public static FeedbackSummaryResponse of(double averageRating, long total,
                                             double recommendPct, Map<Integer, Long> distribution) {
        FeedbackSummaryResponse r = new FeedbackSummaryResponse();
        r.averageRating = averageRating;
        r.total = total;
        r.recommendPct = recommendPct;
        r.distribution = distribution;
        return r;
    }

    public double getAverageRating() {
        return averageRating;
    }

    public long getTotal() {
        return total;
    }

    public double getRecommendPct() {
        return recommendPct;
    }

    public Map<Integer, Long> getDistribution() {
        return distribution;
    }
}