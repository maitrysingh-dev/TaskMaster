package com.taskflow.controller;

import com.taskflow.dto.AttachmentResponse;
import com.taskflow.service.AttachmentDownload;
import com.taskflow.service.AttachmentService;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/tasks/{taskId}/attachments")
public class AttachmentController {

    private final AttachmentService attachmentService;

    public AttachmentController(AttachmentService attachmentService) {
        this.attachmentService = attachmentService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<AttachmentResponse> upload(@AuthenticationPrincipal UserDetails principal,
                                                     @PathVariable Long taskId,
                                                     @RequestParam("file") MultipartFile file) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(attachmentService.upload(taskId, principal.getUsername(), file));
    }

    @GetMapping
    public List<AttachmentResponse> list(@AuthenticationPrincipal UserDetails principal,
                                         @PathVariable Long taskId) {
        return attachmentService.list(taskId, principal.getUsername());
    }

    @GetMapping("/{attachmentId}")
    public ResponseEntity<Resource> download(@AuthenticationPrincipal UserDetails principal,
                                             @PathVariable Long taskId,
                                             @PathVariable Long attachmentId) {
        AttachmentDownload file = attachmentService.download(taskId, attachmentId, principal.getUsername());
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(file.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(file.fileName(), StandardCharsets.UTF_8).build().toString())
                .body(file.resource());
    }

    @DeleteMapping("/{attachmentId}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal UserDetails principal,
                                       @PathVariable Long taskId,
                                       @PathVariable Long attachmentId) {
        attachmentService.delete(taskId, attachmentId, principal.getUsername());
        return ResponseEntity.noContent().build();
    }
}
