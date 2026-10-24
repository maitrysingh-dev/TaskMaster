package com.taskflow.service;

import com.taskflow.dto.AssignRequest;
import com.taskflow.dto.CreateTaskRequest;
import com.taskflow.dto.PageResponse;
import com.taskflow.dto.StatusUpdateRequest;
import com.taskflow.dto.TaskResponse;
import com.taskflow.dto.UpdateTaskRequest;
import com.taskflow.exception.BadRequestException;
import com.taskflow.exception.ForbiddenException;
import com.taskflow.exception.ResourceNotFoundException;
import com.taskflow.model.Attachment;
import com.taskflow.model.Project;
import com.taskflow.model.Task;
import com.taskflow.model.TaskPriority;
import com.taskflow.model.User;
import com.taskflow.repository.AttachmentRepository;
import com.taskflow.repository.CommentRepository;
import com.taskflow.repository.TaskRepository;
import com.taskflow.repository.TaskSpecs;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.Set;

@Service
@Transactional
public class TaskService {

    private static final Set<String> SORTABLE_FIELDS = Set.of("createdAt", "updatedAt", "dueDate", "title");
    private static final int MAX_PAGE_SIZE = 100;

    private final TaskRepository taskRepository;
    private final CommentRepository commentRepository;
    private final AttachmentRepository attachmentRepository;
    private final FileStorageService fileStorage;
    private final ProjectService projectService;
    private final UserService userService;

    public TaskService(TaskRepository taskRepository, CommentRepository commentRepository,
                       AttachmentRepository attachmentRepository, FileStorageService fileStorage,
                       ProjectService projectService, UserService userService) {
        this.taskRepository = taskRepository;
        this.commentRepository = commentRepository;
        this.attachmentRepository = attachmentRepository;
        this.fileStorage = fileStorage;
        this.projectService = projectService;
        this.userService = userService;
    }

    public TaskResponse create(Long projectId, String email, CreateTaskRequest request) {
        User user = userService.findByEmail(email);
        Project project = projectService.requireMembership(projectId, user);
        User assignee = resolveAssignee(project, request.assigneeId());

        TaskPriority priority = request.priority() != null ? request.priority() : TaskPriority.MEDIUM;
        Task task = new Task(request.title().trim(), request.description(), request.dueDate(),
                priority, project, user, assignee);
        return TaskResponse.from(taskRepository.save(task));
    }

    @Transactional(readOnly = true)
    public PageResponse<TaskResponse> listForProject(Long projectId, String email, TaskQuery query) {
        User user = userService.findByEmail(email);
        projectService.requireMembership(projectId, user);

        Specification<Task> spec = applyFilters(TaskSpecs.inProject(projectId), query);
        if (query.assigneeId() != null) {
            spec = spec.and(TaskSpecs.assignedTo(query.assigneeId()));
        }
        return runQuery(spec, query);
    }

    @Transactional(readOnly = true)
    public PageResponse<TaskResponse> listAssignedToMe(String email, TaskQuery query) {
        User user = userService.findByEmail(email);

        Specification<Task> spec = applyFilters(TaskSpecs.assignedTo(user.getId()), query);
        if (query.projectId() != null) {
            spec = spec.and(TaskSpecs.inProject(query.projectId()));
        }
        return runQuery(spec, query);
    }

    @Transactional(readOnly = true)
    public TaskResponse get(Long taskId, String email) {
        User user = userService.findByEmail(email);
        return TaskResponse.from(loadAccessibleTask(taskId, user));
    }

    public TaskResponse update(Long taskId, String email, UpdateTaskRequest request) {
        User user = userService.findByEmail(email);
        Task task = loadAccessibleTask(taskId, user);
        requireCanModify(task, user);

        task.setTitle(request.title().trim());
        task.setDescription(request.description());
        task.setDueDate(request.dueDate());
        if (request.priority() != null) {
            task.setPriority(request.priority());
        }
        return TaskResponse.from(task);
    }

