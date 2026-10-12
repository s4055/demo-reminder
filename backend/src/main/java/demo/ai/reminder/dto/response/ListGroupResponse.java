package demo.ai.reminder.dto.response;

import demo.ai.reminder.domain.ListGroup;

import java.time.LocalDateTime;

public record ListGroupResponse(
        Long id,
        String name,
        int sortOrder,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static ListGroupResponse from(ListGroup group) {
        return new ListGroupResponse(group.getId(), group.getName(), group.getSortOrder(),
                group.getCreatedAt(), group.getUpdatedAt());
    }
}
