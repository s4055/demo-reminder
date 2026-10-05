package demo.ai.reminder.dto;

import demo.ai.reminder.domain.ListMember;
import demo.ai.reminder.domain.ListRole;

import java.time.LocalDateTime;

// joinedAt은 멤버가 된 시각이다 (소유자는 리스트를 만든 시각).
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
