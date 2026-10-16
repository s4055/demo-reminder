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

// repeatInterval을 생략하면 1, repeatDaysOfWeek를 생략하면 마감일의 요일로 반복한다. 요일은 WEEKLY일 때만 지정할 수 있다.
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

    // 요청의 반복 값으로 반복 규칙을 만든다. 간격을 생략하면 1이다.
    // 간격이 범위를 벗어나거나 WEEKLY가 아닌데 요일을 지정하면 도메인이 DomainException(400)을 던진다.
    static Recurrence toRecurrence(RepeatRule repeatRule, Integer repeatInterval, Set<DayOfWeek> repeatDaysOfWeek) {
        return new Recurrence(repeatRule, Objects.requireNonNullElse(repeatInterval, Recurrence.MIN_INTERVAL),
                repeatDaysOfWeek);
    }
}
