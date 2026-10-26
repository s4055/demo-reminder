package demo.ai.reminder.service;

import demo.ai.reminder.exception.BusinessException;
import demo.ai.reminder.common.ResultCode;
import demo.ai.reminder.domain.ListMember;
import demo.ai.reminder.domain.ReminderList;
import demo.ai.reminder.repository.ListMemberRepository;
import demo.ai.reminder.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 현재 사용자의 리스트 접근 권한을 확인한다.
 * 멤버가 아니면 리스트가 있는지 드러내지 않도록 404, 멤버지만 소유자 권한이 필요한 작업이면 403으로 응답한다.
 */
@Component
@RequiredArgsConstructor
class ListAccess {

    private final ListMemberRepository listMemberRepository;
    private final CurrentUser currentUser;

    ListMember membership(Long listId) {
        return listMemberRepository.findByListIdAndUserId(listId, currentUser.id())
                .orElseThrow(() -> new BusinessException(ResultCode.NOT_FOUND, "List not found: " + listId));
    }

    ReminderList memberList(Long listId) {
        return membership(listId).getList();
    }

    ReminderList ownedList(Long listId) {
        ListMember membership = membership(listId);
        if (!membership.isOwner()) {
            throw new BusinessException(ResultCode.FORBIDDEN, "Only the list owner can do this: " + listId);
        }
        return membership.getList();
    }
}
