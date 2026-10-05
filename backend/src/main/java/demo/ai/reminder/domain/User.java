package demo.ai.reminder.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 리스트/리마인더/태그의 소유자. user는 H2 예약어라 테이블 이름은 users로 둔다.
@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 로그인 아이디. 소문자로 정규화해 저장한다.
    @Column(nullable = false, unique = true)
    private String email;

    // BCrypt 해시. 평문 비밀번호는 저장하지 않는다.
    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String name;

    public User(String email, String password, String name) {
        this.email = email;
        this.password = password;
        this.name = name;
    }
}
