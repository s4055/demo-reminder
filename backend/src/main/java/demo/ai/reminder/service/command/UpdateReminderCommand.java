package demo.ai.reminder.service.command;

import demo.ai.reminder.domain.Priority;
import demo.ai.reminder.domain.Recurrence;

import java.time.LocalDateTime;
import java.util.List;

// 리마인더 수정 입력. version은 편집을 시작할 때 받은 리마인더의 버전이다.
// priority나 recurrence가 null이면 각각 '없음', '반복 안 함'이다.
public record UpdateReminderCommand(
        String title,
        String memo,
        LocalDateTime dueAt,
        boolean flagged,
        Priority priority,
        List<String> tagNames,
        Recurrence recurrence,
        Long version
) {
}
