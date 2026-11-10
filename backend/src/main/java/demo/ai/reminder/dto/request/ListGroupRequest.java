package demo.ai.reminder.dto.request;

import demo.ai.reminder.domain.list.ListGroup;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ListGroupRequest(
        @NotBlank @Size(max = ListGroup.NAME_MAX_LENGTH) String name
) {

    public String strippedName() {
        return name.strip();
    }
}
