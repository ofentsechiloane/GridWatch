package com.gridwatch.dto;

public record JustMeResponse(Verdict verdict, String message, long offReports, long onReports,
                             double radiusMeters, int minutes, Long outageId) {

    public enum Verdict {
        OUTAGE_IN_AREA,
        LIKELY_JUST_YOU,
        NOT_ENOUGH_DATA
    }
}