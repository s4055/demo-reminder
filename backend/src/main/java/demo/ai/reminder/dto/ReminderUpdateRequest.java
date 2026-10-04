package demo.ai.reminder.dto;

import demo.ai.reminder.domain.Priority;
import demo.ai.reminder.domain.RepeatRule;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.List;

public record ReminderUpdateRequest(
        @NotBlank String title,
        String memo,
        LocalDateTime dueAt,
        boolean flagged,
        Priority priority,
        List<@NotBlank @Size(max = ReminderRequest.TAG_NAME_MAX_LENGTH) String> tagNames,
        RepeatRule repeatRule
) {
}
