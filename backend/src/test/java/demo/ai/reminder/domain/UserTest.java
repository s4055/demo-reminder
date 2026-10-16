package demo.ai.reminder.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UserTest {

    @Test
    @DisplayName("이메일은 앞뒤 공백을 지우고 소문자로 맞춘다")
    void normalizeEmail_trimsAndLowercases() {
        assertThat(User.normalizeEmail("  Owner@Example.COM ")).isEqualTo("owner@example.com");
    }

    @Test
    @DisplayName("이메일이 null이면 null을 돌려준다")
    void normalizeEmail_returnsNullForNull() {
        assertThat(User.normalizeEmail(null)).isNull();
    }
}
