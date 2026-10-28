package demo.ai.reminder.service;

import demo.ai.reminder.domain.Reminder;
import demo.ai.reminder.domain.User;
import demo.ai.reminder.dto.response.ReminderResponse;
import demo.ai.reminder.repository.ReminderRepository;
import demo.ai.reminder.repository.UserRepository;
import demo.ai.reminder.support.TestAuth;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class FixedClockReminderTest {

    private static final ZoneId ZONE = ZoneId.of("Asia/Seoul");
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 28, 14, 30);

    @TestConfiguration
    static class FixedClockConfig {

        @Bean
        @Primary
        Clock fixedClock() {
            return Clock.fixed(NOW.atZone(ZONE).toInstant(), ZONE);
        }
    }

    @Autowired
    private ReminderService reminderService;

    @Autowired
    private ReminderQueryService reminderQueryService;

    @Autowired
    private ReminderRepository reminderRepository;

    @Autowired
    private UserRepository userRepository;

    private User owner;

    @BeforeEach
    void signIn() {
        owner = TestAuth.signIn(userRepository, "owner@example.com");
    }

    @Test
    @DisplayName("today 뷰는 주입한 Clock의 날짜 기준으로 자정부터 다음 날 자정 전까지만 포함한다")
    void getSmartReminders_today_usesClockMidnightBoundaries() {
        saveDueAt("어제 마지막", LocalDateTime.of(2026, 10, 27, 23, 59, 59));
        saveDueAt("오늘 자정", LocalDateTime.of(2026, 10, 28, 0, 0));
        saveDueAt("오늘 마지막", LocalDateTime.of(2026, 10, 28, 23, 59, 59));
        saveDueAt("내일 자정", LocalDateTime.of(2026, 10, 29, 0, 0));

        assertThat(reminderQueryService.getSmartReminders(SmartView.TODAY))
                .extracting(ReminderResponse::title)
                .containsExactly("오늘 자정", "오늘 마지막");
    }

    @Test
    @DisplayName("완료 토글은 주입한 Clock의 현재 시각을 완료일시로 기록한다")
    void toggleComplete_recordsCompletedAtFromClock() {
        Reminder saved = reminderRepository.save(new Reminder(owner, "우유 사기", null, null, null, null, null));

        ReminderResponse result = reminderService.toggleComplete(saved.getId());

        assertThat(result.completedAt()).isEqualTo(NOW);
    }

    private void saveDueAt(String title, LocalDateTime dueAt) {
        reminderRepository.save(new Reminder(owner, title, null, null, dueAt, null, null));
    }
}
