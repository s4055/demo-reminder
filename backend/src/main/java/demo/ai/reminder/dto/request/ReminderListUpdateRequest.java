package demo.ai.reminder.dto.request;

import demo.ai.reminder.service.command.UpdateListCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record ReminderListUpdateRequest(
        @NotBlank String name,
        @Pattern(regexp = "^#[0-9a-fA-F]{6}$", message = "color must be a hex color like #FF9500") String color,
        String icon,
        @NotNull Long version
) {

    public UpdateListCommand toCommand() {
        return new UpdateListCommand(name, color, icon, version);
    }
}
