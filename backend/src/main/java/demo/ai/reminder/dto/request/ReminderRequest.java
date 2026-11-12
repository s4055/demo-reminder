package demo.ai.reminder.dto.request;

import demo.ai.reminder.domain.reminder.Priority;
import demo.ai.reminder.domain.reminder.Recurrence;
import demo.ai.reminder.domain.reminder.Reminder;
import demo.ai.reminder.domain.reminder.RepeatRule;
import demo.ai.reminder.domain.tag.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;

public record ReminderRequest(
        @NotBlank @Size(max = Reminder.TITLE_MAX_LENGTH) String title,
        @Size(max = Reminder.MEMO_MAX_LENGTH) String memo,
        Long listId,
        Instant dueAt,
        ZoneId timeZone,
        Priority priority,
        List<@NotBlank @Size(max = Tag.NAME_MAX_LENGTH) String> tagNames,
        Long parentId,
        RepeatRule repeatRule,
        @Min(Recurrence.MIN_INTERVAL) @Max(Recurrence.MAX_INTERVAL) Integer repeatInterval,
        Set<@NotNull DayOfWeek> repeatDaysOfWeek
) {

    public String strippedTitle() {
        return title.strip();
    }

    public List<String> strippedTagNames() {
        return TagNames.strip(tagNames);
    }

    public Recurrence recurrence() {
        return Recurrence.of(repeatRule, repeatInterval, repeatDaysOfWeek);
    }
}
