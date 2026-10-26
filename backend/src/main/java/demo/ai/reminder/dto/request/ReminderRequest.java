package demo.ai.reminder.dto.request;

import demo.ai.reminder.domain.Priority;
import demo.ai.reminder.domain.Recurrence;
import demo.ai.reminder.domain.RepeatRule;
import demo.ai.reminder.service.command.CreateReminderCommand;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public record ReminderRequest(
        @NotBlank String title,
        String memo,
        Long listId,
        LocalDateTime dueAt,
        Priority priority,
        List<@NotBlank @Size(max = TAG_NAME_MAX_LENGTH) String> tagNames,
        Long parentId,
        RepeatRule repeatRule,
        @Min(Recurrence.MIN_INTERVAL) @Max(Recurrence.MAX_INTERVAL) Integer repeatInterval,
        Set<@NotNull DayOfWeek> repeatDaysOfWeek
) {

    public static final int TAG_NAME_MAX_LENGTH = 50;

    public CreateReminderCommand toCommand() {
        return new CreateReminderCommand(title, memo, listId, dueAt, priority, tagNames, parentId,
                toRecurrence(repeatRule, repeatInterval, repeatDaysOfWeek));
    }

    static Recurrence toRecurrence(RepeatRule repeatRule, Integer repeatInterval, Set<DayOfWeek> repeatDaysOfWeek) {
        return new Recurrence(repeatRule, Objects.requireNonNullElse(repeatInterval, Recurrence.MIN_INTERVAL),
                repeatDaysOfWeek);
    }
}
