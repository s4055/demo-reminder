package demo.ai.demoreminder;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDateTime;

@Configuration
public class DataInitializer {

    @Bean
    CommandLineRunner seedReminders(ReminderRepository reminderRepository) {
        return args -> {
            reminderRepository.save(new Reminder("생일 축하 메시지 보내기", "친구 생일", LocalDateTime.now().plusDays(1)));
            reminderRepository.save(new Reminder("회의 준비", "분기 보고서 정리", LocalDateTime.now().plusHours(3)));
        };
    }
}
