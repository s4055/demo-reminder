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
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.OptimisticLock;

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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String name;

    private String color;

    private String icon;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id")
    private ListGroup group;

    @Column(nullable = false)
    private int sortOrder;

    // 멤버 초대/제거는 리스트 편집 내용(이름/색상/아이콘)이 아니므로 리스트 버전을 올리지 않는다.
    @OptimisticLock(excluded = true)
    @OneToMany(mappedBy = "list", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    private List<ListMember> members = new ArrayList<>();

    // 동시 수정 충돌 감지용 버전. 변경이 저장될 때마다 1씩 늘어난다. 컬럼 추가 전에 저장된 행은 기본값 0으로 채워진다.
    @Version
    @ColumnDefault("0")
    @Column(nullable = false)
    private Long version;

    public ReminderList(User user, String name, String color, String icon, int sortOrder) {
        this.user = user;
        this.name = name;
        this.color = color;
        this.icon = validIcon(icon);
        this.sortOrder = sortOrder;
        this.members.add(new ListMember(this, user, ListRole.OWNER));
    }

    public void update(String name, String color, String icon) {
        this.name = name;
        this.color = color;
        this.icon = validIcon(icon);
    }

    public void changeGroup(ListGroup group) {
        if (!isOwner(group.getUser())) {
            throw new DomainException("A list can only join a group of its owner");
        }
        this.group = group;
    }

    public void leaveGroup() {
        this.group = null;
    }

    private boolean isOwner(User other) {
        return isSameUser(other, user);
    }

    private static boolean isSameUser(User a, User b) {
        return a == b || (a.getId() != null && a.getId().equals(b.getId()));
    }

    private static String validIcon(String icon) {
        if (!ListIcon.isAllowed(icon)) {
            throw new DomainException("Unknown list icon: " + icon);
        }
        return icon;
    }

    public void changeSortOrder(int sortOrder) {
        this.sortOrder = sortOrder;
    }

    public List<ListMember> getMembers() {
        return Collections.unmodifiableList(members);
    }

    public ListMember addMember(User user) {
        if (members.stream().anyMatch(member -> isSameUser(member.getUser(), user))) {
            throw new DomainException("Already a member of the list");
        }
        ListMember member = new ListMember(this, user, ListRole.EDITOR);
        members.add(member);
        return member;
    }

    public void removeMember(ListMember member) {
        if (member.isOwner()) {
            throw new DomainException("The owner cannot leave or be removed from the list");
        }
        members.remove(member);
    }
}
