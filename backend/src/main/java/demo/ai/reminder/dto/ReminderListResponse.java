package demo.ai.reminder.dto;

import demo.ai.reminder.domain.ListRole;
import demo.ai.reminder.repository.ReminderListSummary;

import java.time.LocalDateTime;

// role은 조회한 사용자의 역할이고, memberCount는 소유자를 포함한 멤버 수다 (2 이상이면 공유된 리스트).
public record ReminderListResponse(
        Long id,
        String name,
        String color,
        int sortOrder,
        long reminderCount,
        ListRole role,
        long memberCount,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static ReminderListResponse from(ReminderListSummary summary) {
        return new ReminderListResponse(
                summary.list().getId(),
                summary.list().getName(),
                summary.list().getColor(),
                summary.list().getSortOrder(),
                summary.reminderCount(),
                summary.role(),
                summary.memberCount(),
                summary.list().getCreatedAt(),
                summary.list().getUpdatedAt()
        );
    }
}
