package demo.ai.reminder.service;

import demo.ai.reminder.domain.ListRole;
import demo.ai.reminder.domain.ReminderList;
import demo.ai.reminder.domain.User;
import demo.ai.reminder.repository.ReminderListRepository;
import demo.ai.reminder.repository.UserRepository;
import demo.ai.reminder.support.TestAuth;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class ListOwnerBackfillTest {

    @Autowired
    private ListOwnerBackfill listOwnerBackfill;

    @Autowired
    private ReminderListService reminderListService;

    @Autowired
    private ReminderListRepository reminderListRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    @DisplayName("멤버가 없는(공유 기능 이전에 만든) 리스트는 소유자를 OWNER 멤버로 채워 다시 조회할 수 있게 한다")
    void restoreOwnerMembers_restoresOwnerOfListsWithoutMembers() {
        User owner = TestAuth.signIn(userRepository, "owner@example.com");
        ReminderList legacy = reminderListRepository.save(new ReminderList(owner, "예전 리스트", null));
        ReminderList current = reminderListRepository.save(new ReminderList(owner, "새 리스트", null));
        entityManager.flush();
        // 공유 기능 이전 데이터처럼 멤버 행을 지운다.
        entityManager.createNativeQuery("delete from list_member where list_id = :listId")
                .setParameter("listId", legacy.getId())
                .executeUpdate();
        entityManager.clear();
        assertThat(reminderListService.getLists()).extracting(summary -> summary.list().getId())
                .containsExactly(current.getId());

        int restored = listOwnerBackfill.restoreOwnerMembers();
        entityManager.flush();
        entityManager.clear();

        assertThat(restored).isEqualTo(1);
        assertThat(reminderListService.getLists()).extracting(summary -> summary.list().getId())
                .containsExactlyInAnyOrder(legacy.getId(), current.getId());
        assertThat(reminderListRepository.findById(legacy.getId()).orElseThrow().getMembers())
                .singleElement()
                .satisfies(member -> {
                    assertThat(member.getRole()).isEqualTo(ListRole.OWNER);
                    assertThat(member.getUser().getId()).isEqualTo(owner.getId());
                });
        assertThat(listOwnerBackfill.restoreOwnerMembers()).isZero();
    }
}
