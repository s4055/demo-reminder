package demo.ai.reminder.ci;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

// CI가 실패를 PR에 표시하는지 확인하기 위한 임시 테스트. 확인 후 되돌린다.
class CiFailureCheckTest {

    @Test
    @DisplayName("CI 실패 표시 확인용으로 일부러 실패한다")
    void failsIntentionally() {
        assertThat(1 + 1).isEqualTo(3);
    }
}
