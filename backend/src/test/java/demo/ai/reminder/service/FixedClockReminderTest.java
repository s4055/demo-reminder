package demo.ai.reminder.service;

import demo.ai.reminder.domain.reminder.Reminder;
import demo.ai.reminder.domain.user.User;
import demo.ai.reminder.dto.response.ReminderResponse;
import demo.ai.reminder.exception.BusinessException;
import demo.ai.reminder.repository.reminder.ReminderRepository;
import demo.ai.reminder.repository.user.UserRepository;
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
import java.time.Instant;
import java.time.ZoneOffset;

import static demo.ai.reminder.support.TestTimes.SEOUL;
import static demo.ai.reminder.support.TestTimes.seoul;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class FixedClockReminderTest {

    private static final Instant NOW = seoul(2026, 10, 28, 8, 30);

    @TestConfiguration
    static class FixedClockConfig {

        @Bean
        @Primary
        Clock fixedClock() {
            return Clock.fixed(NOW, ZoneOffset.UTC);
        }
    }

    @Autowired
    private ReminderService reminderService;

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
    @DisplayName("UTC Clock이어도 today 뷰는 요청한 시간대의 자정부터 다음 날 자정 전까지를 오늘로 본다")
    void getSmartReminders_today_usesRequestedZoneMidnightBoundaries() {
        saveTodayBoundaryReminders();

        assertThat(NOW).isEqualTo(Instant.parse("2026-10-27T23:30:00Z"));
        assertThat(reminderService.getSmartReminders(SmartView.TODAY, SEOUL))
                .extracting(ReminderResponse::title)
                .containsExactly("오늘 자정", "오늘 오전", "오늘 마지막");
    }

    @Test
    @DisplayName("같은 순간이어도 시간대가 다르면 today 뷰의 날짜 구간이 달라진다")
    void getSmartReminders_today_dependsOnZone() {
        saveTodayBoundaryReminders();

        assertThat(reminderService.getSmartReminders(SmartView.TODAY, ZoneOffset.UTC))
                .extracting(ReminderResponse::title)
                .containsExactly("어제 마지막", "오늘 자정", "오늘 오전");
    }

    @Test
    @DisplayName("today 뷰를 시간대 없이 조회하면 예외가 발생한다")
    void getSmartReminders_todayWithoutZone_throws() {
        assertThatThrownBy(() -> reminderService.getSmartReminders(SmartView.TODAY, null))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("완료 토글은 주입한 Clock의 현재 시각을 완료일시로 기록한다")
    void toggleComplete_recordsCompletedAtFromClock() {
        Reminder saved = reminderRepository.save(new Reminder(owner, "우유 사기", null, null, null, null, null, null));

        reminderService.toggleComplete(saved.getId());

        assertThat(saved.getCompletedAt()).isEqualTo(NOW);
    }

    private void saveTodayBoundaryReminders() {
        saveDueAt("어제 마지막", seoul(2026, 10, 27, 23, 59, 59));
        saveDueAt("오늘 자정", seoul(2026, 10, 28, 0, 0));
        saveDueAt("오늘 오전", seoul(2026, 10, 28, 8, 59));
        saveDueAt("오늘 마지막", seoul(2026, 10, 28, 23, 59, 59));
        saveDueAt("내일 자정", seoul(2026, 10, 29, 0, 0));
    }

    private void saveDueAt(String title, Instant dueAt) {
        reminderRepository.save(new Reminder(owner, title, null, null, dueAt, SEOUL, null, null));
    }
}
