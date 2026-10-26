package demo.ai.reminder.service;

import demo.ai.reminder.exception.BusinessException;
import demo.ai.reminder.common.ResultCode;
import demo.ai.reminder.domain.ListMember;
import demo.ai.reminder.domain.ReminderList;
import demo.ai.reminder.domain.User;
import demo.ai.reminder.dto.response.ListMemberResponse;
import demo.ai.reminder.repository.ListMemberRepository;
import demo.ai.reminder.repository.UserRepository;
import demo.ai.reminder.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ListMemberService {

    private final ListAccess listAccess;
    private final ListMemberRepository listMemberRepository;
    private final UserRepository userRepository;
    private final CurrentUser currentUser;

    public List<ListMemberResponse> getMembers(Long listId) {
        return listAccess.memberList(listId).getMembers().stream()
                .map(ListMemberResponse::from)
                .toList();
    }

    @Transactional
    public ListMemberResponse invite(Long listId, String email) {
        ReminderList list = listAccess.ownedList(listId);
        String normalizedEmail = User.normalizeEmail(email);
        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new BusinessException(ResultCode.NOT_FOUND, "User not found: " + normalizedEmail));
        return ListMemberResponse.from(listMemberRepository.save(list.addMember(user)));
    }

    @Transactional
    public void removeMember(Long listId, Long userId) {
        ListMember me = listAccess.membership(listId);
        boolean leaving = currentUser.id().equals(userId);
        if (!leaving && !me.isOwner()) {
            throw new BusinessException(ResultCode.FORBIDDEN, "Only the list owner can remove members: " + listId);
        }
        ListMember target = leaving ? me : listMemberRepository.findByListIdAndUserId(listId, userId)
                .orElseThrow(() -> new BusinessException(ResultCode.NOT_FOUND, "Member not found: " + userId));
        me.getList().removeMember(target);
    }
}
