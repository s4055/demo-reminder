package demo.ai.reminder.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
    @DisplayName("허용된 아이콘으로 만들고 update로 바꾸거나 지울 수 있다")
    void icon_canBeSetChangedAndCleared() {
        ReminderList list = new ReminderList(owner, "장보기", null, "shopping-cart", 0);
        assertThat(list.getIcon()).isEqualTo("shopping-cart");

        list.update("장보기", null, "gift");
        assertThat(list.getIcon()).isEqualTo("gift");

        list.update("장보기", null, null);
        assertThat(list.getIcon()).isNull();
    }

    @Test
    @DisplayName("허용 목록에 없는 아이콘은 지정할 수 없다")
    void icon_throws_whenNotAllowed() {
        assertThatThrownBy(() -> new ReminderList(owner, "장보기", null, "rocket-ship", 0))
                .isInstanceOf(DomainException.class);
        ReminderList list = new ReminderList(owner, "장보기", null);
        assertThatThrownBy(() -> list.update("장보기", null, "unknown"))
                .isInstanceOf(DomainException.class);
    }

    @Test
    @DisplayName("changeGroup으로 소유자의 그룹에 넣고 null로 그룹에서 뺄 수 있다")
    void changeGroup_setsAndClearsOwnersGroup() {
        ReminderList list = new ReminderList(owner, "장보기", null);
        ListGroup group = new ListGroup(owner, "생활", 0);

        list.changeGroup(group);
        assertThat(list.getGroup()).isSameAs(group);

        list.changeGroup(null);
        assertThat(list.getGroup()).isNull();
    }

    @Test
    @DisplayName("다른 사용자의 그룹에는 넣을 수 없다")
    void changeGroup_throws_whenGroupBelongsToAnotherUser() {
        ReminderList list = new ReminderList(owner, "장보기", null);
        User other = new User("other@example.com", "password", "다른 사람");

        assertThatThrownBy(() -> list.changeGroup(new ListGroup(other, "남의 그룹", 0)))
                .isInstanceOf(DomainException.class);
        assertThat(list.getGroup()).isNull();
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

        list.update("업무", "#007AFF", null);

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
        ReminderList list = new ReminderList(owner, "장보기", null, null, 3);

        list.changeSortOrder(1);

        assertThat(list.getSortOrder()).isEqualTo(1);
    }

    @Test
    @DisplayName("리스트를 만들면 만든 사용자가 OWNER 멤버가 된다")
    void constructor_addsOwnerAsOwnerMember() {
        ReminderList list = new ReminderList(owner, "장보기", null);

        assertThat(list.getMembers()).singleElement().satisfies(member -> {
            assertThat(member.getUser()).isSameAs(owner);
            assertThat(member.getRole()).isEqualTo(ListRole.OWNER);
            assertThat(member.isOwner()).isTrue();
            assertThat(member.getList()).isSameAs(list);
        });
    }

    @Test
    @DisplayName("addMember로 추가한 멤버는 EDITOR이고 소유자 뒤에 추가된다")
    void addMember_addsEditorAfterOwner() {
        ReminderList list = new ReminderList(owner, "장보기", null);
        User editor = new User("editor@example.com", "password", "편집자");

        ListMember member = list.addMember(editor);

        assertThat(member.getRole()).isEqualTo(ListRole.EDITOR);
        assertThat(member.isOwner()).isFalse();
        assertThat(list.getMembers()).extracting(ListMember::getUser).containsExactly(owner, editor);
    }

    @Test
    @DisplayName("removeMember로 편집자를 제거할 수 있다")
    void removeMember_removesEditor() {
        ReminderList list = new ReminderList(owner, "장보기", null);
        ListMember member = list.addMember(new User("editor@example.com", "password", "편집자"));

        list.removeMember(member);

        assertThat(list.getMembers()).extracting(ListMember::getUser).containsExactly(owner);
    }

    @Test
    @DisplayName("소유자는 멤버에서 제거할 수 없다")
    void removeMember_rejectsOwner() {
        ReminderList list = new ReminderList(owner, "장보기", null);
        ListMember ownerMember = list.getMembers().getFirst();

        assertThatThrownBy(() -> list.removeMember(ownerMember)).isInstanceOf(DomainException.class);
        assertThat(list.getMembers()).containsExactly(ownerMember);
    }

    @Test
    @DisplayName("멤버 목록은 외부에서 직접 변경할 수 없다")
    void getMembers_isUnmodifiable() {
        ReminderList list = new ReminderList(owner, "장보기", null);

        assertThatThrownBy(() -> list.getMembers().clear()).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("restoreOwnerMember는 멤버가 이미 있으면 아무것도 하지 않는다")
    void restoreOwnerMember_doesNothingWhenMembersExist() {
        ReminderList list = new ReminderList(owner, "장보기", null);

        list.restoreOwnerMember();

        assertThat(list.getMembers()).hasSize(1);
    }
}
