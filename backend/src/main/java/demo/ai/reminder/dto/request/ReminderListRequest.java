package demo.ai.reminder.dto.request;

import demo.ai.reminder.dto.validation.HexColor;
import jakarta.validation.constraints.NotBlank;

public record ReminderListRequest(
        @NotBlank String name,
        @HexColor String color,
        String icon
) {

    public String strippedName() {
        return name.strip();
    }
}
