package demo.ai.reminder.repository.tag;

import demo.ai.reminder.domain.tag.Tag;

public record TagSummary(Tag tag, long reminderCount) {
}
