package demo.ai.reminder.dto;

import jakarta.validation.constraints.NotNull;

import java.util.List;

// ids 순서가 곧 새 표시 순서다. 모든 리스트의 id를 빠짐없이 한 번씩 담아야 한다.
public record ListOrderRequest(
        @NotNull List<@NotNull Long> ids
) {
}
