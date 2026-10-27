package demo.ai.reminder.dto.request;

import demo.ai.reminder.dto.validation.HexColor;
import demo.ai.reminder.service.command.CreateListCommand;
import jakarta.validation.constraints.NotBlank;

public record ReminderListRequest(
        @NotBlank String name,
        @HexColor String color,
        String icon
) {

    public CreateListCommand toCommand() {
        return new CreateListCommand(name.strip(), color, icon);
    }
}
