package demo.ai.reminder.dto;

import jakarta.validation.constraints.NotNull;

// listId는 옮겨 갈 리스트다. 현재 사용자가 멤버인 리스트여야 한다.
public record ReminderMoveRequest(
        @NotNull Long listId
) {
}
