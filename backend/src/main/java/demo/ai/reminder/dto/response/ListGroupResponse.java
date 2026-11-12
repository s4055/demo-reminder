package demo.ai.reminder.dto.response;

import demo.ai.reminder.domain.list.ListGroup;

import java.time.Instant;

public record ListGroupResponse(
        Long id,
        String name,
        int sortOrder,
        Instant createdAt
) {

    public static ListGroupResponse from(ListGroup group) {
        return new ListGroupResponse(group.getId(), group.getName(), group.getSortOrder(), group.getCreatedAt());
    }
}
