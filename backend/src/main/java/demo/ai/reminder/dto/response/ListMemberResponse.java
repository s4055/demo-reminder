package demo.ai.reminder.dto.response;

import demo.ai.reminder.domain.list.ListMember;
import demo.ai.reminder.domain.list.ListRole;

import java.time.Instant;

public record ListMemberResponse(
        Long userId,
        String email,
        String name,
        ListRole role,
        Instant joinedAt
) {

    public static ListMemberResponse from(ListMember member) {
        return new ListMemberResponse(
                member.getUser().getId(),
                member.getUser().getEmail(),
                member.getUser().getName(),
                member.getRole(),
                member.getCreatedAt()
        );
    }
}
