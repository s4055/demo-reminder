package demo.ai.reminder.service.command;

import demo.ai.reminder.domain.Priority;
import demo.ai.reminder.domain.Recurrence;

import java.time.LocalDateTime;
import java.util.List;

// 리마인더 생성 입력. parentId를 지정하면 그 리마인더의 하위 작업이 된다.
// priority나 recurrence가 null이면 각각 '없음', '반복 안 함'이다.
public record CreateReminderCommand(
        String title,
        String memo,
        Long listId,
        LocalDateTime dueAt,
        Priority priority,
        List<String> tagNames,
        Long parentId,
        Recurrence recurrence
) {
}
