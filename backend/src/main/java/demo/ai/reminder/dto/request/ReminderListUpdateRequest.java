package demo.ai.reminder.dto.request;

import demo.ai.reminder.dto.validation.HexColor;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ReminderListUpdateRequest(
        @NotBlank String name,
        @HexColor String color,
        String icon,
        @NotNull Long version
) {

    public String strippedName() {
        return name.strip();
    }
}
