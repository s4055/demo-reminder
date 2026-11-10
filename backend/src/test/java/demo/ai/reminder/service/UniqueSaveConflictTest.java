package demo.ai.reminder.service;

import demo.ai.reminder.common.ResultCode;
import demo.ai.reminder.domain.list.ReminderList;
import demo.ai.reminder.domain.tag.Tag;
import demo.ai.reminder.domain.user.User;
import demo.ai.reminder.dto.request.ReminderListRequest;
import demo.ai.reminder.dto.request.SignupRequest;
import demo.ai.reminder.exception.BusinessException;
import demo.ai.reminder.repository.tag.TagRepository;
import demo.ai.reminder.repository.user.UserRepository;
import demo.ai.reminder.support.TestAuth;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;

@SpringBootTest
@Transactional
class UniqueSaveConflictTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private TagService tagService;

    @Autowired
    private ReminderListService reminderListService;

    @Autowired
    private ListMemberService listMemberService;

    @MockitoSpyBean
    private UserRepository userRepository;

    @MockitoSpyBean
    private TagRepository tagRepository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("중복 확인 뒤 다른 요청이 같은 이메일로 먼저 가입했다면 500 대신 CONFLICT로 거부한다")
    void signup_throwsConflict_whenSameEmailIsSavedConcurrently() {
        userRepository.save(new User("alice@example.com", "{noop}password", "앨리스"));
        doReturn(false).when(userRepository).existsByEmail("alice@example.com");

        assertConflict(() -> authService.signup(new SignupRequest("alice@example.com", "password1", "앨리스")));
    }

    @Test
    @DisplayName("조회 뒤 다른 요청이 같은 이름의 태그를 먼저 만들었다면 500 대신 CONFLICT로 거부한다")
    void resolveTags_throwsConflict_whenSameTagIsSavedConcurrently() {
        User alice = TestAuth.signIn(userRepository, "alice@example.com");
        tagRepository.save(new Tag(alice, "집"));
        doReturn(List.of()).when(tagRepository).findByUserIdAndNameIn(eq(alice.getId()), any());

        assertConflict(() -> tagService.resolveTags(List.of("집")));
    }

    @Test
    @DisplayName("멤버 확인 뒤 다른 요청이 같은 사용자를 먼저 초대했다면 500 대신 CONFLICT로 거부한다")
    void invite_throwsConflict_whenSameMemberIsSavedConcurrently() {
        User bob = userRepository.save(new User("bob@example.com", "{noop}password", "밥"));
        TestAuth.signIn(userRepository, "alice@example.com");
        Long listId = reminderListService.createList(new ReminderListRequest("장보기", null, null)).id();
        assertThat(entityManager.find(ReminderList.class, listId).getMembers()).hasSize(1);
        entityManager.flush();
        jdbcTemplate.update(
                "insert into list_member (list_id, user_id, role, created_at, updated_at) values (?, ?, 'EDITOR', now(), now())",
                listId, bob.getId());

        assertConflict(() -> listMemberService.invite(listId, "bob@example.com"));
    }

    private static void assertConflict(Executable executable) {
        assertThatThrownBy(executable::execute)
                .isInstanceOf(BusinessException.class)
                .extracting("resultCode")
                .isEqualTo(ResultCode.CONFLICT);
    }
}
