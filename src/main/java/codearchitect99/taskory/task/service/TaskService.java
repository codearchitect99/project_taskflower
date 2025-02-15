package codearchitect99.taskory.task.service;

import codearchitect99.taskory.event.Event;
import codearchitect99.taskory.event.EventNotFoundException;
import codearchitect99.taskory.event.EventRepository;
import codearchitect99.taskory.hashtag.HashtagRepository;
import codearchitect99.taskory.tag.TagNotFoundException;
import codearchitect99.taskory.tag.TagRepository;
import codearchitect99.taskory.tag.model.Tag;
import codearchitect99.taskory.task.exception.InvalidDeadlineException;
import codearchitect99.taskory.task.exception.InvalidStatusNameException;
import codearchitect99.taskory.task.exception.TaskNotFoundException;
import codearchitect99.taskory.task.model.Status;
import codearchitect99.taskory.task.model.Task;
import codearchitect99.taskory.task.model.TaskItem;
import codearchitect99.taskory.task.payload.SaveTaskRequest;
import codearchitect99.taskory.task.payload.TaskItemDto;
import codearchitect99.taskory.task.payload.TaskResponse;
import codearchitect99.taskory.task.payload.TaskSummary;
import codearchitect99.taskory.task.repository.TaskItemRepository;
import codearchitect99.taskory.task.repository.TaskRepository;
import codearchitect99.taskory.user.model.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Slf4j
@Service
public class TaskService {

    private final TaskRepository taskRepository;
    private final TaskItemRepository taskItemRepository;
    private final EventRepository eventRepository;
    private final TagRepository tagRepository;
    private final HashtagRepository hashtagRepository;

    @Autowired
    public TaskService(TaskRepository taskRepository, TaskItemRepository taskitemRepository, EventRepository eventRepository, TagRepository tagRepository, HashtagRepository hashtagRepository) {
        this.taskRepository = taskRepository;
        this.taskItemRepository = taskitemRepository;
        this.eventRepository = eventRepository;
        this.tagRepository = tagRepository;
        this.hashtagRepository = hashtagRepository;
    }

    /**
     * Saves a new task for the specified user.
     *
     * @param user the user to associate the task with
     * @param saveTaskRequest the task details to be saved
     * @return a {@link TaskResponse} representing the saved task
     */
    @Transactional
    public TaskResponse save(User user, SaveTaskRequest saveTaskRequest)
            throws InvalidStatusNameException, EventNotFoundException, TagNotFoundException {

        Status status = validateAndGetStatus(saveTaskRequest.getStatus());

        Event event = validateAndGetEntityById(
                saveTaskRequest.getEventId(),
                eventRepository,
                () -> new EventNotFoundException("Event not found for ID: " + saveTaskRequest.getEventId())
        );

        Tag tag = validateAndGetEntityById(
                saveTaskRequest.getTagId(),
                tagRepository,
                () -> new TagNotFoundException("Tag not found for ID: " + saveTaskRequest.getTagId())
        );

        Task task = Task.builder()
                .user(user)
                .title(saveTaskRequest.getTitle())
                .event(event)
                .tag(tag)
                .hashtags(saveTaskRequest.getHashtagIds() != null ?
                        hashtagRepository.findAllById(saveTaskRequest.getHashtagIds())
                        :null)
                .description(saveTaskRequest.getDescription())
                .status(status)
                .build();

        taskRepository.save(task);

        if (saveTaskRequest.getItems() != null && !saveTaskRequest.getItems().isEmpty()) {
            List<TaskItem> itemList = saveTaskRequest.getItems().stream()
                    .map(itemDto -> TaskItem.builder()
                            .task(task)
                            .completed(itemDto.isCompleted())
                            .title(itemDto.getTitle())
                            .build())
                    .toList();

            taskItemRepository.saveAll(itemList);

            task.setItems(new ArrayList<>(itemList));
            taskRepository.save(task);
        }

        return new TaskResponse(task);
    }

