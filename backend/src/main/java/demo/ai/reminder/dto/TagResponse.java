package demo.ai.reminder.dto;

import demo.ai.reminder.repository.TagSummary;

import java.time.LocalDateTime;

public record TagResponse(
        Long id,
        String name,
        long reminderCount,
        LocalDateTime createdAt
) {

    public static TagResponse from(TagSummary summary) {
        return new TagResponse(
                summary.tag().getId(),
                summary.tag().getName(),
                summary.reminderCount(),
                summary.tag().getCreatedAt()
        );
    }
}
