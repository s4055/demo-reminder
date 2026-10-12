package demo.ai.reminder.dto.request;

// groupId가 null이면 리스트를 그룹에서 뺀다.
public record ListGroupAssignRequest(
        Long groupId
) {
}
