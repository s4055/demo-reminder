package demo.ai.reminder.dto.request;

import jakarta.validation.constraints.NotNull;

import java.util.List;

public record ReminderOrderRequest(
        @NotNull Long listId,
        @NotNull List<@NotNull Long> ids
) {
}
