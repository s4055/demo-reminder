package demo.ai.reminder.dto;

import demo.ai.reminder.domain.Priority;
import demo.ai.reminder.domain.Recurrence;
import demo.ai.reminder.domain.RepeatRule;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

// repeatInterval을 생략하면 1, repeatDaysOfWeek를 생략하면 마감일의 요일로 반복한다. 요일은 WEEKLY일 때만 지정할 수 있다.
public record ReminderUpdateRequest(
        @NotBlank String title,
        String memo,
        LocalDateTime dueAt,
        boolean flagged,
        Priority priority,
        List<@NotBlank @Size(max = ReminderRequest.TAG_NAME_MAX_LENGTH) String> tagNames,
        RepeatRule repeatRule,
        @Min(Recurrence.MIN_INTERVAL) @Max(Recurrence.MAX_INTERVAL) Integer repeatInterval,
        Set<@NotNull DayOfWeek> repeatDaysOfWeek
) {

    // 간격/요일 없이 주기만 지정하는 경우
    public ReminderUpdateRequest(String title, String memo, LocalDateTime dueAt, boolean flagged, Priority priority,
                                 List<String> tagNames, RepeatRule repeatRule) {
        this(title, memo, dueAt, flagged, priority, tagNames, repeatRule, null, null);
    }
}
