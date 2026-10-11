package demo.ai.reminder.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

// icon은 ListIcon.KEYS 중 하나여야 하며(아니면 400), 생략하거나 null이면 기본 아이콘이다.
public record ReminderListRequest(
        @NotBlank String name,
        @Pattern(regexp = "^#[0-9a-fA-F]{6}$", message = "color must be a hex color like #FF9500") String color,
        String icon
) {

    // 아이콘 없이 이름과 색상만 지정하는 경우
    public ReminderListRequest(String name, String color) {
        this(name, color, null);
    }
}
