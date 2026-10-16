package demo.ai.reminder.dto.request;

import demo.ai.reminder.domain.Priority;
import demo.ai.reminder.domain.Recurrence;
import demo.ai.reminder.domain.RepeatRule;
import demo.ai.reminder.service.command.UpdateReminderCommand;
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
// version은 편집을 시작할 때 받은 리마인더의 버전이며, 그 사이 다른 사용자가 수정했으면 409(VERSION_CONFLICT)다.
public record ReminderUpdateRequest(
        @NotBlank String title,
        String memo,
        LocalDateTime dueAt,
        boolean flagged,
        Priority priority,
        List<@NotBlank @Size(max = ReminderRequest.TAG_NAME_MAX_LENGTH) String> tagNames,
        RepeatRule repeatRule,
        @Min(Recurrence.MIN_INTERVAL) @Max(Recurrence.MAX_INTERVAL) Integer repeatInterval,
        Set<@NotNull DayOfWeek> repeatDaysOfWeek,
        @NotNull Long version
) {

    public UpdateReminderCommand toCommand() {
        return new UpdateReminderCommand(title, memo, dueAt, flagged, priority, tagNames,
                ReminderRequest.toRecurrence(repeatRule, repeatInterval, repeatDaysOfWeek), version);
    }
}
