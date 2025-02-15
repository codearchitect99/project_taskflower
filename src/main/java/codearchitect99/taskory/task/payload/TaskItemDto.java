package codearchitect99.taskory.task.payload;

import codearchitect99.taskory.task.model.TaskItem;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class TaskItemDto {
    private Long id;
    private Long taskId;
    @NotNull private String title;
    @NotNull private boolean completed;

    public TaskItemDto(TaskItem taskItem) {
        this.id = taskItem.getId();
        this.taskId = taskItem.getTask().getId();
        this.title = taskItem.getTitle();
        this.completed = taskItem.isCompleted();
    }
}
