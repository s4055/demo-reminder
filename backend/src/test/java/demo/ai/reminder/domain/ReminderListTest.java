package demo.ai.reminder.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ReminderListTest {

    private final User owner = new User("owner@example.com", "password", "테스터");

    @Test
    @DisplayName("생성자로 만들면 이름과 색상이 설정된다")
    void constructor_setsNameAndColor() {
        ReminderList list = new ReminderList(owner, "장보기", "#FF9500");

        assertThat(list.getName()).isEqualTo("장보기");
        assertThat(list.getColor()).isEqualTo("#FF9500");
    }

    @Test
    @DisplayName("색상 없이도 생성할 수 있다")
    void constructor_allowsNullColor() {
        ReminderList list = new ReminderList(owner, "장보기", null);

        assertThat(list.getColor()).isNull();
    }

    @Test
    @DisplayName("update를 호출하면 이름과 색상이 변경된다")
    void update_changesNameAndColor() {
        ReminderList list = new ReminderList(owner, "장보기", "#FF9500");

        list.update("업무", "#007AFF");

        assertThat(list.getName()).isEqualTo("업무");
        assertThat(list.getColor()).isEqualTo("#007AFF");
    }

    @Test
    @DisplayName("순서를 지정하지 않고 생성하면 표시 순서는 0이다")
    void constructor_defaultsSortOrderToZero() {
        ReminderList list = new ReminderList(owner, "장보기", null);

        assertThat(list.getSortOrder()).isZero();
    }

    @Test
    @DisplayName("changeSortOrder를 호출하면 표시 순서가 변경된다")
    void changeSortOrder_changesSortOrder() {
        ReminderList list = new ReminderList(owner, "장보기", null, 3);

        list.changeSortOrder(1);

        assertThat(list.getSortOrder()).isEqualTo(1);
    }
}
