package demo.ai.reminder.dto.response;

import demo.ai.reminder.domain.ListMember;
import demo.ai.reminder.domain.ListRole;

import java.time.LocalDateTime;

public record ListMemberResponse(
        Long userId,
        String email,
        String name,
        ListRole role,
        LocalDateTime joinedAt
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
