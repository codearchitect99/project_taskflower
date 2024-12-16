package codeartist99.taskflower.event.payload;

import codeartist99.taskflower.event.Event;
import codeartist99.taskflower.tag.payload.TagResponse;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@NoArgsConstructor
@Getter
@ToString
public class EventSummary {
    private Long id;
    private String title;
    private TagResponse tag;
    private String startDateTime;
    private String dueDateTime;

    public EventSummary(Event event) {
        this.id = event.getId();
        this.title = event.getTitle();
        this.tag = new TagResponse(event.getTag());
        this.startDateTime = event.getStartDateTime().toString();
        this.dueDateTime = event.getDueDateTime().toString();
    }
}
