package demo.ai.reminder.dto;

import demo.ai.reminder.domain.Priority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.List;

public record ReminderRequest(
        @NotBlank String title,
        String memo,
        Long listId,
        LocalDateTime dueAt,
        Priority priority,
        List<@NotBlank @Size(max = TAG_NAME_MAX_LENGTH) String> tagNames
) {

    public static final int TAG_NAME_MAX_LENGTH = 50;
}
