package demo.ai.reminder.dto.request;

import java.util.List;

final class TagNames {

    private TagNames() {
    }

    static List<String> strip(List<String> tagNames) {
        return tagNames == null ? List.of() : tagNames.stream().map(String::strip).toList();
    }
}
