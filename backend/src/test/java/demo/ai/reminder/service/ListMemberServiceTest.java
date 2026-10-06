package demo.ai.reminder.service;

import demo.ai.reminder.common.BusinessException;
import demo.ai.reminder.common.ResultCode;
import demo.ai.reminder.domain.ListRole;
import demo.ai.reminder.domain.User;
import demo.ai.reminder.dto.ListMemberResponse;
import demo.ai.reminder.dto.ReminderListRequest;
import demo.ai.reminder.repository.UserRepository;
import demo.ai.reminder.support.TestAuth;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

// 앨리스(소유자)가 리스트를 만들고 밥을 초대/제거하며, 캐럴은 초대받지 않은 사용자다.
@SpringBootTest
@Transactional
class ListMemberServiceTest {

    @Autowired
    private ListMemberService listMemberService;

    @Autowired
    private ReminderListService reminderListService;

    @Autowired
    private UserRepository userRepository;

    private User alice;
    private User bob;
    private User carol;
    private Long listId;

    @BeforeEach
    void setUp() {
        bob = userRepository.save(new User("bob@example.com", "{noop}password", "밥"));
        carol = userRepository.save(new User("carol@example.com", "{noop}password", "캐럴"));
        alice = TestAuth.signIn(userRepository, "alice@example.com");
        listId = reminderListService.createList(new ReminderListRequest("장보기", null)).id();
    }

    @Test
    @DisplayName("새 리스트의 멤버는 소유자 한 명이다")
    void getMembers_ofNewList_containsOnlyOwner() {
        assertThat(listMemberService.getMembers(listId)).singleElement().satisfies(member -> {
            assertThat(member.userId()).isEqualTo(alice.getId());
            assertThat(member.role()).isEqualTo(ListRole.OWNER);
        });
    }

    @Test
    @DisplayName("소유자가 이메일(대소문자 무시)로 초대하면 EDITOR 멤버로 추가되고, 멤버 목록은 소유자 다음에 초대 순으로 나온다")
    void invite_addsEditorMember() {
        ListMemberResponse member = listMemberService.invite(listId, "  BOB@example.com ");
        listMemberService.invite(listId, "carol@example.com");

        assertThat(member.userId()).isEqualTo(bob.getId());
        assertThat(member.role()).isEqualTo(ListRole.EDITOR);
        assertThat(member.joinedAt()).isNotNull();
        assertThat(listMemberService.getMembers(listId))
                .extracting(ListMemberResponse::userId, ListMemberResponse::role)
                .containsExactly(
                        tuple(alice.getId(), ListRole.OWNER),
                        tuple(bob.getId(), ListRole.EDITOR),
                        tuple(carol.getId(), ListRole.EDITOR));
    }

    @Test
    @DisplayName("가입하지 않은 이메일로 초대하면 404다")
    void invite_withUnknownEmail_throwsNotFound() {
        assertResultCode(() -> listMemberService.invite(listId, "nobody@example.com"), ResultCode.NOT_FOUND);
    }

    @Test
    @DisplayName("이미 멤버인 사용자(소유자 본인 포함)를 초대하면 400이다")
    void invite_existingMember_throwsBadRequest() {
        listMemberService.invite(listId, "bob@example.com");

        assertResultCode(() -> listMemberService.invite(listId, "bob@example.com"), ResultCode.BAD_REQUEST);
        assertResultCode(() -> listMemberService.invite(listId, "alice@example.com"), ResultCode.BAD_REQUEST);
    }

    @Test
    @DisplayName("편집자는 다른 사용자를 초대할 수 없다 (403)")
    void invite_byEditor_throwsForbidden() {
        listMemberService.invite(listId, "bob@example.com");
        TestAuth.signIn(bob);

        assertResultCode(() -> listMemberService.invite(listId, "carol@example.com"), ResultCode.FORBIDDEN);
    }

    @Test
    @DisplayName("멤버가 아닌 사용자에게는 리스트가 없는 것처럼 404로 응답한다")
    void nonMember_cannotSeeOrManageMembers() {
        TestAuth.signIn(carol);

        assertResultCode(() -> listMemberService.getMembers(listId), ResultCode.NOT_FOUND);
        assertResultCode(() -> listMemberService.invite(listId, "bob@example.com"), ResultCode.NOT_FOUND);
        assertResultCode(() -> listMemberService.removeMember(listId, alice.getId()), ResultCode.NOT_FOUND);
    }

    @Test
    @DisplayName("편집자도 멤버 목록을 볼 수 있다")
    void getMembers_byEditor_returnsMembers() {
        listMemberService.invite(listId, "bob@example.com");
        TestAuth.signIn(bob);

        assertThat(listMemberService.getMembers(listId)).hasSize(2);
    }

    @Test
    @DisplayName("소유자가 멤버를 제거하면 멤버 목록과 그 사용자의 리스트 목록에서 빠진다")
    void removeMember_byOwner_removesMember() {
        listMemberService.invite(listId, "bob@example.com");

        listMemberService.removeMember(listId, bob.getId());

        assertThat(listMemberService.getMembers(listId)).extracting(ListMemberResponse::userId)
                .containsExactly(alice.getId());
        TestAuth.signIn(bob);
        assertThat(reminderListService.getLists()).isEmpty();
    }

    @Test
    @DisplayName("편집자는 자기 자신을 제거해 리스트에서 나갈 수 있다")
    void removeMember_self_leavesList() {
        listMemberService.invite(listId, "bob@example.com");
        TestAuth.signIn(bob);

        listMemberService.removeMember(listId, bob.getId());

        assertThat(reminderListService.getLists()).isEmpty();
        assertResultCode(() -> listMemberService.getMembers(listId), ResultCode.NOT_FOUND);
    }

    @Test
    @DisplayName("편집자는 다른 멤버를 제거할 수 없다 (403)")
    void removeMember_otherByEditor_throwsForbidden() {
        listMemberService.invite(listId, "bob@example.com");
        listMemberService.invite(listId, "carol@example.com");
        TestAuth.signIn(bob);

        assertResultCode(() -> listMemberService.removeMember(listId, carol.getId()), ResultCode.FORBIDDEN);
        assertResultCode(() -> listMemberService.removeMember(listId, alice.getId()), ResultCode.FORBIDDEN);
    }

    @Test
    @DisplayName("소유자는 리스트에서 나갈 수 없다 (400)")
    void removeMember_ownerSelf_throwsBadRequest() {
        assertResultCode(() -> listMemberService.removeMember(listId, alice.getId()), ResultCode.BAD_REQUEST);
        assertThat(listMemberService.getMembers(listId)).hasSize(1);
    }

    @Test
    @DisplayName("멤버가 아닌 사용자를 제거하면 404다")
    void removeMember_nonMember_throwsNotFound() {
        assertResultCode(() -> listMemberService.removeMember(listId, carol.getId()), ResultCode.NOT_FOUND);
    }

    private static void assertResultCode(Executable executable, ResultCode resultCode) {
        assertThatThrownBy(executable::execute)
                .isInstanceOf(BusinessException.class)
                .extracting("resultCode")
                .isEqualTo(resultCode);
    }
}
