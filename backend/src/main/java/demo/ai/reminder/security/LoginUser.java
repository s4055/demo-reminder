package demo.ai.reminder.security;

import demo.ai.reminder.domain.User;
import lombok.Getter;
import org.springframework.security.core.CredentialsContainer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;

/**
 * 세션에 저장되는 로그인 사용자. 엔티티 대신 id/email/name만 담는다.
 * 인증이 끝나면 비밀번호 해시는 지워지고(eraseCredentials), toString에도 나오지 않는다.
 */
@Getter
public class LoginUser implements UserDetails, CredentialsContainer, Serializable {

    private final Long id;
    private final String email;
    private final String name;
    private String password;

    public LoginUser(User user) {
        this.id = user.getId();
        this.email = user.getEmail();
        this.name = user.getName();
        this.password = user.getPassword();
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of();
    }

    @Override
    public void eraseCredentials() {
        this.password = null;
    }

    @Override
    public String toString() {
        return "LoginUser[id=" + id + ", email=" + email + "]";
    }
}
