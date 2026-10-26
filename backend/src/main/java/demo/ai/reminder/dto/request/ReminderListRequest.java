package demo.ai.reminder.dto.request;

import demo.ai.reminder.service.command.CreateListCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ReminderListRequest(
        @NotBlank String name,
        @Pattern(regexp = "^#[0-9a-fA-F]{6}$", message = "color must be a hex color like #FF9500") String color,
        String icon
) {

    public CreateListCommand toCommand() {
        return new CreateListCommand(name, color, icon);
    }
}
