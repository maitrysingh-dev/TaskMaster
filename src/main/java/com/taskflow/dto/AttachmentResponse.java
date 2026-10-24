package com.taskflow.dto;

import com.taskflow.model.Attachment;

import java.time.Instant;

public record AttachmentResponse(Long id, Long taskId, String fileName, String contentType, long size,
                                 UserSummary uploadedBy, Instant uploadedAt) {

    public static AttachmentResponse from(Attachment attachment) {
        return new AttachmentResponse(attachment.getId(), attachment.getTask().getId(),
                attachment.getOriginalFileName(), attachment.getContentType(), attachment.getFileSize(),
                UserSummary.from(attachment.getUploadedBy()), attachment.getUploadedAt());
    }
}