    public TaskResponse updateStatus(Long taskId, String email, StatusUpdateRequest request) {
        User user = userService.findByEmail(email);
        Task task = loadAccessibleTask(taskId, user);
        requireCanModify(task, user);

        task.setStatus(request.status());
        return TaskResponse.from(task);
    }

    public TaskResponse assign(Long taskId, String email, AssignRequest request) {
        User user = userService.findByEmail(email);
        Task task = loadAccessibleTask(taskId, user);
        requireCanModify(task, user);

        task.setAssignee(resolveAssignee(task.getProject(), request.assigneeId()));
        return TaskResponse.from(task);
    }

    public void delete(Long taskId, String email) {
        User user = userService.findByEmail(email);
        Task task = loadAccessibleTask(taskId, user);

        boolean isCreator = Objects.equals(task.getCreator().getId(), user.getId());
        if (!isCreator && !projectService.isOwner(task.getProject(), user)) {
            throw new ForbiddenException("Only the task creator or the project owner can delete a task");
        }

        commentRepository.deleteByTaskId(taskId);
        List<Attachment> attachments = attachmentRepository.findByTaskIdOrderByUploadedAtAsc(taskId);
        attachments.forEach(a -> fileStorage.delete(a.getStoredName()));
        attachmentRepository.deleteAll(attachments);
        taskRepository.delete(task);
    }

    /** Loads a task and makes sure the user belongs to the project it lives in. Also used by comments/attachments. */
    @Transactional(readOnly = true)
    public Task loadAccessibleTask(Long taskId, User user) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task " + taskId + " not found"));
        projectService.requireMembership(task.getProject().getId(), user);
        return task;
    }

    // ---- internals ----

    private Specification<Task> applyFilters(Specification<Task> spec, TaskQuery query) {
        if (query.status() != null) {
            spec = spec.and(TaskSpecs.hasStatus(query.status()));
        }
        if (query.search() != null && !query.search().isBlank()) {
            spec = spec.and(TaskSpecs.matchesText(query.search().trim()));
        }
        return spec;
    }

    private PageResponse<TaskResponse> runQuery(Specification<Task> spec, TaskQuery query) {
        Page<Task> page = taskRepository.findAll(spec, toPageable(query));
        return PageResponse.from(page.map(TaskResponse::from));
    }

    private Pageable toPageable(TaskQuery query) {
        if (!SORTABLE_FIELDS.contains(query.sortBy())) {
            throw new BadRequestException("Cannot sort by '" + query.sortBy() + "'. Allowed: "
                    + String.join(", ", SORTABLE_FIELDS.stream().sorted().toList()));
        }
        Sort.Direction direction;
        if ("asc".equalsIgnoreCase(query.direction())) {
            direction = Sort.Direction.ASC;
        } else if ("desc".equalsIgnoreCase(query.direction())) {
            direction = Sort.Direction.DESC;
        } else {
            throw new BadRequestException("Sort direction must be 'asc' or 'desc'");
        }
        int page = Math.max(query.page(), 0);
        int size = Math.min(Math.max(query.size(), 1), MAX_PAGE_SIZE);
        // The id is a tie-breaker so paging stays stable when many tasks share the same sort value.
        return PageRequest.of(page, size, Sort.by(direction, query.sortBy()).and(Sort.by("id")));
    }

    private User resolveAssignee(Project project, Long assigneeId) {
        if (assigneeId == null) {
            return null;
        }
        User assignee = userService.findById(assigneeId);
        if (!projectService.isMember(project.getId(), assignee.getId())) {
            throw new BadRequestException("The assignee must be a member of the project");
        }
        return assignee;
    }

    /** Creator, current assignee and the project owner may edit a task. */
    private void requireCanModify(Task task, User user) {
        boolean isCreator = Objects.equals(task.getCreator().getId(), user.getId());
        boolean isAssignee = task.getAssignee() != null && Objects.equals(task.getAssignee().getId(), user.getId());
        if (!isCreator && !isAssignee && !projectService.isOwner(task.getProject(), user)) {
            throw new ForbiddenException("Only the creator, the assignee or the project owner can change this task");
        }
    }
}
