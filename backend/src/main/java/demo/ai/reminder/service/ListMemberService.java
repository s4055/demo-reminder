package demo.ai.reminder.service;

import demo.ai.reminder.common.BusinessException;
import demo.ai.reminder.common.ResultCode;
import demo.ai.reminder.domain.ListMember;
import demo.ai.reminder.domain.ReminderList;
import demo.ai.reminder.domain.User;
import demo.ai.reminder.repository.ListMemberRepository;
import demo.ai.reminder.repository.UserRepository;
import demo.ai.reminder.security.CurrentUser;
import demo.ai.reminder.security.LoginUserDetailsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

// 리스트 공유. 멤버 목록은 멤버 누구나 볼 수 있고, 초대와 다른 멤버 제거는 소유자만 할 수 있다.
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ListMemberService {

    private final ListAccess listAccess;
    private final ListMemberRepository listMemberRepository;
    private final UserRepository userRepository;
    private final CurrentUser currentUser;

    // 소유자가 먼저, 나머지는 초대한 순서다.
    public List<ListMember> getMembers(Long listId) {
        return listAccess.memberList(listId).getMembers();
    }

    // 가입한 사용자만 초대할 수 있다. 이메일은 가입 때와 같이 소문자로 맞춰 찾는다.
    @Transactional
    public ListMember invite(Long listId, String email) {
        ReminderList list = listAccess.ownedList(listId);
        String normalizedEmail = LoginUserDetailsService.normalizeEmail(email);
        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new BusinessException(ResultCode.NOT_FOUND, "User not found: " + normalizedEmail));
        if (listMemberRepository.existsByListIdAndUserId(listId, user.getId())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "Already a member of the list: " + normalizedEmail);
        }
        return listMemberRepository.save(list.addMember(user));
    }

    /**
     * userId가 본인이면 리스트에서 나가고, 다른 사용자이면 그 멤버를 제거한다(소유자만).
     * 소유자는 나가거나 제거될 수 없다. 멤버가 만든 리마인더는 리스트에 그대로 남는다.
     */
    @Transactional
    public void removeMember(Long listId, Long userId) {
        ListMember me = listAccess.membership(listId);
        boolean leaving = currentUser.id().equals(userId);
        if (!leaving && !me.isOwner()) {
            throw new BusinessException(ResultCode.FORBIDDEN, "Only the list owner can remove members: " + listId);
        }
        ListMember target = leaving ? me : listMemberRepository.findByListIdAndUserId(listId, userId)
                .orElseThrow(() -> new BusinessException(ResultCode.NOT_FOUND, "Member not found: " + userId));
        if (target.isOwner()) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "The owner cannot leave or be removed: " + listId);
        }
        me.getList().removeMember(target);
    }
}
