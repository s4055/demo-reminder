package demo.ai.reminder.dto.request;

import jakarta.validation.constraints.NotNull;

import java.util.List;

public record ListOrderRequest(
        @NotNull List<@NotNull Long> ids
) {
}
