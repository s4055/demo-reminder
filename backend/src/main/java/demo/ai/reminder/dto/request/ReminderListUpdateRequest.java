package demo.ai.reminder.dto.request;

import demo.ai.reminder.dto.validation.HexColor;
import demo.ai.reminder.service.command.UpdateListCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ReminderListUpdateRequest(
        @NotBlank String name,
        @HexColor String color,
        String icon,
        @NotNull Long version
) {

    public UpdateListCommand toCommand() {
        return new UpdateListCommand(name.strip(), color, icon, version);
    }
}
