package codeartitect.taskflower.task.taskitem;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@EqualsAndHashCode
@ToString
public class TaskItemResponse {
    private Long id;
    private String title;
    private boolean completed;

    public TaskItemResponse(TaskItem taskItem) {
        this.id = taskItem.getId();
        this.title = taskItem.getTitle();
        this.completed = taskItem.isCompleted();
    }
}
