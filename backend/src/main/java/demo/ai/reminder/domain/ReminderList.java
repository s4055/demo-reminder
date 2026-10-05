package demo.ai.reminder.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ReminderList extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 소유자. 다른 사용자의 리스트는 조회/수정할 수 없다.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String name;

    private String color;

    // 사이드바 표시 순서. 작을수록 위에 표시된다.
    @Column(nullable = false)
    private int sortOrder;

    public ReminderList(User user, String name, String color) {
        this(user, name, color, 0);
    }

    public ReminderList(User user, String name, String color, int sortOrder) {
        this.user = user;
        this.name = name;
        this.color = color;
        this.sortOrder = sortOrder;
    }

    public void update(String name, String color) {
        this.name = name;
        this.color = color;
    }

    public void changeSortOrder(int sortOrder) {
        this.sortOrder = sortOrder;
    }
}
