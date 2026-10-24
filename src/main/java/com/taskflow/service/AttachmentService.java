package com.taskflow.service;

import com.taskflow.dto.AttachmentResponse;
import com.taskflow.exception.BadRequestException;
import com.taskflow.exception.ForbiddenException;
import com.taskflow.exception.ResourceNotFoundException;
import com.taskflow.model.Attachment;
import com.taskflow.model.Task;
import com.taskflow.model.User;
import com.taskflow.repository.AttachmentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Objects;

@Service
@Transactional
public class AttachmentService {

    private final AttachmentRepository attachmentRepository;
    private final FileStorageService fileStorage;
    private final TaskService taskService;
    private final ProjectService projectService;
    private final UserService userService;

    public AttachmentService(AttachmentRepository attachmentRepository, FileStorageService fileStorage,
                             TaskService taskService, ProjectService projectService, UserService userService) {
        this.attachmentRepository = attachmentRepository;
        this.fileStorage = fileStorage;
        this.taskService = taskService;
        this.projectService = projectService;
        this.userService = userService;
    }

    public AttachmentResponse upload(Long taskId, String email, MultipartFile file) {
        User user = userService.findByEmail(email);
        Task task = taskService.loadAccessibleTask(taskId, user);

        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Please choose a non-empty file to upload");
        }
        String originalName = StringUtils.cleanPath(Objects.requireNonNullElse(file.getOriginalFilename(), ""));
        if (originalName.isBlank() || originalName.contains("..")) {
            throw new BadRequestException("Invalid file name");
        }
        String contentType = file.getContentType() != null ? file.getContentType() : "application/octet-stream";

        String storedName = fileStorage.store(file);
        Attachment attachment = attachmentRepository.save(
                new Attachment(task, user, originalName, storedName, contentType, file.getSize()));
        return AttachmentResponse.from(attachment);
    }

    @Transactional(readOnly = true)
    public List<AttachmentResponse> list(Long taskId, String email) {
        User user = userService.findByEmail(email);
        taskService.loadAccessibleTask(taskId, user);
        return attachmentRepository.findByTaskIdOrderByUploadedAtAsc(taskId).stream()
                .map(AttachmentResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public AttachmentDownload download(Long taskId, Long attachmentId, String email) {
        User user = userService.findByEmail(email);
        taskService.loadAccessibleTask(taskId, user);
        Attachment attachment = findForTask(taskId, attachmentId);
        return new AttachmentDownload(fileStorage.load(attachment.getStoredName()),
                attachment.getOriginalFileName(), attachment.getContentType());
    }

    public void delete(Long taskId, Long attachmentId, String email) {
        User user = userService.findByEmail(email);
        Task task = taskService.loadAccessibleTask(taskId, user);
        Attachment attachment = findForTask(taskId, attachmentId);

        boolean isUploader = Objects.equals(attachment.getUploadedBy().getId(), user.getId());
        if (!isUploader && !projectService.isOwner(task.getProject(), user)) {
            throw new ForbiddenException("Only the uploader or the project owner can delete an attachment");
        }
        fileStorage.delete(attachment.getStoredName());
        attachmentRepository.delete(attachment);
    }

    private Attachment findForTask(Long taskId, Long attachmentId) {
        Attachment attachment = attachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Attachment " + attachmentId + " not found"));
        // Guards against reading an attachment of another task by mixing up the ids in the URL.
        if (!Objects.equals(attachment.getTask().getId(), taskId)) {
            throw new ResourceNotFoundException("Attachment " + attachmentId + " not found on task " + taskId);
        }
        return attachment;
    }
}
