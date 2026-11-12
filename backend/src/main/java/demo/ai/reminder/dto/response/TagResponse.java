package demo.ai.reminder.dto.response;

import demo.ai.reminder.repository.tag.TagSummary;

import java.time.Instant;

public record TagResponse(
        Long id,
        String name,
        long reminderCount,
        Instant createdAt
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
