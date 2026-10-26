package demo.ai.reminder.dto.request;

import jakarta.validation.constraints.NotNull;

public record ReminderMoveRequest(
        @NotNull Long listId
) {
}