    /**
     * Validates and retrieves the corresponding Status enum from the provided status string.
     *
     * @param statusString the string representation of the status
     * @return the corresponding Status enum, or null if statusString is null
     * @throws InvalidStatusNameException if the statusString does not match any valid Status
     */
    private static Status validateAndGetStatus(String statusString) throws InvalidStatusNameException {
        Status status = null;
        if (statusString != null) {
            try {
                status = Status.valueOf(statusString);
            } catch (IllegalArgumentException e) {
                throw new InvalidStatusNameException("Invalid status value: " + statusString +
                        ". Valid values are: " + Arrays.toString(Status.values()));
            }
        }
        return status;
    }

    /**
     * Validates and retrieves an entity by its ID from the given repository.
     *
     * @param id the ID of the entity to retrieve
     * @param repository the repository to search for the entity
     * @param exceptionSupplier the supplier that provides the exception if the entity is not found
     * @param <T> the type of the entity
     * @param <E> the type of exception to be thrown if the entity is not found
     * @return the entity if found, or null if the ID is null
     * @throws E if the entity is not found
     */
    private <T, E extends Exception> T validateAndGetEntityById(Long id, JpaRepository<T, Long> repository, Supplier<E> exceptionSupplier) throws E {
        return id != null ? repository.findById(id).orElseThrow(exceptionSupplier) : null;
    }


    /**
     * Retrieves a task by its ID.
     *
     * @param id the ID of the task to retrieve
     * @return a {@link TaskResponse} representing the task
     * @throws TaskNotFoundException if no task with the specified ID is found
     */
    public TaskResponse getById(Long id) throws TaskNotFoundException {
        Task task = taskRepository.findById(id).orElseThrow(() -> new TaskNotFoundException("Task not found for id: " + id));
        return new TaskResponse(task);
    }

    /**
     * Finds all tasks associated with a given user.
     *
     * @param user the user whose tasks are to be retrieved
     * @return a list of {@link TaskSummary} representing the user's tasks
     */
    public List<TaskSummary> findAll(User user) {
        return taskRepository.findAllByUser(user)
                .stream()
                .map(TaskSummary::new)
                .toList();
    }

    /**
     * Finds all tasks filtered by flow or event.
     * At least one of {@code flow} or {@code event} must be non-null.
     *
     * @param user the user whose tasks are to be retrieved
     * @param eventId the eventId to filter tasks by (nullable)
     * @return a list of {@link TaskResponse} representing the filtered tasks
     * @throws IllegalStateException if both {@code flow} and {@code event} are null
     */
    public List<TaskSummary> findAllByEventId(User user, Long eventId) throws EventNotFoundException {
        List<Task> tasks;
        Event event = eventRepository.findById(eventId).orElseThrow(() -> new EventNotFoundException("Event not found for id: " + eventId));
        if (event == null) {
            throw new IllegalStateException("Both flow and event cannot be null.");
        } else {
            tasks = taskRepository.findAllByUserAndEvent(user, event);
        }

        return tasks.stream()
                .map(TaskSummary::new)
                .toList();
    }

    /**
     * Updates an existing task with new information, including task details and task items.
     *
     * @param taskId the ID of the task to be updated
     * @param saveTaskRequest the new task details, including task items
     * @return a {@link TaskResponse} representing the updated task
     * @throws TaskNotFoundException if no task with the specified ID is found
     * @throws InvalidStatusNameException if the provided status name is invalid
     * @throws EventNotFoundException if no event with the specified ID is found
     * @throws TagNotFoundException if no tag with the specified ID is found
     */
    @Transactional
    public TaskResponse updateTask(Long taskId, SaveTaskRequest saveTaskRequest) throws TaskNotFoundException, InvalidStatusNameException, EventNotFoundException, TagNotFoundException, InvalidDeadlineException {
        // Validate and get Status, Event, and Tag, deadline
        Status status = validateAndGetStatus(saveTaskRequest.getStatus());

        LocalDate deadline = validateAndGetDeadline(saveTaskRequest.getDeadline(), status);

        Event event = validateAndGetEntityById(
                saveTaskRequest.getEventId(),
                eventRepository,
                () -> new EventNotFoundException("Event not found for ID: " + saveTaskRequest.getEventId())
        );

        Tag tag = validateAndGetEntityById(
                saveTaskRequest.getTagId(),
                tagRepository,
                () -> new TagNotFoundException("Tag not found for ID: " + saveTaskRequest.getTagId())
        );

        // Get existing Task
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new TaskNotFoundException("Task not found for ID: " + taskId));

