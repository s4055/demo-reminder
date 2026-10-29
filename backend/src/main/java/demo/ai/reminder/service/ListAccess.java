package demo.ai.reminder.service;

import demo.ai.reminder.exception.BusinessException;
import demo.ai.reminder.common.ResultCode;
import demo.ai.reminder.domain.ListMember;
import demo.ai.reminder.domain.ReminderList;
import demo.ai.reminder.repository.ListMemberRepository;
import demo.ai.reminder.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

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
