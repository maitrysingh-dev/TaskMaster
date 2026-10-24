package com.taskflow.dto;

import java.time.Instant;

/** {@code inviteCode} is only filled in for the project owner. */
public record ProjectResponse(Long id, String name, String description, Long ownerId, String ownerName,
                              long memberCount, String inviteCode, Instant createdAt) {
}