        // Update task fields
        task.setTitle(saveTaskRequest.getTitle());
        task.setEvent(event);
        task.setTag(tag);
        task.setHashtags(hashtagRepository.findAllById(saveTaskRequest.getHashtagIds()));
        task.setDescription(saveTaskRequest.getDescription());
        task.setStatus(status);
        task.setDeadline(deadline);

        // Fetch existing TaskItems associated with the task
        List<TaskItem> existingItems = taskItemRepository.findByTask(task);
        Map<Long, TaskItem> existingItemsMap = existingItems.stream()
                .collect(Collectors.toMap(TaskItem::getId, item -> item));

        // Update or create TaskItems
        List<TaskItem> newItems = new ArrayList<>();
        if (saveTaskRequest.getItems() != null && !saveTaskRequest.getItems().isEmpty()) {
            List<Long> requestItemIds = saveTaskRequest.getItems().stream()
                    .map(TaskItemDto::getId)
                    .filter(Objects::nonNull)
                    .toList();

            for (TaskItemDto itemDto : saveTaskRequest.getItems()) {
                if (itemDto.getId() != null && existingItemsMap.containsKey(itemDto.getId())) {
                    // Update existing item
                    TaskItem existingItem = existingItemsMap.get(itemDto.getId());
                    existingItem.setTitle(itemDto.getTitle());
                    existingItem.setCompleted(itemDto.isCompleted());
                } else {
                    // Add new item
                    TaskItem newItem = TaskItem.builder()
                            .task(task)
                            .completed(itemDto.isCompleted())
                            .title(itemDto.getTitle())
                            .build();
                    newItems.add(newItem);
                }
            }

            // Delete items that are not in the request
            List<TaskItem> itemsToDelete = existingItems.stream()
                    .filter(item -> !requestItemIds.contains(item.getId()))
                    .toList();
            taskItemRepository.deleteAll(itemsToDelete);

            // Ensure the task's items collection references only the remaining and new items
            existingItems.removeAll(itemsToDelete);
        }

        // Add new items to the existing collection
        task.getItems().clear(); // Clear to avoid stale references
        task.getItems().addAll(existingItems); // Add remaining items
        task.getItems().addAll(newItems); // Add new items

        taskRepository.save(task);

        return new TaskResponse(task);
    }

    private LocalDate validateAndGetDeadline(String deadline, Status status) throws InvalidDeadlineException {
        switch (status) {
            case PROGRESS, TODO -> {
                if (deadline == null || deadline.isEmpty()) {
                    throw new InvalidDeadlineException("Deadline must have value, when status is TODO or PROGRESS");
                }
                return LocalDate.parse(deadline);
            } case DONE -> {
                if (deadline == null || deadline.isEmpty()) {
                    return null;
                } else {
                    return LocalDate.parse(deadline);
                }
            } case BACKLOG -> {
                return null;
            } default -> {
                return null;
            }
        }
    }

    public TaskSummary updateTaskStatus(Long taskId, String updateStatus, String updateDeadline) throws TaskNotFoundException, InvalidStatusNameException, InvalidDeadlineException {
        Status status = validateAndGetStatus(updateStatus);

        LocalDate deadline = validateAndGetDeadline(updateDeadline, status);

        Task task = taskRepository.findById(taskId).orElseThrow(TaskNotFoundException::new);

        task.setStatus(status);
        task.setDeadline(deadline);

        taskRepository.save(task);

        return new TaskSummary(task);
    }

    /**
     * Deletes a task by its ID.
     *
     * @param id the ID of the task to be deleted
     * @throws TaskNotFoundException if no task with the specified ID is found
     */
    public void deleteById(Long id) throws TaskNotFoundException {
        if (!taskRepository.existsById(id)) {
            throw new TaskNotFoundException("Task not found for id: " + id);
        }
        taskRepository.deleteById(id);
    }

    /**
     * Find all tasks by tag ids
     * @param tagIds tag ids for find
     * @return TaskSummary list
     */
    public List<TaskSummary> findAllByTags(List<Long> tagIds) {
        List<Task> tasks = taskRepository.findByTag_IdIn(tagIds);
        return tasks.stream().map(TaskSummary::new).toList();
    }
}
