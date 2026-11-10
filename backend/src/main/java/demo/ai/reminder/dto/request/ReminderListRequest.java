package demo.ai.reminder.dto.request;

import demo.ai.reminder.domain.list.ReminderList;
import demo.ai.reminder.dto.validation.HexColor;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ReminderListRequest(
        @NotBlank @Size(max = ReminderList.NAME_MAX_LENGTH) String name,
        @HexColor String color,
        String icon
) {

    public String strippedName() {
        return name.strip();
    }
}
