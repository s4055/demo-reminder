package demo.ai.reminder.dto;

import demo.ai.reminder.domain.ListRole;
import demo.ai.reminder.repository.ReminderListSummary;

import java.time.LocalDateTime;

// role은 조회한 사용자의 역할이고, memberCount는 소유자를 포함한 멤버 수다 (2 이상이면 공유된 리스트).
// groupId는 소유자의 사이드바 그룹이며, 공유받은 멤버에게는 항상 null이다.
public record ReminderListResponse(
        Long id,
        String name,
        String color,
        String icon,
        Long groupId,
        int sortOrder,
        long reminderCount,
        ListRole role,
        long memberCount,
        Long version,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static ReminderListResponse from(ReminderListSummary summary) {
        boolean owner = summary.role() == ListRole.OWNER;
        return new ReminderListResponse(
                summary.list().getId(),
                summary.list().getName(),
                summary.list().getColor(),
                summary.list().getIcon(),
                owner && summary.list().getGroup() != null ? summary.list().getGroup().getId() : null,
                summary.list().getSortOrder(),
                summary.reminderCount(),
                summary.role(),
                summary.memberCount(),
                summary.list().getVersion(),
                summary.list().getCreatedAt(),
                summary.list().getUpdatedAt()
        );
    }
}
