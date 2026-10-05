package demo.ai.reminder.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ReminderList extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 소유자(만든 사람). 접근 권한은 members로 판단하며, 소유자도 OWNER 멤버로 함께 저장된다.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String name;

    private String color;

    // 소유자의 사이드바 표시 순서. 작을수록 위에 표시된다. 공유받은 멤버의 사이드바에서는 쓰지 않는다.
    @Column(nullable = false)
    private int sortOrder;

    // 리스트를 삭제하면 멤버도 함께 삭제된다.
    @OneToMany(mappedBy = "list", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    private List<ListMember> members = new ArrayList<>();

    public ReminderList(User user, String name, String color) {
        this(user, name, color, 0);
    }

    public ReminderList(User user, String name, String color, int sortOrder) {
        this.user = user;
        this.name = name;
        this.color = color;
        this.sortOrder = sortOrder;
        this.members.add(new ListMember(this, user, ListRole.OWNER));
    }

    public void update(String name, String color) {
        this.name = name;
        this.color = color;
    }

    public void changeSortOrder(int sortOrder) {
        this.sortOrder = sortOrder;
    }

    public List<ListMember> getMembers() {
        return Collections.unmodifiableList(members);
    }

    // 이미 멤버인지는 호출하는 쪽이 확인한다 (사용자 id 비교에 저장소 조회가 필요하다).
    public ListMember addMember(User user) {
        ListMember member = new ListMember(this, user, ListRole.EDITOR);
        members.add(member);
        return member;
    }

    // 소유자는 리스트를 떠나거나 제거될 수 없다. 리스트를 없애려면 리스트를 삭제한다.
    public void removeMember(ListMember member) {
        if (member.isOwner()) {
            throw new IllegalStateException("The owner cannot be removed from the list");
        }
        members.remove(member);
    }

    /**
     * 공유 기능 이전에 만든 리스트에는 멤버가 없으므로, 소유자를 OWNER 멤버로 채운다. 이미 멤버가 있으면 아무것도 하지 않는다.
     */
    public void restoreOwnerMember() {
        if (members.isEmpty()) {
            members.add(new ListMember(this, user, ListRole.OWNER));
        }
    }
}
