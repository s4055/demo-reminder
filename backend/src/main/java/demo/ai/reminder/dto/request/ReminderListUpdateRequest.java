package demo.ai.reminder.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

// icon은 ListIcon.KEYS 중 하나여야 하며(아니면 400), 생략하거나 null이면 기본 아이콘이다.
// version은 편집을 시작할 때 받은 리스트의 버전이며, 그 사이 다른 사용자가 수정했으면 409(VERSION_CONFLICT)다.
public record ReminderListUpdateRequest(
        @NotBlank String name,
        @Pattern(regexp = "^#[0-9a-fA-F]{6}$", message = "color must be a hex color like #FF9500") String color,
        String icon,
        @NotNull Long version
) {

    // 아이콘 없이 이름과 색상만 지정하는 경우
    public ReminderListUpdateRequest(String name, String color, Long version) {
        this(name, color, null, version);
    }
}
