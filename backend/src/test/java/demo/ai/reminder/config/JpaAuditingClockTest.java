package demo.ai.reminder.config;

import demo.ai.reminder.domain.reminder.Reminder;
import demo.ai.reminder.domain.user.User;
import demo.ai.reminder.repository.reminder.ReminderRepository;
import demo.ai.reminder.repository.user.UserRepository;
import demo.ai.reminder.support.MutableClock;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class JpaAuditingClockTest {

    private static final Instant NOW = Instant.parse("2026-11-15T03:00:00Z");

    @TestConfiguration
    static class MutableClockConfig {

        @Bean
        @Primary
        MutableClock mutableClock() {
            return new MutableClock(NOW, ZoneOffset.UTC);
        }
    }

    @Autowired
    private ReminderRepository reminderRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private MutableClock clock;

    @BeforeEach
    void resetClock() {
        clock.setInstant(NOW);
    }

    @Test
    @DisplayName("저장하면 createdAt과 updatedAt을 주입한 Clock의 시각으로 기록한다")
    void save_recordsAuditTimesFromClock() {
        User user = userRepository.saveAndFlush(new User("alice@example.com", "{noop}password", "앨리스"));

        assertThat(user.getCreatedAt()).isEqualTo(NOW);
        assertThat(user.getUpdatedAt()).isEqualTo(NOW);
    }

    @Test
    @DisplayName("Clock을 옮긴 뒤 수정하면 updatedAt만 새 시각으로 바뀌고 createdAt은 그대로다")
    void update_changesOnlyUpdatedAt() {
        User user = userRepository.saveAndFlush(new User("alice@example.com", "{noop}password", "앨리스"));
        Reminder reminder = reminderRepository.saveAndFlush(new Reminder(user, "우유 사기", null, null, null, null, null, null));
        Instant later = NOW.plus(Duration.ofHours(2));
        clock.setInstant(later);

        reminder.toggleFlag();
        entityManager.flush();
        entityManager.clear();

        Reminder reloaded = reminderRepository.findById(reminder.getId()).orElseThrow();
        assertThat(reloaded.getCreatedAt()).isEqualTo(NOW);
        assertThat(reloaded.getUpdatedAt()).isEqualTo(later);
    }
}
