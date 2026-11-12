package demo.ai.reminder.service;

import demo.ai.reminder.domain.reminder.Reminder;
import demo.ai.reminder.domain.user.User;
import demo.ai.reminder.repository.reminder.ReminderRepository;
import demo.ai.reminder.repository.user.UserRepository;
import demo.ai.reminder.support.TestAuth;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class CommitTimeVersionConflictTest {

    private static final String EMAIL = "commit-conflict@example.com";

    @Autowired
    private ReminderService reminderService;

    @Autowired
    private ReminderRepository reminderRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private Long reminderId;

    @BeforeEach
    void setUp() {
        User owner = TestAuth.signIn(userRepository, EMAIL);
        reminderId = reminderRepository.save(new Reminder(owner, "우유 사기", null, null, null, null, null, null)).getId();
    }

    @AfterEach
    void cleanUp() {
        reminderRepository.deleteById(reminderId);
        userRepository.findByEmail(EMAIL).ifPresent(userRepository::delete);
    }

    @Test
    @DisplayName("같은 리마인더를 동시에 수정해 커밋 시점에 충돌하면 낙관적 락 예외로 변환되고 먼저 커밋한 변경만 남는다")
    void concurrentToggle_throwsOptimisticLockingFailure_atCommit() {
        TransactionTemplate outer = new TransactionTemplate(transactionManager);
        TransactionTemplate inner = new TransactionTemplate(transactionManager);
        inner.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

        assertThatThrownBy(() -> outer.executeWithoutResult(status -> {
            reminderService.toggleFlag(reminderId);
            inner.executeWithoutResult(innerStatus -> reminderService.toggleFlag(reminderId));
        })).isInstanceOf(ObjectOptimisticLockingFailureException.class);

        Reminder stored = reminderRepository.findById(reminderId).orElseThrow();
        assertThat(stored.getVersion()).isEqualTo(1L);
        assertThat(stored.isFlagged()).isTrue();
    }
}
