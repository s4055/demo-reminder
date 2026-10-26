package demo.ai.reminder.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.DayOfWeek;

@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"reminder_id", "day_of_week"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ReminderRepeatDay extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reminder_id", nullable = false)
    private Reminder reminder;

    @Enumerated(EnumType.STRING)
    @Column(name = "day_of_week", nullable = false)
    private DayOfWeek dayOfWeek;

    ReminderRepeatDay(Reminder reminder, DayOfWeek dayOfWeek) {
        this.reminder = reminder;
        this.dayOfWeek = dayOfWeek;
    }
}
