package com.artsync.dto;

import com.artsync.domain.space.Space;

import java.time.LocalDateTime;

public record SpaceResponse(
        Long id,
        String name,
        String description,
        Long ownerId,
        LocalDateTime createdAt
) {
    public static SpaceResponse of(Space space) {
        return new SpaceResponse(
                space.getId(),
                space.getName(),
                space.getDescription(),
                space.getOwnerId(),
                space.getCreatedAt());
    }
}
