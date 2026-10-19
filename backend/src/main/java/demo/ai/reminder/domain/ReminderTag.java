package demo.ai.reminder.domain;

import jakarta.persistence.Entity;
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

// 리마인더에 붙은 태그. 한 리마인더에 같은 태그는 한 번만 붙으며, Reminder를 통해서만 붙이고 뗀다.
@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"reminder_id", "tag_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ReminderTag extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reminder_id", nullable = false)
    private Reminder reminder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tag_id", nullable = false)
    private Tag tag;

    ReminderTag(Reminder reminder, Tag tag) {
        this.reminder = reminder;
        this.tag = tag;
    }

    // 같은 객체이거나 저장된 같은 태그(id 일치)인지
    boolean isFor(Tag other) {
        return tag == other || (tag.getId() != null && tag.getId().equals(other.getId()));
    }
}
