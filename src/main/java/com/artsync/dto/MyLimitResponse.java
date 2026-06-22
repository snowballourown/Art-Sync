package com.artsync.dto;

public record MyLimitResponse(
        long used,
        int limit,
        int baseLimit,
        int carryover
) {}
